/*
 * Audio del multijugador: copia el sonido que el núcleo FBNeo entrega a
 * LibretroDroid para enviarlo también a los invitados.
 *
 * LibretroDroid le da al núcleo su función de audio con retro_set_audio_sample_batch.
 * Aquí se pone otra delante («gancho»): guarda una copia de cada bloque de muestras
 * en un búfer circular y después llama a la de LibretroDroid, así que el sonido del
 * anfitrión no cambia. Kotlin lee el búfer, lo comprime en AAC y lo envía.
 */
#include <jni.h>
#include <dlfcn.h>
#include <stdatomic.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

typedef size_t (*fn_lote_audio)(const int16_t* datos, size_t marcos);
typedef void (*fn_poner_lote)(fn_lote_audio cb);

struct retro_game_geometry { unsigned base_width, base_height, max_width, max_height; float aspect_ratio; };
struct retro_system_timing { double fps; double sample_rate; };
struct retro_system_av_info { struct retro_game_geometry geometry; struct retro_system_timing timing; };
typedef void (*fn_info_av)(struct retro_system_av_info* info);

/* Búfer circular de un productor (hilo del emulador) y un consumidor (hilo de Kotlin) */
#define CAPACIDAD (48000 * 2)   /* marcos estéreo: unos 2 s */
static int16_t anillo[CAPACIDAD * 2];
static atomic_size_t escrito = 0;
static atomic_size_t leido = 0;
static atomic_int activo = 0;

static fn_lote_audio original = NULL;
static fn_poner_lote poner_lote = NULL;
/* Nombre del núcleo: antes de tocarlo se comprueba que sigue cargado. Al cerrar la
 * partida LibretroDroid lo descarga (dlclose) y llamar a poner_lote cerraba la app. */
static char nombre_nucleo[256] = "";

/* Vuelve a buscar retro_set_audio_sample_batch solo si el núcleo sigue en memoria */
static fn_poner_lote poner_lote_vivo(void) {
    if (!nombre_nucleo[0]) return NULL;
    void* hn = dlopen(nombre_nucleo, RTLD_NOW | RTLD_NOLOAD);
    if (!hn) return NULL;
    fn_poner_lote f = (fn_poner_lote)dlsym(hn, "retro_set_audio_sample_batch");
    dlclose(hn);
    return f;
}

static size_t gancho(const int16_t* datos, size_t marcos) {
    if (atomic_load_explicit(&activo, memory_order_acquire) && datos) {
        size_t w = atomic_load_explicit(&escrito, memory_order_relaxed);
        size_t r = atomic_load_explicit(&leido, memory_order_acquire);
        /* Si el lector se queda atrás, se descarta lo nuevo (no se pisa lo que está leyendo) */
        if (w - r + marcos <= CAPACIDAD) {
            for (size_t i = 0; i < marcos; i++) {
                size_t p = ((w + i) % CAPACIDAD) * 2;
                anillo[p] = datos[i * 2];
                anillo[p + 1] = datos[i * 2 + 1];
            }
            atomic_store_explicit(&escrito, w + marcos, memory_order_release);
        }
    }
    return original ? original(datos, marcos) : marcos;
}

/* Devuelve la frecuencia de muestreo del juego, o 0 si no se puede capturar el audio */
JNIEXPORT jint JNICALL
Java_com_diasdefuria_red_AudioRed_nativoIniciar(JNIEnv* env, jclass c, jstring nucleo) {
    (void)c;
    const char* n = (*env)->GetStringUTFChars(env, nucleo, NULL);
    void* hn = dlopen(n, RTLD_NOW | RTLD_NOLOAD);
    strncpy(nombre_nucleo, n, sizeof(nombre_nucleo) - 1);
    nombre_nucleo[sizeof(nombre_nucleo) - 1] = 0;
    (*env)->ReleaseStringUTFChars(env, nucleo, n);
    void* hl = dlopen("liblibretrodroid.so", RTLD_NOW | RTLD_NOLOAD);
    if (!hn || !hl) return 0;
    poner_lote = (fn_poner_lote)dlsym(hn, "retro_set_audio_sample_batch");
    fn_info_av info = (fn_info_av)dlsym(hn, "retro_get_system_av_info");
    if (!original)
        original = (fn_lote_audio)dlsym(hl, "_ZN13libretrodroid13LibretroDroid31callback_set_audio_sample_batchEPKsm");
    if (!original) /* 32 bits: size_t es unsigned int */
        original = (fn_lote_audio)dlsym(hl, "_ZN13libretrodroid13LibretroDroid31callback_set_audio_sample_batchEPKsj");
    dlclose(hn);
    dlclose(hl);
    if (!poner_lote || !original || !info) return 0;
    struct retro_system_av_info av;
    memset(&av, 0, sizeof(av));
    info(&av);
    int frecuencia = (int)(av.timing.sample_rate + 0.5);
    if (frecuencia < 8000 || frecuencia > 96000) frecuencia = 48000;
    atomic_store(&leido, atomic_load(&escrito));
    atomic_store(&activo, 1);
    poner_lote(gancho);
    return frecuencia;
}

/* Copia en [destino] las muestras disponibles (estéreo intercalado). Devuelve los marcos copiados. */
JNIEXPORT jint JNICALL
Java_com_diasdefuria_red_AudioRed_nativoLeer(JNIEnv* env, jclass c, jshortArray destino) {
    (void)c;
    size_t r = atomic_load_explicit(&leido, memory_order_relaxed);
    size_t w = atomic_load_explicit(&escrito, memory_order_acquire);
    size_t disponibles = w - r;
    size_t max = (size_t)(*env)->GetArrayLength(env, destino) / 2;
    size_t n = disponibles < max ? disponibles : max;
    if (n == 0) return 0;
    jshort* d = (*env)->GetShortArrayElements(env, destino, NULL);
    for (size_t i = 0; i < n; i++) {
        size_t p = ((r + i) % CAPACIDAD) * 2;
        d[i * 2] = anillo[p];
        d[i * 2 + 1] = anillo[p + 1];
    }
    (*env)->ReleaseShortArrayElements(env, destino, d, 0);
    atomic_store_explicit(&leido, r + n, memory_order_release);
    return (jint)n;
}

/* Devuelve el audio del núcleo a LibretroDroid directamente */
JNIEXPORT void JNICALL
Java_com_diasdefuria_red_AudioRed_nativoDetener(JNIEnv* env, jclass c) {
    (void)env; (void)c;
    atomic_store(&activo, 0);
    fn_poner_lote f = poner_lote_vivo();
    if (f && original) f(original);
    poner_lote = NULL;
}

/* Vuelve a poner el gancho (por si LibretroDroid ha reinstalado su función de audio,
 * por ejemplo al volver de la pausa o de segundo plano). */
JNIEXPORT void JNICALL
Java_com_diasdefuria_red_AudioRed_nativoReenganchar(JNIEnv* env, jclass c) {
    (void)env; (void)c;
    if (!atomic_load(&activo)) return;
    fn_poner_lote f = poner_lote_vivo();
    if (f) f(gancho);
}
