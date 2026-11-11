plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.aritxonly.bdfun.infra.bili"
    compileSdk = 37
    buildToolsVersion = "37.0.0"
    defaultConfig { minSdk = 33 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin { jvmToolchain(21) }

dependencies {
    testImplementation(libs.junit)
    implementation(libs.okhttp)
    implementation(libs.coroutines.android)
}
