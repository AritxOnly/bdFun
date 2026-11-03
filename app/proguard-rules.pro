# 原生迁移保留 ProGuard 默认规则

-keep class top.yukonga.miuix.** { *; }
-keep class com.kyant.** { *; }
-keep class com.aritxonly.deadliner.** { *; }
-dontwarn org.json.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn coil.**
-dontwarn androidx.media3.**
