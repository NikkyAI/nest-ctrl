enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
//        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
//        kotlin("jvm").version(extra["kotlin.version"] as String)
//        id("org.jetbrains.compose").version(extra["compose.version"] as String)
    }
}
rootProject.name = "nestctrl"

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    // See https://jmfayard.github.io/refreshVersions
    id("de.fayard.refreshVersions") version "0.60.5"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
////                                                   # available:"0.6.0"
////                                                   # available:"0.7.0"
////                                                   # available:"0.8.0"
////                                                   # available:"0.9.0"
////                                                   # available:"0.10.0"
}