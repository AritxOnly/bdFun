plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeCompiler) apply false
}

// 新模块接线前先检查边界，避免将平台实现带回核心层。
val checkLayering by tasks.registering(Exec::class) {
    group = "verification"
    commandLine("python3", rootProject.file("tools/check-layering.py"))
}
subprojects {
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(rootProject.tasks.named("checkLayering"))
    }
}
