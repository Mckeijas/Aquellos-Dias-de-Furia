# Novedades de Días de Furia

Cada versión sube su número con `gradlew nuevaVersion` y se anota aquí.
Formato: MAYOR.MENOR.PARCHE (parche = correcciones, menor = funciones nuevas, mayor = hitos).
El historial del armazón (Leyendas de Mystara hasta la 0.14.9) está en `CHANGELOG-armazon.md`.

## 0.22.2 · 10/11/2026
- **«Multiplayer» pasa a llamarse «Multijugador»** en el menú principal y en todos los textos que lo nombran (ayuda de invitar, mensaje para compartir el código, ayuda de la pausa), como ya se llamaba la opción de la pausa.

## 0.22.1 · 10/11/2026
- **Multijugador: el invitado se conectaba pero no veía la imagen** (visto en la Odin 2 Portal, en la misma wifi). Cuatro salvaguardas:
  - **Decodificador que falla**: si el decodificador de vídeo del invitado daba un error una sola vez (por ejemplo con la primera imagen), el código lo ignoraba y seguía usándolo, y ya no pintaba nada en toda la partida; además, el hilo que pone las imágenes en pantalla se terminaba al primer error. Ahora se rehace el decodificador, se le vuelve a dar la configuración (SPS/PPS) y se pide al anfitrión una imagen completa, y el hilo sigue funcionando.
  - **Vigilante en el invitado**: si pasan 3 s sin poner ninguna imagen (y el anfitrión no está en pausa), pide una imagen completa al anfitrión, que además le reenvía la configuración del vídeo.
  - **El anfitrión insiste**: mientras un invitado espera una imagen completa, la pide al codificador cada medio segundo (hay codificadores que no hacen caso a la primera), y hace una completa cada 5 s en lugar de cada 10.
  - Fuera la clave de baja latencia de Qualcomm en el codificador del anfitrión que se añadió en la 0.21 sin poder probarla (sospechosa).
- **Diagnóstico** en la pausa del invitado: «Imágenes: N recibidas · M mostradas». Si llegan pero no se muestran, el fallo está en el invitado; si no llega ninguna, en el anfitrión o en la red.
- El protocolo no cambia (sigue en la versión 6), pero el arreglo necesita la 0.22.1 en los dos dispositivos.

## 0.22.0 · 04/11/2026
- **Arreglado el aviso «¿Continuar donde lo dejaste?»**:
  - **Se saltaba**: en la ruleta el juego se elige al *pulsar* A, el aviso aparecía con «Continuar» seleccionado y, al *soltar* A, ese mismo botón se daba por pulsado: el juego entraba sin dejar elegir. Ahora A solo cuenta si se pulsa con el aviso ya en pantalla.
  - **La cruceta se iba a la lista de detrás**: a veces el foco se quedaba en la ruleta y la cruceta movía los juegos del fondo. Mientras el aviso está abierto, la app recibe la cruceta, A y B antes que nadie y solo se mueve entre sus dos botones (`MandoDialogo` en `MainActivity.kt`).
- **Zoom en la galería** (y en los pantallazos): R2 amplía y L2 reduce (hasta ×5); en un teclado, + y −; en la pantalla, pellizcando con dos dedos. Con zoom, la cruceta o un dedo mueven la foto, L1/R1 siguen pasando de foto y B (o tocar) quita el zoom. Al cambiar de foto empieza sin zoom.
- **Salir cierra la app de verdad**: «Salir» cierra todas las pantallas, quita la app de las recientes y termina el proceso; antes se quedaba en segundo plano.

## 0.21.1 · 04/11/2026
- **Diálogo «¿Realmente quieres salir de la aplicación?»** (B o Atrás en la portada): con mando o teclado físico no se podía elegir Sí o No, solo tocando la pantalla. Dependía de que el foco cayera en el botón «No», y si no caía, la cruceta no tenía nada que mover. Ahora la portada lleva ella misma la opción elegida: izquierda/arriba = Sí, derecha/abajo = No, A o Enter confirma y B o Atrás = No. Un marco claro señala la opción elegida.

## 0.21.0 · 04/11/2026
- **Multijugador con menos retraso y sin tirones**:
  - La imagen se copia y se envía en cuanto el juego la dibuja. Antes se copiaba con un reloj propio de 62,5 veces por segundo, desacompasado con los 60 del juego: de vez en cuando salía una imagen repetida o se saltaba una (un tirón), y cada imagen esperaba de media unos 8 ms a que tocase copiarla. El reloj queda solo de respaldo.
  - Modo de baja latencia también en el codificador de vídeo de los chips Qualcomm (Odin 2 Portal, Retroid), como ya lo tenía el decodificador.
  - El sonido del invitado va como mucho 100 ms por detrás (antes 150 ms): va más a la par con la imagen.
- **Medir el retraso**: en la pausa del invitado sale ahora, además del retraso de la conexión, el **retraso de la imagen**: lo que tarda de media cada imagen desde que el anfitrión la copia del juego hasta que se ve en el invitado (comprimir, enviar y descomprimir). Los relojes de los dos dispositivos se igualan con el ping, como hace NTP. Para eso el anfitrión manda su hora en cada respuesta al ping: protocolo 6, anfitrión e invitados tienen que tener esta versión.
- **Historia**: estrenan PDF Altered Beast, Asterix, Bad Dudes, Burning Fight, Double Dragon II, Gang Wars, Night Slashers y Sengoku 2 (copiados de `extras\historia`). Ya están los 29.
- README de GitHub: fuera las «guías».

## 0.20.1 · 04/11/2026
- **Fuera la sección «Guías» de Extras.** Quedan Galería, Historia y Pantallazos. El lector de PDF y de texto sigue, porque lo usa Historia. La carpeta `assets/guias` (solo tenía el LEEME) queda en `_to_delete\guias`.

## 0.20.0 · 04/11/2026
- **Multijugador: «Esperando al anfitrión...»**. Cuando el anfitrión abre su menú de pausa (o sale a otra app), los invitados ven ese aviso en el centro de la pantalla, y se quita al volver a la partida. Quien entra con la partida ya en pausa también lo ve. Mensaje nuevo `PAUSA` en el protocolo, que pasa a la versión 5: anfitrión e invitados tienen que tener esta versión.

## 0.19.1 · 04/11/2026
- **Arreglado: Asterix, Captain America, Golden Axe II, Gang Wars, Metamorphic Force, Night Slashers y Violent Storm cerraban la app al arrancar.** Son los 7 juegos con trucos convertidos de MAME. Esos archivos empezaban con un comentario y una línea en blanco, y el FBNeo de libretro, al leer cualquier línea que no sea un comentario antes del primer truco, intenta apuntarla a un truco que todavía no existe y se cae (lo dice el informe de errores de la Odin 2 Portal: fallo en `strncpy` dentro de `retro_load_game`). Ahora los 7 `.ini` empiezan directamente por `cheat`, con saltos de línea de Windows como los oficiales, y `herramientas/trucos/mame_a_fbneo.py` ya los genera así.

## 0.19.0 · 03/11/2026
- **Multijugador: salir al menú ya no cierra la app.** Al salir de una partida en red (desde la pausa o al terminar), la app se cerraba de golpe: el sonido de los invitados se devolvía al núcleo FBNeo cuando LibretroDroid ya lo había descargado de memoria. Ahora la partida en red se cierra antes de cerrar el juego, y el código nativo (`cpp/audio_red.c`) comprueba que el núcleo sigue cargado antes de tocarlo. También se esperan los hilos del vídeo y del sonido antes de liberar el codificador, el decodificador y la pista de audio (otras dos formas de cerrar la app al salir, en el anfitrión y en el invitado).
- **Sonido en multijugador más fiable**:
  - El sonido va en su propia cola. Antes compartía una cola corta con el vídeo y, al vaciarla por un atasco de la wifi, se podía perder la configuración del sonido: ese invitado ya no oía nada en toda la partida.
  - Algunos dispositivos solo dan la configuración AAC como formato de salida del codificador (no como bloque aparte); ahora también se recoge y se envía.
  - Al volver de la pausa o de segundo plano se vuelve a enganchar la captura del sonido, por si LibretroDroid la había quitado.
  - LibretroDroid de 32 bits también se reconoce.
- **Sonido del invitado (visto en Moonwalker)**: en la 0.18, si se perdía el único aviso con la configuración del sonido, el invitado no oía nada en toda la partida. Además de la cola propia del sonido, ahora el anfitrión repite esa configuración cada 3 s (el invitado la ignora si ya la tiene), vuelve a enganchar la captura si pasa más de 1 s sin sonido con invitados conectados, y el invitado prueba el modo de audio normal si su dispositivo no admite el de baja latencia.
- **Menos texto durante la partida en red**: arriba a la izquierda ya no sale la frase con los jugadores, el código o el retraso, solo un icono con el número de jugadores (y, en el invitado, un punto verde, amarillo o rojo según el retraso). El detalle está en la pausa: el anfitrión tiene el botón «Quién está conectado» (con «Invitar jugadores» al lado) y el invitado lo ve en su menú, con su número de jugador y el retraso en milisegundos (`ui/componentes/IndicadorRed.kt`).
- **Unirse desde la pausa**: en la pausa, *Multijugador* tiene ahora también «Unirse a otra partida», con las partidas de la wifi y la casilla para el código. La partida que se estaba jugando se guarda (como al salir) y se entra como invitado.
- **Multijugador en casa más estable y con menos retraso**:
  - Tres colas por invitado con prioridad: control, sonido y vídeo.
  - El invitado recibe y decodifica en hilos distintos: antes, mientras el decodificador iba ocupado, no se leía la red (ni el sonido). Si el decodificador se queda atrás, se descarta lo atrasado y se pide al anfitrión una imagen completa (mensaje nuevo `PEDIR_CLAVE`; protocolo 4, los dos dispositivos tienen que tener esta versión).
  - Un fotograma clave cada 10 s en lugar de cada 3 s: eran el pico de datos que daba un tirón periódico.
  - Los paquetes se marcan como vídeo (DSCP AF41) en lugar de voz (EF): la cola de voz de la wifi no agrupa paquetes y se saturaba con el caudal del vídeo.
  - En casa: 4,5 Mb/s de salida (entre 1,5 y 7, se ajusta solo), cola de vídeo de 8 fotogramas y menos búfer en el sistema (64 KB), así se acumula menos retraso.

## 0.18.0 · 30/10/2026
- **Pantallas pequeñas y casi cuadradas** (Retroid Pocket Classic, móviles pequeños):
  - Si la pantalla mide menos de 680 dp de ancho, toda la interfaz se dibuja proporcionalmente más pequeña (hasta un 72 %), como en un móvil normal, en lugar de estrechar las columnas hasta partir el texto (`ui/componentes/Escala.kt`). En móviles y tabletas normales no cambia nada. Vale para los menús, la pausa del juego y la pantalla del invitado.
  - **Intro**: la ciudad ya no sale cortada por los dos lados. En pantallas estrechas se ve como mínimo el 92 % del ancho de la imagen, algo desplazada a la derecha para no perder el neón, y arriba y abajo se rellena con la misma imagen difuminada. Nubes, neón y farola usan el mismo encuadre (`ui/componentes/Portada.kt`).
  - **Ficha del juego**: la portada del juego ya no pasa del 40 % del ancho de la ficha (antes su ancho salía solo del alto y en una pantalla casi cuadrada dejaba los datos y las versiones en una columnita), y si la ficha es muy estrecha no se pone.
  - Los botones Todos / ★ Favoritos / Orden se desplazan de lado si no caben, sin mover el contador.

## 0.17.1 · 30/10/2026
- Ruleta de juegos: **«★ Favoritos» tiene su propio botón**, siempre a la vista junto a «Todos» y «Orden». «Todos» alterna ahora entre Todos y Listos, y Favoritos se enciende y se apaga con su botón. Con mando, Y sigue pasando por los tres (Todos › Listos › Favoritos).

## 0.17.0 · 30/10/2026
- **Neón «VIDEO GAMES» arreglado**: la V salía deformada (parecía una H) en la portada de la intro y en el fondo del menú. Se ha borrado y redibujado como un tubo de neón igual que las demás letras (`herramientas/portada/arreglar_v.py`), y se han rehecho las capas del neón apagado para que el parpadeo coincida.
- **Galería de Extras actualizada** desde `extras\arte`: Altered Beast pasa a 19 imágenes y estrenan galería Bad Dudes vs. Dragonninja (18) y Gang Wars (11).

## 0.16.0 · 30/10/2026
- **«Bezel del juego»**: nueva opción en *Marco de los laterales (bezel)*, tanto en Ajustes de audio y vídeo como en la pausa (R1 › Marco lateral). Cada partida usa el marco de su juego, hecho con su propia ilustración (el flyer de la ruleta partido por la mitad: una mitad a cada lado de la pantalla, con sombra hacia el juego). En la vista previa de Ajustes se ve con el último juego elegido en la ruleta. También lo ven los invitados en multijugador. Los 29 marcos están en `assets/bezels_juegos/` y se generan con `herramientas/bezels/bezels_juegos.py` (no se suben a GitHub).
- **Trucos revisados**: los 29 juegos tienen su archivo de trucos y todos se instalan para cualquier versión. Se ocultan los 6 trucos que FBNeo no puede activar porque tienen más de 127 opciones («Sound Test» de Captain Commando, Final Fight, Knights of the Round y Turtles in Time, y «Tamaño del jugador» de Mutation Nation): aparecían en la lista pero no hacían nada.

## 0.15.0 · 30/10/2026
- **Nuevo juego: Gang Wars** (Alpha Denshi / SNK, 1989, placa Alpha 68K, 2 jugadores, 3 botones). Ya son 29. Se aceptan sus versiones oficiales de FBNeo: `gangwars` (mundial), `gangwarsj` (Japón) y `gangwarsu` (EE. UU.); el bootleg `gangwarsb` no, como el resto de bootlegs.
- Con todo lo que tienen los demás: imagen de la ruleta (el flyer de SNK), vídeo de demostración, tabla de récords, 7 trucos (tiempo, vidas, energía y puntos de mejora infinitos; «Puntos de mejora infinitos» traducido) y reglas de la recreativa (dificultad, vidas 3/4 y velocidad del reloj).

## 0.14.1 · 30/10/2026
- El **ruido de la calle** de la intro suena un poco más alto (del 30 % al 45 % del volumen).

## 0.14.0 · 30/10/2026
- **La farola de la intro titila**: su luz cálida respira suavemente y, cada pocos segundos, tintinea con parpadeos flojos o, más rara vez, un bajón de tensión que casi la apaga y vuelve titubeando. La luz se refleja también en la acera mojada bajo la farola y sube y baja con ella. Son dos capas nuevas (`farola_luz.png` y `farola_apagada.png`) generadas con `herramientas/portada/farola.py`.
- Fuera el diagnóstico temporal de las nubes (marco y punto rojos): ya está confirmado que se mueven.

## 0.13.2 · 30/10/2026
- **Nubes mucho más visibles**: más opacas, con bordes más definidos y el borde iluminado en rosa; las de arriba, más claras. Bordes más suaves alrededor de la farola y la aguja.
- **Diagnóstico temporal (solo en compilaciones debug)**: un marco rojo alrededor de la zona de nubes y un punto rojo que la recorre con el mismo reloj que las mueve. Se quitará en cuanto se confirme que se mueven.

## 0.13.1 · 30/10/2026
- Los botones **Ruta** y **Refresca** suben a la línea del título «Juegos», arriba a la derecha. La cabecera de la ruleta queda con el filtro (Todos / Listos / ★ Favoritos), el orden y el contador, que ya caben bien.

## 0.13.0 · 30/10/2026
- **B / Atrás en el menú principal** ya no deja la app en segundo plano: vuelve a la portada (la intro, sin menú, con el ruido de la ciudad y sin la canción). Desde ahí, otra vez B / Atrás pregunta «¿Realmente quieres salir de la aplicación?»: Sí la cierra y No (elegido de entrada) vuelve a la portada. START sigue entrando al menú.
- **Vistas previas de los filtros**: estaban vacías (eran capturas de Leyendas de Mystara que no se copiaron). Ahora son de TMNT: Turtles in Time, con cada filtro imitado (nítido, bilineal, mejorado, CRT con líneas de barrido y LCD con rejilla), hechas con `herramientas/vistas/vistas.py`.
- **Marcos laterales (bezels)**: no había ninguno para elegir. Cuatro nuevos con el estilo de la app, hechos con `herramientas/bezels/bezels.py`: la portada desenfocada (por defecto), muro de ladrillo, tubos de neón y rayas ochenteras.

## 0.12.0 · 29/10/2026
- **Bad Dudes vs. Dragonninja** (Data East, 1988): 28 juegos y 156 versiones, con portada, trucos de FBNeo-cheats, tabla de récords y ajustes rápidos (dificultad y vidas, incluidas infinitas).
- **Vídeos de demostración para los 28 juegos** (`assets/videos/`, 29 MB): de los que había varios se ha elegido el más largo y de mejor calidad (p. ej. la partida de 30 s y no el clip de 10 s). Convertidos con `herramientas/videos.py` a 480×360, 30 fps, H.264 baseline, sin sonido y como mucho 30 s.
- Portada nueva de Altered Beast (el arte del lateral de la máquina, más grande).

## 0.11.0 · 29/10/2026
- **Altered Beast** (Sega, System 16B): 27 juegos y 154 versiones. Con sus trucos de FBNeo-cheats, tabla de récords y ajustes rápidos (dificultad, vidas —incluidas «infinitas»— y barras de energía).
- **Portadas de la ruleta** para los 6 juegos nuevos (Asterix, Burning Fight, Double Dragon II, Night Slashers, Sengoku 2 y Altered Beast).
- **Galería de Extras** de los 6 juegos nuevos: 75 imágenes convertidas a JPG ≤1600 px (también las .jfif, .webp y .avif).

## 0.10.0 · 29/10/2026
- **5 juegos nuevos (26 en total)**: Asterix (Konami), Burning Fight (SNK, Neo Geo), Double Dragon II: The Revenge (Technos), Night Slashers (Data East) y Sengoku 2 (SNK, Neo Geo). El catálogo pasa a 146 versiones; también se reconoce The Simpsons 2 jugadores (set 2).
- **Trucos para 6 juegos más**: Captain America, Golden Axe II, Metamorphic Force, Violent Storm, Asterix y Night Slashers, convertidos de los trucos de MAME (cheat.7z) con `herramientas/trucos/mame_a_fbneo.py` (solo los de RAM: energía, vidas, magia, créditos, tiempo, invencible…). Burning Fight, Double Dragon II y Sengoku 2 traen los de FBNeo-cheats. Algunos nombres más traducidos.
- **Ajustes rápidos de dificultad y vidas** (Trucos › Reglas de la recreativa) para Captain America, Captain Commando, Final Fight, Knights of the Round (frecuencia y fuerza del ataque enemigo), Double Dragon, Double Dragon II, Golden Axe (con barras de energía), Hook y Moonwalker (con vitalidad). Sacados de las listas DIP de FBNeo con `herramientas/reglas.py`. En el resto, la dificultad está en el menú de servicio de la placa y la app lo explica.
- **¡Al azar!**: botón bajo la ruleta (y R2 con el mando) que la hace girar unas vueltas y frenar en un juego al azar del filtro actual.
- **Favoritos**: estrella en la ficha (o L2 en la ruleta), ★ en la lista y filtro «★ Favoritos» junto a Todos y Listos.
- **Estadísticas en la ficha**: partidas, tiempo jugado (solo con el juego en marcha, sin contar la pausa) y última vez. **Récord** de la máquina en los juegos cuya tabla lo guarda aparte (Final Fight, Captain Commando, Knights of the Round, Warriors of Fate, The Punisher, Double Dragon y Double Dragon II).
- **Vídeo de demostración** en la ficha: si hay `assets/videos/<id>.mp4`, al quedarse un momento en un juego se reproduce en silencio y en bucle en lugar de la portada. `herramientas/videos.py` los prepara.
- **Arreglado: las tablas de récords no se guardaban** en ningún juego, porque `hiscore.dat` seguía siendo el de Leyendas de Mystara. Ahora es un extracto del de FBNeo con las 146 versiones (`herramientas/hiscore.py`; 128 tienen tabla).
- **Ambiente de la intro**: ahora es «City noise» (freesound_community, Pixabay), montado en bucle sin corte.
- Aviso legal con los titulares de los juegos nuevos.

## 0.9.0 · 29/10/2026
- **Logo de Patrulla Retro al abrir la app**: negro → fundido al logo (grande, en el centro) → fundido a negro → intro. Tocar o pulsar un botón lo acorta. Los fundidos van por fotogramas, así funcionan aunque las animaciones del sistema estén desactivadas.
- **Nubes de la intro rehechas otra vez** (seguían sin verse moverse): ahora solo se pintan imágenes con transparencia una encima de otra, sin máscaras, modos de fusión ni bitmaps que se reescriben. Encima de las nubes va `nubes_delante.png` (la propia portada recortada, opaca en la farola, la aguja y los edificios), así pasan por detrás. Además se ven más: arriba, donde el cielo es oscuro, salen iluminadas por la ciudad.
- Las nubes también se mueven en el **fondo del menú principal**.

## 0.8.1 · 27/10/2026
- **Listo para GitHub** (https://github.com/Mckeijas/dias-de-furia): `subir_a_github.bat` en la carpeta del proyecto crea el repositorio local la primera vez, lo enlaza con GitHub y sube; las siguientes veces guarda los cambios con la versión como mensaje. Antes de subir comprueba que no vaya ninguna ROM, firma ni núcleo.
- `.gitignore` en la carpeta del proyecto que solo deja pasar `proyecto-android/`, `herramientas/` y `README.md` (las ROMs, `extras`, el armazón, las versiones compiladas y las imágenes sueltas se quedan en el PC); `.kotlin/` también fuera.
- `README.md` para la página del repositorio.

## 0.8.0 · 27/10/2026
- **Pantallazos de logros**: nueva pestaña «Pantallazos» en Extras. Al saltar un logro de RetroAchievements se captura la partida (720 px de alto, con la proporción con que se ve) y se le pinta encima el aviso del logro con su icono, nombre, descripción y puntos, más el nombre de la app y la fecha. Se ven en cuadrícula (del más reciente al más antiguo) y a pantalla completa, con el juego, la fecha y los puntos; se pueden borrar con «Borrar» o Y.
- Corregido el texto de «Extras» sin imágenes, que estaba repetido dentro de sí mismo en `strings.xml`.

## 0.7.1 · 27/10/2026
- Menú principal, abajo a la derecha: solo el número de versión (sin la fecha) y el autor, «v0.7.1 · Mckeijas», más pequeño y translúcido.

## 0.7.0 · 27/10/2026
- **Botones TEST y SERVICE en el menú de servicio** de la recreativa: X (botón 3) = TEST e Y (botón 4) = SERVICE, con el mando y con los botones táctiles 3 y 4. Arriba se ve un recordatorio; al abrir la pausa, X e Y vuelven a ser los botones del juego. SERVICE llega a la placa solo donde FBNeo lo conecta (Sega System 32, como Golden Axe II).
- **Nubes de la intro reescritas**: no se veían moverse. Ahora la mezcla de nubes y máscara se hace píxel a píxel en un bitmap pequeño, sin modos de fusión ni capas fuera de pantalla, que en algunas GPU no funcionaban.

## 0.6.3 · 27/10/2026
- Nubes de la intro **mucho más rápidas** (una vuelta en 30 s en lugar de 160 s) y movidas con un reloj propio por fotogramas: se mueven aunque las animaciones del sistema estén desactivadas.

## 0.6.2 · 27/10/2026
- Nubes de la intro algo más grandes (bancos anchos en lugar de jirones pequeños).

## 0.6.1 · 27/10/2026
- **Nubes horizontales**: los jirones que cruzan el cielo ahora son alargados en horizontal (estratos), no «lanzas» verticales, y hay más.
- Contador de la ruleta («13/21») con letra más pequeña y siempre en una línea.
- **Sonido de START** nuevo: golpe seco en un cubo de basura metálico, con eco (archivo aportado, generado con ElevenLabs; sustituye al puñetazo sintetizado). Sigue llamándose `res/raw/punetazo.ogg`.

## 0.6.0 · 27/10/2026
- El **neón empieza a parpadear nada más abrirse la app**: arranca apagado y se enciende a trompicones, y enseguida vuelve a fallar.
- **Nubes en movimiento** en la intro: jirones de nube cruzan despacio el cielo del fondo, por detrás de la farola, la aguja, la escalera de incendios y los edificios (`nubes_cielo.png` + `nubes_mascara.png`, generadas con `herramientas/portada/nubes.py`).
- Ambiente de la intro: **bocinas más presentes** (antes apenas se oían) y **dos ambulancias** pasando por el fondo con la sirena (ulular y «yelp», efecto Doppler y eco de calle).
- **Versiones en la ficha de Juegos**: en los juegos de nombre largo (Golden Axe 2, Moonwalker, TMNT: Turtles in Time, Cadillacs and Dinosaurs, Captain America and the Avengers) el título ocupaba dos líneas y la fila de versiones quedaba fuera de la ficha. Ahora las versiones van fijas abajo y siempre se ven; los datos de arriba se desplazan si no caben y los nombres largos van algo más pequeños.

## 0.5.1 · 27/10/2026
- Ambiente de la intro: más bocinas (toques cortos, dobles, una larga y alguna más cercana) y volumen más bajo (0,32 → 0,22).

## 0.5.0 · 27/10/2026
- **Intro con ambiente**: suena bajito el tráfico de la ciudad de noche (coches sobre asfalto mojado, rumor lejano, alguna bocina y una sirena a lo lejos; `res/raw/ambiente_trafico.ogg`, sintetizado para el proyecto, en bucle).
- **Neón que falla**: el letrero «ARCADE / VIDEO GAMES» de la portada parpadea de forma irregular, cada tubo por su cuenta (ráfagas de chispazos, apagones que vuelven tartamudeando y bajones de tensión). Son dos capas `neon_*_apagado.png` encima de la portada, generadas con `herramientas/portada/neon.py`.
- Al pulsar START se apaga el ambiente, suena el puñetazo y en el menú empieza la música.
- Scripts de los sonidos en `herramientas/audio/` (puñetazo y tráfico).

## 0.4.2 · 27/10/2026
- Corregido error de compilación en la intro (faltaban los imports de `coroutineScope` y `launch` del golpe al pulsar START).

## 0.4.1 · 27/10/2026
- **Firma propia de Días de Furia** (`firma/dias-de-furia.jks`, alias `diasdefuria`, RSA 4096, hasta 2054), distinta de la de Leyendas de Mystara. Las release ya no se firman con la clave de depuración. La clave y `firma.properties` no se suben a GitHub.

## 0.4.0 · 27/10/2026
- **Botones especiales «Ruta» y «Refresca»** en la ruleta de Juegos, junto al de orden (mismo formato los dos: pegatina roja con contorno amarillo, icono y letras Bangers).
  - **Ruta**: elige la carpeta del móvil con las ROMs. El permiso se guarda, así que solo se elige una vez, y al elegirla se importa lo que haya.
  - **Refresca** (también SELECT en el mando): importa todos los .zip y .key de la carpeta y sus subcarpetas. Reconoce cada archivo por su contenido como siempre, conserva todas las versiones de cada juego y solo copia lo nuevo o cambiado. Al terminar muestra un resumen y el veredicto de cada juego.

## 0.3.2 · 27/10/2026
- La **intro espera a que se pulse START**: ya no pasa sola al menú a los 8 s.
- La misma imagen sirve para la intro y el fondo del menú.

## 0.3.1 · 27/10/2026
- **Intro sin música**: al pulsar START (o tocar) suena un puñetazo (`res/raw/punetazo.ogg`, sintetizado para el proyecto), la imagen da un golpe con destello y temblor y se pasa al menú. Sustituye al sonido de revelación.
- La **música** empieza al llegar al menú y sigue en todas sus pantallas.
- **Portada y fondo del menú provisionales**: nueva imagen del callejón con «DÍAS DE FURIA» en el muro y el neón «ARCADE».

## 0.3.0 · 27/10/2026
- **Estética callejera ochentera** en toda la app: asfalto casi negro, amarillo, naranja y rojo de spray, blanco tiza; títulos y botones con Bangers y textos con Barlow Condensed (OFL).
- **Portada nueva** (intro y fondo del menú): callejón de noche con el muro de ladrillo en escorzo y «DÍAS DE FURIA» pintado con spray (halo azul, contorno negro, degradado amarillo-rojo y chorretones), cubo de basura, charcos, vapor, farola, escalera de incendios y un neón «ARCADE». En el menú el rótulo queda a la izquierda, a la vista junto a los botones, y el panel ya no repite el título.
- **Música**: versión metal del tema de Golden Axe (`res/raw/musica_menu.m4a`); empieza con la intro y sigue en el menú. La intro dura 8 s y dice «PULSA START».
- **Icono** de la app: el puño pintado en el muro.

## 0.2.0 · 27/10/2026
- **Días de Furia**: paquete `com.diasdefuria`, nombre de la app, APK `DiasDeFuria-v….apk`, icono «DF» y menú/intro provisionales (collage de portadas con el título).
- **21 beat 'em up** en la ruleta: Alien vs. Predator, Armored Warriors, Cadillacs and Dinosaurs, Captain America and the Avengers, Captain Commando, Double Dragon, Final Fight, Golden Axe, Golden Axe: The Revenge of Death Adder, Hook, Knights of the Round, Metamorphic Force, Moonwalker, Mutation Nation, TMNT: Turtles in Time, The Punisher, The Simpsons, Vendetta, Violent Storm, Warriors of Fate y X-Men.
- **Catálogo de ROMs para cualquier placa** (CPS-1, CPS-2, Konami, Sega, Irem, Data East, Technos, Neo Geo): `herramientas/catalogo.py` lee todo FBNeo y genera 131 versiones oficiales (sin bootlegs ni hacks), una función por versión para no pasar el límite de la JVM.
- Importación más lista:
  - Reconoce los ZIP «split» de los clones (solo traen lo que los distingue del padre) además de los completos y los «merged».
  - Recorta los archivos «sobredumpeados» (el doble de tamaño y repetidos), como los avp.14m–20m de 2 MB de Alien vs. Predator.
  - **BIOS de Neo Geo**: si el ZIP del juego la trae dentro, se guarda aparte como neogeo.zip; también se puede importar neogeo.zip suelto. Si falta, la ficha lo explica.
- Ficha de la ruleta con la portada en vertical a la izquierda (fondo de la misma imagen) y los datos en columna: fabricante, año, placa, género, jugadores, botones, ROM, versión y tamaño. Las versiones importadas salen primero.
- Sin preview propia, la ruleta usa la primera imagen de la galería del juego.
- **Extras por juego**: Galería con un selector de juego (495 imágenes convertidas a JPG ≤1600 px, también las .avif/.jfif/.webp), Guías e Historia (un PDF por juego, `historia/<id>.pdf`, titulado con el nombre del juego).
- Controles genéricos: Botón 1 · Golpe, Botón 2 · Salto, Botón 3 y Botón 4 (solo en los juegos que los tienen) y Especial (1 + 2). Los táctiles muestran el tercer botón en los juegos de 3.
- Trucos de FBNeo-cheats para 17 de los 21 juegos (no hay para captaven, ga2, metamrph ni viostorm) y más traducciones de trucos.
- Sin reglas DIP de momento (eran de King of Dragons). Aviso legal con los titulares de los nuevos juegos.

## 0.1.0 · 27/10/2026
- Proyecto nuevo a partir del armazón de Leyendas de Mystara v0.14.9, preparado para muchos juegos.
- **Ruleta de selección** (sustituye a las tres fichas fijas de «Leyendas», sección «Juegos»):
  - Izquierda: lista de juegos que gira con la cruceta (mantener pulsado para correr), con L1/R1 (saltan de letra en orden A–Z, o de 10 en 10) o arrastrando con el dedo. Da la vuelta al llegar al final cuando hay 8 juegos o más. Cada fila marca si el juego está listo (✔), incompleto (⚠) o sin importar.
  - Derecha: ficha del juego del centro con su imagen, fabricante · año · placa · género · jugadores, la ROM importada (ZIP, región y tamaño) y las versiones (regiones) que se aceptan. Una versión importada se toca para jugar con ella; una sin importar abre el importador.
  - Botones Jugar / Cambiar ZIP, Completar o Importar ROM. A (o Start) en la ruleta hace la acción principal; derecha pasa a los botones.
  - Filtro Todos / Listos (Y) y orden lista / A–Z / año (X). Se recuerdan el último juego, el filtro y el orden.
- `Juego.kt`: el nombre va como texto (sin strings.xml) y cada juego lleva fabricante, año, placa, género y jugadores. La imagen sale de `assets/previews/<id>.jpg|png|webp` y es opcional: sin ella se dibuja una placa con el nombre.
- Quitadas las referencias fijas a King of Dragons / Tower of Doom: juego por defecto = el primero de la lista, reglas DIP por id y aviso de multijugador como campo del juego.
- Partidas guardadas: solo se listan los juegos que tienen alguna partida.
- La sección Multiplayer pasa a su propio archivo (`PantallaMultijugador.kt`).
