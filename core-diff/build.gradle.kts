plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    jvmToolchain(21)

    jvm()            // used by unit tests now, and by the future Spring Boot backend
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ksoup)
            implementation(libs.okio)
            // FakeFileSystem gives us an in-memory filesystem to hand a zip's raw bytes to
            // Okio's openZip() without needing a real platform file path — used in production
            // (ZipImport), not just tests.
            implementation(libs.okio.fakefilesystem)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "app.followlens.diff"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
