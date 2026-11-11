# AGENTS.md — bdFun 开发与重构指南

> **这份文件是给谁看的**：任何在本仓库工作的 AI 编码助手（Codex / Claude / OpenCode / Cursor 等），以及新加入的人类协作者。
> **它解决什么问题**：AI 每次都是"失忆上岗"——不知道项目现状、不知道哪里不能碰、不知道团队约定的写法。这份文件就是它的**入职手册**。
> **怎么维护**：架构或约定变了，先改这份文件，再改代码。文件末尾有变更记录。

---

## 0. 三条铁律（如果只读一段，读这里）

1. **不许破坏现有体验。** 任何改动必须满足：能编译、能装机、能回滚。不确定就问，不要"顺手优化"。
2. **新代码必须分层；旧代码按 `docs/MIGRATION_TODO.md` 逐步搬，不在旧代码上继续加深耦合。**
3. **B 站接口的字段含义不许猜。** 拿不准就看真实返回、看 `docs/` 里的对齐文档，或问人。猜错字段名的代价是线上白屏。

---

## 1. 项目是什么

- **bdFun**：第三方哔哩哔哩 Android 客户端（非官方，仅学习研究）。
- **包名**：`com.aritxonly.bdfun`（debug 构建会自动加 `.debug` 后缀，两者可共存）。
- **技术栈**：Kotlin 2.4.21 + Jetpack Compose 1.12.2 + Miuix（HyperOS 风格组件库）+ OkHttp + Media3/ExoPlayer + Coil。
- **系统要求**：`minSdk 33` / `compileSdk 37` / JDK 21。
- **界面语言**：简体中文，文案直接写在 Kotlin 里（**历史现状，暂不引入 `strings.xml`**，见 §5）。

### 构建命令

```bash
# Windows
gradlew.bat :app:assembleDebug
gradlew.bat :app:testDebugUnitTest
gradlew.bat :danmaku-engine:test

# Linux / macOS
./gradlew :app:assembleDebug
```

**构建前必须知道的三件事**：

1. 本项目用**本地 Miuix 源码**参与 composite build。源码放在 `../miuix-src/miuix-45645854cd46c32275c5666e3aafa9980d8f104e`，或用环境变量 `MIUIX_SOURCE` 指定。也可运行 `python3 tools/setup-miuix.py` 准备 `.local/` 下的固定快照；找不到时回退到 Maven 依赖，但**版本必须对齐**（AGP 9.5.1 不能和别的 AGP 混用）。
2. 每次构建会把时间戳写进 `BuildConfig.BUILD_STAMP` 和 `versionName`，所以**每次构建产物的版本号都不同**，这是故意的，别当 bug 修。
3. 可以用 `-Ppkg=com.example.app` 覆盖包名做实验，不加时 debug 包名是 `com.aritxonly.bdfun.debug`。

---

## 2. 目录地图（当前）

| 位置 | 内容 | 状态 |
|---|---|---|
| `app/src/main/kotlin/com/aritxonly/bdfun/` | 20 个 Activity | 生命周期、旋转、画中画、媒体键、下载服务 |
| `.../bdfun/ui/` | 76 个 Compose 文件 | 界面 + 大量业务逻辑（待拆） |
| `.../bdfun/data/` | 20 个文件 | `BiliApi`(3758 行)、`SettingsStore`(888 行)、模型、缓存 |
| `.../bdfun/AppGraph.kt` | 进程级单例 + 页面间传参 | 待替换为 `AppContainer` |
| `.../bdfun/App.kt` | `BdFunApp` 主壳 + 底栏 | 523 行，待拆 |
| `.../bdfun/ui/PlayerUiState.kt` | `VideoPageBus` / `PlayerUiState` / `ReplyTarget` | 全局 Bus，迁移期保留 |
| `.../top/yukonga/miuix/kmp/effect/bg/` | 背景特效（随项目内联的 miuix 片段） | 纯 UI，与业务无关 |
| `TeakUI/` | 设计系统模块（玻璃材质、页面骨架、导航组件） | **已分层，勿动** |
| `danmaku-engine/` | 弹幕解析与渲染引擎 | **已分层、19 个测试，勿动** |
| `core/model/` | P0-01 纯 Kotlin 业务模型（迁移期与旧模型并存） | 已建骨架 |
| `core/common/`、`core/domain/` | 格式化、结果类型与只读仓储接口 | P0 骨架 |
| `core/application/`、`core/navigation/` | SessionStore 与 Route 契约 | P0 空壳，未替换运行时 |
| `app/.../di/AppContainer.kt` | 进程依赖创建与初始化 | AppGraph 委托 |
| `infra/bili/` | HTTP 执行、取消、重试与异常类型 | P1 迁移中 |
| `tools/` | 核心层依赖检查、固定 Miuix 快照准备脚本 | Python 3 |
| `docs/` | 架构审查、对齐文档、来源说明 | |

### 现状规模（2026-11 实测，别当永久事实）

- 271 个 Kotlin 文件 / 约 63,700 行；`:app` 占 44,390 行。
- `ui/` 里直接调用 `api.xxx()` **115 处**，直接读全局设置 **63 处**。
- ViewModel / StateFlow / Repository / UseCase：**0 处**。
- 全局 `object`（含 5 个 `*Bus`）：**23 个**。
- 测试：`:app` 7 个、`TeakUI` 2 个、`danmaku-engine` 19 个。

---

## 3. 目标架构（重构完成后长这样）

完整论证见 **`docs/ARCHITECTURE_REVIEW.md`**；这里只给执行时需要的部分。

```
:app                   组装壳：路由表 + AppContainer(依赖注入) + 启动逻辑
feature:*              按业务切：home / video / dynamic / message / search /
                       space / library / live / settings / onboarding
                      每个内部四层：presentation → application → (domain)
core:model             实体与值对象（纯 Kotlin）
core:domain            仓储接口 + 纯业务策略（纯 Kotlin，无 Android/网络/JSON）
core:application       跨屏状态：Session、PlaybackController、DownloadQueue
core:navigation        路由契约（feature 之间不互相依赖）
core:common            结果类型、错误、日志、格式化
core:ui                = 现有 `:TeakUI` 模块的**目标称呼**；不重命名、不新建
infra:bili             HTTP、签名、Cookie、各业务 Api 分片、dto、mapper
infra:storage          设置(DataStore)、播放进度、下载缓存
infra:playback         ExoPlayer 适配、后台播放 Service、媒体通知、画中画
```

### 依赖规则（违反即打回）

| 规则 | 说明 |
|---|---|
| `presentation` **只能**依赖 `application` 与 `core:ui` | 不许出现 `api.xxx()`、不许 `import okhttp/org.json` |
| `application` **只能**依赖 `domain` 与 `core:model` | 不许出现 Compose 的 `@Composable` |
| `domain` **零依赖** | 不许出现 `import android.*` / `okhttp3` / `org.json` / `coil` |
| `infra` 实现 `domain` 的接口 | 只被 `:app` 在启动时装配，`feature` 看不见实现 |
| `feature` 之间禁止互相 `import` | 跨 feature 走 `core:navigation` 契约或 `core:application` 状态 |
| 完成标准 | 单文件 ≤ 400 行，单函数 ≤ 80 行，单函数参数 ≤ 12 个 |

> **模块名对照**：上图是**目标**模块名。当前仓库只有 `:app`、`:TeakUI`、`:danmaku-engine` 三个模块。
> `:core:ui` 指的就是现在的 `:TeakUI`（**不要新建第四个 UI 模块**）；其余 `core:*` / `infra:*` / `feature:*`
> 按 `docs/MIGRATION_TODO.md` 的 P0、P1、P5 逐步创建，不要一步到位。

### 迁移期的"双轨制"

新旧代码会并存一段时间，按下面办：

1. **新功能一律写在新位置**（feature/core/infra），不写进 `BiliApi` 和巨型 Composable。
2. **搬旧代码只做"搬迁"**：不改行为、不改逻辑、不顺手重命名。要改行为，另开一个任务。
3. **搬完就删旧文件**，不留 `XxxOld.kt` / 注释掉的副本 / 双写。
4. **一个 PR 只做一件事**，PR 描述里写任务编号（如 `P1-03`）。

---

## 4. 代码约定（从现有代码提炼，不是凭空规定）

| 项 | 约定 |
|---|---|
| 缩进 / 风格 | 4 空格；`kotlin.code.style=official`；项目**没有** ktlint/detekt 配置，别引入 |
| 注释 | **中文**，解释"为什么这么做"，不解释"这行在干嘛" |
| 可见性 | 模块内部优先 `internal`，只有跨模块契约才 `public` |
| Composable 命名 | 大写驼峰（`VideoGridCard`）；`@Composable` 只出现在 presentation 层 |
| 状态 | 新代码用**不可变 `UiState` + `StateFlow`**；`mutableStateOf` 只允许出现在 StateHolder 内部 |
| 错误处理 | 旧代码大量 `runCatching{}.getOrNull()`（历史写法，允许保留）；**新代码用显式 `Result<T, AppError>` 或领域错误类型** |
| 日志 | 用 `data/LogCollector.kt`（仅 debug 生效，内存 3000 行、文件 2MB 上限）；**禁止 `println`** |
| 依赖 | 版本锁在 `gradle/libs.versions.toml`；**新增依赖必须在 PR 里写明理由与替代方案评估** |
| 文案 | 中文硬编码（现状）。不要"顺手"迁移到 `strings.xml`，那是独立任务 |
| 网络 | 不要在主线程；`BiliApi` 内部已 `withContext(Dispatchers.IO)`，新代码沿用这个边界 |
| 线程 | 不要自己 `Thread{}`（少数历史代码除外）；用 coroutines |

---

## 5. 常见任务手册

### 5.1 新增一个 B 站接口

1. 先用真实请求确认路径、参数、返回字段（别猜）。
2. 落点：`infra:bili` 下对应分片（`VideoApi` / `CommentApi` / …）。
3. 写 `dto`（只描述 JSON 结构）+ `mapper`（转成 `core:model` 的实体）。
4. 在 `core:domain` 的仓储接口上加方法；实现放 `infra:bili`。
5. 补 mapper 单测：拿一段**真实返回的 JSON 字符串**当固定样本。
6. 迁移期如果 UI 还要走旧的 `BiliApi`，就加一个薄转发，**不要复制逻辑**。

### 5.2 新增一个页面

1. 建 `feature:<名字>`，内部 `presentation/` + `application/`。
2. 路由写在 `core:navigation` 的契约里，`Activity`/导航壳只负责传参。
3. 页面入参**必须显式**，不许写进 `AppGraph.pendingXxx`（迁移期旧页面除外）。
4. 首屏状态要有三种：加载中 / 空 / 出错（现有页面的通病是只有"加载中"和"空白"）。

### 5.3 新增一个设置项

1. 落点：`core:domain` 的 `SettingsRepository` 分组接口（外观 / 播放 / 弹幕 / 消息 / 通用）。
2. 存储实现放 `infra:storage`；**不要**往 888 行的 `SettingsStore` 里再加字段。
3. 默认值必须写在一个地方（不要在 UI 里散落 `?: 80`）。
4. 涉及播放器/弹幕的，要在设置页和实际生效处都验证一遍。

### 5.4 改播放器行为

1. 先读 `ui/VideoCollapse.kt` 里的纯函数（那是全项目少有的、已抽出来的决策逻辑，照着写）。
2. 播放器的三个敏感点：**旋转、画中画、后台播放**。改任意一个，另两个必须回归验证。
3. 后台播放的入口在 `ui/BackgroundPlayback.kt` + `BackgroundPlaybackService.kt` + `MediaPlayback.kt`，三处状态要一致。
4. 自动暂停/恢复的逻辑有单测（`app/src/test/.../VideoExitOwnershipTest.kt` 等），别绕过。

### 5.5 改弹幕渲染

`danmaku-engine` 里已有清晰分层（`parser` / `engine` / `render` / `control`），**按它的接口扩展，不要在 `app` 里写渲染逻辑**。改动必须补测试——它是全项目测试最全的模块，别拉低水位。

---

## 6. 已知坑（真实踩过 / 代码里写死的约束）

| 坑 | 说明 |
|---|---|
| Miuix 版本锁 | composite build 不能混 AGP 版本，升级 Miuix 要同时对齐 AGP / Kotlin / Compose |
| 主界面限帧 | `MainActivity` 把窗口钉在 60Hz（底栏玻璃每帧要录整屏）。**删了会掉帧**，别当冗余代码清理 |
| `BUILD_STAMP` | 每次构建都变，导致 `versionName` 变化。是特性，不是 bug |
| debug 包名 | 自动加 `.debug` 后缀；`-Ppkg=` 会覆盖并**取消**该后缀 |
| `minSdk 33` | 不要加降级兼容代码；也不要用 34+ 才有的 API 而不做版本判断 |
| `FreezeWatchdog` | 只在 debug 启动（`AppGraph.init`）。别在 release 打开 |
| `LogCollector` | release 不初始化、不写文件；只有 debug 有崩溃记录 |
| 大量 `remember` 状态 | 屏幕旋转/进程回收会丢；现有 `*Memory` 单例是补丁。新代码不要再加这种补丁 |
| `AppGraph.pendingXxx` | 隐式传参，少写一行就打开空白页。新代码禁止使用 |
| R8 fullMode | `android.enableR8.fullMode=true`；反射/序列化相关改动要在 release 构建下验证 |
| 测试稀疏 | `:app` 只有 7 个测试。**新逻辑没测试就等于没有保障**，这是当前最大风险 |

---

## 7. 测试与验证

```bash
gradlew.bat :danmaku-engine:test          # 快，先跑这个
gradlew.bat :app:testDebugUnitTest        # app 逻辑测试
gradlew.bat :app:assembleDebug            # 最终必须过
```

**硬要求**：

- `core:domain` 及任何纯逻辑的新增/修改，**必须**有纯 JVM 测试（不启模拟器、不联网）。
- 覆盖不到的 UI 改动，在 PR 里贴**手动验证清单**。视频相关至少验证：进入播放 → 切清晰度 → 切分P → 全屏旋转 → 画中画 → 后台播放 → 退出 → 重新进入恢复进度。
- 不许为了让测试过而改测试的断言（除非原断言本身就是错的，且 PR 里说明）。

---

## 8. 文档索引

| 文件 | 内容 |
|---|---|
| `docs/ARCHITECTURE_REVIEW.md` | 现状审查、新旧架构对比图、文件去向表、验收标准 |
| `docs/MIGRATION_TODO.md` | 可领取的迁移任务清单（编号、前置、验收） |
| `docs/DEADLINER_GLASS_SYNC.md` | 玻璃材质与 Deadliner 的同步约定 |
| `docs/HYPERPILI_DYNAMIC_ALIGNMENT.md` | 动态页与上游实现的对齐说明 |
| `docs/HYPERPILI_MESSAGE_ALIGNMENT.md` | 消息页与上游实现的对齐说明 |
| `TeakUI/docs/PAGE_CHROME_SPEC.md` | 页面骨架规范 |
| `TeakUI/docs/PROVENANCE.md` | 组件来源与许可 |
| `README.md` | 面向用户的介绍与构建说明 |

---

## 9. 给 AI 助手的工作流程（建议照做）

1. **先读**：本文件 → `docs/ARCHITECTURE_REVIEW.md` 的"六个病灶" → 当前任务编号在 `docs/MIGRATION_TODO.md` 里的条目。
2. **再定位**：用搜索找真实调用点，不要凭文件名猜。
3. **小步改**：一次只做任务卡上写的事；扩大的部分记下来，另开任务。
4. **自检**：对着 §3 的依赖规则表和 §3 的完成标准（行数/参数）检查一遍。
5. **报告**：说明改了什么、为什么、怎么验证的、有什么没做。

---

## 10. 变更记录

| 日期 | 变更 | 作者 |
|---|---|---|
| 2026-11-11 | 初版：现状盘点、目标分层、依赖规则、约定、已知坑 | 架构审查 |
