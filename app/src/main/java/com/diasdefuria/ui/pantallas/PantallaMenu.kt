package com.diasdefuria.ui.pantallas

import androidx.compose.foundation.Image
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import com.diasdefuria.ui.componentes.FormaPlaca
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.diasdefuria.R
import com.diasdefuria.ui.componentes.BotonMystara
import com.diasdefuria.ui.componentes.EstiloBoton
import com.diasdefuria.ui.tema.Colores
import com.diasdefuria.ui.componentes.NubesEnMovimiento

/** Destinos a los que lleva el menú. */
enum class Seccion { LEYENDAS, FILTROS, MODS, CONTROLES, PARTIDAS, ARTE, MULTIJUGADOR }

/**
 * Menú principal. El arte ocupa toda la pantalla y los botones van en un panel
 * a la derecha, que además tapa la zona donde estaba el logo original.
 */
@Composable
fun PantallaMenu(
    version: String,
    onSeccion: (Seccion) -> Unit,
    onSalir: () -> Unit,
) {
    var legal by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Colores.Piedra)) {
        Image(
            painter = painterResource(R.drawable.fondo_menu),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterStart, // el rótulo del muro queda siempre a la vista
            modifier = Modifier.fillMaxSize(),
        )
        // Las nubes del fondo también se mueven en el menú (misma imagen que la intro)
        NubesEnMovimiento(Modifier.fillMaxSize(), sesgoX = -1f)
        // Degradado que oscurece la mitad derecha para dar contraste a los botones
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0.00f to Colores.Piedra.copy(alpha = 0f),
                    0.42f to Colores.Piedra.copy(alpha = 0f),
                    0.58f to Colores.Piedra.copy(alpha = 0.88f),
                    1.00f to Colores.Piedra.copy(alpha = 0.96f),
                )
            )
        )

        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.44f)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)
        ) {
        // Centrado vertical si cabe; desplazable si la pantalla es baja
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            // El título va pintado en el muro del fondo (fondo_menu.jpg): el panel solo lleva botones
            // Los juegos están dentro de «Juegos» (la ruleta)
            BotonMystara(
                texto = stringResource(R.string.menu_leyendas),
                icono = Icons.Filled.AutoStories,
                estilo = EstiloBoton.PRINCIPAL,
                onClick = { onSeccion(Seccion.LEYENDAS) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            )
            Spacer(Modifier.height(2.dp))
            FilaBotones(
                stringResource(R.string.menu_filtros) to Icons.Filled.Tune to { onSeccion(Seccion.FILTROS) },
                stringResource(R.string.menu_partidas) to Icons.Filled.Save to { onSeccion(Seccion.PARTIDAS) },
            )
            FilaBotones(
                stringResource(R.string.menu_controles) to Icons.Filled.SportsEsports to { onSeccion(Seccion.CONTROLES) },
                stringResource(R.string.menu_mods) to Icons.Filled.AutoFixHigh to { onSeccion(Seccion.MODS) },
            )
            FilaBotones(
                stringResource(R.string.menu_arte) to Icons.Filled.Collections to { onSeccion(Seccion.ARTE) },
                stringResource(R.string.menu_salir) to Icons.AutoMirrored.Filled.ExitToApp to onSalir,
            )
            BotonMystara(
                texto = stringResource(R.string.menu_multijugador),
                icono = Icons.Filled.Groups,
                onClick = { onSeccion(Seccion.MULTIJUGADOR) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        }

        // Botón «C»: aviso legal y derechos
        BotonLegal(
            Modifier
                .align(Alignment.BottomStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 10.dp, bottom = 6.dp),
        ) { legal = true }

        Text(
            // Versión y autor: pequeño y translúcido, que no compita con el fondo
            text = stringResource(R.string.menu_version, version),
            style = MaterialTheme.typography.labelSmall,
            color = Colores.Pergamino.copy(alpha = 0.45f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(end = 10.dp, bottom = 2.dp),
        )

        if (legal) PanelLegal { legal = false }
    }
}

/** Pequeño círculo con una «C» (de copyright) que abre el aviso legal. */
@Composable
private fun BotonLegal(modifier: Modifier, onClick: () -> Unit) {
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()
    Box(
        modifier
            .size(30.dp)
            .background(Colores.Piedra.copy(alpha = 0.75f), CircleShape)
            .border(if (enfocado) 2.5.dp else 1.2.dp, if (enfocado) Colores.OroClaro else Colores.Oro, CircleShape)
            .clickable(interactionSource = interaccion, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("C", style = MaterialTheme.typography.titleMedium, color = if (enfocado) Colores.OroClaro else Colores.Oro)
    }
}

/** Aviso legal: proyecto sin ánimo de lucro, derechos de los autores, juegos no incluidos… */
@Composable
private fun PanelLegal(onCerrar: () -> Unit) {
    BackHandler(onBack = onCerrar)
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
    val secciones = listOf(
        R.string.legal_1_titulo to R.string.legal_1_texto,
        R.string.legal_2_titulo to R.string.legal_2_texto,
        R.string.legal_3_titulo to R.string.legal_3_texto,
        R.string.legal_4_titulo to R.string.legal_4_texto,
        R.string.legal_5_titulo to R.string.legal_5_texto,
        R.string.legal_6_titulo to R.string.legal_6_texto,
        R.string.legal_7_titulo to R.string.legal_7_texto,
    )
    Box(Modifier.fillMaxSize().background(Colores.Piedra.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth(0.82f)
                .fillMaxHeight(0.92f)
                .background(Colores.Piedra, FormaPlaca)
                .border(2.dp, Colores.Oro, FormaPlaca)
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("©  " + stringResource(R.string.legal_titulo), style = MaterialTheme.typography.headlineMedium,
                    color = Colores.Oro, modifier = Modifier.weight(1f))
                BotonMystara(stringResource(R.string.volver), onCerrar, Modifier.focusRequester(foco), estilo = EstiloBoton.DISCRETO)
            }
            Spacer(Modifier.height(8.dp))
            val desplazamiento = rememberScrollState()
            val alcance = rememberCoroutineScope()
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(desplazamiento)
                    .onPreviewKeyEvent { e ->
                        // La cruceta también mueve el texto
                        if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (e.key) {
                            Key.DirectionDown -> { alcance.launch { desplazamiento.animateScrollBy(160f) }; true }
                            Key.DirectionUp -> { alcance.launch { desplazamiento.animateScrollBy(-160f) }; true }
                            else -> false
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                secciones.forEach { (titulo, texto) ->
                    Text(stringResource(titulo), style = MaterialTheme.typography.titleMedium, color = Colores.OroClaro)
                    Text(stringResource(texto), style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

private typealias DefBoton = Pair<Pair<String, androidx.compose.ui.graphics.vector.ImageVector>, () -> Unit>

@Composable
private fun FilaBotones(izquierda: DefBoton, derecha: DefBoton) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(izquierda, derecha).forEach { (textoIcono, accion) ->
            BotonMystara(
                texto = textoIcono.first,
                icono = textoIcono.second,
                onClick = accion,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
