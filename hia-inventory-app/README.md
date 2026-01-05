# hia-inventory-app

## Overview
This project is an Android application designed for book inventory management. It allows users to record the location of books, take photos, and manage photo uploads.

## Features
- **Inventory Management**: Book inventory layout (placeholder UI).
- **Photo Management**: Photo add/manage layout (placeholder UI).
- **Settings Configuration**: Basic settings layout (placeholder UI).
- **Navigation**: Bottom navigation between Inventory, Photos, and Settings.

# HIA Inventory App

A Kotlin Android app using Jetpack Compose Material 3 for book inventory, location tagging, photo management, and upload. This repository ships with a working UI skeleton matching the provided prototypes. Business logic can be added later.

## Requirements
- Android Studio 2025.2
- JDK 17 (required by AGP 8.2)
- Kotlin 1.9.22
- Material 3 (Jetpack Compose)
- Minimum SDK 23; Target SDK 33 (Android 13)

## Project Setup
1. Open Android Studio and select "Open". Choose the folder hia-inventory-app.
2. Android Studio will import Gradle settings from [settings.gradle.kts](settings.gradle.kts) and sync dependencies.
3. If you see the error "Gradle wrapper jar not found":
	 - Install Gradle locally, then generate the wrapper:

	 ### Ubuntu 22
	 ```bash
	 sudo apt update
	 sudo apt install gradle
	 cd hia-inventory-app
	 gradle wrapper
	 ./gradlew clean
	 ./gradlew assembleDebug
	 ```

	 ### Windows (PowerShell)
	 ```powershell
	 winget install Gradle.Gradle
	 Push-Location "C:\Users\zen82746.LI\Desktop\hia\hia-inventory-app"
	 gradle wrapper
	 .\gradlew.bat clean
	 .\gradlew.bat assembleDebug
	 ```

4. In Android Studio, use "Build > Make Project" or run the Gradle task assembleDebug.

## What’s Implemented
- Compose Material 3 UI scaffolds
	- Inventory screen: [app/src/main/kotlin/com/example/hia/ui/screens/InventoryScreen.kt](app/src/main/kotlin/com/example/hia/ui/screens/InventoryScreen.kt)
	- Photos screen: [app/src/main/kotlin/com/example/hia/ui/screens/PhotosScreen.kt](app/src/main/kotlin/com/example/hia/ui/screens/PhotosScreen.kt)
	- Settings screen: [app/src/main/kotlin/com/example/hia/ui/screens/SettingsScreen.kt](app/src/main/kotlin/com/example/hia/ui/screens/SettingsScreen.kt)
- Bottom navigation + NavHost in [app/src/main/kotlin/com/example/hia/MainActivity.kt](app/src/main/kotlin/com/example/hia/MainActivity.kt)
- Material 3 theme in [app/src/main/kotlin/com/example/hia/ui/theme/Theme.kt](app/src/main/kotlin/com/example/hia/ui/theme/Theme.kt) and [Type.kt](app/src/main/kotlin/com/example/hia/ui/theme/Type.kt)
- Gradle configured for Compose:
	- Root plugins in [build.gradle.kts](build.gradle.kts)
	- Repositories in [settings.gradle.kts](settings.gradle.kts)
	- Compose setup and dependencies in [app/build.gradle.kts](app/build.gradle.kts)

## Notes
- Manifest uses Theme.Material3.DayNight.NoActionBar for Compose: [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml)
- Permissions declared for camera and location; upload/storage logic to be added later.
- Compose BOM and Navigation Compose are used; Kotlin compiler extension set to 1.5.12.

## Next Steps
- Wire real inventory data and location capture.
- Integrate camera/gallery and upload flows.
- Replace placeholder texts with strings.xml resources where needed.
1. Clone the repository.
2. Open the project in Android Studio 2025.2.
3. Ensure JDK 17 is selected (AGP 8 requires Java 17).
4. Let Android Studio sync Gradle; it will download/update the Gradle wrapper as needed.
5. Build and run the application on an Android 13 device/emulator.

### Optional: Build from terminal (Windows)
If you prefer command line builds, generate the Gradle wrapper JAR first:

```powershell
Push-Location "C:\Users\<you>\path\to\hia-inventory-app"; gradle wrapper; ./gradlew.bat clean; ./gradlew.bat assembleDebug
```

Note: `gradle wrapper` requires a local Gradle installation. Android Studio can handle this automatically if you open the project.

## Future Enhancements
- Implement additional business logic for inventory and photo management.
- Improve user interface based on user feedback.
 - Match UI precisely to `proto/*.png` designs.