# ProGuard Rules for Blood Souls (LibGDX + Kotlin + VisUI + Firebase)

-dontobfuscate
-dontoptimize

# Keep LibGDX Core, Maps, Graphics, Utils, and Reflection
-keep class com.badlogic.gdx.** { *; }
-keep interface com.badlogic.gdx.** { *; }
-keep enum com.badlogic.gdx.** { *; }
-keep class com.badlogic.gdx.maps.** { *; }
-keep class com.badlogic.gdx.maps.tiled.** { *; }
-keep class com.badlogic.gdx.utils.** { *; }
-keep class com.badlogic.gdx.utils.reflect.** { *; }
-dontwarn com.badlogic.gdx.**

# Keep VisUI Library
-keep class com.kotcrab.vis.ui.** { *; }
-dontwarn com.kotcrab.vis.ui.**

# Keep Game Core & Android Classes
-keep class com.BloodSouls.Souls.** { *; }
-keepclassmembers class com.BloodSouls.Souls.** { *; }
-keep class com.darkforge.bloodsouls.** { *; }
-keepclassmembers class com.darkforge.bloodsouls.** { *; }

# Keep Firebase & Google Play Services
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep Kotlin Classes & Metadata
-keep class kotlin.** { *; }
-keep interface kotlin.** { *; }
-dontwarn kotlin.**

# Keep Native JNI Calls
-keepclasseswithmembernames class * {
    native <methods>;
}

-keepattributes Signature, InnerClasses, AnnotationDefault, EnclosingMethod, LineNumberTable, SourceFile
