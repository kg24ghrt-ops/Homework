plugins {
    // AGP 9.0+ now has built-in Kotlin support; 
    // org.jetbrains.kotlin.android is no longer strictly required.
    id("com.android.application") version "9.0.1"
    
    // Kotlin 2.0+ uses the Compose Compiler Gradle Plugin
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

android {
    namespace = "com.codingwithumair.app.vidcompose"
    compileSdk = 35 // Updated for 2026

    defaultConfig {
        applicationId = "com.codingwithumair.app.vidcompose"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Enabled for production optimization
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
    
    // NOTE: composeOptions.kotlinCompilerExtensionVersion is DEPRECATED.
    // The version is now managed by the "org.jetbrains.kotlin.plugin.compose" plugin above.
}

dependencies {
    // --- Media3: The 2026 way ---
    // Use the native Compose module (no more AndroidView wrapping needed for basic use!)
    implementation("androidx.media3:media3-exoplayer:1.9.2")
    implementation("androidx.media3:media3-ui:1.9.2")
    implementation("androidx.media3:media3-ui-compose:1.9.2") // Native PlayerSurface
    implementation("androidx.media3:media3-session:1.9.2")

    // --- UI & Lifecycle ---
    implementation(platform("androidx.compose:compose-bom:2026.01.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    
    // --- Navigation & Images ---
    implementation("androidx.navigation:navigation-compose:2.9.2") // Supports Type-Safety
    implementation("io.coil-kt.coil3:coil-compose:3.0.0") // Coil 3.0 is the standard now
    
    // Landscapist/Glide if you still need specific Glide transformations
    implementation("com.github.skydoves:landscapist-glide:2.4.0")

    // --- Core & Testing ---
    implementation("androidx.core:core-ktx:1.15.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
