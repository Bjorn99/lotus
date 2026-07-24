# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile



# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.dn0ne.player.**$$serializer { *; }
-keepclassmembers class com.dn0ne.player.** {
    *** Companion;
}
-keepclasseswithmembers class com.dn0ne.player.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Koin DI
-keep class com.dn0ne.player.app.di.** { *; }

# Strip verbose/debug/info logging from RELEASE builds. Lotus is
# zero-telemetry; shipping logcat output that can contain user library
# metadata or search queries (e.g. the MusicBrainz provider logs track
# title/artist at Log.d) is a privacy leak. R8 treats these calls as
# side-effect-free and removes them — and the evaluation of their
# arguments — in release. warn/error are kept so genuine failures still
# surface in logcat.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
