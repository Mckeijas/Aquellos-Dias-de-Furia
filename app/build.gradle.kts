import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ── Versión ─────────────────────────────────────────────────────────────────
// La versión vive en version.properties (raíz del proyecto). Para cada
// actualización se sube con la tarea «nuevaVersion» (ver LEEME.md).
val archivoVersion = rootProject.file("version.properties")
val propsVersion = Properties().apply { archivoVersion.inputStream().use { load(it) } }
val vMayor = propsVersion.getProperty("MAYOR").toInt()
val vMenor = propsVersion.getProperty("MENOR").toInt()
val vParche = propsVersion.getProperty("PARCHE").toInt()
val nombreVersion = "$vMayor.$vMenor.$vParche"
// 0.2.0 → 200, 1.4.12 → 10412: siempre crece al subir la versión
val codigoVersion = vMayor * 10000 + vMenor * 100 + vParche

val fechaCompilacion: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
val commitGit: String = runCatching {
    providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
}.getOrDefault("").ifBlank { "sin-git" }

// ── Firma ───────────────────────────────────────────────────────────────────
// firma.properties (raíz del proyecto, NO se sube a GitHub) apunta a tu keystore.
// Si no existe, la versión release se firma con la clave de depuración.
val archivoFirma = rootProject.file("firma.properties")
val propsFirma = Properties().apply { if (archivoFirma.exists()) archivoFirma.inputStream().use { load(it) } }
val hayFirma = propsFirma.getProperty("storeFile")?.let { rootProject.file(it).exists() } == true

android {
    namespace = "com.diasdefuria"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.diasdefuria"
        minSdk = 24
        targetSdk = 35
        versionCode = codigoVersion
        versionName = nombreVersion
        buildConfigField("String", "FECHA_COMPILACION", "\"$fechaCompilacion\"")
        buildConfigField("String", "COMMIT", "\"$commitGit\"")
        // El núcleo FBNeo se compila para arm64 (Odin 2 Portal y móviles actuales)
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    // Logros de RetroAchievements: rcheevos (C) compilado con el NDK
    externalNativeBuild {
        cmake { path = file("src/main/cpp/CMakeLists.txt") }
    }

    // El núcleo .so se extrae al instalar para que LibretroDroid pueda cargarlo
    packaging {
        jniLibs { useLegacyPackaging = true }
    }

    signingConfigs {
        if (hayFirma) {
            create("propia") {
                storeFile = rootProject.file(propsFirma.getProperty("storeFile"))
                storePassword = propsFirma.getProperty("storePassword")
                keyAlias = propsFirma.getProperty("keyAlias")
                keyPassword = propsFirma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        // Sin sufijo: la app muestra solo la versión (y la fecha), sea depuración o release
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = if (hayFirma) signingConfigs.getByName("propia") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // El APK lleva solo la versión en el nombre: DiasDeFuria-v0.3.0.apk
    applicationVariants.all {
        val variante = this
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "DiasDeFuria-v${variante.versionName}.apk"
        }
    }
}

if (!hayFirma) {
    logger.warn("Días de Furia: sin firma.properties, la versión release se firma con la clave de depuración.")
}

/**
 * Sube la versión antes de una actualización.
 *   gradlew nuevaVersion              → 0.2.0 → 0.2.1 (correcciones)
 *   gradlew nuevaVersion -Ptipo=menor → 0.2.1 → 0.3.0 (funciones nuevas)
 *   gradlew nuevaVersion -Ptipo=mayor → 0.3.0 → 1.0.0
 * Después, añade lo que cambia en CHANGELOG.md.
 */
tasks.register("nuevaVersion") {
    group = "dias de furia"
    description = "Sube la versión en version.properties"
    val tipo = (project.findProperty("tipo") as String?) ?: "parche"
    val archivo = archivoVersion
    doLast {
        val p = Properties().apply { archivo.inputStream().use { load(it) } }
        var mayor = p.getProperty("MAYOR").toInt()
        var menor = p.getProperty("MENOR").toInt()
        var parche = p.getProperty("PARCHE").toInt()
        when (tipo) {
            "mayor" -> { mayor++; menor = 0; parche = 0 }
            "menor" -> { menor++; parche = 0 }
            else -> parche++
        }
        archivo.writeText("MAYOR=$mayor\nMENOR=$menor\nPARCHE=$parche\n")
        println("Nueva versión: $mayor.$menor.$parche (código ${mayor * 10000 + menor * 100 + parche})")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Emulación: frontend libretro (GLRetroView) que carga el núcleo FBNeo
    implementation("com.github.Swordfish90:LibretroDroid:0.14.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Carga de imágenes para la galería de arte
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
