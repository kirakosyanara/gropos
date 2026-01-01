// ============================================================================
// GroPOS - Android Application
// ============================================================================
// Entry point for Android POS devices (Sunmi, PAX, etc.)
// Target API 24-34 per PLATFORM_REQUIREMENTS.md
// ============================================================================

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core.ktx)
                
                // Koin for dependency injection
                implementation(libs.koin.android)
            }
        }
    }
}

// ============================================================================
// Android Configuration
// ============================================================================

android {
    namespace = "com.unisight.gropos"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.unisight.gropos"
        minSdk = 24      // Android 7.0 per PLATFORM_REQUIREMENTS.md
        targetSdk = 34   // Android 14 per PLATFORM_REQUIREMENTS.md
        versionCode = 1
        versionName = "1.0.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

