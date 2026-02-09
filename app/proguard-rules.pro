# MaiBot Android ProGuard 规则
# 生产环境代码混淆配置

# 保留类名和成员，防止反射调用问题
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# 保留 Chaquopy Python 相关类
-keep class com.chaquo.python.** { *; }
-dontwarn com.chaquo.python.**

# 保留 OkHttp 相关类
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**

# 保留 Gson 相关类
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# 保留 AndroidX 和 Support Library
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# 保留 MaiBot 应用类
-keep class com.maibot.groupchat.** { *; }
-keepclassmembers class com.maibot.groupchat.** { *; }

# 保留模型类（用于JSON序列化）
-keepclassmembers class com.maibot.groupchat.model.** {
    <fields>;
}

# 保留服务类
-keep class com.maibot.groupchat.service.MaiBotService { *; }
-keep class com.maibot.groupchat.service.MaiBotService$* { *; }

# 保留广播接收器
-keep class com.maibot.groupchat.activity.MainActivity$* { *; }

# 移除日志代码（生产环境）
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# 优化配置
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# 保留泛型信息
-keepattributes Signature

# 保留行号信息（方便崩溃日志分析）
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 移除未使用的资源
-dontwarn android.support.**
-dontwarn org.jetbrains.**

# 保留 JNI 方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留 Parcelable 实现类
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留 Serializable 实现类
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
