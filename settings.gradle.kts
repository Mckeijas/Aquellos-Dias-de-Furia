pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Necesario más adelante para LibretroDroid (emulación)
        maven("https://jitpack.io")
    }
}

rootProject.name = "DiasDeFuria"
include(":app")
