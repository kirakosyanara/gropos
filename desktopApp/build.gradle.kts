// ============================================================================
// GroPOS - Desktop Application
// ============================================================================
// Entry point for Windows and Linux desktop builds
// ============================================================================

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                
                // Coroutines for Swing event loop
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        
        val desktopTest by getting {
            dependencies {
                implementation(libs.kotlin.test.junit)
                implementation(libs.junit)
            }
        }
    }
}

// ============================================================================
// Desktop Application Configuration
// ============================================================================

compose.desktop {
    application {
        mainClass = "MainKt"
        
        // Native distribution settings
        nativeDistributions {
            // Target platforms
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            
            packageName = "GroPOS"
            packageVersion = "1.0.0"
            description = "GroPOS - Point of Sale System"
            vendor = "UniSight"
            
            // Windows-specific
            windows {
                menuGroup = "GroPOS"
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
            
            // Linux-specific
            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
                debMaintainer = "support@unisight.com"
                appCategory = "Office"
            }
            
            // macOS-specific (for development)
            macOS {
                iconFile.set(project.file("src/main/resources/icon.icns"))
                bundleID = "com.unisight.gropos"
            }
            
            // JVM options
            jvmArgs(
                "-Xmx512m",
                "-Dfile.encoding=UTF-8"
            )
        }
    }
}

