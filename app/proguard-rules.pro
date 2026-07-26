# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers in crash reports, but hide original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Gson ---------------------------------------------------------------
# DTOs are populated reflectively by field name, so their names must survive.
-keep class dev.devscroll.data.remote.dto.** { *; }
-keepattributes Signature,*Annotation*
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# TypeToken relies on generic signatures at runtime.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# --- Room ---------------------------------------------------------------
# Entities are mapped by column/field name by generated code.
-keep class dev.devscroll.data.local.entity.** { *; }
-keep class dev.devscroll.data.local.dao.** { *; }

# --- Retrofit / OkHttp --------------------------------------------------
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keep,allowobfuscation interface dev.devscroll.data.remote.*ApiService
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
