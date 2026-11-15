package com.diasdefuria.ui.componentes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.diasdefuria.R
import com.diasdefuria.ui.tema.Colores
import com.diasdefuria.ui.tema.Pinceles

/** Forma de placa con esquinas cortadas, como la del título. */
val FormaPlaca = CutCornerShape(10.dp)

enum class EstiloBoton { PRINCIPAL, SECUNDARIO, DISCRETO }

/**
 * Botón con borde dorado. Se resalta al recibir el foco, para poder
 * navegar el menú con mando o cruceta.
 */
@Composable
fun BotonMystara(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    estilo: EstiloBoton = EstiloBoton.SECUNDARIO,
    habilitado: Boolean = true,
    cargando: Boolean = false,
) {
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()
    val pulsado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = when {
            pulsado -> 0.97f
            enfocado -> 1.04f
            else -> 1f
        },
        label = "escala",
    )
    val fondo: Brush = when (estilo) {
        EstiloBoton.PRINCIPAL -> Pinceles.carmesi
        EstiloBoton.SECUNDARIO -> Pinceles.piedra
        EstiloBoton.DISCRETO -> Brush.verticalGradient(listOf(Color(0x99201814), Color(0x99100A08)))
    }
    val borde = if (enfocado) BorderStroke(3.dp, Colores.OroClaro) else BorderStroke(1.5.dp, Pinceles.oro)
    val colorTexto = if (estilo == EstiloBoton.PRINCIPAL || enfocado) Colores.OroClaro else Colores.Pergamino

    Row(
        modifier = modifier
            .scale(escala)
            .alpha(if (habilitado) 1f else 0.5f)
            .heightIn(min = 38.dp)
            .background(fondo, FormaPlaca)
            .border(borde, FormaPlaca)
            .clickable(
                interactionSource = interaccion,
                indication = null,
                enabled = habilitado && !cargando,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        when {
            cargando -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Colores.Oro,
            )
            icono != null -> Icon(icono, contentDescription = null, tint = Colores.Oro, modifier = Modifier.size(20.dp))
        }
        if (cargando || icono != null) Spacer(Modifier.width(10.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            color = colorTexto,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Fondo común de las pantallas secundarias: el arte oscurecido. */
@Composable
fun FondoMystara(oscuridad: Float = 0.82f, contenido: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Colores.Piedra)) {
        Image(
            painter = painterResource(R.drawable.fondo_menu),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Colores.Piedra.copy(alpha = oscuridad)))
        contenido()
    }
}

/** Pantalla de sección: cabecera con «Volver» y título, y contenido desplazable. */
@Composable
fun PantallaSeccion(
    titulo: String,
    onVolver: () -> Unit,
    desplazable: Boolean = true,
    /** Botones opcionales a la derecha, en la misma línea que el título. */
    acciones: @Composable RowScope.() -> Unit = {},
    contenido: @Composable ColumnScope.() -> Unit,
) {
    FondoMystara {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BotonMystara(
                    texto = stringResource(R.string.volver),
                    icono = Icons.AutoMirrored.Filled.ArrowBack,
                    estilo = EstiloBoton.DISCRETO,
                    onClick = onVolver,
                )
                Spacer(Modifier.width(20.dp))
                Text(titulo, style = MaterialTheme.typography.headlineMedium, color = Colores.Oro)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), content = acciones)
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Colores.OroOscuro)
            val mod = if (desplazable) Modifier.verticalScroll(rememberScrollState()) else Modifier
            Column(mod.fillMaxWidth().weight(1f), content = contenido)
        }
    }
}

/** Fila de ajuste con interruptor. Toda la fila es pulsable y enfocable. */
@Composable
fun FilaInterruptor(texto: String, valor: Boolean, onCambio: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onCambio(!valor) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = valor,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Colores.OroClaro,
                checkedTrackColor = Colores.Carmesi,
                checkedBorderColor = Colores.Oro,
            ),
        )
    }
}

@Composable
fun FilaDeslizador(
    texto: String,
    valor: Float,
    onCambio: (Float) -> Unit,
    habilitado: Boolean = true,
    rango: ClosedFloatingPointRange<Float> = 0.1f..1f,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).alpha(if (habilitado) 1f else 0.5f)) {
        Text("$texto: ${(valor * 100).toInt()} %", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = valor,
            onValueChange = onCambio,
            valueRange = rango,
            enabled = habilitado,
            colors = SliderDefaults.colors(thumbColor = Colores.Oro, activeTrackColor = Colores.Carmesi),
        )
    }
}

@Composable
fun Subtitulo(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleLarge,
        color = Colores.Oro,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
fun AvisoProximamente(texto: String) {
    Text(
        text = "⚒  $texto",
        style = MaterialTheme.typography.bodyMedium,
        color = Colores.TextoSuave,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}
