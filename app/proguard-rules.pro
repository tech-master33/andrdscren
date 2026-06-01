# andrdscren — ProGuard / R8 rules

# ── Accessibility service must survive shrinking ───────────────────────────
# Android's AccessibilityService is bound by the system by class name.
# Renaming or removing it will break screen reader activation.
-keep class org.baosp.andrdscren.** { *; }
-keep class com.example.andrd.** { *; }

# ── AndroidX Accessibility framework ──────────────────────────────────────
-keep class androidx.core.view.accessibility.** { *; }
-keep class androidx.core.accessibilityservice.** { *; }
-keep class android.accessibilityservice.** { *; }

# ── Jetpack Compose ────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# ── Kotlin metadata (required for coroutines, Compose compiler) ───────────
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# ── Standard Android components ───────────────────────────────────────────
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.accessibilityservice.AccessibilityService

# ── Suppress known-safe warnings ──────────────────────────────────────────
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn androidx.compose.**
