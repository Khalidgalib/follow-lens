plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core-diff"))
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies { implementation(libs.sqldelight.android.driver) }
        iosMain.dependencies { implementation(libs.sqldelight.native.driver) }
    }
}

sqldelight {
    databases {
        create("FollowLensDb") {
            packageName.set("app.followlens.data.db")
        }
    }
}

android {
    namespace = "app.followlens.data"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
}
