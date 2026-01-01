# ============================================================================
# GroPOS - ProGuard Rules
# ============================================================================
# Per zero-trust.mdc: Ensure proper obfuscation for release builds
# ============================================================================

# ----------------------------------------------------------------------------
# Kotlin
# ----------------------------------------------------------------------------
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }

# ----------------------------------------------------------------------------
# Kotlinx Serialization
# ----------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep domain models that use @Serializable
-keep,includedescriptorclasses class com.unisight.gropos.**$$serializer { *; }
-keepclassmembers class com.unisight.gropos.** {
    *** Companion;
}
-keepclasseswithmembers class com.unisight.gropos.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ----------------------------------------------------------------------------
# Ktor
# ----------------------------------------------------------------------------
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ----------------------------------------------------------------------------
# CouchbaseLite
# ----------------------------------------------------------------------------
-keep class com.couchbase.lite.** { *; }
-dontwarn com.couchbase.lite.**

# ----------------------------------------------------------------------------
# Compose
# ----------------------------------------------------------------------------
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ----------------------------------------------------------------------------
# Koin
# ----------------------------------------------------------------------------
-keep class org.koin.** { *; }
-dontwarn org.koin.**

