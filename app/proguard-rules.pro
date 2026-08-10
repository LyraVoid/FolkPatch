-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn java.beans.Introspector
-dontwarn java.beans.VetoableChangeListener
-dontwarn java.beans.VetoableChangeSupport
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.PropertyDescriptor

# Keep ini4j Service Provider Interface
-keep,allowobfuscation,allowoptimization class org.ini4j.spi.** { *; }

# Keep native methods and JNI classes
-keep class me.bmax.apatch.Natives {
    *;
}

-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class me.bmax.apatch.Natives$Profile { *; }
-keep class me.bmax.apatch.Natives$KPMCtlRes { *; }

# Keep RootServices
-keep class me.bmax.apatch.services.RootServices { *; }

# Keep AIDL interfaces
-keep class me.bmax.apatch.IAPRootService { *; }
-keep class me.bmax.apatch.IAPRootService$Stub { *; }
-keep class rikka.parcelablelist.ParcelableListSlice { *; }

# Keep Shizuku server built-in (app_process launches by class name, must not be obfuscated)
-keep class rikka.shizuku.server.** { *; }
-keep class moe.shizuku.server.** { *; }
-keep class moe.shizuku.api.BinderContainer { *; }
# ServiceStarter is launched by app_process via a hardcoded class-name string in
# the generated user-service command; obfuscating it breaks UserService launch.
-keep class moe.shizuku.starter.** { *; }

# Custom Shizuku provider (referenced by the manifest)
-keep class me.bmax.apatch.util.ShizukuManagerProvider { *; }

# Legacy REQUEST_BINDER broadcast receiver (referenced by the manifest)
-keep class me.bmax.apatch.receiver.ShizukuReceiver { *; }
-keep class moe.shizuku.common.util.** { *; }
-keep class rikka.rish.** { *; }
-keep class rikka.hidden.compat.** { *; }
# Keep ScriptInfo for Gson serialization in release
-keep class me.bmax.apatch.data.ScriptInfo { *; }
-keepclassmembers class me.bmax.apatch.data.ScriptInfo { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Kotlin
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}

# Keep Umount configuration classes
-keep class me.bmax.apatch.ui.component.UmountConfig { *; }
-keep class me.bmax.apatch.ui.component.UmountConfigManager { *; }
-keep class me.bmax.apatch.ui.screen.UmountConfigScreen { *; }

-keepclassmembers class me.bmax.apatch.ui.component.UmountConfigManager {
    public static *;
}

-keepclassmembers class me.bmax.apatch.ui.component.UmountConfig {
    public <init>(boolean, java.lang.String);
}

# Keep Umount destination
-keep class me.bmax.apatch.ui.screen.destinations.UmountConfigScreenDestination { *; }

-repackageclasses
-allowaccessmodification
-overloadaggressively
-renamesourcefileattribute SourceFile
