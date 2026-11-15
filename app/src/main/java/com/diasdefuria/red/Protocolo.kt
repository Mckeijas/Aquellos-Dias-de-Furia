package com.diasdefuria.red

/**
 * Multijugador en red local («retransmisión desde el anfitrión»).
 *
 * El dispositivo que crea la partida (anfitrión) es el único que emula el juego.
 * Envía la imagen comprimida en H.264 a los invitados y recibe de ellos los
 * botones, que entran en el emulador como jugadores 2, 3 y 4. Así todos ven
 * exactamente la misma partida (no puede desincronizarse) y los invitados no
 * necesitan tener las ROMs.
 *
 * Una conexión TCP por invitado, con mensajes [tipo:byte][datos]:
 *
 *   Invitado → anfitrión
 *     HOLA       versión:int, nombre:utf
 *     BOTON      tecla:int (código RetroPad de Android), pulsado:bool
 *     DIRECCION  x:float, y:float (cruceta, -1..1)
 *     PING       marca:long (el anfitrión la devuelve tal cual en PONG)
 *     PEDIR_CLAVE (la imagen se ha atascado: el anfitrión manda un fotograma completo)
 *     ADIOS
 *
 *   Anfitrión → invitado
 *     BIENVENIDO jugador:int (2-4), juego:utf, ancho:int, alto:int
 *     LLENO      (no quedan puestos)
 *     VERSION    (versión de protocolo distinta)
 *     VIDEO_CFG  longitud:int, datos (SPS/PPS de H.264)
 *     VIDEO      tiempo:long, clave:bool, longitud:int, datos
 *     JUGADORES  n:int, nombres:utf×n
 *     PONG       marca:long, hora del anfitrión:long (ns, para medir el retraso de la imagen)
 *     AUDIO_CFG  frecuencia:int, canales:int, longitud:int, datos (AAC)
 *     AUDIO      tiempo:long, longitud:int, datos (AAC)
 *     FIN        (el anfitrión cierra la partida)
 *     PAUSA      pausado:bool (el anfitrión ha abierto o cerrado su menú de pausa)
 *
 * Por internet la conexión es directa entre dispositivos: el anfitrión abre su
 * puerto en el router (UPnP) y comparte un código con su IP pública y el puerto.
 */
/**
 * Calidad del vídeo según dónde se juega. Ancho y alto: 4:3 y múltiplos de 16
 * (requisito del codificador). Por internet se baja para que quepa en la subida
 * del anfitrión y llegue antes; el bitrate se ajusta solo según cómo va la red.
 */
enum class CalidadRed(val ancho: Int, val alto: Int, val bitrate: Int, val bitrateMin: Int, val bitrateMax: Int, val cola: Int) {
    CASA(768, 576, 4_500_000, 1_500_000, 7_000_000, 8),
    INTERNET(512, 384, 2_500_000, 700_000, 4_000_000, 4),
}

object Protocolo {
    const val VERSION = 6
    const val SERVICIO_NSD = "_diasdefuria._tcp."

    const val HOLA: Byte = 1
    const val BOTON: Byte = 2
    const val DIRECCION: Byte = 3
    const val ADIOS: Byte = 4
    const val PING: Byte = 5
    const val PEDIR_CLAVE: Byte = 6

    const val BIENVENIDO: Byte = 10
    const val LLENO: Byte = 11
    const val VERSION_DISTINTA: Byte = 12
    const val VIDEO_CFG: Byte = 13
    const val VIDEO: Byte = 14
    const val JUGADORES: Byte = 15
    const val FIN: Byte = 16
    const val PONG: Byte = 17
    /** Audio: configuración AAC (frecuencia:int, canales:int, longitud:int, datos) y bloques (tiempo:long, longitud:int, datos). */
    const val AUDIO_CFG: Byte = 18
    const val AUDIO: Byte = 19
    /** El anfitrión pausa (true) o vuelve a la partida (false): el invitado ve «Esperando al anfitrión...». */
    const val PAUSA: Byte = 20

    /** Puerto fijo de las partidas (para poder abrirlo en el router a mano si hace falta). */
    const val PUERTO = 47800

    const val FPS_VIDEO = 60

    /**
     * Marca de prioridad de los paquetes (DSCP AF41, «vídeo interactivo»): la wifi los manda
     * por su cola de vídeo. Antes era EF (voz): esa cola no agrupa paquetes y con el caudal
     * del vídeo la wifi se saturaba antes, con tirones y retraso.
     */
    const val CLASE_TRAFICO = 0x88

    /** Máximo de jugadores: el anfitrión (1) + 3 invitados. */
    const val MAX_JUGADORES = 4
}
