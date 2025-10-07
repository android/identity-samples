/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.authentication.shrinewear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.authentication.shrine"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String", "API_BASE_URL",
            "\"https://project-sesame-426206.appspot.com\""
        )
        buildConfigField(
            "String", "GOOGLE_SIGN_IN_SERVER_CLIENT_ID",
             "\"PASTE_YOUR_SERVER_CLIENT_ID_HERE\""
        )
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file(project.rootProject.file("debug.keystore"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    dependencies {
        // The trailing comments indicate version change status post migration.
        // TODO(johnzoeller): Remove trailing comments once mobile dependency versions are updated.
        implementation(platform(libs.compose.bom))      // "2025.05.00" to .01
        implementation(libs.core.ktx)                   // 1.15.0 to 1.13.1
        implementation(libs.activity.compose)           // bom 2025.05.00 to specific 1.9
        implementation(libs.appcompat)                  // NO CHANGE
        implementation(libs.compose.material.icons)     // Bom "2025.05.00" to .01
        implementation(libs.compose.ui)                 // bom "2025.05.00" to .01
        implementation(libs.credentials)                // NO CHANGE
        implementation(libs.credentials.play.services.auth) // NO CHANGE
        implementation(libs.lifecycle.runtime.ktx)      // 2.8.7 -> 2.7.0
        implementation(libs.lifecycle.viewmodel.compose)    // 2.8.7 -> 2.7.0
        implementation(libs.navigation.compose)         // 2.9.0 -> 2.7.7

        // Wear Androidx Dependencies
        implementation(libs.wear.compose.material3)     // New
        implementation(libs.wear.compose.navigation)    // New
        implementation(libs.wear.compose.ui.tooling)    // New
        implementation(libs.wear.compose.foundation)    // New
        implementation(libs.wear.remote.interactions)   // New

        // KotlinX
        implementation(platform(libs.kotlin.bom))       // New
        implementation(libs.kotlinx.coroutines.core)    // New
        implementation(libs.kotlinx.coroutines.android) // New
        implementation(libs.kotlinx.serialization.core) // New
        implementation(libs.kotlinx.serialization.json) // New

        // Wear Horologist SIWG composables
        implementation(libs.horologist.auth.ui)
        implementation(libs.horologist.compose.layout)

        // GMS
        implementation(libs.google.id)                  // NO CHANGE
        implementation(libs.playServicesWearable)       // New

        // Http Server
        implementation(libs.okhttp)                     // NO CHANGE
        implementation(libs.okhttp.logging.interceptor) // NO CHANGE

        // For Legacy Sign in With Google
        implementation(libs.play.services.auth)         // 21.1.1 -> 21.3.0
    }
}
