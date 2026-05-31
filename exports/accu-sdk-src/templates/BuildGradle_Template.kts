// ╔══════════════════════════════════════════════════════════════════════════╗
// ║              ACCU SDK — app/build.gradle.kts Template                   ║
// ║                                                                          ║
// ║  Sections marked ▶ ADD are required for ACCU integration.               ║
// ╚══════════════════════════════════════════════════════════════════════════╝

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.yourcompany.yourapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yourcompany.yourapp"
        minSdk = 29        // ▶ MINIMUM: ACCU requires API 29+
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    // ▶ ADD: AIDL support — required to compile the IAccuService interface
    buildFeatures {
        aidl = true        // ◄── THIS IS REQUIRED
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")

    // ▶ ADD: Coroutines — AccuClient.requestPermission() is a suspend function
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Optional: Jetpack Compose (if your app uses it)
    // val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    // implementation(composeBom)
    // implementation("androidx.compose.ui:ui")
    // implementation("androidx.compose.material3:material3")
    // implementation("androidx.activity:activity-compose:1.9.0")

    // ▶ DO NOT add any ACCU dependency — ACCU is an installed app,
    //   not a library. You only copy the AIDL files and SDK helpers.
}

// ────────────────────────────────────────────────────────────────────────────
// HOW AIDL WORKS IN GRADLE
// ────────────────────────────────────────────────────────────────────────────
//
// When `aidl = true` is set above, the Android Gradle Plugin looks for
// .aidl files in:
//
//   app/src/main/aidl/
//
// Copy the three AIDL files from the SDK package there:
//
//   app/src/main/aidl/com/accu/api/IAccuService.aidl
//   app/src/main/aidl/com/accu/api/IAccuPermissionCallback.aidl
//   app/src/main/aidl/com/accu/api/IAccuProcessCallback.aidl
//
// Gradle will automatically generate Java/Kotlin stubs at build time.
// You do NOT commit the generated files — only the .aidl sources.
