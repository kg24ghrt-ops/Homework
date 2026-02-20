plugins {
    // Update this to match what you want for the whole project
    alias(libs.plugins.android.application) apply false 
    // OR if you aren't using Version Catalogs:
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
}
