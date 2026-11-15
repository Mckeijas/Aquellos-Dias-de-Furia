package com.diasdefuria.red

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.net.InetAddress

/** Una partida anunciada en la red local. */
data class PartidaEnRed(
    val nombreServicio: String,
    val juego: String,
    val anfitrion: String,
    val direccion: InetAddress,
    val puerto: Int,
)

/**
 * Anuncio y búsqueda de partidas en la wifi mediante NSD (mDNS/Bonjour),
 * sin tener que escribir direcciones IP.
 */
class Descubrimiento(context: Context) {
    private val app = context.applicationContext
    private val nsd = app.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val principal = Handler(Looper.getMainLooper())
    private var registro: NsdManager.RegistrationListener? = null
    private var busqueda: NsdManager.DiscoveryListener? = null
    private var bloqueoMulticast: WifiManager.MulticastLock? = null

    // ── Anfitrión ────────────────────────────────────────────────────────────

    fun anunciar(puerto: Int, juego: String, anfitrion: String) {
        dejarDeAnunciar()
        val info = NsdServiceInfo().apply {
            serviceName = "Días de Furia · $anfitrion"
            serviceType = Protocolo.SERVICIO_NSD
            port = puerto
            setAttribute("juego", juego)
            setAttribute("anfitrion", anfitrion)
            setAttribute("v", Protocolo.VERSION.toString())
        }
        val l = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(i: NsdServiceInfo) {}
            override fun onRegistrationFailed(i: NsdServiceInfo, e: Int) {}
            override fun onServiceUnregistered(i: NsdServiceInfo) {}
            override fun onUnregistrationFailed(i: NsdServiceInfo, e: Int) {}
        }
        registro = l
        runCatching { nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, l) }
    }

    fun dejarDeAnunciar() {
        registro?.let { runCatching { nsd.unregisterService(it) } }
        registro = null
    }

    // ── Invitado ─────────────────────────────────────────────────────────────

    /** Busca partidas; [alCambiar] recibe la lista completa cada vez que cambia (en el hilo principal). */
    fun buscar(alCambiar: (List<PartidaEnRed>) -> Unit) {
        dejarDeBuscar()
        bloqueoMulticast = (app.getSystemService(Context.WIFI_SERVICE) as? WifiManager)
            ?.createMulticastLock("diasdefuria-nsd")?.apply { setReferenceCounted(false); acquire() }
        val encontradas = linkedMapOf<String, PartidaEnRed>()
        fun avisar() = principal.post { alCambiar(encontradas.values.toList()) }

        val l = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(tipo: String) {}
            override fun onDiscoveryStopped(tipo: String) {}
            override fun onStartDiscoveryFailed(tipo: String, e: Int) {}
            override fun onStopDiscoveryFailed(tipo: String, e: Int) {}
            override fun onServiceFound(info: NsdServiceInfo) {
                if (!info.serviceType.contains("diasdefuria")) return
                resolver(info) { p -> encontradas[p.nombreServicio] = p; avisar() }
            }
            override fun onServiceLost(info: NsdServiceInfo) {
                encontradas.remove(info.serviceName); avisar()
            }
        }
        busqueda = l
        runCatching { nsd.discoverServices(Protocolo.SERVICIO_NSD, NsdManager.PROTOCOL_DNS_SD, l) }
    }

    @Suppress("DEPRECATION")
    private fun resolver(info: NsdServiceInfo, listo: (PartidaEnRed) -> Unit) {
        // resolveService admite una única resolución a la vez en Android < 14
        nsd.resolveService(info, object : NsdManager.ResolveListener {
            override fun onResolveFailed(i: NsdServiceInfo, e: Int) {
                if (e == NsdManager.FAILURE_ALREADY_ACTIVE) principal.postDelayed({ resolver(info, listo) }, 300)
            }
            override fun onServiceResolved(i: NsdServiceInfo) {
                val dir = if (Build.VERSION.SDK_INT >= 34) i.hostAddresses.firstOrNull() else i.host
                dir ?: return
                val attr = i.attributes
                listo(
                    PartidaEnRed(
                        nombreServicio = i.serviceName,
                        juego = attr["juego"]?.toString(Charsets.UTF_8) ?: "",
                        anfitrion = attr["anfitrion"]?.toString(Charsets.UTF_8) ?: i.serviceName,
                        direccion = dir,
                        puerto = i.port,
                    )
                )
            }
        })
    }

    fun dejarDeBuscar() {
        busqueda?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        busqueda = null
        bloqueoMulticast?.let { runCatching { it.release() } }
        bloqueoMulticast = null
    }

    fun cerrar() {
        dejarDeAnunciar()
        dejarDeBuscar()
    }
}

/** Nombre con el que este dispositivo aparece en las partidas. */
fun nombreDispositivo(): String =
    (Build.MODEL ?: "Android").replaceFirstChar { it.uppercase() }.take(24)
