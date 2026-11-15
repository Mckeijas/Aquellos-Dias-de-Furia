/*
 * Puente entre la app y rcheevos (RetroAchievements).
 *
 * - La memoria del juego se lee directamente del núcleo FBNeo que ya ha cargado
 *   LibretroDroid (mismo proceso): retro_get_memory_data(RETRO_MEMORY_SYSTEM_RAM).
 * - Las peticiones al servidor las hace Kotlin (HTTPS) y devuelve la respuesta aquí.
 * - Los avisos (sesión iniciada, juego cargado, logro conseguido…) se envían al
 *   objeto Kotlin que creó el cliente.
 */
#include <jni.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>

#include "rc_client.h"

#define ETIQUETA "Logros"
#define RETRO_MEMORY_SYSTEM_RAM 2

static JavaVM* jvm = NULL;

typedef void* (*fn_memoria)(unsigned id);
typedef size_t (*fn_tamano)(unsigned id);
static fn_memoria memoria_nucleo = NULL;
static fn_tamano tamano_nucleo = NULL;

/* Datos asociados a cada cliente: el objeto Kotlin que recibe los avisos */
typedef struct {
    jobject oyente;
    jmethodID peticion;
    jmethodID sesion;
    jmethodID juego;
    jmethodID logro;
    jmethodID aviso;
} Oyente;

/* Petición pendiente de respuesta del servidor */
typedef struct {
    rc_client_server_callback_t callback;
    void* datos;
} Pendiente;

jint JNI_OnLoad(JavaVM* vm, void* reservado) {
    (void)reservado;
    jvm = vm;
    return JNI_VERSION_1_6;
}

static JNIEnv* entorno(void) {
    JNIEnv* env = NULL;
    if ((*jvm)->GetEnv(jvm, (void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        (*jvm)->AttachCurrentThread(jvm, &env, NULL);
    }
    return env;
}

static jstring cadena(JNIEnv* env, const char* s) {
    return (*env)->NewStringUTF(env, s ? s : "");
}

static Oyente* oyente_de(const rc_client_t* client) {
    return (Oyente*)rc_client_get_userdata(client);
}

/* ── Memoria del juego ─────────────────────────────────────────────────── */

static uint32_t leer_memoria(uint32_t direccion, uint8_t* buffer, uint32_t n, rc_client_t* client) {
    (void)client;
    if (!memoria_nucleo || !tamano_nucleo) return 0;
    uint8_t* ram = (uint8_t*)memoria_nucleo(RETRO_MEMORY_SYSTEM_RAM);
    size_t tam = tamano_nucleo(RETRO_MEMORY_SYSTEM_RAM);
    if (!ram || direccion >= tam) return 0;
    if ((size_t)direccion + n > tam) n = (uint32_t)(tam - direccion);
    memcpy(buffer, ram + direccion, n);
    return n;
}

/* ── Servidor: Kotlin hace la petición HTTPS ───────────────────────────── */

static void llamar_servidor(const rc_api_request_t* req, rc_client_server_callback_t callback,
                            void* datos, rc_client_t* client) {
    Oyente* o = oyente_de(client);
    JNIEnv* env = entorno();
    Pendiente* p = (Pendiente*)malloc(sizeof(Pendiente));
    if (!o || !env || !p) {
        rc_api_server_response_t r = { "", 0, RC_API_SERVER_RESPONSE_CLIENT_ERROR };
        free(p);
        callback(&r, datos);
        return;
    }
    p->callback = callback;
    p->datos = datos;
    jstring url = cadena(env, req->url);
    jstring post = req->post_data ? cadena(env, req->post_data) : NULL;
    jstring tipo = req->content_type ? cadena(env, req->content_type) : NULL;
    (*env)->CallVoidMethod(env, o->oyente, o->peticion, url, post, tipo, (jlong)(intptr_t)p);
    (*env)->DeleteLocalRef(env, url);
    if (post) (*env)->DeleteLocalRef(env, post);
    if (tipo) (*env)->DeleteLocalRef(env, tipo);
}

JNIEXPORT void JNICALL
Java_com_diasdefuria_logros_ClienteLogros_nativoRespuesta(JNIEnv* env, jclass c, jlong pendiente,
                                                                 jbyteArray cuerpo, jint estado) {
    (void)c;
    Pendiente* p = (Pendiente*)(intptr_t)pendiente;
    if (!p) return;
    jsize n = cuerpo ? (*env)->GetArrayLength(env, cuerpo) : 0;
    char* texto = (char*)malloc((size_t)n + 1);
    if (texto && n > 0) (*env)->GetByteArrayRegion(env, cuerpo, 0, n, (jbyte*)texto);
    if (texto) texto[n] = '\0';
    rc_api_server_response_t r;
    r.body = texto ? texto : "";
    r.body_length = texto ? (size_t)n : 0;
    r.http_status_code = estado;
    p->callback(&r, p->datos);
    free(texto);
    free(p);
}

/* ── Avisos hacia Kotlin ───────────────────────────────────────────────── */

static void al_iniciar_sesion(int resultado, const char* error, rc_client_t* client, void* userdata) {
    (void)userdata;
    Oyente* o = oyente_de(client);
    JNIEnv* env = entorno();
    if (!o || !env) return;
    const rc_client_user_t* u = rc_client_get_user_info(client);
    jstring je = cadena(env, error);
    jstring ju = cadena(env, u ? u->username : NULL);
    jstring jd = cadena(env, u ? u->display_name : NULL);
    jstring jt = cadena(env, u ? u->token : NULL);
    (*env)->CallVoidMethod(env, o->oyente, o->sesion, resultado, je, ju, jd, jt, (jint)(u ? u->score_casual : 0));
    (*env)->DeleteLocalRef(env, je); (*env)->DeleteLocalRef(env, ju);
    (*env)->DeleteLocalRef(env, jd); (*env)->DeleteLocalRef(env, jt);
}

static void al_cargar_juego(int resultado, const char* error, rc_client_t* client, void* userdata) {
    (void)userdata;
    Oyente* o = oyente_de(client);
    JNIEnv* env = entorno();
    if (!o || !env) return;
    const rc_client_game_t* g = rc_client_get_game_info(client);
    rc_client_user_game_summary_t r;
    memset(&r, 0, sizeof(r));
    if (resultado == RC_OK) rc_client_get_user_game_summary(client, &r);
    jstring je = cadena(env, error);
    jstring jt = cadena(env, g ? g->title : NULL);
    (*env)->CallVoidMethod(env, o->oyente, o->juego, resultado, je, (jint)(g ? g->id : 0), jt,
                           (jint)r.num_unlocked_achievements, (jint)r.num_promoted_achievements,
                           (jint)r.points_unlocked, (jint)r.points_available);
    (*env)->DeleteLocalRef(env, je); (*env)->DeleteLocalRef(env, jt);
}

static void al_evento(const rc_client_event_t* e, rc_client_t* client) {
    Oyente* o = oyente_de(client);
    JNIEnv* env = entorno();
    if (!o || !env) return;
    switch (e->type) {
        case RC_CLIENT_EVENT_ACHIEVEMENT_TRIGGERED: {
            const rc_client_achievement_t* a = e->achievement;
            char url[512] = "";
            if (rc_client_achievement_get_image_url(a, RC_CLIENT_ACHIEVEMENT_STATE_UNLOCKED, url, sizeof(url)) != RC_OK)
                url[0] = '\0';
            jstring jt = cadena(env, a->title);
            jstring jd = cadena(env, a->description);
            jstring ju = cadena(env, url);
            (*env)->CallVoidMethod(env, o->oyente, o->logro, jt, jd, (jint)a->points, ju);
            (*env)->DeleteLocalRef(env, jt); (*env)->DeleteLocalRef(env, jd); (*env)->DeleteLocalRef(env, ju);
            break;
        }
        case RC_CLIENT_EVENT_GAME_COMPLETED:
        case RC_CLIENT_EVENT_DISCONNECTED:
        case RC_CLIENT_EVENT_RECONNECTED:
        case RC_CLIENT_EVENT_SERVER_ERROR: {
            const char* m = (e->type == RC_CLIENT_EVENT_SERVER_ERROR && e->server_error) ? e->server_error->error_message : NULL;
            jstring jm = cadena(env, m);
            (*env)->CallVoidMethod(env, o->oyente, o->aviso, (jint)e->type, jm);
            (*env)->DeleteLocalRef(env, jm);
            break;
        }
        default:
            break;
    }
}

static void registro(const char* mensaje, const rc_client_t* client) {
    (void)client;
    __android_log_print(ANDROID_LOG_INFO, ETIQUETA, "%s", mensaje);
}

/* ── Funciones nativas de ClienteLogros ────────────────────────────────── */

JNIEXPORT jlong JNICALL
Java_com_diasdefuria_logros_ClienteLogros_nativoCrear(JNIEnv* env, jclass c, jobject oyente) {
    (void)c;
    rc_client_t* client = rc_client_create(leer_memoria, llamar_servidor);
    if (!client) return 0;
    Oyente* o = (Oyente*)calloc(1, sizeof(Oyente));
    if (!o) { rc_client_destroy(client); return 0; }
    o->oyente = (*env)->NewGlobalRef(env, oyente);
    jclass k = (*env)->GetObjectClass(env, oyente);
    o->peticion = (*env)->GetMethodID(env, k, "peticion", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;J)V");
    o->sesion = (*env)->GetMethodID(env, k, "alIniciarSesion", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
    o->juego = (*env)->GetMethodID(env, k, "alCargarJuego", "(ILjava/lang/String;ILjava/lang/String;IIII)V");
    o->logro = (*env)->GetMethodID(env, k, "alConseguirLogro", "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V");
    o->aviso = (*env)->GetMethodID(env, k, "alAviso", "(ILjava/lang/String;)V");
    (*env)->DeleteLocalRef(env, k);
    rc_client_set_userdata(client, o);
    rc_client_set_event_handler(client, al_evento);
    rc_client_enable_logging(client, RC_CLIENT_LOG_LEVEL_WARN, registro);
    /* Modo normal (softcore): se pueden usar partidas guardadas */
    rc_client_set_hardcore_enabled(client, 0);
    return (jlong)(intptr_t)client;
}

JNIEXPORT void JNICALL
Java_com_diasdefuria_logros_ClienteLogros_nativoDestruir(JNIEnv* env, jclass c, jlong ptr) {
    (void)c;
    rc_client_t* client = (rc_client_t*)(intptr_t)ptr;
    if (!client) return;
    Oyente* o = oyente_de(client);
    rc_client_destroy(client);
    if (o) {
        (*env)->DeleteGlobalRef(env, o->oyente);
        free(o);
    }
}

JNIEXPORT jstring JNICALL
Java_com_diasdefuria_logros_ClienteLogros_nativoAgente(JNIEnv* env, jclass c, jlong ptr) {
    (void)c;
    char buf[128] = "";
    rc_client_get_user_agent_clause((rc_client_t*)(intptr_t)ptr, buf, sizeof(buf));
    return cadena(env, buf);
}

JNIEXPORT void JNICALL
Java_com_diasdefuria_logros_ClienteLogros_nativoSesionClave(JNIEnv* env, jclass c, jlong ptr,
                                                                  jstring usuario, jstring clave) {
    (void)c;
    const char* u = (*env)->GetStringUTFChars(env, usuario, NULL);
    const char* p = (*env)->GetStringUTFChars(env, clave, NULL);
    rc_client_begin_login_with_password((rc_client_t*)(intptr_t)ptr, u, p, al_iniciar_sesion, NULL);
    (*env)->ReleaseStringUTFChars(env, usuario, u);
    (*env)->ReleaseStringUTFChars(env, clave, p);
}

JNIEXPORT void JNICALL
Java_com_diasdefuria_logros_ClienteLogros_nativoSesionToken(JNIEnv* env, jclass c, jlong ptr,
                                                                  jstring usuario, jstring token) {
    (void)c;
    const char* u = (*env)->GetStringUTFChars(env, usuario, NULL);
    const char* t = (*env)->GetStringUTFChars(env, token, NULL);
    rc_client_begin_login_with_token((rc_client_t*)(intptr_t)ptr, u, t, al_iniciar_sesion, NULL);
    (*env)->ReleaseStringUTFChars(env, usuario, u);
    (*env)->ReleaseStringUTFChars(env, token, t);
}

JNIEXPORT void JNICALL
Java_com_diasdefuria_logros_ClienteLogros_nativoCargarJuego(JNIEnv* env, jclass c, jlong ptr, jstring hash) {
    (void)c;
    const char* h = (*env)->GetStringUTFChars(env, hash, NULL);
    rc_client_begin_load_game((rc_client_t*)(intptr_t)ptr, h, al_cargar_juego, NULL);
    (*env)->ReleaseStringUTFChars(env, hash, h);
}

JNIEXPORT void JNICALL
Java_com_diasdefuria_logros_ClienteLogros_nativoDescargarJuego(JNIEnv* env, jclass c, jlong ptr) {
    (void)env; (void)c;
    rc_client_unload_game((rc_client_t*)(intptr_t)ptr);
}

JNIEXPORT void JNICALL
Java_com_diasdefuria_logros_ClienteLogros_nativoFotograma(JNIEnv* env, jclass c, jlong ptr) {
    (void)env; (void)c;
    rc_client_do_frame((rc_client_t*)(intptr_t)ptr);
}

JNIEXPORT void JNICALL
Java_com_diasdefuria_logros_ClienteLogros_nativoEnEspera(JNIEnv* env, jclass c, jlong ptr) {
    (void)env; (void)c;
    rc_client_idle((rc_client_t*)(intptr_t)ptr);
}

/*
 * Busca el núcleo que ya ha cargado LibretroDroid (sin cargar una segunda copia)
 * para poder leer la memoria del juego.
 */
JNIEXPORT jboolean JNICALL
Java_com_diasdefuria_logros_ClienteLogros_nativoEnlazarNucleo(JNIEnv* env, jclass c, jstring nombre) {
    (void)c;
    const char* n = (*env)->GetStringUTFChars(env, nombre, NULL);
    void* h = dlopen(n, RTLD_NOW | RTLD_NOLOAD);
    (*env)->ReleaseStringUTFChars(env, nombre, n);
    if (!h) {
        memoria_nucleo = NULL; tamano_nucleo = NULL;
        return JNI_FALSE;
    }
    memoria_nucleo = (fn_memoria)dlsym(h, "retro_get_memory_data");
    tamano_nucleo = (fn_tamano)dlsym(h, "retro_get_memory_size");
    /* El núcleo sigue cargado por LibretroDroid; esta referencia extra se suelta ya */
    dlclose(h);
    return (memoria_nucleo && tamano_nucleo && tamano_nucleo(RETRO_MEMORY_SYSTEM_RAM) > 0) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_diasdefuria_logros_ClienteLogros_nativoSoltarNucleo(JNIEnv* env, jclass c) {
    (void)env; (void)c;
    memoria_nucleo = NULL;
    tamano_nucleo = NULL;
}
