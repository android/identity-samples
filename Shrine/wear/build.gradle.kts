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
        implementation(platform(libs.compose.bom))
        implementation(libs.core.ktx)
        implementation(libs.activity.compose)
        implementation(libs.appcompat)
        implementation(libs.compose.material.icons)
        implementation(libs.compose.ui)
        implementation(libs.credentials)
        implementation(libs.credentials.play.services.auth)
        implementation(libs.lifecycle.runtime.ktx)
        implementation(libs.lifecycle.viewmodel.compose)
        implementation(libs.navigation.compose)

        // Wear Androidx Dependencies
        implementation(libs.wear.compose.material3)
        implementation(libs.wear.compose.navigation)
        implementation(libs.wear.compose.ui.tooling)
        implementation(libs.wear.compose.foundation)
        implementation(libs.wear.remote.interactions)

        // KotlinX
        implementation(platform(libs.kotlin.bom))
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.coroutines.android)
        implementation(libs.kotlinx.serialization.core)
        implementation(libs.kotlinx.serialization.json)

        // Wear Horologist SIWG composables
        implementation(libs.horologist.auth.ui)
        implementation(libs.horologist.compose.layout)

        // GMS
        implementation(libs.google.id)
        implementation(libs.playServicesWearable)

        // Http Server
        implementation(libs.okhttp)
        implementation(libs.okhttp.logging.interceptor)

        // For Legacy Sign in With Google
        implementation(libs.play.services.auth)
    }
}
