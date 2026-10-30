# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Line numbers and annotation attributes
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepattributes SourceFile, LineNumberTable

# Kotlinx Serialization
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class **$$serializer {
    *;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable companion *;
}
-keep class com.pasindu.nursingotapp.transfer.data.model.** { *; }

# Room Entities & DAOs
-keep class com.pasindu.nursingotapp.data.local.entity.** { *; }
-keep class com.pasindu.nursingotapp.data.local.dao.** { *; }

# Lottie (Animation JSON parsing)
-keep class com.airbnb.lottie.** { *; }

# Firebase (Silence optional transitive warnings; SDK consumer rules handle internals)
-dontwarn com.google.firebase.**
