plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvmToolchain(21)

    jvm()            // fast unit tests for the repository layer (and the future backend)
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // DatabaseDriverFactory is an expect/actual class (still Beta in Kotlin 2.1); opt in quietly.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core-diff"))
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies { implementation(libs.sqldelight.android.driver) }
        iosMain.dependencies { implementation(libs.sqldelight.native.driver) }
        jvmMain.dependencies { implementation(libs.sqldelight.sqlite.driver) }
        jvmTest.dependencies { implementation(libs.sqldelight.sqlite.driver) }
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
