// ============================================================================
// GroPOS - Shared Module
// ============================================================================
// Kotlin Multiplatform shared code
// Source sets: commonMain, desktopMain, androidMain
// ============================================================================

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // ========================================================================
    // Target Platforms
    // ========================================================================
    
    // Android target (API 24+ per PLATFORM_REQUIREMENTS.md)
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    
    // Desktop target (JVM for Windows/Linux)
    jvm("desktop")
    
    // ========================================================================
    // Source Sets
    // ========================================================================
    
    sourceSets {
        // --------------------------------------------------------------------
        // Common (Shared across ALL platforms)
        // --------------------------------------------------------------------
        val commonMain by getting {
            dependencies {
                // Compose UI
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                
                // Coroutines
                implementation(libs.kotlinx.coroutines.core)
                
                // Serialization
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                
                // Networking
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.ktor.client.logging)
                
                // Dependency Injection
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                
                // Navigation
                implementation(libs.voyager.navigator)
                implementation(libs.voyager.screenmodel)
                implementation(libs.voyager.koin)
                implementation(libs.voyager.transitions)
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        
        // --------------------------------------------------------------------
        // Desktop (Windows/Linux - JVM)
        // --------------------------------------------------------------------
        val desktopMain by getting {
            dependencies {
                // Compose Desktop
                implementation(compose.desktop.currentOs)
                
                // Desktop-specific Ktor engine
                implementation(libs.ktor.client.cio)
                
                // Coroutines for Swing
                implementation(libs.kotlinx.coroutines.swing)
                
                // CouchbaseLite for JVM (per DATABASE_SCHEMA.md)
                implementation(libs.couchbase.lite.java)
                
                // Hardware communication
                implementation(libs.jserialcomm)
            }
        }
        
        val desktopTest by getting {
            dependencies {
                implementation(libs.kotlin.test.junit)
                implementation(libs.junit)
            }
        }
        
        // --------------------------------------------------------------------
        // Android
        // --------------------------------------------------------------------
        val androidMain by getting {
            dependencies {
                // Android-specific Ktor engine
                implementation(libs.ktor.client.android)
                
                // Android Coroutines
                implementation(libs.kotlinx.coroutines.android)
                
                // CouchbaseLite for Android (per DATABASE_SCHEMA.md)
                implementation(libs.couchbase.lite.android)
                
                // Koin Android
                implementation(libs.koin.android)
                
                // ----------------------------------------------------------------
                // Android Hardware SDKs (per ANDROID_HARDWARE_GUIDE.md)
                // ----------------------------------------------------------------
                
                // Sunmi POS Printer SDK
                implementation(libs.sunmi.printer)
                
                // CameraX for barcode scanning
                implementation(libs.camerax.core)
                implementation(libs.camerax.lifecycle)
                implementation(libs.camerax.view)
                implementation(libs.camerax.camera2)
                
                // MLKit Barcode Scanning
                implementation(libs.mlkit.barcode)
            }
        }
        
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.kotlin.test.junit)
                implementation(libs.junit)
            }
        }
    }
}

// ============================================================================
// Android Configuration
// ============================================================================

android {
    namespace = "com.unisight.gropos.shared"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 24  // Android 7.0 per PLATFORM_REQUIREMENTS.md
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

