# FitBase ProGuard Rules

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.fitbase.data.model.** { *; }
-keepclassmembers class com.fitbase.data.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Health Connect
-keep class androidx.health.connect.** { *; }
