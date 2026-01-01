// ============================================================================
// GroPOS - Root Build Configuration
// ============================================================================
// Kotlin Multiplatform POS Application
// Targets: Desktop (Windows/Linux), Android
// ============================================================================

plugins {
    // Kotlin Multiplatform - applied to subprojects
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    
    // Android
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    
    // Compose Multiplatform
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
}

// ============================================================================
// Subproject Configuration
// ============================================================================

subprojects {
    // Apply common configurations to all subprojects
    afterEvaluate {
        // Kotlin compiler options (per kotlin-standards.mdc)
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            compilerOptions {
                // Treat all warnings as errors in CI
                // allWarningsAsErrors.set(System.getenv("CI") != null)
                
                // Enable progressive mode for newer language features
                progressiveMode.set(true)
            }
        }
    }
}

// ============================================================================
// Clean Task
// ============================================================================

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

