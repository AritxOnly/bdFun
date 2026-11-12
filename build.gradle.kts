plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeCompiler) apply false
}

val testLayeringRules by tasks.registering(Exec::class) {
    group = "verification"
    commandLine("python3", rootProject.file("tools/test_layering.py"))
}

val testModuleGraphRules by tasks.registering(Exec::class) {
    group = "verification"
    commandLine("python3", rootProject.file("tools/test_module_graph.py"))
}

val moduleGraphReport = layout.buildDirectory.file("reports/architecture/module-graph.tsv")
val exportModuleGraph by tasks.registering {
    group = "verification"
    // 配置完成后读取 Gradle 的 ProjectDependency，不通过正则猜测脚本里的依赖。
    doLast {
        val rows = allprojects.sortedBy { it.path }.flatMap { module ->
            listOf("project\t${module.path}") + module.configurations
                .filterNot { it.name.contains("test", ignoreCase = true) }
                .flatMap { configuration ->
                    configuration.dependencies
                        .withType(org.gradle.api.artifacts.ProjectDependency::class.java)
                        .map { "edge\t${module.path}\t${configuration.name}\t${it.path}" }
                }.distinct().sorted()
        }
        moduleGraphReport.get().asFile.apply {
            parentFile.mkdirs()
            writeText(rows.joinToString("\n", postfix = "\n"))
        }
    }
}

val checkModuleGraph by tasks.registering(Exec::class) {
    dependsOn(exportModuleGraph, testModuleGraphRules)
    group = "verification"
    commandLine("python3", rootProject.file("tools/check-module-graph.py"), moduleGraphReport.get().asFile)
}

// 新模块接线前先检查边界，避免将平台实现带回核心层。
val checkLayering by tasks.registering(Exec::class) {
    dependsOn(testLayeringRules, checkModuleGraph)
    group = "verification"
    commandLine("python3", rootProject.file("tools/check-layering.py"))
}
subprojects {
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(rootProject.tasks.named("checkLayering"))
    }
}
