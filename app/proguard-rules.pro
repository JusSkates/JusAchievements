# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keepclassmembers @kotlinx.serialization.Serializable class app.jusachievements.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class app.jusachievements.data.api.** { *; }

# Retrofit
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
