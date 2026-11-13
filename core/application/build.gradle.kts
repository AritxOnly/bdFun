plugins {
    alias(libs.plugins.kotlinJvm)
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))
    implementation(libs.coroutines.core)
    implementation(project(":core:common"))
    testImplementation(libs.junit)
    implementation(project(":core:domain"))
}
