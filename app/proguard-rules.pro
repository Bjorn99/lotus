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

-keep class org.jaudiotagger.audio.AudioFileIO { *; }
-keep class org.jaudiotagger.tag.FieldKey { *; }
-keep class org.jaudiotagger.tag.Tag { *; }
-keep class org.jaudiotagger.tag.images.AndroidArtwork { *; }
-keepclassmembers class org.jaudiotagger.tag.id3.** { public *; }
-keepclassmembers class org.jaudiotagger.tag.flac.** { public *; }
-keepclassmembers class org.jaudiotagger.tag.vorbiscomment.** { public *; }

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