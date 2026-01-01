// ============================================================================
// GroPOS - Settings
// ============================================================================
// Kotlin Multiplatform POS Application
// Modules: shared, desktopApp, androidApp
// ============================================================================

rootProject.name = "GroPOS"

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        // CouchbaseLite repository
        maven("https://mobile.maven.couchbase.com/maven2/dev/")
    }
}

// ============================================================================
// Module Definitions (per ARCHITECTURE_BLUEPRINT.md)
// ============================================================================

include(":shared")
include(":desktopApp")
include(":androidApp")

// Future: include(":hardware-legacy") when migrating existing Java code

