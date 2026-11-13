plugins { java }
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
dependencies {
    implementation(libs.kotlin.compiler.embeddable)
    testImplementation(libs.junit)
}

fun registerAudit(name: String, reportOnly: Boolean) = tasks.register<JavaExec>(name) {
    group = "verification"
    dependsOn(tasks.test)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.aritxonly.bdfun.tools.KotlinSizeAudit"
    args(providers.gradleProperty("auditRoot").getOrElse(rootProject.projectDir.absolutePath),
        providers.gradleProperty("auditReport").getOrElse(
            rootProject.layout.buildDirectory.file("reports/kotlin-audit/report.json").get().asFile.absolutePath))
    if (reportOnly) args("--report-only")
}
registerAudit("reportKotlinSizes", true)
registerAudit("checkKotlinSizes", false)
