pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        flatDir {
            dirs("libs")
        }
        maven { url = uri("https://jitpack.io") }
    }
}

include(":app")
include(":PluginCore")
include(":ConfigDSL")

rootProject.name = "StarLight"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")