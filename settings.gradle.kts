rootProject.name = "follow-lens"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":core-diff")
include(":data")
include(":app")
// ":backend" is added later, when the online phase starts. It will also depend on ":core-diff".
