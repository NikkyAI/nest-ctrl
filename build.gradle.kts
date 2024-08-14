import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import org.jetbrains.compose.desktop.application.tasks.AbstractRunDistributableTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    kotlin("plugin.power-assert")
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
//    maven("https://oss.sonatype.org/content/repositories/snapshots/") {
//        this.mavenContent {
//            this.includeGroup("com.ryanharter.kotlinx.serialization")
//        }
//    }
    google()
}

kotlin {
    jvm("desktop")
    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)

            implementation(Kotlin.stdlib)
            implementation(Kotlin.stdlib.common)

            implementation(KotlinX.coroutines.core)
            implementation(KotlinX.serialization.json)
            implementation(KotlinX.datetime)

            implementation(Ktor.client.core)
            implementation(Ktor.client.json)
            implementation(Ktor.client.cio)
            implementation("io.ktor:ktor-network:_")

            implementation("org.deepsymmetry:lib-carabiner:_")

            implementation("com.illposed.osc:javaosc-core:_")
//            implementation("com.illposed.osc:javaosc-java-se-addons:_")

//            implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:_")

            implementation("io.github.pdvrieze.xmlutil:serialization:_")
//            implementation("com.ryanharter.kotlinx.serialization:kotlinx-serialization-xml:_")
            implementation("io.github.xn32:json5k:_")

            implementation("com.github.doyaaaaaken:kotlin-csv-jvm:_")

            implementation("io.github.cdimascio:dotenv-kotlin:_")

            implementation("io.github.oshai:kotlin-logging:_")
//    implementation("io.klogging:klogging-jvm:_")
//    implementation("io.klogging:slf4j-klogging:_")
            implementation("ch.qos.logback:logback-classic:_")

            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:_")
            implementation("dev.reformator.stacktracedecoroutinator:stacktrace-decoroutinator-jvm-legacy:_")

            // Include the Test API
//    testImplementation(compose.desktop.uiTestJUnit4)
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>()  {
    compilerOptions {
        // coroutine debugging, disables optimizing away unused variables in coroutines
//        freeCompilerArgs.add("-Xdebug")

        freeCompilerArgs .addAll(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
                    project.layout.buildDirectory.file("compose_metrics").get().asFile.also { logger.lifecycle("metrics: $it") }
        )
        freeCompilerArgs .addAll(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
                    project.layout.buildDirectory.file("compose_metrics").get().asFile
        )
    }
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
powerAssert {
    functions = listOf(
        "kotlin.require",
        "utils.checkNotNullExt",
        "utils.checkNotNullDebug",
        "utils.errorDebug",
    )
    includedSourceSets = listOf("commonMain", "desktopMain")
}

compose.desktop {
    application {
        mainClass = "Main"

        mainJar.set(
            project.file("build").resolve("nestctrl.jar")
        )
        jvmArgs += listOf(
            "-Xmx1G",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED"
        )
        nativeDistributions {
//            targetFormats(
//                TargetFormat.Dmg,
//                TargetFormat.Msi,
//                TargetFormat.Deb,
//            )
            packageName = "nestctrl"
            packageVersion = "0.0.1"
//            this.vendor = null

            windows {
                iconFile.set(project.file("src/desktopMain/resources/drawable/blobhai_trans.ico"))
            }
            modules(
                "java.naming",
//                "java.instrument",
//                "java.management",
//                "jdk.unsupported",
            )
//            includeAllModules = true
        }
    }
}

project.afterEvaluate {
    tasks {
        val run by getting(JavaExec::class) {
            jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
        }
        val createDistributable by getting(AbstractJPackageTask::class) {
            destinationDir.set(project.file("bin"))
//            appImageRootDir.
        }
        val runDistributable by getting(AbstractRunDistributableTask::class) {
//            appImageRootDir.
        }
        val packageJar by creating {
            group = "package"
            dependsOn(getByName("packageUberJarForCurrentOS"))
            doLast {
//            val file = compose.desktop.application.mainJar.asFile.get()
//            file.copyTo(
//                project.file("build").resolve("nestctrl.jar"),
//                overwrite = true
//            )
                copy {
                    from(getByName("packageUberJarForCurrentOS"))
                    into(project.file("build"))
                    rename {
                        "nestctrl.jar"
                    }
                }
            }
        }
        val packageDistributable by creating(Zip::class) {
            group = "package"
            from(getByName("createDistributable"))
            archiveBaseName.set("nestctrl")
            destinationDirectory.set(project.file("build"))
//            doLast {
////            val file = compose.desktop.application.mainJar.asFile.get()
////            file.copyTo(
////                project.file("build").resolve("nestctrl.jar"),
////                overwrite = true
////            )
//                copy {
//                    from(getByName("packageUberJarForCurrentOS"))
//                    into(project.file("build"))
//                    rename {
//                        "nestctrl.jar"
//                    }
//                }
//            }
        }
        val deployDistributable by creating(Copy::class) {
            group = "package"
            doFirst {
                File(System.getProperty("user.home")).resolve("VJ").resolve("nestctrl").deleteRecursively()
            }
            from(getByName("createDistributable"))
            this.destinationDir = File(System.getProperty("user.home")).resolve("VJ")
//            doLast {
////            val file = compose.desktop.application.mainJar.asFile.get()
////            file.copyTo(
////                project.file("build").resolve("nestctrl.jar"),
////                overwrite = true
////            )
//                copy {
//                    from(getByName("packageUberJarForCurrentOS"))
//                    into(project.file("build"))
//                    rename {
//                        "nestctrl.jar"
//                    }
//                }
//            }
        }
    }
}
