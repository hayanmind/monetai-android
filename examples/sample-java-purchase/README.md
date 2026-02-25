# Sample Java Purchase App

A Java sample app demonstrating Monetai SDK integration with dynamic pricing and RevenueCat for in-app purchases.

## Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 26 or later
- JDK 17

## Running the App

1. Open the project in Android Studio
2. Set your SDK key, user ID, and RevenueCat API key in `Constants.java`
3. Sync the project with Gradle files
4. Run the app on an emulator or physical device

## Building from Command Line

```bash
# Build the app
./gradlew :examples:sample-java-purchase:assembleDebug

# Install the app
./gradlew :examples:sample-java-purchase:installDebug
```

## Features

- Monetai SDK initialization and dynamic pricing offers (`getOffer`)
- RevenueCat integration for in-app purchases
- Product list with discount badge display
- Event tracking (`logEvent`, `logViewProductItem`)
- Entitlement status display
- Java implementation using `MonetaiSDKJava` wrapper
