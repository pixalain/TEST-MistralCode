-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,EnclosingMethod

-keep class androidx.media3.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn androidx.media3.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Jetpack Compose
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep @androidx.compose.runtime.Composable class * { *; }
-dontwarn androidx.compose.**
