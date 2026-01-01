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

### Changed
- Renamed application from "GrowPOS" to "GroPOS" across all documentation files (39 occurrences in 8 files)