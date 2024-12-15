pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version("0.7.0")
        kotlin("jvm").version(extra["kotlinVersion"] as String)
        kotlin("plugin.serialization").version(extra["serializationVersion"] as String)
        id("org.jetbrains.compose").version(extra["composeVersion"] as String)
        id("io.ktor.plugin").version(extra["ktorVersion"] as String)
        id("org.jetbrains.kotlin.android") version "1.9.22"
        id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1"
        id("com.google.gms.google-services") version "4.4.1" apply false
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "multi-project"
include("android", "models", "server")
