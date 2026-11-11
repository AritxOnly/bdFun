plugins {
    alias(libs.plugins.kotlinJvm)
}

kotlin { jvmToolchain(21) }

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.core)
    implementation(project(":core:model"))
    implementation(project(":core:common"))
}
