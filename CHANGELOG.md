# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased] - 2026-01-01

### Added
- Initial Kotlin Multiplatform project scaffolding
- Gradle version catalog (`libs.versions.toml`) with all core dependencies
- Root `build.gradle.kts` and `settings.gradle.kts` configuration
- Module structure: `shared/`, `desktopApp/`, `androidApp/`
- Shared module configured for commonMain, desktopMain, androidMain source sets
- CouchbaseLite database dependency configured for offline-first architecture
- ProGuard rules for Android release builds
- Comprehensive `.gitignore` for KMP projects
- Auth feature domain layer: `AuthUser`, `UserRole`, `AuthError` models
- Auth feature domain layer: `AuthRepository` interface
- Auth feature domain layer: `ValidateLoginUseCase` with PIN validation (4-digit rule)
- Auth feature data layer: `FakeAuthRepository` with simulated 500ms network delay
- Unit tests for `ValidateLoginUseCase` (7 test cases, TDD approach)
- `gradle.properties` with AndroidX and KMP configuration

### Changed
- Renamed application from "GrowPOS" to "GroPOS" across all documentation files (39 occurrences in 8 files)