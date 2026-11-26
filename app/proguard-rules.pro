# ProGuard / R8 rules for Tilawa Quran Release builds

# Preserve annotations, generics, and Kotlin metadata for Moshi & Retrofit
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Keep all Moshi-generated adapters and model classes
-keep class com.example.model.** { *; }
-keep class com.example.data.**JsonAdapter { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.Json <methods>;
}
-dontwarn com.squareup.moshi.**

# Keep Retrofit & OkHttp interfaces cleanly
-keepinterface retrofit2.**
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep Media3 / ExoPlayer & playback service components
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class com.example.playback.QuranPlaybackService { *; }
-keep class com.example.notifications.DailyWirdReminderReceiver { *; }
