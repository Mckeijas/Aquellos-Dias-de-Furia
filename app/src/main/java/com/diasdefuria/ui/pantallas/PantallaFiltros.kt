package com.diasdefuria.ui.pantallas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.runtime.mutableStateOf
import com.diasdefuria.datos.cargarArte
import com.diasdefuria.ui.componentes.BotonMystara
import com.diasdefuria.ui.componentes.EstiloBoton
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.diasdefuria.R
import com.diasdefuria.datos.Aspecto
import com.diasdefuria.datos.Bezels
import com.diasdefuria.datos.Juego
import com.diasdefuria.datos.ClaveAjuste
import com.diasdefuria.datos.Filtro
import com.diasdefuria.datos.rememberAjuste
import com.diasdefuria.datos.rememberAjusteAspecto
import com.diasdefuria.datos.rememberAjusteBezel
import com.diasdefuria.datos.rememberAjusteFiltro
import com.diasdefuria.ui.componentes.FilaInterruptor
import com.diasdefuria.ui.componentes.FilaDeslizador
import com.diasdefuria.audio.MusicaMenu
import com.diasdefuria.ui.componentes.FormaPlaca
import com.diasdefuria.ui.componentes.PantallaSeccion
import com.diasdefuria.ui.componentes.Subtitulo
import com.diasdefuria.ui.tema.Colores

/**
 * Filtros y mejoras gráficas. A la izquierda las opciones; a la derecha una vista
 * previa de cómo quedará la pantalla de juego (proporción + bezel).
 * Los ajustes se guardan al momento; la pantalla de juego los aplicará.
 */
@Composable
fun PantallaFiltros(onVolver: () -> Unit) {
    val context = LocalContext.current
    var filtro by rememberAjusteFiltro()
    var aspecto by rememberAjusteAspecto()
    var bezel by rememberAjusteBezel()
    var escaladoEntero by rememberAjuste(ClaveAjuste.ESCALADO_ENTERO, false)
    val bezels = remember { Bezels.disponibles(context) }
    var musica by rememberAjuste(ClaveAjuste.MUSICA, true)
    var volumen by rememberAjuste(ClaveAjuste.VOLUMEN_MUSICA, 0.7f)
    var efectos by rememberAjuste(ClaveAjuste.EFECTOS, true)
    var eligiendoImagen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
    PantallaSeccion(stringResource(R.string.filtros_titulo), onVolver, desplazable = false) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            // Opciones
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                Subtitulo(stringResource(R.string.filtros_aspecto))
                OpcionRadio(stringResource(R.string.aspecto_4_3), stringResource(R.string.aspecto_4_3_detalle),
                    aspecto == Aspecto.ARCADE_4_3) { aspecto = Aspecto.ARCADE_4_3 }
                OpcionRadio(stringResource(R.string.aspecto_panoramico), stringResource(R.string.aspecto_panoramico_detalle),
                    aspecto == Aspecto.PANORAMICO) { aspecto = Aspecto.PANORAMICO }

                Subtitulo(stringResource(R.string.filtros_shader))
                listOf(
                    Triple(Filtro.NINGUNO, R.string.filtro_ninguno, R.string.filtro_ninguno_detalle),
                    Triple(Filtro.MEJORADO, R.string.filtro_mejorado, R.string.filtro_mejorado_detalle),
                    Triple(Filtro.BILINEAL, R.string.filtro_bilineal, R.string.filtro_bilineal_detalle),
                    Triple(Filtro.CRT, R.string.filtro_crt, R.string.filtro_crt_detalle),
                    Triple(Filtro.LCD, R.string.filtro_lcd, R.string.filtro_lcd_detalle),
                ).forEach { (opcion, texto, detalle) ->
                    OpcionRadio(stringResource(texto), stringResource(detalle), filtro == opcion) { filtro = opcion }
                }

                Subtitulo(stringResource(R.string.filtros_otros))
                FilaInterruptor(stringResource(R.string.filtros_escalado_entero), escaladoEntero) { escaladoEntero = it }

                Subtitulo(stringResource(R.string.sonido_titulo))
                if (MusicaMenu.disponible(context)) {
                    FilaInterruptor(stringResource(R.string.sonido_musica), musica) {
                        musica = it
                        if (it) MusicaMenu.reproducir(context, volumen) else MusicaMenu.pausar()
                    }
                    FilaDeslizador(stringResource(R.string.sonido_volumen), volumen, {
                        volumen = it
                        MusicaMenu.cambiarVolumen(it)
                    }, habilitado = musica)
                } else {
                    Text(stringResource(R.string.sonido_sin_musica), style = MaterialTheme.typography.labelMedium,
                        color = Colores.TextoSuave)
                }
                FilaInterruptor(stringResource(R.string.sonido_efectos), efectos) { efectos = it }
            }

            // Vista previa y elección de bezel
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                Subtitulo(stringResource(R.string.filtros_bezel))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    bezels.forEach { b ->
                        MiniaturaBezel(b, seleccionado = b == bezel) { bezel = b }
                    }
                    // La imagen de la galería elegida aparece también en la fila
                    if (Bezels.esDeGaleria(bezel)) MiniaturaBezel(bezel, seleccionado = true) {}
                }
                BotonMystara(stringResource(R.string.bezel_elegir_imagen), { eligiendoImagen = true },
                    Modifier.padding(top = 8.dp), icono = Icons.Filled.Collections)
                Text(
                    stringResource(R.string.filtros_bezel_ayuda),
                    style = MaterialTheme.typography.labelMedium,
                    color = Colores.TextoSuave,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Spacer(Modifier.height(6.dp))
                Subtitulo(stringResource(R.string.filtros_vista_previa))
                VistaPreviaPantalla(aspecto, bezel, filtro)
                Spacer(Modifier.height(6.dp))
                Subtitulo(stringResource(R.string.filtros_detalle))
                DetalleFiltro(filtro)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
    if (eligiendoImagen) {
        SelectorImagenBezel(onElegir = { bezel = it; eligiendoImagen = false }, onCerrar = { eligiendoImagen = false })
    }
    }
}

/** Elegir cualquier imagen de Extras (la galería de cualquier juego) como fondo lateral. */
@Composable
private fun SelectorImagenBezel(onElegir: (String) -> Unit, onCerrar: () -> Unit) {
    val context = LocalContext.current
    val imagenes = remember { cargarArte(context) }
    BackHandler(onBack = onCerrar)
    Column(
        Modifier.fillMaxSize().background(Colores.Piedra.copy(alpha = 0.97f)).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.bezel_elegir_imagen), style = MaterialTheme.typography.headlineMedium,
                color = Colores.Oro, modifier = Modifier.weight(1f))
            BotonMystara(stringResource(R.string.volver), onCerrar, estilo = EstiloBoton.DISCRETO)
        }
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(imagenes, key = { it.uri }) { img ->
                AsyncImage(
                    model = img.uri,
                    contentDescription = img.archivo,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .aspectRatio(20f / 9f)
                        .border(1.5.dp, Colores.OroOscuro, FormaPlaca)
                        .clickable { onElegir(Bezels.PREFIJO_ARTE + img.carpeta + "/" + img.archivo) },
                )
            }
        }
    }
}

@Composable
private fun OpcionRadio(texto: String, detalle: String?, seleccionado: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = seleccionado,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = Colores.Oro),
        )
        Column(Modifier.padding(start = 8.dp)) {
            Text(texto, style = MaterialTheme.typography.bodyLarge)
            if (detalle != null) {
                Text(detalle, style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave)
            }
        }
    }
}

/**
 * Fondo de bezel: negro o una imagen. Lo usa también la pantalla de juego. [juego] es el que se
 * juega, para «Bezel del juego»; si no se pasa, se usa el último elegido en la ruleta.
 */
@Composable
fun FondoBezel(bezel: String, modifier: Modifier = Modifier, juego: Juego? = null) {
    val context = LocalContext.current
    val muestra = remember { Bezels.juegoDeMuestra(context) }
    val uri = Bezels.uriAsset(bezel, juego ?: muestra)
    if (uri == null) {
        Box(modifier.background(Color.Black))
    } else {
        Box(modifier.background(Color.Black)) {
            AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            // Las imágenes de la galería se oscurecen un poco para que no distraigan del juego
            if (Bezels.esDeGaleria(bezel)) Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.35f)))
        }
    }
}

/**
 * Simula una pantalla de móvil apaisado (20:9) con la imagen del juego centrada
 * en la proporción elegida y el bezel ocupando los huecos laterales.
 */
@Composable
private fun VistaPreviaPantalla(aspecto: Aspecto, bezel: String, filtro: Filtro) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(20f / 9f)
            .border(1.5.dp, Colores.OroOscuro, FormaPlaca)
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        FondoBezel(bezel, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxHeight()
                .aspectRatio(aspecto.proporcion)
                .background(Color.Black)
                .border(1.dp, Colores.Oro),
            contentAlignment = Alignment.TopEnd,
        ) {
            // Captura real del juego con el filtro elegido (imagen 4:3 estirada como en el juego)
            AsyncImage(
                model = "file:///android_asset/vistas/${filtro.name.lowercase()}.webp",
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                text = if (aspecto == Aspecto.ARCADE_4_3) "4:3" else "≈16:9",
                style = MaterialTheme.typography.labelMedium,
                color = Colores.OroClaro,
                modifier = Modifier.padding(4.dp).background(Colores.Piedra.copy(alpha = 0.7f), FormaPlaca).padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}

/** Ampliación de los personajes para ver bien la diferencia entre filtros. */
@Composable
private fun DetalleFiltro(filtro: Filtro) {
    AsyncImage(
        model = "file:///android_asset/vistas/${filtro.name.lowercase()}_detalle.webp",
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        filterQuality = FilterQuality.None,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(192f / 112f)
            .border(1.5.dp, Colores.OroOscuro, FormaPlaca)
            .padding(3.dp),
    )
}

@Composable
fun MiniaturaBezel(bezel: String, seleccionado: Boolean, juego: Juego? = null, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FondoBezel(
            bezel,
            juego = juego,
            modifier =
            Modifier
                .width(96.dp)
                .aspectRatio(20f / 9f)
                .border(if (seleccionado) 3.dp else 1.dp, if (seleccionado) Colores.OroClaro else Colores.OroOscuro, FormaPlaca)
                .clickable(role = Role.RadioButton, onClick = onClick),
        )
        Text(
            text = nombreBezel(bezel),
            style = MaterialTheme.typography.labelMedium,
            color = if (seleccionado) Colores.OroClaro else Colores.TextoSuave,
            modifier = Modifier.width(96.dp),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun nombreBezel(bezel: String): String =
    if (bezel == Bezels.NEGRO) stringResource(R.string.bezel_negro)
    else if (bezel == Bezels.DEL_JUEGO) stringResource(R.string.bezel_del_juego)
    else if (Bezels.esDeGaleria(bezel)) stringResource(R.string.bezel_de_galeria)
    else bezel.removePrefix(Bezels.PREFIJO_ASSET).substringBeforeLast('.')
        .replace(Regex("^\\d+_"), "").replace('_', ' ').replaceFirstChar { it.uppercase() }
