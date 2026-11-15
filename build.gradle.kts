plugins {
    // AGP 9 compiles Kotlin itself; the Kotlin plugins below pin the Kotlin version it uses.
    id("com.android.application") version "9.5.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21" apply false
}
