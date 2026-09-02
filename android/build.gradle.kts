// Root build file. Declares plugin versions once here (apply false = "make
// available, don't apply to this project") so the app module below can
// apply them without repeating version numbers.
plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
