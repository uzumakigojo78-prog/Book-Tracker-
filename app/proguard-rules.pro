# The Anthropic SDK ships its own keep rules (META-INF/proguard). These silence
# warnings for optional JVM-only classes its dependencies reference but never
# use on Android.
-dontwarn java.beans.**
-dontwarn javax.annotation.**
-dontwarn javax.lang.model.**
-dontwarn org.slf4j.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn io.swagger.v3.oas.annotations.**
