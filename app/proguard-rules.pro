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

# jaudiotagger — broad keep is required.
#
# jaudiotagger uses reflection extensively to instantiate ID3 frame body
# classes (FrameBodyTXXX, FrameBodyPOPM, FrameBodyPairs, etc.) and to
# invoke their copy constructors during AudioFileIO.read() via
# ID3Tags.copyObject(). R8 cannot see these reflection sites and by
# default strips the reflectively-invoked constructors and sometimes the
# whole class, which surfaces at runtime as:
#
#   NoSuchMethodException: Error finding constructor to create copy:<obfuscated>
#
# (Issues #95 and #103.) The previous narrow rules used
# `-keepclassmembers ... { public *; }`, but `public *` in ProGuard
# member syntax only matches methods and fields — not constructors,
# which have no return type — so copy constructors were being removed
# even though the rules looked broad.
#
# Broadening to `-keep class org.jaudiotagger.** { *; }` preserves every
# class, method, field and constructor across the library. APK size
# grows by ~1–2 MB (jaudiotagger's compiled size) but the whole class
# of reflection-vs-R8 crashes goes away. Do not narrow this without
# verifying every ID3 frame body class survives.
-keep class org.jaudiotagger.** { *; }

-dontwarn java.awt.Graphics2D
-dontwarn java.awt.Image
-dontwarn java.awt.geom.AffineTransform
-dontwarn java.awt.image.BufferedImage
-dontwarn java.awt.image.ImageObserver
-dontwarn java.awt.image.RenderedImage
-dontwarn javax.imageio.ImageIO
-dontwarn javax.imageio.ImageWriter
-dontwarn javax.imageio.stream.ImageInputStream
-dontwarn javax.imageio.stream.ImageOutputStream
-dontwarn javax.swing.filechooser.FileFilter

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
