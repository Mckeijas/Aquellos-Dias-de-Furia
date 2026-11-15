# Días de Furia

Launcher para Android con 29 beat 'em up arcade (Capcom, Konami, Sega, Irem, Data East, Technos, SNK y Alpha Denshi) y emulador integrado (FBNeo). Los juegos se eligen en una ruleta; cada uno tiene su ficha, su galería de Extras y su historia.

**Estado:** ver `version.properties` y `CHANGELOG.md`. Está hecho a partir del armazón de *Leyendas de Mystara* (`../ARMAZON.md`).

## ROMs

La app no trae ninguna: se importan desde la ficha de cada juego (se reconocen por CRC, no por el nombre). Las versiones que se aceptan están en `datos/CatalogoRomsets.kt`, generado con `herramientas/catalogo.py` (lee FBNeo). Notas:

- **Alien vs. Predator** (avsp, avsph) necesita su clave `.key` de 20 bytes; si la tienes suelta, elígela junto al ZIP.
- **Mutation Nation** necesita la BIOS de Neo Geo: vale `neogeo.zip` o un ZIP del juego que la lleve dentro (la app la guarda aparte como neogeo.zip).

## Núcleo del emulador (obligatorio, una sola vez)

La emulación usa el núcleo **FBNeo para libretro**, que no va en el repositorio:

1. Descarga https://buildbot.libretro.com/nightly/android/latest/arm64-v8a/fbneo_libretro_android.so.zip
2. Descomprímelo y renombra `fbneo_libretro_android.so` a **`libfbneo_libretro_android.so`**.
3. Cópialo en `app/src/main/jniLibs/arm64-v8a/` y compila.

Si falta, la app funciona igual pero al pulsar «Jugar a…» avisa de que no está.

## Abrir y compilar

1. Abre esta carpeta con **Android Studio** (Ladybug o posterior): *File › Open*.
2. Espera a que termine la sincronización de Gradle (descarga lo necesario la primera vez).
3. Conecta el móvil con la depuración USB activada y pulsa **Run ▶**.

Requisitos: Android 7.0 o superior. La app se abre siempre en horizontal y en pantalla completa.

## Versiones y firma

**Versión.** Está en `version.properties` y se muestra abajo a la derecha del menú junto al autor, por ejemplo `v0.7.1 · Mckeijas`. Para cada actualización:

1. Sube la versión desde la terminal de Android Studio:
   - `gradlew nuevaVersion` → correcciones (0.2.0 → 0.2.1)
   - `gradlew nuevaVersion -Ptipo=menor` → funciones nuevas (0.2.1 → 0.3.0)
   - `gradlew nuevaVersion -Ptipo=mayor` → hitos (0.3.0 → 1.0.0)
2. Anota los cambios en `CHANGELOG.md`.
3. Compila. El APK sale solo con la versión en el nombre: `DiasDeFuria-v0.3.0.apk`, y el menú muestra solo la versión y el autor («v0.7.1 · Mckeijas»).

**Firma.** Días de Furia tiene su propia clave, distinta de la de Leyendas de Mystara: `firma\dias-de-furia.jks` (alias `diasdefuria`, RSA 4096, válida hasta 2054), con las contraseñas en `firma.properties`. Ya está creada y configurada; las versiones release se firman solas con ella.

Huella SHA-256 del certificado: `24:BF:EB:04:DC:C9:82:D1:D5:5B:6E:C4:20:72:5D:E7:5A:38:16:5F:90:DE:0C:E0:3A:19:13:66:B3:0E:60:0D`

Si alguna vez hiciera falta crearla de nuevo (por ejemplo en otro PC sin copia):
1. Ejecuta `crear_firma.bat`. Te pide una contraseña y tus datos, y crea `firma\dias-de-furia.jks` (no sobrescribe la existente).
2. Copia `firma.properties.ejemplo` como `firma.properties` y pon tus contraseñas.

Para compilar la versión release: *Build › Generate Signed App Bundle / APK* o `gradlew assembleRelease`.

Guarda una copia del `.jks` y de las contraseñas fuera del PC: sin ellos no podrás publicar actualizaciones que se instalen encima de las anteriores. Ni la clave ni `firma.properties` se suben a GitHub.

Un APK firmado con tu clave no se instala encima de uno de depuración (y al revés): la primera vez que cambies de uno a otro, desinstala la app de la Odin.

## GitHub

Repositorio: https://github.com/Mckeijas/dias-de-furia. Se sube desde el PC con **`subir_a_github.bat`** (en la carpeta del proyecto, junto a `proyecto-android`): la primera vez crea el repositorio local y lo enlaza con GitHub; las siguientes, guarda los cambios con el número de versión como mensaje y los sube. Antes de subir comprueba que no se cuele ninguna ROM, clave de firma ni núcleo `.so`.

El `.gitignore` de la carpeta del proyecto (`Dias de Furia\.gitignore`) solo deja pasar `proyecto-android/`, `herramientas/`, `README.md` y el propio `.gitignore`: las ROMs, `extras`, el armazón, las versiones compiladas y las imágenes sueltas se quedan en el PC. Dentro, el `.gitignore` de `proyecto-android` deja fuera las ROMs, todo el arte de los juegos (y lo derivado de él: fondo, portada, galería y bezels) y la firma. Quien clone el repositorio tiene que poner sus propias imágenes con los mismos nombres para que compile: en `res/drawable-nodpi/` `fondo_menu.jpg`, `portada_leyendas.jpg`, `icono_fondo.png`, `neon_arcade_apagado.png`, `neon_video_apagado.png`, `nubes_cielo.png`, `nubes_delante.png`, `farola_luz.png` y `farola_apagada.png`, y los `ic_launcher.png` de `res/mipmap-*`. Las imágenes de la ruleta (`assets/previews/<id>.jpg`) son opcionales: sin ellas se compila igual.

## Importar ROMs

En **Juegos**, cada ficha tiene «Importar ROM», que permite elegir **varios archivos a la vez** (ZIP padre, clon, clave `.key` o `neogeo.zip`). La app reconoce cada ZIP por su contenido (CRC de cada archivo, según las listas de FBNeo), no por su nombre, y guarda una copia con el nombre que espera el emulador. Tus archivos originales no se tocan.

- Como FBNeo, una versión está completa si entre su ZIP y el del padre están todos los archivos. Si falta algo, el botón pasa a **«Completar»** y explica qué falta.
- Los chips de **Versiones** muestran todas las regiones que se aceptan: las importadas se tocan para jugar con ellas, las demás abren el importador.
- Juegos con versión Hispanic (en español): Alien vs. Predator (`avsph`) y The Punisher (`punisherh`). Se prefiere esa si está importada.
- Para regenerar el catálogo tras cambiar de juegos: `python herramientas/catalogo.py <clon de FBNeo> kotlin <…>/datos/CatalogoRomsets.kt` (y `… comparar zip1 zip2…` para comprobar ZIPs reales).

**Importar una carpeta entera**: arriba a la derecha de la pantalla Juegos, en la línea del título, están los botones especiales **Ruta** y **Refresca**.
- **Ruta** elige la carpeta del móvil donde tienes las ROMs (selector de carpetas de Android). Se elige una vez: el permiso se guarda. Al elegirla se importa ya lo que haya.
- **Refresca** (o SELECT en el mando) importa todos los `.zip` y `.key` de esa carpeta y sus subcarpetas (3 niveles). No borra otras versiones del mismo juego, y solo copia lo nuevo o cambiado (o lo que se borró de la app). Sin ruta elegida, abre el selector de carpeta.
- El código está en `datos/CarpetaRoms.kt`.

## Menú de servicio de la recreativa

Desde la pausa, «Configuración de la recreativa» abre el menú de servicio (test) de la placa: la app pulsa Select + L + R, que es la combinación configurada en FBNeo (`fbneo-diagnostic-input`). Dentro:
- La cruceta y Atacar mueven y eligen, como en la recreativa.
- **X (botón 3) = TEST** de la placa. FBNeo lo pulsa con esa misma combinación, que la app mantiene mientras se mantenga X.
- **Y (botón 4) = SERVICE** de la placa. FBNeo solo lo conecta (a R3) en las placas que lo necesitan para moverse por el menú, como Sega System 32 (Golden Axe II). En las demás no hace nada, porque el núcleo no lo conecta a ningún botón.
- Arriba se ve un recordatorio. Al abrir la pausa, X e Y vuelven a ser los botones 3 y 4 del juego.

## Pantalla: proporción y bezels

En **Filtros y mejoras** se elige la proporción, siempre sin deformar la imagen:

- **4:3** · como se veía en el monitor de la recreativa.
- **16:9 · píxeles cuadrados** · la imagen original de 384×224 (12:7, casi 16:9).

El espacio que sobra a los lados se rellena con el **bezel** elegido (negro o una imagen). Para añadir bezels, copia imágenes en `app/src/main/assets/bezels/`; el número inicial del nombre fija el orden (`01_…`, `02_…`). Hay una vista previa en la misma pantalla.

La opción **«Bezel del juego»** pone en cada partida el marco de ese juego: `assets/bezels_juegos/<id>.jpg`, hecho con su ilustración de la ruleta partida por la mitad (mitad izquierda a la izquierda, mitad derecha a la derecha). Se generan con `python herramientas/bezels/bezels_juegos.py app/src/main/assets/previews app/src/main/assets/bezels_juegos`; al añadir un juego hay que volver a pasarlo. En la vista previa de Ajustes se ve con el último juego elegido en la ruleta.

## Sonido

- `res/raw/punetazo.ogg`: golpe al pulsar START en la intro (golpe seco en un cubo de basura metálico, con eco; generado con ElevenLabs).
- `res/raw/ambiente_trafico.ogg`: ruido de ciudad de fondo en la intro, bajito y en bucle («City noise» de freesound_community, Pixabay).
- `res/raw/musica_menu.m4a`: música en bucle (versión metal de Golden Axe, 5 min 15 s). Suena en el menú y sus pantallas (la intro va sin música). No se sube a GitHub (derechos de autor). Si falta, la app funciona sin música. Para cambiarla, sustituye el archivo manteniendo el nombre `musica_menu` (vale .m4a, .mp3 u .ogg; solo uno).

## Arte y Extras

- `assets/previews/<id>.jpg`: portada de cada juego en la ruleta (vertical, tipo flyer). Si falta, se usa la primera de su galería.
- `assets/arte/<id>/01.jpg, 02.jpg…`: galería de Extras de cada juego.
- `assets/historia/<id>.pdf` (o .txt): historia de cada juego.
- El `<id>` es el del juego en `datos/Juego.kt` (= romset padre de FBNeo: ffight, wof, xmen…).
- `assets/videos/<id>.mp4`: vídeo de demostración de la ficha (opcional, no se sube a GitHub). Se reproduce en silencio y en bucle cuando el juego lleva un momento elegido en la ruleta. Para prepararlos: `python herramientas/videos.py <carpeta con vídeos> app/src/main/assets/videos` (necesita ffmpeg; cada vídeo con el id del juego al principio del nombre).
- **Pantallazos** (pestaña de Extras): cada vez que salta un logro de RetroAchievements se guarda una captura de la partida con el aviso del logro (icono, nombre, descripción y puntos) pintado encima. Se guardan en la memoria de la app (`files/pantallazos/`, un .jpg y un .json por logro), se ven a pantalla completa y se pueden borrar (botón «Borrar» o Y). Código: `datos/Pantallazos.kt`.

Para cambiar la portada o el fondo del menú, sustituye `portada_leyendas.jpg` o `fondo_menu.jpg` en `app/src/main/res/drawable-nodpi/` manteniendo el nombre (ahora son provisionales). Si cambia la portada, hay que rehacer las capas del neón y de las nubes (`herramientas/portada/`).

## Intro

Antes de la intro sale el logo de Patrulla Retro con fundidos desde y hacia negro (`ui/pantallas/PantallaLogo.kt`, imagen `res/drawable-nodpi/logo_patrulla_retro.png`).

La intro está en `ui/pantallas/PantallaIntro.kt`. De momento muestra la portada con un fundido, sin música: suena bajito el tráfico de la ciudad y el neón «ARCADE / VIDEO GAMES» parpadea de forma irregular (capas `neon_arcade_apagado.png` y `neon_video_apagado.png`, generadas con `herramientas/portada/neon.py`). Las nubes del cielo se mueven por detrás de la farola y los edificios, también en el fondo del menú (`ui/componentes/Nubes.kt`; imágenes `nubes_cielo.png` y `nubes_delante.png`, de `herramientas/portada/nubes.py`). La farola titila y su luz se refleja en la acera (`farola_luz.png` y `farola_apagada.png`, de `herramientas/portada/farola.py`). La V de «VIDEO GAMES» de la portada está redibujada con `herramientas/portada/arreglar_v.py` (si se cambia la portada, no hace falta). Espera a que se pulse START (no pasa sola). Se sale tocando o pulsando un botón del mando: se apaga el ambiente, suena el golpe en el cubo de basura, la imagen da un golpe y entra al menú, donde empieza la música. Cuando llegue el material definitivo se sustituye solo esa pantalla. Para desactivarla: `MOSTRAR_INTRO = false` en `MainActivity.kt`.

## Estructura

```
datos/      Juego.kt (los 29 juegos), Estadisticas.kt, Puntuaciones.kt (récords), CatalogoRomsets.kt (generado), RepositorioRoms.kt (importar ZIP, BIOS), CarpetaRoms.kt (Ruta/Refresca),
            Ajustes.kt (filtros, controles…), Arte.kt (galería)
ui/tema/    colores y tipografía
ui/componentes/  botones y pantallas base (estilo heredado de Mystara)
ui/pantallas/    Intro, Menú, Juegos (ruleta), Multijugador, Filtros, Mods, Controles, Partidas, Arte
juego/      JuegoActivity (emulación), PantallaEmulacion, ControlesTactiles, Mando
red/        multijugador en la wifi (anfitrión, invitado, retransmisión)
```



## Trucos

- **Trucos**: archivos `.ini` del repositorio oficial [FBNeo-cheats](https://github.com/finalburnneo/FBNeo-cheats), en `assets/trucos/`. Al arrancar, la app copia el del juego a `sistema/fbneo/cheats/<romset>.ini` y activa los elegidos como opciones del núcleo (`fbneo-cheat-…`). En la partida, el menú de pausa permite cambiarlos al momento. Los nombres se traducen en `datos/Trucos.kt`. Los juegos que no están en FBNeo-cheats (Captain America, Golden Axe II, Metamorphic Force, Violent Storm, Asterix, Night Slashers) llevan los de MAME convertidos con `herramientas/trucos/mame_a_fbneo.py`.
- **Reglas de la recreativa** (ajustes rápidos de dificultad y vidas): interruptores DIP (`fbneo-dipswitch-<romset>-<nombre>`), definidos en `datos/Reglas.kt` para los juegos que los tienen (Altered Beast, Bad Dudes, Captain America, Captain Commando, Final Fight, Gang Wars, Knights of the Round, Double Dragon, Double Dragon II, Golden Axe, Hook y Moonwalker). `python herramientas/reglas.py <clon de FBNeo> <romset>` lista los DIP de un juego con sus valores exactos. Las placas CPS-2, CPS-1 QSound, Konami con EEPROM, Sega System 32 y Neo Geo los guardan en su menú de servicio.

## Multijugador (wifi local)

Modelo de **retransmisión desde el anfitrión** (como el juego remoto de las consolas):

- El anfitrión pulsa *Multijugador › Crear partida*. Su app emula el juego normalmente, anuncia la partida en la wifi por NSD/mDNS (`_diasdefuria._tcp`) y abre un servidor TCP.
- Los invitados la ven en *Unirse a una partida*. Al entrar reciben el puesto de jugador 2, 3 o 4.
- El anfitrión copia la imagen del emulador (PixelCopy), la comprime en H.264 768×576 a 60 fps y 6 Mbit/s con el codificador por hardware y la envía; los invitados la decodifican y la muestran con su propia proporción y bezel.
- Los invitados envían sus botones y la cruceta; entran en el emulador por los puertos 1-3 de LibretroDroid.
- Ventajas: una sola emulación, así que no hay desincronización ni hace falta tener las ROMs en los invitados. Limitaciones: el sonido solo suena en el anfitrión; la latencia añade unos 30-60 ms según la wifi.

Código en `red/`: `Protocolo.kt` (mensajes), `Descubrimiento.kt` (NSD), `Anfitrion.kt` (servidor), `Retransmision.kt` (captura y codificación), `Invitado.kt` (conexión y decodificación) e `InvitadoActivity.kt` (pantalla del invitado).

## Controles

| Mando (Odin 2 Portal) | Recreativa |
|---|---|
| Cruceta o stick izquierdo | Mover |
| Abajo (A) | Botón 1 · Atacar |
| Derecha (B) | Botón 2 · Saltar |
| Izquierda (X) | Botón 3 · Elegir objeto |
| Arriba (Y) | Botón 4 · Usar objeto |
| Select | Moneda |
| Start | Start |
| Atrás / Home | Menú de pausa |

Los controles táctiles se ocultan cuando hay un mando conectado (se puede cambiar en Controles).

## Licencias y uso

- Fuentes IM Fell English y EB Garamond: SIL Open Font License 1.1 (`licencias/`).
- LibretroDroid: GPL-3.0. Núcleo FBNeo: licencia no comercial. Si algún día compartes la app, debe ser gratuita y con el código disponible.
- El arte de los juegos y las ROMs pertenecen a sus propietarios (Capcom, Wizards of the Coast). Este proyecto es para uso personal: no incluyas ROMs ni arte original si algún día lo compartes.


## Logros (RetroAchievements)

La app incluye **rcheevos** (`app/src/main/cpp/rcheevos`, licencia MIT), la biblioteca oficial de RetroAchievements, y un puente JNI (`app/src/main/cpp/logros.c`). Para compilarla, Android Studio necesita el **NDK** y **CMake**: *Tools › SDK Manager › SDK Tools* → marca «NDK (Side by side)» y «CMake» → *Apply*.

Los logros leen la memoria del juego directamente del núcleo FBNeo que carga LibretroDroid (`retro_get_memory_data`). Cada romset se identifica con el MD5 de su nombre, como en RetroArch.
