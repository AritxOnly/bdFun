# 重构决策与验证记录

日期：2026-11-11。分支：`codex/p0-01-core-model`。

## 用户授权的范围

全量分层重构；自行处理技术债及统一页面脚手架；最终以默认签名构建 release，附发布说明发送给飞书当前账号本人。任务未验收之前，不将阶段安装包描述为全量重构完成版。

## 已落地决策

- P0 先建立五个纯 JVM 核心模块；保留旧模型和现有页面接线。
- 41 个模型声明原样复制，包含动态直播卡片的关联模型；不猜接口字段。
- `Outcome` 和领域错误类型集中到 common；仓储方法取消语义要求保留协程取消。
- 格式化保留原有行为，包括负数；三项 JVM 测试覆盖边界、超大数、跨年。
- `AppContainer` 收拢初始化，MainActivity 使用 AppGraph.ensure，避免重复创建进程依赖；保留主题赋值时机、debug 日志及 Watchdog 条件。
- 架构守卫接入 Gradle check；临时反例 Android import 验证返回非零。
- 找到原锁定快照的本机副本：`~/Documents/Codex/2026-11-10/https-github-com-qxteak-bdfun-https/work/miuix-src/miuix-45645854cd46c32275c5666e3aafa9980d8f104e`。含 miuix-glass，AGP/Kotlin/Compose 与工程一致。
- 源码准备脚本固定提交，写入被忽略的 `.local/`，不修改机器上的其他项目，也不把绝对路径写进构建配置。
- 页面统一采用现有 Teak 页面骨架规范，不新建第四个 UI 库；特殊播放器页面的显隐约束保留。
- 默认签名指现有 Gradle 在没有 keystore.properties 时使用 debug keystore；交付时明确标注，不能称为正式发布证书。

- 修复 MainActivity 链接解析的裸 Thread：使用 lifecycleScope + Dispatchers.IO，Activity 销毁后不再回调导航。

- 已从 GitHub codeload 下载并核对原始快照：上游 AGP 9.5.1 / Kotlin 2.4.21 / Compose 1.12.2 与本机副本一致；下载端点返回 200。
- `:infra:bili` 收拢 HTTP 异步回调、响应解析和重试；保留共享连接池和 CookieJar 注入，旧 BiliException 路径通过 typealias 兼容。签名与 URL 准备尚在旧 BiliApi，P1-01 仍未完全验收。
- 新增依赖评估：新模块复用工程已有 OkHttp、coroutines 与 JUnit，不引入另一套网络框架或依赖注入框架。

## 验证

- core:model:build：通过。
- core:common:test：3 项通过，0 失败。
- core:domain/build、core:application/build、core:navigation/build：通过。
- 架构守卫正例与反例：通过。
- P0 + HTTP 抽离后集成验证通过：App 45 项 + common 3 项 + HTTP 3 项，共 51 项测试，0 失败；`:app:assembleDebug` 通过。
- 首轮 release 遭遇 lint 内部 AsyncExecutionService/PSI 异常；那轮构建期间仍修改过源码，不能用于最终验收。固定源码后单独串行重跑 release，通过 lint 和 R8；默认签名 APK 的 apksigner v2 验证通过。
- 装机：adb devices 未发现设备，不能声称完成手动回归。
- 飞书：已验证当前账号身份，未发送阶段包。

## 尚未完成

P1 HTTP 执行与重试已抽离待集成验证；尚余签名、接口分片、DTO/mapper 与业务策略；P2 页面状态迁移；P3 大文件拆分；P4 全局 Bus 与隐式参数清除；P5 可选模块化。不得因骨架可编译将以上项目勾选完成。
