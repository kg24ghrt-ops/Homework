// settings.gradle.kts

pluginManagement {
    repositories {
        google()                // ← REQUIRED - Android plugin lives here
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()                // ← REQUIRED - AndroidX, Compose, etc.
        mavenCentral()
    }
}

rootProject.name = "Homework"
include(":app")