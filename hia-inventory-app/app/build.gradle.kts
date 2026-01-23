plugins {
    id("com.android.application")
    kotlin("android")
}

// Release signing via environment variables with fallback to debug signing
val keystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")
val keystorePassword: String? = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val keyAliasEnv: String? = System.getenv("ANDROID_KEY_ALIAS")
val keyPasswordEnv: String? = System.getenv("ANDROID_KEY_PASSWORD")

android {
    compileSdk = 34
    namespace = "com.example.hia"

    defaultConfig {
        applicationId = "com.example.hia"
        minSdk = 23
        targetSdk = 33
        versionCode = 1

        //kzeng changed to 1.0.6
        // versionName = "1.0"
        versionName = "1.0.8"
    }

    signingConfigs {
        create("release") {
            storeFile = keystorePath?.let { file(it) }
            storePassword = keystorePassword
            keyAlias = keyAliasEnv
            keyPassword = keyPasswordEnv
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (
                keystorePath != null &&
                keystorePassword != null &&
                keyAliasEnv != null &&
                keyPasswordEnv != null
            ) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.12"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Material Components (View system) for Theme.Material3 styles
    implementation("com.google.android.material:material:1.12.0")

    // Compose Foundation (pager, grids, gestures)
    implementation("androidx.compose.foundation:foundation")

    // CameraX dependencies
    val camerax_version = "1.3.2"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")
    implementation("androidx.camera:camera-extensions:${camerax_version}")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.4.0")

    // Preferences DataStore for persisting settings
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // FTP client for connectivity testing
    implementation("commons-net:commons-net:3.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}


//kzeng added to rename debug apk
// Temporarily commented out due to Gradle compatibility issue
// androidComponents {
//     onVariants(selector().withBuildType("debug")) { variant ->
//         variant.outputs.forEach { output ->
//             // 设置输出文件名
//             output.outputFileName.set("boku-hia.apk")
//         }
//     }
// }
