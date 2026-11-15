package com.diasdefuria.ui.pantallas

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.diasdefuria.R
import com.diasdefuria.logros.ClienteLogros
import com.diasdefuria.logros.CuentaLogros
import com.diasdefuria.ui.componentes.BotonMystara
import com.diasdefuria.ui.componentes.EstiloBoton
import com.diasdefuria.ui.componentes.FilaInterruptor
import com.diasdefuria.ui.componentes.FormaPlaca
import com.diasdefuria.ui.tema.Colores

/** Colores propios de la sección de logros, para que destaque del resto. */
private val AzulLogros = Brush.horizontalGradient(listOf(Color(0xFF1C2A6B), Color(0xFF3B1F6E), Color(0xFF5A2A7A)))
private val BordeLogros = Color(0xFFB8C7FF)

/** Botón especial de la sección Trucos: abre la conexión con RetroAchievements. */
@Composable
fun BotonRetroAchievements(onClick: () -> Unit) {
    val context = LocalContext.current
    val cuenta = remember { CuentaLogros(context) }
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(AzulLogros, RoundedCornerShape(12.dp))
            .border(if (enfocado) 3.dp else 1.5.dp, if (enfocado) Color.White else BordeLogros, RoundedCornerShape(12.dp))
            .clickable(interactionSource = interaccion, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD86B), modifier = Modifier.size(34.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.ra_titulo), style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(
                if (cuenta.conectada) stringResource(R.string.ra_boton_conectado, cuenta.nombre ?: "", cuenta.puntos)
                else stringResource(R.string.ra_boton_desconectado),
                style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

/** Panel para conectar la cuenta de RetroAchievements y elegir cómo funcionan los logros. */
@Composable
fun PanelRetroAchievements(onCerrar: () -> Unit) {
    val context = LocalContext.current
    val cuenta = remember { CuentaLogros(context) }
    var version by remember { mutableIntStateOf(0) } // refresca al conectar o desconectar
    BackHandler(onBack = onCerrar)

    Box(Modifier.fillMaxSize().background(Colores.Piedra.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth(0.8f)
                .background(Colores.Piedra, FormaPlaca)
                .border(2.dp, BordeLogros, FormaPlaca)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD86B), modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.ra_titulo), style = MaterialTheme.typography.headlineMedium,
                    color = Color.White, modifier = Modifier.weight(1f))
                BotonMystara(stringResource(R.string.volver), onCerrar, estilo = EstiloBoton.DISCRETO)
            }
            Text(stringResource(R.string.ra_explicacion), style = MaterialTheme.typography.bodyMedium)

            if (!ClienteLogros.disponible) {
                Text(stringResource(R.string.ra_no_disponible), color = Colores.OroClaro)
            } else if (version >= 0 && cuenta.conectada) {
                Text(stringResource(R.string.ra_conectado_como, cuenta.nombre ?: "", cuenta.puntos),
                    style = MaterialTheme.typography.titleMedium, color = BordeLogros)
                var activados by remember { mutableStateOf(cuenta.activados) }
                var sonido by remember { mutableStateOf(cuenta.sonido) }
                FilaInterruptor(stringResource(R.string.ra_activados), activados) { activados = it; cuenta.activados = it }
                FilaInterruptor(stringResource(R.string.ra_sonido), sonido) { sonido = it; cuenta.sonido = it }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BotonMystara(stringResource(R.string.ra_ver_perfil), {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://retroachievements.org/user/" + Uri.encode(cuenta.usuario ?: ""))))
                        }
                    }, icono = Icons.AutoMirrored.Filled.OpenInNew)
                    BotonMystara(stringResource(R.string.ra_desconectar), { cuenta.cerrarSesion(); version++ },
                        icono = Icons.AutoMirrored.Filled.Logout, estilo = EstiloBoton.DISCRETO)
                }
            } else {
                FormularioSesion(onConectado = { version++ })
            }
            Text(stringResource(R.string.ra_notas), style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave)
        }
    }
}

@Composable
private fun FormularioSesion(onConectado: () -> Unit) {
    val context = LocalContext.current
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var conectando by remember { mutableStateOf(false) }
    var fallo by remember { mutableStateOf<String?>(null) }
    val cliente = remember { arrayOfNulls<ClienteLogros>(1) }
    DisposableEffect(Unit) { onDispose { cliente[0]?.cerrar() } }
    val textoFallo = stringResource(R.string.ra_error_conexion)

    fun conectar() {
        if (usuario.isBlank() || clave.isBlank() || conectando) return
        conectando = true
        fallo = null
        val principal = Handler(Looper.getMainLooper())
        cliente[0]?.cerrar()
        val c = ClienteLogros(object : ClienteLogros.Eventos {
            override fun sesion(ok: Boolean, error: String, usuario: String, nombre: String, token: String, puntos: Int) {
                principal.post {
                    conectando = false
                    if (ok && token.isNotEmpty()) {
                        // Se guarda el token de sesión, nunca la contraseña
                        CuentaLogros(context).guardar(usuario, nombre, token, puntos)
                        clave = ""
                        onConectado()
                    } else {
                        fallo = error.ifBlank { textoFallo }
                    }
                }
            }
        })
        cliente[0] = c
        c.iniciarSesion(usuario.trim(), clave)
    }

    val colores = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.White, unfocusedBorderColor = BordeLogros, cursorColor = Color.White,
        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
        focusedLabelColor = BordeLogros, unfocusedLabelColor = BordeLogros.copy(alpha = 0.8f),
    )
    OutlinedTextField(
        value = usuario, onValueChange = { usuario = it.take(64) }, singleLine = true,
        label = { Text(stringResource(R.string.ra_usuario)) }, colors = colores,
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = clave, onValueChange = { clave = it.take(128) }, singleLine = true,
        label = { Text(stringResource(R.string.ra_clave)) }, colors = colores,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { conectar() }),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BotonMystara(stringResource(R.string.ra_conectar), ::conectar, icono = Icons.AutoMirrored.Filled.Login,
            estilo = EstiloBoton.PRINCIPAL, habilitado = usuario.isNotBlank() && clave.isNotBlank(), cargando = conectando)
        BotonMystara(stringResource(R.string.ra_crear_cuenta), {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://retroachievements.org/createaccount.php")))
            }
        }, icono = Icons.AutoMirrored.Filled.OpenInNew, estilo = EstiloBoton.DISCRETO)
        if (conectando) CircularProgressIndicator(Modifier.size(20.dp), color = BordeLogros, strokeWidth = 2.dp)
    }
    fallo?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Colores.OroClaro) }
}
