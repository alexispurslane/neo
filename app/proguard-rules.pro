# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontobfuscate

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


-dontwarn kotlin.reflect.jvm.internal.**
-keep class kotlin.reflect.jvm.internal.** { *; }
-keep interface javax.annotation.Nullable

-keepattributes SourceFile,LineNumberTable,*Annotation*,EnclosingMethod,Signature,Exceptions,InnerClasses
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking class retrofit2.Response

-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }

-keep,allowshrinking class io.github.alexispurslane.bloc.data.SinglePolyUnwrappedDeserializer {
    public <init>(***);
    *;
}
-keep,allowshrinking class io.github.alexispurslane.bloc.data.SinglePolyUnwrappedDeserializer$** { *; }
-keep,allowshrinking class io.github.alexispurslane.bloc.data.SinglePolyUnwrappedDeserializerImpl { *; }
-keep,allowshrinking class io.github.alexispurslane.bloc.data.SinglePolyUnwrappedDeserializerImpl$** { *; }
-keepclassmembers class io.github.alexispurslane.bloc.data.** {
  @com.fasterxml.jackson.annotation.JsonCreator *;
  @com.fasterxml.jackson.annotation.JsonProperty *;
  ** serialize*(...);
  ** deserialize*(...);
}

-keep class io.github.alexispurslane.bloc.data.network.models.** {
    @com.fasterxml.jackson.annotation.** *;
    *;
}
-keep class io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketRequest$** {
    @com.fasterxml.jackson.annotation.** *;
    *;
}
-keep class io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketResponse$** {
    @com.fasterxml.jackson.annotation.** *;
    *;
}
-keep class com.fasterxml.jackson.** {
  *;
}

-dontwarn com.fasterxml.jackson.databind.**

-keep class * implements java.io.Serializable
-keep interface com.fasterxml.jackson.** { *; }
-keep class com.fasterxml.** { *; }
-dontwarn com.fasterxml.jackson.databind.**
-keep @com.fasterxml.jackson.annotation.JsonIgnoreProperties class * { *; }
-keep class com.fasterxml.jackson.annotation.** {
  *;
}
-keep class androidx.datastore.*.** {*;}