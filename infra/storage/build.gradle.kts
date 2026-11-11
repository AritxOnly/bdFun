plugins { alias(libs.plugins.androidLibrary) }
android {
    namespace = "com.aritxonly.bdfun.infra.storage"
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
    implementation(project(":core:domain"))
    testImplementation(libs.junit)
    testImplementation(libs.json.jvm)
}
