# MonetAI SDK ProGuard rules for consumers
# This file is automatically included in the consumer's ProGuard configuration

# Keep all MonetAI SDK classes, interfaces, and enums to prevent obfuscation issues
# Since the SDK is open source, there's no security concern with preserving class names
-keep class com.monetai.sdk.** { *; }
-keep interface com.monetai.sdk.** { *; }
-keep enum com.monetai.sdk.** { *; } 