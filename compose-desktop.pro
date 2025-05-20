# from: https://github.com/Anamorphosee/stacktrace-decoroutinator#using-proguard
-keep @kotlin.coroutines.jvm.internal.DebugMetadata class * { *; }
-keep @dev.reformator.stacktracedecoroutinator.provider.DecoroutinatorTransformed class * { *; }

# from: https://github.com/java-native-access/jna/issues/1187#issuecomment-626251894
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }

# from: https://github.com/JetBrains/compose-multiplatform/issues/4883#issuecomment-2156012785
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.collection.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.compose.ui.text.platform.ReflectionUtil { *; }

# We're excluding Material 2 from the project as we're using Material 3
-dontwarn androidx.compose.material.**

# Kotlinx coroutines rules seems to be outdated with the latest version of Kotlin and Proguard
### -keep class kotlinx.coroutines.** { *; }

# from: https://github.com/Kotlin/kotlinx.serialization/issues/2719#issuecomment-2189193638
-keepclassmembers public class **$$serializer {
    private ** descriptor;
}

# attempting to fix json5k
-keep class io.github.xn32.json5k.** { *; }

# Ktor
-keep class io.ktor.client.engine.java.** { *; }
-keep class io.ktor.serialization.kotlinx.json.** { *; }

## remove some stupid warnings for things we do not use
-dontwarn jakarta.**
-dontwarn org.tukaani.xz.**
-dontwarn lombok.**
-dontwarn edu.umd.cs.findbugs.**
-dontwarn org.junit.**
-dontwarn org.codehaus.commons.**
-dontwarn org.codehaus.janino.**
-dontwarn com.oracle.svm.**
-dontwarn javax.servlet.**
-dontwarn org.eclipse.jetty.**
-dontwarn ch.qos.cal10n.**
-dontwarn javassist.**

# -dontwarn reactor.blockhound.**
-dontwarn javassist.**

# logging

-keep public class org.slf4j.** { *; }
-keep public class ch.** { *; }

# blockhound
-dontnote reactor.blockhound
-dontwarn reactor.blockhound
# -keep class reactor.blockhound.** { *; }
-dontwarn reactor.core.scheduler.NonBlocking
-dontwarn io.reactivex.internal.schedulers.NonBlockingThread
-dontwarn **.AutoService
-dontwarn javax.annotation.**

-printmapping mapping.txt
# -dontoptimize


-ignorewarnings