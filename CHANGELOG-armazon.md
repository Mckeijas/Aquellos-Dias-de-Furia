# Novedades de Leyendas de Mystara

Cada versión sube su número con `gradlew nuevaVersion` y se anota aquí.
Formato: MAYOR.MENOR.PARCHE (parche = correcciones, menor = funciones nuevas, mayor = hitos).

## 0.14.9 · 27/10/2026
- El APK se exporta solo con la versión (`LeyendasDeMystara-v0.14.9.apk`), sin «-prueba» ni «-release», y el menú principal muestra abajo a la derecha solo la versión y la fecha.

## 0.14.8 · 27/10/2026
- **Importar solo acepta versiones del propio juego**: desde la ficha de un juego solo se instalan sus versiones (Hispanic, Europa o Japón). Un ZIP (o una clave .key) de otro juego, o cualquier otra ROM, da un aviso y no se instala ni se borra nada.
- Reconocidas más versiones: Tower of Doom Europa 940113 y Japón 940412/940125/940113; Shadow over Mystara Europa 960223/960209/960208 y Japón 960619/960223/960206.
- Leyendas: todo lo que va bajo la portada tiene alto fijo y el botón dice solo «Jugar»; las tres portadas miden exactamente lo mismo, con una o varias versiones.

## 0.14.7 · 27/10/2026
- Leyendas: sin ZIP importado, la ficha muestra solo la placa y el año (sin banderas).

## 0.14.6 · 27/10/2026
- Leyendas: las tres fichas con el mismo diseño; King of Dragons muestra «Mundial 910805» y su botón «Mundial 🏆» (versión con logros), aunque haya una sola versión.

## 0.14.5 · 27/10/2026
- Leyendas: con dos versiones, la línea de arriba muestra la versión elegida (p. ej. «CPS-2 · 1993 · Hispanic 940125» o «Europa 940412») y se quita la línea del romset; las tres portadas vuelven a tener el mismo tamaño.

## 0.14.4 · 27/10/2026
- Leyendas: bajo el nombre de cada juego, la bandera del idioma de la versión que se va a jugar (🇪🇸 Hispanic, 🇬🇧 europea/mundial, 🇯🇵 japonesa); cambia al elegir otra versión.

## 0.14.3 · 27/10/2026
- Extras: las pestañas (Ilustraciones, Flyers, Cabinets, Capturas, Guías, Historia) van centradas, con más espacio respecto al botón de logros y a las miniaturas.

## 0.14.2 · 27/10/2026
- **Cambiar ZIP cambia de versión**: importar la europea sustituye a la Hispanic (y se juega con la europea, con logros); importar la Hispanic pasa a jugarse en español y conserva la europea, que necesita para funcionar. Las partidas y los récords se mantienen.
- Menú principal: abajo a la derecha solo la versión y la fecha; abajo a la izquierda, botón «C» con el aviso legal y los derechos.
- Imagen y sonido: «Efectos de sonido» sin el texto entre paréntesis.

## 0.14.1 · 27/10/2026
- Extras › Historia abre los PDF (mismo lector que las guías) y los ordena como Leyendas: King of Dragons, Tower of Doom y Shadow over Mystara.

## 0.14.0 · 27/10/2026
- **Cambiar ZIP** borra la versión anterior del mismo tipo (Hispanic por Hispanic; europea, mundial, americana… entre sí). Se conservan el ZIP padre que necesita la nueva y la del otro tipo, para seguir pudiendo elegir entre español y la de los logros. La recién importada pasa a ser la que se juega.
- **Guías en PDF**: Extras › Guías abre también PDF (páginas con zoom, cruceta para moverse y L1/R1 para pasar página).
- **Galería con cruceta**: en el visor se pasa de foto con izquierda/derecha (o L1/R1) y con las flechas en pantalla.
- **Marco lateral**: en Imagen y sonido la elección del marco va arriba del todo («Franjas negras» o cualquier marco), y en la pausa hay «Marco lateral» para cambiarlo jugando, viendo el cambio al momento.
- **Récords guardados**: las tablas de puntuaciones de las recreativas (hiscore.dat) se guardan al salir y se recuperan al volver, también tras reiniciar. Las versiones del mismo juego comparten la tabla, así que cambiar de ZIP no la borra.

## 0.13.0 · 25/10/2026
- **Multijugador con sonido para todos**: el anfitrión envía el audio del juego (AAC) junto con la imagen; los invitados lo oyen con poco retraso y, si la red se atasca, se recorta para no ir desfasado. Protocolo de red v3.
- **Elegir versión**: si hay varias versiones completas de un juego (p. ej. Hispanic en español y la europea, que es la reconocida por RetroAchievements), en su ficha de Leyendas se elige con cuál jugar. La versión principal se marca con 🏆.
- Leyendas: King of Dragons a la izquierda, Tower of Doom en el centro y Shadow over Mystara a la derecha.
- Nueva música del menú: «Enchantress».
- **Bezels temáticos**: Tower of Doom, Shadow over Mystara, King of Dragons, Lucia y Crassus, Moriah y Dimsdale, Dragón rojo, Héroes de Mystara y Dragón de fuego; además se puede usar cualquier imagen de Extras o franjas negras.
- **Controles táctiles rediseñados**: cruceta en cruz con relieve y botones con volumen, brillo, icono y animación al pulsar.
- **Configuración de la recreativa** en la pausa: abre el menú de servicio de la placa original (dificultad, vidas, etc.); se sale con EXIT. Mantener Start ya no lo abre por accidente.
- Extras: pestaña **Historia** (textos en assets/historia), guías nuevas de King of Dragons, Tower of Doom y Shadow over Mystara (lector con tamaño de letra ajustable) y 47 imágenes nuevas, con la primera colección de **Cabinets**.

## 0.12.0 · 23/10/2026
- Menú principal: «Partidas guardadas» pasa junto a «Imagen y sonido» y «Trucos» junto a «Controles».
- «Arte» pasa a llamarse **Extras**: Ilustraciones, Flyers, Cabinets, Capturas, **Guías** (lector de texto, con cruceta y L1/R1 para pasar página) y el botón de **Logros de RetroAchievements**, que sale de Trucos.
- Guía de Tower of Doom en español (Night Stalker).
- **Multijugador desde la pausa**: en una partida en solitario, «Multijugador» abre la partida en casa o por internet sin reiniciar, y muestra cómo invitar.

## 0.11.0 · 23/10/2026
- **Logros de RetroAchievements** (botón especial en Trucos): se conecta la cuenta de retroachievements.org con usuario y contraseña (solo se guarda la sesión). Durante la partida se comprueban los logros de cada juego y, al conseguir uno, aparece un aviso arriba con su insignia, el nombre y los puntos, con un sonido de trofeo propio.
- Al empezar la partida indica cuántos logros llevas; si la versión (romset) no está registrada en RetroAchievements, lo avisa.
- Modo normal (softcore): se pueden usar partidas guardadas. Con trucos activos los logros se pausan.
- Si se pierde la conexión, los logros conseguidos se envían al volver.
- Incluye rcheevos (biblioteca oficial de RetroAchievements, licencia MIT) compilada con el NDK.

## 0.10.1 · 23/10/2026
- **Controles táctiles transparentes** (nuevo estilo por defecto): botones de cristal semitransparentes que dejan ver el juego. En Controles se puede volver al estilo dorado clásico.
- Botón «Mostrar / Ocultar controles táctiles» en el menú de pausa (también en el invitado del multijugador), para usarlos aunque haya un mando conectado.

## 0.10.0 · 23/10/2026
- **Multiplayer por internet, de dispositivo a dispositivo** (sin servidores). «Por internet» abre el puerto en el router automáticamente (UPnP) y da un **código de invitación** (p. ej. LNY6U-B74ZA) para compartir por WhatsApp desde «Invitar jugadores» en la pausa. Si el router no lo permite, explica cómo abrirlo a mano o si la compañía usa CG-NAT.
- **Unirse con código**: código, IP:puerto o la IP de Tailscale/ZeroTier. Las partidas «En casa» siguen apareciendo solas en la lista.
- Puerto fijo 47800 (TCP).
- **Menos retraso**: H.264 Baseline a tasa constante y sin espera en el codificador; decodificador en modo baja latencia (Qualcomm) que muestra siempre la imagen más reciente; wifi en modo baja latencia y pantalla siempre encendida; búfer de envío corto y tráfico marcado como prioritario; la calidad se ajusta sola si la red no da abasto (por internet empieza en 512×384 a 2,5 Mbps).
- El invitado ve su ping con el anfitrión.
- Protocolo de red v2: todos los dispositivos deben tener esta versión.

## 0.9.1 · 23/10/2026
- El menú de pausa pasa a **R1**; **Start** vuelve a ser el Start de la recreativa (tras meter moneda, entrar a jugar o continuar). El ataque especial pasa a L1.
- «Moneda también pulsa Start» queda desactivado por defecto (se puede activar en Controles).
- Si ya habías cambiado algún botón en Controles, se respeta tu asignación; «Botones de fábrica» aplica la nueva.

## 0.9.0 · 23/10/2026
- **Start abre el menú de pausa** durante la partida (también Atrás/Home y el botón ☰ táctil). Desde ahí: continuar, guardar, cargar, trucos, reiniciar y **salir al menú principal**.
- **Guardar y cargar partida** en 6 ranuras por juego, con miniatura de la imagen del momento y fecha.
- **Se guarda dónde lo dejaste**: al salir al menú, al pulsar Inicio de Android y cada minuto de juego. Al volver a entrar en el juego pregunta «Continuar donde lo dejé» o «Empezar de nuevo».
- **Controles → Mando físico**: cada acción (Atacar, Saltar, Elegir objeto, Usar objeto, Ataque especial, Moneda, Start del juego y Menú de pausa) se asigna al botón que quieras del mando o de un teclado. Por defecto Start = pausa, L1 = Start del juego, R1 = ataque especial.
- Opción «Moneda también pulsa Start»: un solo botón mete crédito y entra a jugar (activada por defecto).
- **Controles táctiles para móvil**: tamaño ajustable y editor para colocar la cruceta, los botones y Moneda/Start arrastrándolos.
- Los invitados del multijugador usan la misma asignación de botones.

## 0.8.2 · 23/10/2026
- Arte: la galería incluye todas las ilustraciones de la carpeta arte (54 imágenes): 38 ilustraciones, 15 flyers y folletos, y una captura. Se quitan las repetidas.

## 0.8.1 · 23/10/2026
- Imagen y sonido: la vista previa usa una captura real de Tower of Doom con el filtro elegido (Original, Mejorado, Suavizado, CRT, LCD) y añade un detalle ampliado de los personajes para comparar bien cada filtro.

## 0.8.0 · 23/10/2026
- **Multiplayer en la wifi** (hasta 4 jugadores). Quien crea la partida juega en su dispositivo y la retransmite en vídeo (H.264 por hardware); los invitados se unen desde una lista (sin escribir IPs), ven la partida y juegan como jugadores 2, 3 y 4 con su mando o los controles táctiles. No pueden desincronizarse y los invitados no necesitan las ROMs.
- Indicador de jugadores conectados en la pantalla del anfitrión, y de número de jugador en la del invitado.
- King of Dragons: nueva regla «Jugadores a la vez» (2 o 3), necesaria para jugar 3.
- Se quita la parte de mods IPS; la sección pasa a llamarse **Trucos** (trucos y reglas de la recreativa).
- Intro: el cartel «Leyendas de Mystara» se separa del borde derecho.
- Ficha de King of Dragons: portada sin la franja roja de CAPCOM.

## 0.7.0 · 23/10/2026
- Nueva sección **Mods y trucos** (fase 3), con una pestaña por juego.
- **Trucos** de FBNeo traducidos al español: generales (tiempo infinito, fase inicial, llaves…) y por jugador (energía infinita, invencible, objetos y magia infinitos, equipo…). 71 en Tower of Doom, 69 en Shadow over Mystara y 32 en The King of Dragons.
- Los trucos también se activan **durante la partida** desde el menú de pausa (botón «Trucos») y se aplican al momento.
- **Reglas de la recreativa** para The King of Dragons (interruptores DIP): dificultad, vidas por crédito, partida gratis y continuar. Los juegos CPS-2 no tienen DIP; se explica en la sección.
- **Mods IPS** en formato FBNeo (.dat + .ips, sueltos o en .zip): se añaden, se activan o desactivan y se borran por juego. Se aplican en memoria al arrancar, sin modificar el ZIP.
- Se oculta el truco «Select Region» porque cambiaría el juego a otra región y se perdería el español.

## 0.6.0 · 23/10/2026
- Fichas de «Leyendas» con la portada de cada juego.
- Nuevo icono de la app: el dragón rojo, con marco dorado (icono adaptativo en Android 8 y posteriores).
- Nueva tipografía de libro antiguo: IM Fell English en títulos y botones, EB Garamond en los textos.
- Corregido: los textos de las secciones salían en negro y apenas se leían; ahora usan el color pergamino.
- El sonido al entrar desde la intro pasa a ser una «revelación» (soplo ascendente, coro y campanas), sintetizado para el proyecto.

## 0.5.0 · 23/10/2026
- Nuevo menú principal: botón «Leyendas» (los juegos), Imagen y sonido, Mods, Controles, Partidas guardadas, Arte, Salir y, abajo del todo, «Multiplayer».
- Sección «Leyendas» con una ficha por juego: placa, año, idioma, versión importada y botones de importar, jugar o completar.
- Nuevo juego: **The King of Dragons** (CPS-1, 1991). Versiones reconocidas: `kod` (Mundial 910805), `kodr1`, `kodr2`, `kodu`, `kodj`, `kodja`. No existe versión en español.
- Los chips PLD de las placas CPS-1 se consideran opcionales: no hacen falta para jugar.
- Controles táctiles con solo Atacar y Saltar en los juegos de 2 botones.
- Sección «Multiplayer» reservada para el juego por wifi local.

## 0.4.1 · 23/10/2026
- Música del menú recortada a un bucle de 2 min 22 s (de 8 MB a 2 MB), con la costura fundida para que no se note el salto. Pasa a formato OGG, que Android repite sin silencio entre vueltas.

## 0.4.0 · 23/10/2026
- Al pulsar un botón en la intro suena un choque de espadas (efecto sintetizado propio) y se entra al menú.
- Música de fondo en bucle en todas las pantallas del menú, con fundido de entrada; se pausa al jugar o salir de la app.
- La sección «Filtros y mejoras» pasa a llamarse «Imagen y sonido»: activar o desactivar la música, su volumen y los efectos.

## 0.3.0 · 23/10/2026
- **Los juegos ya se pueden jugar**: emulación con el núcleo FBNeo mediante LibretroDroid, en una pantalla de juego propia.
- Mando físico (Odin 2 Portal y mandos Bluetooth/USB): cruceta y stick izquierdo; abajo Atacar, derecha Saltar, izquierda Elegir objeto, arriba Usar; Select = Moneda.
- Controles táctiles con cruceta de 8 direcciones, 4 botones, Moneda y Start; se ocultan solos si hay un mando conectado.
- Menú de pausa (botón Atrás/Home del mando o icono ☰): continuar, guardar partida, cargar la última, reiniciar y salir al menú.
- Guardado automático al salir y pantalla «Partidas guardadas» para cargar o borrar partidas.
- Mejoras gráficas aplicadas en el juego: filtros Original, Mejorado, Suavizado, CRT y LCD; proporción 4:3 o 16:9 de píxeles cuadrados; escalado entero; bezel en los laterales.
- En los menús, A confirma y B vuelve atrás.
- Retirada la opción run-ahead: LibretroDroid no la admite.

## 0.2.0 · 23/10/2026
- Importación por contenido: se pueden elegir varios ZIP a la vez y la app reconoce cada versión por los CRC de FBNeo, sin importar el nombre del archivo.
- Versiones Hispanic reconocidas: Tower of Doom `ddtodh`, `ddtodhr1`, `ddtodhr2`; Shadow over Mystara `ddsomh`. Padres `ddtod` y `ddsom` como alternativa en inglés.
- Mensaje tras importar que indica si cada juego se puede jugar y qué falta (por ejemplo, la clave `.key`).
- Botón «Completar…» para juegos incompletos, con opción de jugar la versión en inglés.
- Filtros y mejoras: proporción 4:3 o 16:9 sin deformar, con bezel en los laterales y vista previa.
- Galería de Arte con flyer, ilustraciones y portadas.
- Firma de las versiones release y número de versión, fecha y commit visibles en el menú.

## 0.1.0 · 22/10/2026
- Primera versión del launcher: intro (provisional), menú principal con los dos juegos, Filtros, Mods, Controles, Partidas guardadas, Arte y Salir.
- Título «Leyendas de Mystara» sobre el arte de portada.
