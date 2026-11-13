# Kotlin 尺寸审计（开发工具）

使用与项目相同的 Kotlin 2.4.21 PSI。编译器只在此 Java/JVM 工具的运行时中使用，不加入 Android APK。

```bash
# 查看完整报告，包含超标项时仍返回 0，不能作为尺寸通过证据
./gradlew :tools:kotlin-audit:reportKotlinSizes

# 严格检查：超标、语法解析失败或未找到源码均失败
./gradlew :tools:kotlin-audit:checkKotlinSizes
```

任务先运行审计器的 JVM 测试，再扫描默认仓库根目录。完整 JSON 写入根目录 `build/reports/kotlin-audit/report.json`；终端最多列出 60 条违规。

范围为自有 app main/debug 的 `com/aritxonly/bdfun`，以及 core/infra/feature 的 `src/main` Kotlin；不扫描测试、生成目录、TeakUI、弹幕引擎或内联供应商源码。类、函数、lambda、访问器、主次构造器、init 块和函数类型均按 PSI 节点定位，不以正则猜括号或屏蔽整个字符串插值。

规则：文件最多 400 物理行，函数体/初始化块最多 80 行，声明输入参数最多 12 个。主构造器的属性参数也计入；函数类型和显式 context 参数计入，不计隐式 this/扩展接收者。普通块包含花括号，表达式体按完整表达式范围，lambda 按整个字面量（含花括号与声明参数）计行。注释与空行仍属于所处物理范围；CRLF/CR 先归一为 LF，使 PSI 偏移与行号一致。

这是源码语法树尺寸检查，不做符号解析或业务正确性验证。无法扫描时会移除旧报告，已扫描但超标时写入当轮诊断。模块图守卫禁止app依赖此开发工具。当前真实源码有超标项，严格任务尚未并入 `checkLayering`；清零前不能用 report 任务或旧具名块函数扫描宣称达标。

`auditRoot` 和 `auditReport` 属性仅用于独立正反例验证；正常检查使用默认仓库根目录。构建环境和 Miuix 源码配置沿用根 AGENTS.md。
