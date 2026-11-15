package com.diasdefuria.red

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL

/**
 * Apertura automática del puerto en el router (UPnP IGD) para que un invitado de
 * fuera de casa pueda conectarse directamente al anfitrión, sin servidores.
 * Todas las llamadas bloquean: usar desde un hilo de fondo.
 */
class AperturaPuerto {
    private var control: String? = null
    private var servicio: String? = null
    private var puertoAbierto = 0

    data class Resultado(
        /** El router ha aceptado abrir el puerto. */
        val abierto: Boolean,
        /** IP pública que da el router (puede ser de CG-NAT). */
        val ipRouter: String?,
        /** IP con la que se ve este dispositivo desde internet. */
        val ipPublica: String?,
        val ipLocal: String?,
    ) {
        /** Doble NAT o CG-NAT: el router no tiene la IP pública real y nadie podrá entrar. */
        val cgnat: Boolean get() = ipRouter != null && (esPrivada(ipRouter) || (ipPublica != null && ipPublica != ipRouter))
        /** IP que va en el código de invitación. */
        val ipInvitacion: String? get() = ipPublica ?: ipRouter?.takeIf { !esPrivada(it) }
    }

    fun abrir(puerto: Int, descripcion: String): Resultado {
        var ipRouter: String? = null
        var ipLocal: String? = null
        var abierto = false
        runCatching {
            for (ubicacion in buscarRouters()) {
                val (ctrl, tipo) = servicioWan(ubicacion) ?: continue
                val u = URI(ubicacion)
                ipLocal = ipLocalHacia(u.host, if (u.port > 0) u.port else 80) ?: ipLocal
                val local = ipLocal ?: continue
                ipRouter = soap(ctrl, tipo, "GetExternalIPAddress", "")
                    ?.let { Regex("<NewExternalIPAddress>([^<]*)<").find(it)?.groupValues?.get(1) }?.trim()
                // Primero con caducidad (se cierra sola si la app se cierra mal); hay routers que solo admiten 0
                for (lease in listOf(7200, 0)) {
                    val r = soap(ctrl, tipo, "AddPortMapping", """
                        <NewRemoteHost></NewRemoteHost><NewExternalPort>$puerto</NewExternalPort>
                        <NewProtocol>TCP</NewProtocol><NewInternalPort>$puerto</NewInternalPort>
                        <NewInternalClient>$local</NewInternalClient><NewEnabled>1</NewEnabled>
                        <NewPortMappingDescription>$descripcion</NewPortMappingDescription>
                        <NewLeaseDuration>$lease</NewLeaseDuration>""".trimIndent())
                    if (r != null && !r.contains("Fault", ignoreCase = true)) { abierto = true; break }
                }
                if (abierto) {
                    control = ctrl; servicio = tipo; puertoAbierto = puerto
                    break
                }
            }
        }
        if (ipLocal == null) ipLocal = ipLocalWifi()
        return Resultado(abierto, ipRouter, ipPublicaReal(), ipLocal)
    }

    fun cerrar() {
        val c = control ?: return
        val t = servicio ?: return
        runCatching {
            soap(c, t, "DeletePortMapping",
                "<NewRemoteHost></NewRemoteHost><NewExternalPort>$puertoAbierto</NewExternalPort><NewProtocol>TCP</NewProtocol>")
        }
        control = null
    }

    // ── SSDP: buscar el router ───────────────────────────────────────────────

    private fun buscarRouters(): List<String> {
        val ubicaciones = linkedSetOf<String>()
        DatagramSocket().use { s ->
            s.soTimeout = 700
            val destino = InetSocketAddress("239.255.255.250", 1900)
            val tipos = listOf(
                "urn:schemas-upnp-org:device:InternetGatewayDevice:1",
                "urn:schemas-upnp-org:device:InternetGatewayDevice:2",
                "urn:schemas-upnp-org:service:WANIPConnection:1",
                "urn:schemas-upnp-org:service:WANPPPConnection:1",
            )
            repeat(2) {
                for (st in tipos) {
                    val m = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: $st\r\n\r\n"
                    val b = m.toByteArray()
                    runCatching { s.send(DatagramPacket(b, b.size, destino)) }
                }
                val fin = System.currentTimeMillis() + 2200
                val buf = ByteArray(2048)
                while (System.currentTimeMillis() < fin) {
                    val p = DatagramPacket(buf, buf.size)
                    try { s.receive(p) } catch (_: SocketTimeoutException) { continue }
                    val txt = String(p.data, 0, p.length)
                    Regex("(?im)^location:\\s*(\\S+)").find(txt)?.groupValues?.get(1)?.let { ubicaciones += it }
                }
                if (ubicaciones.isNotEmpty()) return ubicaciones.toList()
            }
        }
        return ubicaciones.toList()
    }

    /** Lee la descripción del router y devuelve (URL de control, tipo de servicio) de la conexión WAN. */
    private fun servicioWan(ubicacion: String): Pair<String, String>? {
        val xml = http("GET", ubicacion, null, null) ?: return null
        val base = Regex("<URLBase>\\s*([^<]+?)\\s*</URLBase>").find(xml)?.groupValues?.get(1) ?: ubicacion
        val servicios = Regex("(?s)<service>(.*?)</service>").findAll(xml).map { it.groupValues[1] }
        for (pref in listOf("WANIPConnection:2", "WANIPConnection:1", "WANPPPConnection:1")) {
            val s = servicios.firstOrNull { it.contains(pref) } ?: continue
            val tipo = Regex("<serviceType>\\s*([^<]+?)\\s*</serviceType>").find(s)?.groupValues?.get(1) ?: continue
            val ctrl = Regex("<controlURL>\\s*([^<]+?)\\s*</controlURL>").find(s)?.groupValues?.get(1) ?: continue
            return URI(base).resolve(ctrl).toString() to tipo
        }
        return null
    }

    private fun soap(control: String, tipo: String, accion: String, args: String): String? {
        val cuerpo = """<?xml version="1.0"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
<s:Body><u:$accion xmlns:u="$tipo">$args</u:$accion></s:Body></s:Envelope>"""
        return http("POST", control, cuerpo, "\"$tipo#$accion\"")
    }

    /**
     * HTTP/1.0 mínimo sobre un socket: el router está en la red local y no usa HTTPS
     * (así no hace falta permitir tráfico sin cifrar en toda la app).
     */
    private fun http(metodo: String, url: String, cuerpo: String?, accionSoap: String?): String? {
        val u = URI(url)
        val puerto = if (u.port > 0) u.port else 80
        val ruta = (u.rawPath?.ifEmpty { "/" } ?: "/") + (u.rawQuery?.let { "?$it" } ?: "")
        return Socket().use { s ->
            s.connect(InetSocketAddress(u.host, puerto), 2500)
            s.soTimeout = 3000
            val datos = cuerpo?.toByteArray() ?: ByteArray(0)
            val cab = buildString {
                append("$metodo $ruta HTTP/1.0\r\nHost: ${u.host}:$puerto\r\nConnection: close\r\n")
                if (cuerpo != null) {
                    append("Content-Type: text/xml; charset=\"utf-8\"\r\nContent-Length: ${datos.size}\r\n")
                    if (accionSoap != null) append("SOAPAction: $accionSoap\r\n")
                }
                append("\r\n")
            }
            s.getOutputStream().apply { write(cab.toByteArray()); write(datos); flush() }
            val todo = ByteArrayOutputStream()
            runCatching { s.getInputStream().copyTo(todo) }
            val txt = todo.toString("UTF-8")
            val estado = txt.substringBefore("\r\n")
            val cuerpoResp = txt.substringAfter("\r\n\r\n", "")
            // Los errores SOAP vienen con 500 y un <Fault>: se devuelven para poder detectarlos
            if (estado.contains(" 200") || cuerpoResp.contains("Fault")) cuerpoResp else null
        }
    }

    private fun ipLocalHacia(host: String, puerto: Int): String? = runCatching {
        Socket().use { s ->
            s.connect(InetSocketAddress(host, puerto), 2000)
            (s.localAddress as? Inet4Address)?.hostAddress
        }
    }.getOrNull()

    companion object {
        /** IP con la que nos ve internet (servicio público de consulta; solo devuelve la IP). */
        fun ipPublicaReal(): String? = runCatching {
            val c = URL("https://api.ipify.org").openConnection() as HttpURLConnection
            c.connectTimeout = 3000; c.readTimeout = 3000
            c.inputStream.bufferedReader().use { it.readText().trim() }.takeIf { esIpv4(it) }
        }.getOrNull()

        fun esIpv4(t: String) = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$").matches(t)

        fun esPrivada(ip: String): Boolean {
            val p = ip.split(".").mapNotNull { it.toIntOrNull() }
            if (p.size != 4) return true
            return p[0] == 10 || p[0] == 127 || (p[0] == 172 && p[1] in 16..31) || (p[0] == 192 && p[1] == 168) ||
                (p[0] == 100 && p[1] in 64..127) || (p[0] == 169 && p[1] == 254) || p[0] == 0
        }

        /** IP de este dispositivo en la red local (wifi o ethernet). */
        fun ipLocalWifi(): String? = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .sortedBy { if (it.name.startsWith("wlan")) 0 else if (it.name.startsWith("eth")) 1 else 2 }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }?.hostAddress
        }.getOrNull()
    }
}

/**
 * Código de invitación: IP + puerto en 10 letras y números fáciles de dictar
 * (sin 0/O ni 1/I), por ejemplo «KX7M2-PQ4TA». También se acepta escribir
 * directamente «ip:puerto», una IP sola o un nombre (por ejemplo de Tailscale).
 */
object CodigoInvitacion {
    /** 32 símbolos: letras sin I ni O, y cifras del 2 al 9. */
    private const val TABLA = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun crear(ip: String, puerto: Int): String? {
        val p = ip.split(".").mapNotNull { it.toIntOrNull() }
        if (p.size != 4 || puerto !in 1..65535) return null
        var v = 0L
        p.forEach { v = (v shl 8) or it.toLong() }
        v = (v shl 16) or puerto.toLong()
        val control = ((p.sum() + puerto) % 4).toLong()
        v = (v shl 2) or control // 50 bits
        val sb = StringBuilder()
        repeat(10) { i ->
            val desplazamiento = (9 - i) * 5
            sb.append(TABLA[((v shr desplazamiento) and 31).toInt()])
        }
        return sb.insert(5, '-').toString()
    }

    /** Devuelve (host, puerto) o null si no se entiende. */
    fun leer(texto: String): Pair<String, Int>? {
        val t = texto.trim()
        if (t.isEmpty()) return null
        // ip:puerto, ip sola o nombre de equipo
        if (t.contains('.') || t.contains(':')) {
            val host = t.substringBeforeLast(':').trim().removePrefix("[").removeSuffix("]")
            val puerto = if (t.contains(':')) t.substringAfterLast(':').trim().toIntOrNull() else Protocolo.PUERTO
            return if (host.isNotEmpty() && puerto != null && puerto in 1..65535) host to puerto else null
        }
        val limpio = t.uppercase().replace("-", "").replace(" ", "")
        if (limpio.length != 10) return null
        var v = 0L
        for (c in limpio) {
            val i = TABLA.indexOf(c)
            if (i < 0) return null
            v = (v shl 5) or i.toLong()
        }
        val control = (v and 3).toInt()
        v = v shr 2
        val puerto = (v and 0xFFFF).toInt()
        v = v shr 16
        val bytes = (0..3).map { ((v shr ((3 - it) * 8)) and 0xFF).toInt() }
        if ((bytes.sum() + puerto) % 4 != control || puerto == 0) return null
        return bytes.joinToString(".") to puerto
    }
}

/** Wifi en modo baja latencia mientras se juega en red (evita el ahorro de energía de la wifi). */
class BloqueoWifi(context: Context) {
    @Suppress("DEPRECATION")
    private val bloqueo = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)
        ?.createWifiLock(
            if (Build.VERSION.SDK_INT >= 29) WifiManager.WIFI_MODE_FULL_LOW_LATENCY else WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            "diasdefuria-red",
        )?.apply { setReferenceCounted(false) }

    fun activar() { runCatching { bloqueo?.acquire() } }
    fun soltar() { runCatching { if (bloqueo?.isHeld == true) bloqueo.release() } }
}

/** Resuelve un nombre o IP (bloquea: usar en un hilo de fondo). */
internal fun resolver(host: String): InetAddress = InetAddress.getByName(host)

/** Qué se puede decir a los invitados sobre cómo unirse (lo muestra el anfitrión). */
data class Invitacion(
    val internet: Boolean,
    val puerto: Int,
    val ipLocal: String? = null,
    val codigoCasa: String? = null,
    /** Mientras se pregunta al router. */
    val preparando: Boolean = internet,
    val codigoInternet: String? = null,
    val ipPublica: String? = null,
    val apertura: Apertura = Apertura.PREPARANDO,
) {
    enum class Apertura {
        PREPARANDO,
        /** El router ha abierto el puerto: lo normal es que funcione. */
        ABIERTO,
        /** El router no ha respondido a UPnP: hay que abrir el puerto a mano. */
        MANUAL,
        /** La compañía comparte la IP (CG-NAT) o hay dos routers: no se puede entrar desde fuera. */
        CGNAT,
        SIN_INTERNET,
    }
}
