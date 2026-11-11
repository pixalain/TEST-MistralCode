-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,EnclosingMethod

-keepclassmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}

-keep class com.f1live.app.data.model.** { *; }

-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Jetpack Compose: keep everything referenced from composable functions
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-dontwarn androidx.compose.**
