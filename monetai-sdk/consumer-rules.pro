# Rules for consumers of the library
# This file is automatically included in the consumer's build.gradle

# Keep public API classes
-keep public class com.monetai.sdk.MonetaiSDK { *; }
-keep public class com.monetai.sdk.MonetaiSDKJava { *; }
-keep public class com.monetai.sdk.models.** { *; }

# Keep public methods and fields
-keepclassmembers public class com.monetai.sdk.** {
    public *;
}

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
} 