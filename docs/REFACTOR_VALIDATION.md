# 重构验证记录

## 自动验证

本轮使用 JDK 21，Miuix 固定到 `45645854cd46c32275c5666e3aafa9980d8f104e`。准备源码后运行：

```bash
python3 tools/setup-miuix.py # 已准备则无需重复运行
python3 tools/check-layering.py
bash ./gradlew :infra:bili:testDebugUnitTest :core:common:test :app:testDebugUnitTest :app:assembleDebug --no-parallel --max-workers=2
bash ./gradlew :app:assembleRelease --no-parallel --max-workers=2
```

- App：45 项通过，0 失败。
- core:common：3 项通过，覆盖格式化边界和跨年。
- infra:bili：3 项通过，覆盖只对 GET 重试、永久错误、退避抖动边界。
- debug APK：构建通过。
- 核心层依赖检查：通过；临时 Android import 反例验证非零退出。
- release：固定源码后串行构建通过，lintVital 与 R8 全部通过（4m 8s）。
- `apksigner verify --verbose --print-certs`：APK v2 签名校验通过，使用用户指定的项目默认 Android Debug 证书。
- APK：`app/build/outputs/apk/release/app-release.apk`。
- APK SHA-256：`d4b8993518b2b732237824341be04205289c3080720202bec7740e4c55b023b6`。

## 手动验证清单（未执行）

`adb devices -l` 未检测到连接设备。本清单不能用自动单测结果替代：

- [ ] 冷启动首页、刷新与翻页、进入视频再返回保持列表。
- [ ] 扫码登录、退出、重启保持登录。
- [ ] 视频进入播放、切清晰度、切分 P、全屏旋转。
- [ ] 画中画、后台播放、通知栏与耳机按键、退出再进恢复进度。
- [ ] 评论、收藏、投币等写操作不重复提交。
- [ ] 消息页各 tab、私信、系统消息删除。
- [ ] 空间页小店请求、空间投稿与合集。
- [ ] 断网、取消页面加载、恢复网络。
- [ ] 冷启动链接跳转、解析过程中退出 Activity。

## 交付条件

当前是 P0 与 HTTP 执行层抽离的阶段结果，不等同于全量重构完成。release 验证产物不作为全量完成版发送飞书；全量任务、回归与发布说明完成后，再按用户授权交付。

## 合入 main 后的请求与签名迁移验证

- main 基线：`671d264`；合并提交：`e63e590`。阶段收尾 fetch 无新增提交。
- 使用仓库 `.local/` 自动发现的 Miuix 快照，无需临时 MIUIX_SOURCE 路径。
- `checkLayering :core:common:test :infra:bili:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug --no-parallel --max-workers=2 --offline`：通过。
- App 45 项、common 3 项、infra:bili 14 项，共 62 项，0 失败。
- Pixel_9 只读临时实例：安装成功，MainActivity COLD 启动成功；首页推荐加载真实数据，视频详情与起播成功；个人主页头部和滚动小标题可见；crash 缓冲为空。临时实例已关闭，未保存原 AVD 快照。
- 截图：`build/reports/refactor/main-home.png`、`main-video.png`、`main-space.png`、`main-space-scrolled.png`（构建目录，不提交）。
- 上述验证没有覆盖切清晰度、切分 P、横屏、系统画中画、后台控制、扫码登录或完整消息流程。此前的 release 产物不对应本阶段源码，最终交付前须重新构建签名 release。

## 设置升级验证

- infra:storage 的 JSON 编码、格式错误回退测试通过。
- 81 项可写设置逐项默认值及往返测试通过；4 项迁移/范围/浮点合并/搜索历史测试通过。App 全量单测合计 116 项通过；设置覆盖协议的筛选重跑也通过。
- 临时 Pixel_9 AVD：安装迁移前 APK，写入全部旧偏好键，冷启动记录落盘基线；覆盖安装新 APK，冷启动成功。81 个设置键 + 搜索历史共 82 项比对，0 缺失、0 变化，crash 缓冲为空。
- 结果文件：build/reports/refactor/settings-upgrade-result.json（构建目录，不提交偏好值）。
- debug 构建通过。该检查点不代表尚未完成的页面状态和播放器迁移已经验收。

## 五类页面状态与账号作用域检查点

- 2026-11-11：`checkLayering :core:application:test :app:testDebugUnitTest :app:assembleDebug` 通过。
- app 157、infra:bili 35、infra:storage 2、core:domain 28、core:common 4、core:application 5 项失败/错误均为 0。
- 弹幕引擎 97 项现有测试在补齐 Kotlin test / Robolectric 后全部通过，生产引擎未修改。
- Pixel_9 只读临时实例覆盖安装：首页真实推荐加载、进入视频实际起播；空间迁移检查点继续运行检查。当前安装检查点早于 Session 改造，不能冒充账号切换装机验证。
- 新增纯逻辑用例覆盖：消息补全过期、退出隔离、重复分页、关注/点赞取消回滚、删除失败保留、移动到原夹、历史全游标、空间合集切换；没有使用线上账号发起写操作。

## P2-07 详情与相关推荐检查点

- 2026-11-11：checkLayering、:app:testDebugUnitTest、:app:assembleDebug 通过；app 共 163 项，失败/错误 0。
- 新增 6 项纯 JVM 测试：缓存推荐快照、失败读取不覆盖缓存、离线清空、详情错误与合集请求参数、详情/合集恢复保留推荐、两类读取的取消传播。
- TeakUI 和 VideoActivity 对 main 671d264 的源码差异为零；这不等于截图/运行效果验收。
- 按用户安排，完整视觉和行为对照后续统一进行。待验收：进入播放、清晰度、分P/合集、相关推荐返回、旋转、画中画、后台播放、退出重进、离线及错误文案。
- 本轮未装机；受限环境内 adb 无法绑定本地监听端口。debug APK 已构建，不能以构建通过代替安装或播放验证。

## 视频、评论、显式路由与页面作用域

- 最新检查：checkLayering、:core:application:test、:app:testDebugUnitTest、:app:assembleDebug 通过。app 200、core/application 12 项均无失败/错误；:infra:storage:testDebugUnitTest 7 项通过。
- 新增逻辑覆盖播放先就绪/弹幕后补、旧清晰度回包、清晰度与分P的弹幕版本差别、离线恢复失效旧请求；互动取消回滚、重复提交、三连部分失败；字幕关闭/切P/在线人数旧响应；评论排序/根楼切换、取消分页、发送取消、账号切换；页面状态隔离与所有权暂停。
- 导航快照测试完整比较模型，包含嵌套动态与视频快照；不把编解码测试当作实际进程杀死恢复验证。
- 当前 adb devices 无连接设备。本阶段尚未安装或截图对照；用户已安排视觉/行为对照留到统一验收。仍需完成播放器/后台/画中画/旋转/恢复进度及动态评论的装机回归，再构建对应最终源码的默认签名 release。
- TeakUI 库未修改；VideoActivity 仅迁路由与页面状态提供，不改变 tab/浮层控件树。展示区块拆文件保留现有尺寸、颜色和动画参数，但不能以源码检查代替视觉验收。

## 共享播放控制器与模拟器检查点

- 中间版本：:core:application:test 与 :app:testDebugUnitTest 通过，app 204、core/application 22 项无失败/错误。页面壳与全屏展示拆分各检查点的 checkLayering / debug 构建通过。系统媒体迁入和评论拆分后的最终检查仍以本段后续记录为准，不能将旧 APK 对应到最新源码。
- Pixel_9 只读临时 AVD（emulator-5580）安装了共享播放控制器检查点：首页推荐真实加载，打开 BV1orW46aEba，详情/合集/相关推荐出现，画面实际渲染并播放到结束；评论 tab 加载共 228 条、首条附图及折叠回复可见。crash 缓冲为空。没有执行任何点赞、投币、关注、收藏或评论发送等线上写操作。
- 截图证据暂存 /tmp/bdfun-controller-video-ready.png、/tmp/bdfun-controller-comments.png；它们不是最新完整迁移版与 main 的最终逐页对照。
- 模拟器冷启动出现一次 System UI 无响应等待弹窗，点击等待后恢复。没有将其认定为 bdFun 崩溃；软件渲染与并行构建的设备负载不作为真实性能验收依据。

- 最新源码检查点：/tmp/bdfun-playback-final-checkpoint.log 显示 BUILD SUCCESSFUL；checkLayering、:core:application:test、:app:testDebugUnitTest、:app:assembleDebug 均通过（1m 51s）。该构建包含媒体基础设施迁移、页面壳局部状态、播放器/全屏/评论展示拆分。后续装机应使用这份 APK；全量验收仍未完成。

## 队列、刷新率、表情缓存与评论拆分检查点

- :core:domain:test、:core:application:test、:app:testDebugUnitTest、checkLayering 与 :app:assembleDebug 在 /tmp/bdfun-comment-list-sections.log 中通过（34s）。此前队列/刷新率、EmoteStore、播放器参数分组各次检查点均通过。
- 新增覆盖队列顺序/缺失锚点/列表快照、刷新率门槛、表情并发预取/取消/账号切换/失败重试。追加所有权用例验证旧页面不能清掉新页面 fullscreen 提示。
- 最新安装版经正式导出的 MainActivity VIEW 链接进入 BV1orW46aEba，截图 /tmp/bdfun-latest-runtime.png 显示详情、合集、推荐、弹幕与播放结束画面，crash 缓冲为空。该 APK 早于本段新改动；本段新改动已构建，尚未安装，不能混报。

## 楼中楼状态与详情元信息展示检查点

- /tmp/bdfun-thread-focus-final.log：checkLayering、:app:testDebugUnitTest、:app:assembleDebug 全部通过（33s）；app 208 项，失败/错误 0。
- 新增 4 项 JVM 测试验证根楼/parent/本地回复投影、null 返回不产生假回复、0 ID 时间戳回退、删除失败保留与另一根楼状态隔离。没有线上发帖或删除评论。
- 本段仅编译/逻辑验证；拆出的元信息、楼中楼窗口/输入区尚未在最新 APK 上做 main 截图对照，仍在完整视觉与播放回归待办中。

## 播放入参与前后台选集领域策略

- /tmp/bdfun-page-policy.log：checkLayering、:core:domain:test、:app:testDebugUnitTest、:app:assembleDebug 通过（32s）。app 210 项、core/domain 34 项失败/错误均为 0。
- 新增 3 项选集策略测试覆盖原多P优先/合集去重/单合集条目回退，2 项展示投影测试覆盖详情未回包时元数据、自动文案/进度与手动清晰度/实际应用状态。
- 详情几何、列表尾部与状态投影拆分尚未在对应最新 APK 上逐项运行验证，最终仍需与 main 基准对照；源代码搬迁与纯函数测试不替代完整播放、旋转、画中画和后台交接回归。

## 播放进度旧格式与详情 effect 检查点

- /tmp/bdfun-detail-effects.log：checkLayering、:app:testDebugUnitTest、:app:assembleDebug 通过（25s），app 213 项无失败/错误。此前 /tmp/bdfun-progress-storage.log 的 :core:domain:test 与构建通过，core/domain 42 项无失败/错误。
- 新增历史跟踪 3 项、历史门槛 3 项、进度兼容与边界 5 项：覆盖慢请求不重复、取消后 busy 释放但保留原重试周期、关闭片段拒绝旧响应，旧 CSV/未知时长/非法数据、3秒与末尾5秒严格边界、短视频、500→400淘汰与坏时间戳。
- 本轮拆分与进度适配的最新 APK 仅完成构建，尚未安装；最终运行回归与 main 视觉对照不能由上述测试推定。

## 2026-11-12 · 播放器 effect 与基本装机检查点

- /tmp/bdfun-player-ui-state.log、bdfun-player-lifecycle.log、bdfun-player-media-bridge.log、bdfun-player-control-observer.log、bdfun-player-source-progress.log、bdfun-player-window-danmaku.log 中 checkLayering、:app:testDebugUnitTest、:app:assembleDebug 均通过。最后一次构建 32s，app 213 项无失败/错误。
- 安装 bdfun-player-window-danmaku 构建对应 APK 到 emulator-5580，正式 VIEW 入口打开 BV1orW46aEba：视频实际渲染、弹幕、详情、合集、相关推荐出现；返回首页后唱片入口重开视频，保留播放完成状态；crash 缓冲为空。截图 /tmp/bdfun-effect-runtime.png、/tmp/bdfun-effect-disc-return.png。
- 向已在前台的 MainActivity 再投递相同 VIEW Intent 后停留首页，后续用唱片入口重开。此现象需与 main 的 onNewIntent 行为核对；本轮不将其当作“深链重入通过”或擅自修改行为。
- 本次未执行清晰度/分P、旋转、画中画、进程回收、登录态写操作或 main 截图对照；也不据播放结束后的重开推定“中途进度恢复通过”。后续展示区块拆分的 APK 需要再次安装。

## 2026-11-12 · 播放器展示拆分与横竖屏检查点

- /tmp/bdfun-nov12-checkpoint.log：checkLayering、core/common/model/domain/application、infra/bili/storage、danmaku-engine 与 app 测试及 debug 构建通过（41s）。有用例模块分别为 common 4、domain 42、application 30、bili 35、storage 7、danmaku 97、app 213，失败/错误均为 0。
- 后续全屏动作、画面层、手势绑定、全屏覆盖层与弹幕输入拆分的 app 测试/架构检查/debug 构建逐次通过；最后 /tmp/bdfun-player-input-cleanup.log 为 17s。一次手势抽取遗漏属性后逗号导致编译失败，已更正，/tmp/bdfun-player-gesture-bindings-fixed.log 通过。没有更改测试断言。
- 最新 APK（input-cleanup 构建，仅随后删除 EOF 空行）重新安装到临时 emulator-5580，停止旧进程后经正式 VIEW 冷启动，BV1orW46aEba 实际渲染并显示弹幕；半屏点击呼出控件、点全屏按钮进入横屏、点击呼出全屏控件、返回键恢复竖屏；crash 缓冲为空。系统首次全屏提示经 Got it 关闭后继续验证。
- 截图：/tmp/bdfun-player-split-runtime.png、/tmp/bdfun-player-controls-immediate.png、/tmp/bdfun-player-split-fullscreen.png、/tmp/bdfun-player-fullscreen-controls-ready.png、/tmp/bdfun-player-portrait-return.png。全屏切换时视频已播放完成，不据此推定播放中的旋转/连续性完整通过。
- VideoPlayer 为 399 行、VideoScreen 为 375 行；主组装函数仍超过 80 行，部分控件参数与其他大页面仍需继续拆分。TeakUI 未修改；未完成 main 逐页视觉对照、清晰度/分P、画中画、后台播放、进程回收、中途进度恢复、最终 release 签名与交付，全量目标仍在进行。

## 2026-11-12 · 播放器短组装、半屏契约与小窗入口检查点

- /tmp/bdfun-inline-contract.log、inline-more-contract.log、inline-bindings.log、inline-more-bindings.log、player-composition-root.log、player-sponsor-policy.log、inline-actions-final.log 及 inline-audio-more-sections.log 的架构检查、app 单测、debug 构建均通过。最后一份为 30s，包含 domain 测试；app 214、domain 45 项均无失败/错误。
- 新增领域 3 项、状态容器 1 项，覆盖片段区间/顺序/关闭/无效数据及不同 bvid 的快照隔离。更多面板分块首次缺少 onDismiss 参数导致编译失败，补上传递后最后构建通过；未更改原测试断言。
- 最新 APK（inline-audio-more-sections 构建）已安装，正式 VIEW 冷启动后真实视频、弹幕、详情加载，更多玻璃面板正常展开。4 秒首次截图仍是启动空白，稍后截图已播放；本轮不据此下性能结论，需在最终 main 同环境对照时记录冷启动时间。
- 更多面板“小窗播放”进入 PiP，dumpsys activity 确认 com.aritxonly.bdfun.debug 的任务为 mode=pinned；系统 PiP 控件展开后回到同一详情，crash 缓冲为空。截屏小窗处于播放完成状态，未证实播放中持续推进，也不推定 seek/恢复通过。
- 证据：/tmp/bdfun-inline-contract-runtime-ready.png、/tmp/bdfun-inline-more-open.png、/tmp/bdfun-inline-contract-pip-ready.png、/tmp/bdfun-pip-expanded-page.png。没有发送评论或执行点赞/投币/关注/收藏等线上写操作。
- 主入口函数现为 29 行、VideoPlayer 文件 51 行；其余播放器长函数（全屏听视频、全屏弹幕输入、手势等）、详情长组装及其余大页面继续迁移。最终逐页 main 对照、播放中旋转/PiP/后台/恢复、默认签名 release 与交付仍未完成。

## 2026-11-12 · 全屏长函数与手势会话检查点

- /tmp/bdfun-fullscreen-audio-sections.log、fullscreen-composer-sections.log、player-gesture-session.log、gesture-swipe-contract.log、player-panel-sections.log、fullscreen-chrome-sections.log、player-seekbar-sections.log 和 player-short-functions-final.log：checkLayering、app 单测与 debug 构建逐次通过。最终 13s，app 221 项，失败/错误均为 0；git diff --check 通过。
- 新增输入状态 3 项与手势 4 项 JVM 测试：空/无发送器、busy 防重、trim 与原文本保留、协程执行时样式读取、异常收尾；动态回调与宽度、中心上滑严格边界、双击取消待单击、长按移动只加速并松手结束。没有新增测试运行时依赖。
- 最新 APK（player-short-functions-final）重新装入 emulator-5580，经正式 VIEW 起播并显示真实弹幕；全屏更多、听视频正常显示。后退 10 秒后的一次快速播放点击仍为 PAUSED，随后单独点击播放成功；系统媒体状态两次为 PLAYING，position 从 90034 推进到 91863。此记录证明独立播放操作可用，不据此认定快速连点/调度时序验收通过，最终需与 main 对照。
- 全屏弹幕输入自动取得焦点并弹键盘；点击“较小”“置顶”后，关闭键盘仍显示对应选中样式和颜色列表；退出输入与全屏期间 crash 缓冲为空，未发送弹幕或执行线上互动写操作。
- 截图：/tmp/bdfun-player-short-functions-runtime.png、/tmp/bdfun-fullscreen-short-more.png、/tmp/bdfun-fullscreen-audio-short-runtime.png、/tmp/bdfun-fullscreen-audio-play-state.png、/tmp/bdfun-fullscreen-composer-short-runtime.png、/tmp/bdfun-fullscreen-composer-options.png、/tmp/bdfun-player-short-functions-portrait.png。
- 播放器展示分块不代表全量完成；详情/Activity 与其余大页面、全局对象审计、main 视觉与行为逐项对照、播放中旋转/PiP/后台交接及恢复、最终 release 与交付仍在待办。

## 2026-11-12 · 详情短组装与 Activity 壳检查点

- /tmp/bdfun-detail-screen-contract.log、detail-viewport.log、detail-pager-content-fixed.log、detail-screen-root.log、video-entry-state.log、video-activity-content.log、video-activity-sections.log 的构建及 app 检查通过。pager 提取时漏导入 Color/Dp 的初次编译失败，补齐后通过；未更改原测试断言。
- 最后 /tmp/bdfun-video-activity-final-checkpoint.log：checkLayering、core/domain、core/application、infra/bili、infra/storage 与 app 测试及 debug 构建通过（20s）。app 230、domain 45、application 30、bili 35、storage 7 项，失败/错误均为 0。git diff --check 通过；TeakUI 无源码改动。
- 新增 3 项窗口几何测试覆盖半收起进度、沉浸/非沉浸顶部、全屏/PiP/分栏禁用及超范围/零范围；新增 6 项入口测试覆盖快照与显式进度保留、BV/AV 优先与原字段、缺失/失败关闭、取消传播/释放 busy、旧失败不覆盖新入口。入口测试不联网。
- 最新 APK（video-activity-final-checkpoint）安装到 emulator-5580，正式外部 BV VIEW → Route.Video → VideoEntryStateHolder 路径真实起播，详情/合集/推荐出现；评论 tab 加载 238 条并显示图片、预览回复及回复栏。进入全屏后返回仍选评论，列表位置保留；该次全屏已到播放结束，不推定播放中的旋转连续性。
- 输入覆盖层可打开；一次双 Back 操作返回首页，唱片可重开视频；重开后单 Back 截图仍显示输入条，点击空白处后输入条关闭并留在原视频。这里只确认打开和外部关闭，不将 Back/IME 时序标为最终通过，后续仍与 main 对照。未发送弹幕/评论或执行线上互动写操作，crash 缓冲为空。
- 证据：/tmp/bdfun-video-activity-split-runtime.png、video-activity-comments.png、video-activity-fullscreen.png、video-activity-comments-return.png、video-activity-danmaku-input.png、video-activity-input-dismissed.png、video-activity-input-single-back.png、video-activity-input-outside-dismiss.png（后七项同为 /tmp/bdfun- 前缀）。
- 详情子区块仍有长函数（元信息、合集、描述、互动、推荐尾部等），其余页面/全局对象审计、main 逐页对照、完整播放回归、默认签名 release 及最终交付均未完成；本段不是全量完成声明。

## 2026-11-12 · 详情展示区块与主壳状态检查点

- /tmp/bdfun-metadata-action-split.log、collection-description-split.log、detail-sections-checkpoint.log 与 detail-sections-final.log 的 checkLayering、app 单测和 debug 构建通过；最终 15s，app 230 项无失败/错误。ui/video 目录的源码长度与忽略注释/字符串后的函数体初查未发现文件超过 400 行或函数超过 80 行；这是源码初查，不替代行为验收。
- detail-sections-final APK 安装到 emulator-5580，经正式 BV VIEW 加载作者/标题/统计/简介/互动/合集/推荐；第一次 8 秒截图仍在缓冲，后续实际视频与弹幕正常。合集展开出现原条目和当前集高亮，顶部按钮滚到首集，定位按钮回到当前末集；未点击选集或执行线上互动写操作。
- 切全屏时抓到过系统旋转动画中间帧；媒体状态为 PLAYING、position=66535。随后稳定全屏已播放完成，返回竖屏和评论页正常，238 条评论及回复栏显示；crash 缓冲为空。这里只证明进入全屏时正在播放，不推定整个旋转/后台/PiP/中途恢复回归完成。
- 截图 /tmp/bdfun-detail-sections-runtime.png、detail-collection-expanded.png、detail-collection-top.png、detail-collection-current.png、detail-sections-fullscreen.png、detail-sections-fullscreen-settled.png、detail-sections-portrait-return.png、detail-sections-comments.png（后七项同 /tmp/bdfun- 前缀）。
- 消息角标的 /tmp/bdfun-message-badge-state.log 构建/架构/app 测试通过（28s），新增 5 项测试；app 总数升为 235。测试不联网。主题/底栏拆分初次遗漏唱片回调的 context 迁移，改为显式 onOpenVideo 后 bottom-navigation-fixed 构建通过；main-tabs-shell 随后通过（23s）。没有改原测试断言，没有新增依赖。
- App.kt 当前 392 行，主函数还需拆分；主题、经典/宽屏/悬浮底栏、唱片和 tab 组装位于 presentation/shell。TeakUI 无源码改动。全部页面迁移、全局对象与作用域审计、main 逐页对照、完整播放回归、默认签名 release 和最终交付仍未完成。

- 最后 /tmp/bdfun-main-shell-checkpoint.log：checkLayering、app 测试和 debug 构建通过（20s），XML 汇总 235 项，失败/错误均为 0。对应 APK 安装后首页真实推荐加载，“我的”/“动态”的游客态和选中底栏正常显示，首页搜索热搜正常加载；单次系统 Back 后搜索仍在（焦点/IME 时序未作为通过），右侧 X 仅清空输入，左侧箭头关闭搜索并返回首页，未改变实现。主壳三种样式中的经典/宽屏、已登录角标、重复点击刷新和唱片让位仍需最终 main 对照验证。
- 主壳证据 /tmp/bdfun-main-shell-home.png、main-shell-mine.png、main-shell-dynamic.png、main-shell-search.png、main-shell-search-back.png、main-shell-search-closed.png（实际是清空输入后的搜索页）、main-shell-search-arrow-return.png（其余同 /tmp/bdfun- 前缀）。运行期间 crash 缓冲为空；git diff --check 通过。此轮未生成 release、未执行最终签名或飞书交付，全量目标保持进行。

## 2026-11-12 · 主壳短入口与搜索迁移检查点

- /tmp/bdfun-main-short-root.log、search-state.log、search-sections.log 和 search-final-checkpoint.log 的构建/架构/相应测试通过。最终 13s；app XML 汇总 246、core/domain 50、infra/bili 的 testDebugUnitTest 38 项，失败/错误均为 0。新增 app 11、domain 5、bili 3 项，覆盖搜索防抖/先后/同词缓存、错误与旧结果、清空/取消/过期响应隔离、时钟顺序和协议值，以及异常原文/空文案/取消传播；未更改旧断言，未新增依赖。
- App.kt 与 SearchExperience.kt 均为短入口；主壳与 ui/search 的忽略注释/字符串函数体初查无文件超过 400 行、函数超过 80 行。此为局部源码初查，不能外推所有页面或完成标准已全量满足。git diff --check 通过，TeakUI 无源码改动。
- 最新 search-final-checkpoint APK 安装到 emulator-5580，冷启动首页真实推荐加载；输入 bilibili 返回真实联想，回车提交显示视频结果，用户 tab 显示用户卡片；筛选菜单第一次快速点击未显示，后续单独点击打开，选择“新发布”可见选中样式。未据此认定筛选请求生效或快速操作时序一致。
- 结果页返回后出现保留 bilibili 的联想页，下一次左箭头关闭搜索并返回首页；过程中连点时部分中间截图仍停在原页，未将这些截图记为成功，也不据此修改回调。crash 缓冲为空。最终仍需要 main 同环境下的 IME/返回/连点对照。
- 证据：/tmp/bdfun-main-short-root-home.png、search-state-suggestions.png、search-state-results.png、search-state-users.png、search-state-filters-retry.png、search-state-filter-selected.png、main-short-root-search-return.png（实际为联想页）、main-short-root-search-closed.png（实际回到首页），后七项同 /tmp/bdfun- 前缀。没有发送评论/弹幕或操作关注/点赞/收藏等线上写入。
- 其余大页面、全局对象/作用域、main 逐页视觉和行为对照、完整播放/恢复、默认签名 release 与既有最终交付仍未完成；目标保持全量，不因这次搜索检查点缩减。

- 回到已稳定的首页后分别单独切换“我的”“动态”，游客态与底栏选中样式显示正常，再切回首页；证据 /tmp/bdfun-main-short-root-mine-ready.png、/tmp/bdfun-main-short-root-dynamic-ready.png、/tmp/bdfun-main-short-root-home-return.png。此记录不覆盖登录账号、宽屏、经典底栏或快速连点验收。

## 2026-11-12 · 空间展示短函数与分页检查点

- /tmp/bdfun-space-leaf-sections.log、space-content-fixed.log、space-short-root-fixed.log、space-ui-short-functions-fixed.log、space-live-pagination.log 和 space-final-checkpoint.log 的架构检查、app 测试与 debug 构建通过。最后 19s，app XML 汇总 252 项，失败/错误均为 0。内容提取初次遗漏 space/Dp，随后短入口提取多带了未使用别名、充电区遗漏 mid，均已在对应 fixed 日志中修正；未改原测试断言，未新增依赖。
- 新增 6 项 JVM 测试覆盖后续请求随完成页码从 2→3→4 推进、请求中禁止翻页及取消后恢复、视频结束、合集/总览不翻视频、文章/原第四 tab 的独立结束状态、动态结束；纯测试不联网。不把这些单测当作真实服务端三页请求已验证。
- SpaceActivity 60 行，SpaceScreen 文件 26 行；presentation/space 的忽略注释/字符串函数体初查未发现文件超过 400 行或函数超过 80 行，显式契约人工检查均不超过 12 项参数。git diff --check 通过；TeakUI 源码没有改动。其余页面未由此推定全部满足完成标准。
- space-live-pagination APK 安装到 emulator-5580，正式 BV VIEW → 视频作者头像 → 小猪P_ 的空间（UID 2283296）成功；资料/横幅/四主 tab 加载，动态、投稿和小店可切换；上滑收头后出现昵称与模糊，切动态仍保留收头位置，回投稿选择“小猪的AI剧场”合集后显示真实合集视频。crash 缓冲为空。首次空间截图视频格还未显示，随后投稿和重新安装后的主页视频均加载；本轮未据此下性能或接口错误结论。
- 最后 space-final-checkpoint APK（增加账号控制器 effect 重绑定）重新安装，再经同一路径进入空间；临时将模拟器锁横屏，确认左右两栏与右侧主 tabs/视频格显示，恢复原 accelerometer_rotation=1、user_rotation=0 后回到竖屏资料/视频列表。这里验证初始主页的宽窄渲染，不代表折叠实体设备、已滚动位置或所有分类旋转恢复验收完成。
- 证据 /tmp/bdfun-space-sections-runtime.png、space-sections-dynamic.png、space-sections-submissions.png、space-sections-shop.png、space-sections-collapsed.png、space-sections-collapsed-dynamic.png、space-sections-collection.png、space-final-compact.png、space-final-wide.png、space-final-portrait-return.png（后九项同 /tmp/bdfun- 前缀）。未发送私信/评论/弹幕或执行关注/点赞/收藏等线上互动写入。
- 最终 main 刷新和逐页视觉/行为对照、登录态/真实多页与滚动恢复、其余页面、全局对象/作用域、完整播放回归、默认签名 release 及最终交付仍未完成；全量目标保持进行。

## 2026-11-12 · 私信状态与路由检查点

- /tmp/bdfun-private-chat-routes-checkpoint.log：checkLayering、infra/bili、infra/storage、app 测试及 debug 构建成功（17s）。app 270、bili 45、storage 9 项，失败/错误均为 0；新增私信控制器 18 项、infra JSON/结果 7 项和路由 2 项 JVM 测试，无联网、无新增依赖。
- 覆盖重试/原错误、取消及 busy 收尾、历史合并/cursor、轮询守卫、已读失败、图片成功文字失败重试、编辑中草稿、刷新和滚动失败、关闭时上传与重试失效，以及长整型/中文会话路由与默认值。旧测试断言未改。git diff --check 通过。
- 最新 APK 安装到 emulator-5580，经正式 BV VIEW→作者空间→私信入口，显示小猪P_ 会话标题及游客消息加载失败/重试状态；表情面板可展开，本地输入 local-draft 后键盘和发送图标正常显示。未点击发送、未上传图片，crash 缓冲为空；不将游客检查当作登录后的真实历史、收发与已读验收。
- 证据 /tmp/bdfun-private-chat-space-entry.png、private-chat-guest-runtime.png、private-chat-emoji-runtime.png、private-chat-draft-runtime.png（后三项同 /tmp/bdfun- 前缀）。这只是迁移检查点，main 最终视觉/行为对照、其余大页面/全局对象/作用域、完整播放回归、默认签名 release 和最终交付均未完成。

## 2026-11-12 · 消息中心与首页短函数检查点

- /tmp/bdfun-message-scope-checkpoint.log（44s）、message-target-checkpoint.log（25s）、home-sections-fixed.log（38s）和 message-home-final-checkpoint.log（17s，后三项同 /tmp/bdfun- 前缀）均通过 checkLayering、app 单测与 debug 构建。最终 app XML 283 项，失败/错误为 0。新增消息中心/通知 8 项作用域与游标测试、5 项消息打开策略测试；未新增依赖或改变旧测试断言。
- 初次消息拆分漏了 tab 的 ui 前缀，随后共享 resume helper 的既有关系列表调用遗漏兼容入口，均已修正并通过后续构建。首页首次卡片信息带提取的注释前缀匹配过宽，修正为原完整标记后重新通过编译；没有用修改断言掩盖这些编译问题。
- presentation/home 与 presentation/message（本轮顶层文件）的忽略字符串/注释函数体初查未发现文件超过 400 行或函数超过 80 行，契约人工检查未超过 12 项参数；该局部初查不代表全仓库已满足完成标准。git diff --check 通过，TeakUI 源码无改动。
- 消息 scope-checkpoint APK 安装到 emulator-5580：从首页右上入口进入游客消息页，原快捷入口、双 tab 和登录提示显示；更多菜单显示“刷新消息/全部标为已读”，未点击已读。菜单关闭后一次连续入口点击仍停在会话页，后续单独点击“回复与@”成功进入原三 tab 通知页与“请先登录 · 点击重试”错误态。快速连续操作和“我关注的人”选中效果未据这轮截图标为验收通过，留待 main 同环境对照。
- home-sections-fixed APK 安装到同一模拟器，真实首页推荐加载，下拉释放后回落到原位，长按视频卡片显示原操作面板。未选择稍后再看或不感兴趣。证据 /tmp/bdfun-home-sections-real-feed.png、home-sections-refresh-return.png、home-sections-card-actions.png；消息证据 /tmp/bdfun-message-sessions-guest.png、message-more-sheet.png、message-replies-ready.png（其余各同 /tmp/bdfun- 前缀）。crash 缓冲为空。最后源码仅补了消息清已读后的刷新遇退出时抑制旧提示，新增 JVM 测试覆盖并通过最终构建。
- 最终 main 刷新、逐页像素/行为对照、登录后真实会话/通知分页、首页大卡与宽屏及快速操作、其余大页面/全局作用域、完整播放器回归、默认签名 release 和最终交付仍未完成。全量目标保持进行。

- 首页长按菜单关闭后的连续点击截图仍停在首页；稍后单独点击卡片成功进入 VideoActivity，真实视频与弹幕播放、标题/作者/合集/推荐出现（BV1LHWj6iEwU）。证据 /tmp/bdfun-home-sections-video-ready.png，crash 缓冲为空。不将这次成功外推为菜单关闭期间快速点击或 OneTake 动画时序与 main 已完全一致。

## 2026-11-12 · 动态展示、图片与详情检查点

- /tmp/bdfun-dynamic-sections.log（44s）、dynamic-detail-state.log（35s）、dynamic-final-checkpoint.log（25s）、dynamic-account-checkpoint.log（25s）和 dynamic-entry-diagnostic.log（20s，后四项同 /tmp/bdfun- 前缀）的对应构建/架构/测试检查通过；最后 app XML 306 项、infra/bili 48 项，失败/错误均为 0。新增 app 23 项、infra 3 项，无新依赖，旧断言未改。
- 新 JVM 覆盖图片倍率与单指/pager 边界，详情正文/缺失/失败、IP 条件与并发补全、点赞立即更新/原负数/并发相对回退/取消及过期请求，动态旧账号关闭后的列表/作者门户/作者后续页/点赞迟到结果与未来请求拒收。测试均不联网；未发送真实点赞以验证这些分支。
- 动态展示目录的局部源码初查无文件超过 400 行或函数体超过 80 行；分组后 DynamicCard 4 项参数，其余本轮契约人工检查不超过 12 项。DynamicScreen 42 行、DynamicCard 65 行、DynamicImageViewer 56 行、DynamicDetailActivity 62 行。10 个未改造展示叶子函数体逐字比对与拆分前一致；git diff --check 通过，TeakUI 源码无改动。不把这些局部源码检查当成全量完成或像素验收。
- dynamic-final-checkpoint APK 安装 emulator-5580：首页切动态显示游客提示与原底栏选中样式；正式 BV VIEW → 小猪P_ 空间 → 动态加载置顶正文/两张图片。图片预览打开显示第一张，横向滑动切到第二张，顶部关闭回到原卡片/位置，crash 缓冲为空。未实际验证双指触控、登录后的首页动态分类/作者门户/自动播放与宽屏布局。
- 后续 dynamic-account-checkpoint APK 两次进入同一公开空间出现“暂无动态”，未进入详情；/tmp/bdfun-dynamic-detail-state-runtime.png、dynamic-detail-entry-later.png、dynamic-detail-entry-retry.png 记录的是该空态，不能解释为详情成功。因原有日志没有相关错误摘要，在 SpaceContentEffects 的首次 load 完成后增加仅 debug 的 count/end/error 摘要，不改变 UI 或请求；最后 diagnostic APK 重进记录 mid=2283296 count=13 end=false error=null 并显示原动态。此前空态的具体原因尚未确认，保留最终 main 同环境对照待办。
- 最新 diagnostic APK 中，待 13 条动态加载确认后单独点击置顶正文成功进入 DynamicDetailActivity；/tmp/bdfun-dynamic-detail-ready.png 显示完整 Q&A、详情时间、两张图及共 213 条评论，回复栏和评论内容出现，crash 缓冲为空。该次确认真实详情与评论可加载，不据此认定 IP 兜底、线上点赞、评论发送或全部返回/快速操作时序通过。
- 其它证据 /tmp/bdfun-dynamic-sections-guest.png、dynamic-sections-space-card.png、dynamic-card-images-ready.png、dynamic-image-viewer-first.png、dynamic-image-viewer-second.png、dynamic-image-viewer-return.png、dynamic-entry-diagnostic-runtime.png（后六项同 /tmp/bdfun- 前缀）。没有发送评论/弹幕、点赞/关注/收藏或私信。
- InlineVideoPlayer 的网络/原生播放器与 InlineVideoCache、评论回复条、其它大页面和全局作用域审计仍在迁移；最终 main 刷新/逐页视觉行为一致性、完整播放回归、默认签名 release 与最终交付均未完成，全量目标保持进行。

- 详情左上返回后，dumpsys 确认恢复 SpaceActivity，原动态 tab 与置顶卡片可见，crash 缓冲为空；证据 /tmp/bdfun-dynamic-detail-space-return.png。此为独立返回操作，快速重复返回/转场仍需 main 对照。

## 2026-11-12 · 内嵌预览状态与原生播放器检查点

- /tmp/bdfun-inline-preview-state.log（47s）与 inline-preview-clean-checkpoint.log（31s，同 /tmp/bdfun- 前缀）通过 checkLayering、core/application、infra/bili、app 测试及 debug 构建；最终 app 316、core/application 33、infra/bili 54 项，失败/错误均为 0。新增 app 10、application 3、bili 6 项，无新测试依赖；旧断言未改。
- JVM 覆盖缓存立即命中、空 BV、一次失败与重入、加载防重/取消后重试、关闭拒收、设置晚采样，原清晰度顺序/空地址，视频/像素比例严格边界，以及 12 项缓存、播放失败保留详情、并行详情等待期间另一预览填好播放缓存与取消传播。数据/请求测试不联网。
- 使用临时 debug Activity（未加入正常导航）运行正式 loader、InlineVideoEffects/Surface、AppContainer 原生适配器与真实 BV1orW46aEba；该页只为独立验证原生预览，不代表登录动态列表中的居中选择/滚动与宽屏已验收。首次 probe 未初始化 MainActivity 所负责的日志文件，随后在临时页补日志初始化再复测；首次实际视频截图不当作播放状态日志证据。
- /tmp/bdfun-inline-preview-probe-runtime.log 记录同一 controller id=234992671：静音 volume=0，position 1207→4214 持续推进；切换静音后 volume=1；Home 后 playing=false/ready=false，连续位置保持 9954；回前台同一实例 playing=true，position 10468→12485；测试页把原生播放器定位到结尾前 1500ms 后，position 98050→602→2603、repeat=1、无 error，确认自动循环。全程未更改持久化设置或发送线上互动。
- 临时页左上返回显式 finish 后，原生日志同一 f1081d6 出现 Init→Release，回到 Launcher，crash 缓冲为空。截图 /tmp/bdfun-inline-preview-probe-playing.png、/tmp/bdfun-inline-preview-probe-verified.png。没有将模拟器表现作为性能或音频听感验收。
- 临时 Activity 源码已删除，debug manifest 按构建前备份逐字节恢复，再执行 clean-checkpoint 测试和构建。aapt2 检查最终 APK manifest 不含 InlineVideoProbeActivity，所有 dex 不含该类名或 INLINE-PROBE 标记；app/src/debug 与 TeakUI git 状态无本轮改动，git diff --check 通过。
- 正式源码仍需登录态动态预览、快速滚动/切分类、宽屏和最终 main 同环境 UI/行为对照；评论回复条、其余大页面、全局作用域/缓存审计、完整播放器回归、默认签名 release 与最终交付均未完成，全量目标保持进行。

- 最后 clean-checkpoint APK 重新安装到 emulator-5580，正常 MainActivity 冷启动加载真实推荐与原双列/底栏，crash 缓冲为空；证据 /tmp/bdfun-inline-preview-clean-home.png。当前安装包已恢复为无临时预览入口的正式 debug 源码产物。

### 2026-11-12 · 评论回复条与评论入口检查点

- 状态层首轮 /tmp/bdfun-comment-composer-state.log 成功（24s）；回复条初次接线暴露提取脚本遗留旧文件/导入/属性引用问题，已修正，未修改测试断言。修正接线 /tmp/bdfun-comment-composer-ui-fixed.log 成功（48s）：app 332、infra/bili 57，失败与错误均 0。
- 新增 CommentComposerStateHolderTest 16 项，覆盖文字/图片发送时机、旧图片保留、并发上传原顺序、空但非 null 载荷、无 sender、取消与 close、IME 同步提交后收起。BiliCommentImageRepositoryTest 3 项验证原引用/载荷透传、null、异常及取消。CommentListStateHolderTest 再增加 2 项，验证长期分页回调读取当前 busy/end/page、取消释放和原零 layout count 边界。
- /tmp/bdfun-comment-composer-final-checkpoint.log 成功（32s）；评论页 request/layout 拆分 /tmp/bdfun-comment-page-split-checkpoint.log 成功（41s）；为保持楼中楼事件时的排序快照进一步显式传参，最终 /tmp/bdfun-comment-page-final-checkpoint.log 成功（17s）。各轮 checkLayering、infra/bili:test、app:testDebugUnitTest、app:assembleDebug 均通过；最终 app 334、infra/bili 57，失败与错误均 0。
- emulator-5580 安装回复条 APK，以真实 BV VIEW 进入视频评论。确认 239 条评论加载、local-draft 本地输入、表情面板及系统键盘显示；IME 收起后草稿仍保留。最初在 IME 变化中点击图片坐标未打开选择器；待稳定后重试成功打开系统 Photos/Albums，Back 取消后草稿/表情保留，截图 picker-open-runtime / picker-return-runtime。无选图、上传、评论发送、点赞、举报或屏蔽。
- 安装评论页拆分 APK后，真实评论列表和双回复楼中楼打开/收起成功，截图 /tmp/bdfun-comment-page-list-runtime.png 与 thread-runtime.png。菜单尝试中重复坐标点击触发一次模拟器内复制（已复制提示），并非只读菜单验收；后续单次长按截图实际进入作者空间，未取得可靠的菜单展开/取消截图；因此菜单本轮仍未完成稳态验收。不能把复制提示当成菜单完整验证。系统导航/动画坐标可能有时序影响，最终仍须与 main 稳态对照。
- 原评论入口 299 行、18 项参数被 request/layout 契约替换，根函数 16 行、参数 4 项；本轮 page 各文件均不足 100 行，composer 各文件不足 400 行，人工核查新提取函数体不超过 80 行、契约不超过 12 项。git diff --check 通过，TeakUI 与 app/src/debug 无改动。未把源码拆分或这些局部回归当成像素验收；账号/页面生命周期、真实发送/上传与最终 release/全量回归仍未完成。
- 最终 comment-page-final-checkpoint APK 已重新安装并冷启动 MainActivity；/tmp/bdfun-comment-page-runtime-crash.log 为 0 字节。没有遗留临时调试入口。此为本批迁移检查点，目标继续保持全量完成。

### 2026-11-12 · 评论作用域与楼中楼窗口检查点

- 纯状态首轮 /tmp/bdfun-comment-lifetime-state.log 成功（15s），新增列表 3、动作 7、楼中楼 3 项，验证关闭时保留可见条目/释放 busy、晚到结果和未来请求拒绝、吞掉取消的返回不回写、旧账号不能继续写操作、在途点赞只回退一次以及关闭/换账号后的发送和管理结果不投影。后续另加 1 项 app 控制器代数重建测试与 1 项 core 清理代数可观察测试。原测试断言未修改。
- UI 初次编译失败（9s）：旧单行菜单 open 加守卫后缺少语句换行；第二次提取编译失败（10s）：列表提取先改了匹配文本，导致原列表体没有替换。修正提取脚本并清理导入后 /tmp/bdfun-comment-lifetime-ui-corrected.log 成功（36s）。代数 StateFlow 接线 /tmp/bdfun-comment-lifetime-generation.log 成功（15s），最终按页面 key 记忆嵌套滚动连接后 /tmp/bdfun-comment-lifetime-final-checkpoint.log 成功（16s）。最终 checkLayering、core/application:test（34）、app:testDebugUnitTest（348）、app:assembleDebug 通过，测试失败/错误均 0。
- 楼中楼原评论卡片和排序行渲染 token 与提取前相同；回复行只将原 allReplies.lastIndex 显式传入，替换该局部表达式后渲染 token 相同。原 window 配置实现未改，Column/Box/LazyColumn 层级、contentPadding、圆角、字号、渐变节点与动画时长保持。局部扫描 presentation/comment 的 47 个块函数，最大函数体 58 行，无超过 80 行函数体或 400 行文件；新分组契约人工核查不超过 12 项。git diff --check 通过，TeakUI 与 app/src/debug 无改动。
- emulator-5580 安装 generation APK，真实 BV VIEW 的 239 条评论加载。主评论的“共 2 条回复”打开楼中楼，原评论+两条回复均显示；按当前 uiautomator 语义坐标点击回复，输入 thread-local 并截图，未发送。连续两次 Back 回到主评论，未据此单独证明每一次 Back/IME 的所有中间时序。
- 通过当前语义树读取“更多” bounds [962,1287][1008,1333]，仅点击一次；菜单语义树检出回复/复制/自由复制/分享/举报/加入黑名单六项，截图 /tmp/bdfun-comment-lifetime-menu-visible.png 显示原玻璃菜单。Back 取消截图 menu-cancel.png 未见菜单，本轮没有再执行复制或其他菜单选项。此证据补上前一轮未可靠取得的菜单展开/取消检查，不能替代全部菜单动作或 main 像素验收。
- 本轮没有登录、实际发送/上传、点赞、举报、屏蔽或删除；真实账号切换/页面替换的设备时序仍待最终回归，关闭/失效守卫目前由纯状态测试证明。CommentRow/ReplyBlock 的剩余长函数、其他大页面、全量 main 对照和 release/最终交付仍未完成。
- 重新打开同一根楼时截图 /tmp/bdfun-comment-lifetime-thread-reopen.png 仍显示 thread-local，并重新出现 IME：本轮没有把隐藏面板改成销毁本地回复目标/草稿。该保留行为须在最终 main 对照中一并确认，不能将“Back 返回主列表”误记为草稿已清。最终通过 APK 已安装并冷启动首页，/tmp/bdfun-comment-lifetime-crash.log 为 0 字节。

### 2026-11-12 · 评论行/折叠回复与图片展示检查点

- ui/comment/CommentRow.kt、CommentReplyPreview.kt 的长函数拆入 presentation/comment/row；旧文件删除。提取脚本第一次把没有尾随 lambda 的 AsyncImage 误当成块调用，匹配失败时旧文件仍在，修正为按实参括号匹配后才继续删除/构建。/tmp/bdfun-comment-row-split.log 成功（23s）；没有为本次无行为搬迁增加镜像式测试或修改现有断言。
- 局部尺寸检查发现 CommentImageDialog（96 行函数体）、CommentThreadComposer（82 行函数体）尚未达标，继续拆出图片每页展示状态/原手势循环和楼中楼 BasicTextField。/tmp/bdfun-comment-row-images-checkpoint.log 成功（26s），最终 /tmp/bdfun-comment-row-final-checkpoint.log 成功（19s）：checkLayering、app:testDebugUnitTest、app:assembleDebug 均通过，app 348 项，失败/错误均 0。本轮无 core/infra/danmaku 逻辑改动，未重复扩大测试范围。
- /tmp/bdfun-comment-row-fidelity.py 比较 12 段提取前后 token：头像、作者、点赞、点踩、更多、正文及图片、日期/IP/回复、折叠展开链接、折叠点赞、折叠文本、图片手势循环、楼中楼 BasicTextField 全部相同（回调/展示状态显式参数的局部表达式按搬迁映射替换）。图片手势函数为约 44 行的表达式块，额外人工核查没有遗漏在块函数扫描之外。
- 当前 presentation/comment + ui/comment 共 64 个文件；局部扫描 88 个块函数，最大函数体 60 行，没有超过 400 行文件、80 行块函数体或 12 项函数参数；本轮回调契约 9/4 项，人工核查不超过 12 项。git diff --check 通过，TeakUI 与 app/src/debug 未改。此扫描为局部预检，不等于全仓架构验收。
- emulator-5580 安装最终 APK，真实 BV VIEW → 239 条评论。将 /tmp/bdfun-comment-row-list-tree.xml 与拆分前的 comment-lifetime-list-tree.xml 按文本/描述及同类出现顺序匹配，11 个评论标题/排序/作者/回复/更多控件的 bounds 11/11 一致，无位置差异；仅覆盖这组同一数据/窗口条件下的控件，不声称所有页面像素一致。
- 真实图片缩略图点击打开黑底大图并显示 1/1，轻点空白关闭回原列表；更多菜单展开与 Back 取消正常，未选任何菜单动作。楼中楼打开原评论与两条回复，按当前语义树坐标点击回复，输入 row-check 本地草稿，截图 thread-input.png 确认键盘和原输入样式；两次 Back 返回主列表。未发送、上传、点赞或执行管理操作。/tmp/bdfun-comment-row-crash.log 为 0 字节，测试后 force-stop 并冷启动首页。双指缩放、多图片翻页和所有输入法中间时序仍须最终回归。
- git fetch origin main 成功；origin/main 仍为 671d26416febc4db21bd95cffd5ff469f8dea805，HEAD..origin/main 为 0，且该提交为 HEAD 祖先。随后从该提交真实 CommentScreen.kt 提取 CommentRow/ReplyBlock/CommentImageDialog/CommentPictures，提取前这四个函数体仅将 scoped viewer 与点赞投影入口还原后，token 与 main 全部一致。这补充源码基准证明，不代替最终装机 main 对照；本地 main 指针仍旧，后续不要误用 git show main 代替这个已验证基准。
- app 仍有 8 个超过 400 行的文件（OnboardingScreen、LibraryScreen、DanmakuSettingsContent、SettingsScreen、BackgroundPlayDisc、MineScreen、VideoCacheManager、EmojiPanel），并仍有全局对象/作用域/模块边界审计及完整发布验收工作。本轮评论源码尺寸达标不代表 P3-05 完整行为验收或全量完成。

### 2026-11-12 · 引导页迁移检查点

- 拆分前 ui/OnboardingScreen.kt 与 git show 671d264 的对应文件 cmp 完全一致。用现有 exported TermsActivity + replay_onboarding 记录欢迎、提醒、条款、已查看、勾选、基础设置和完成页；未点击最后“开始使用”，未写完成版本记录。
- 新增 app 级纯 JVM OnboardingStateHolderTest 11 项，使用真实设置层的纯内存适配夹具验证步骤、方向、条款优先返回、查看与勾选独立、保存/恢复仅步骤、未知步骤、六项设置写入/外部变化、重复确认、关闭拒收等。测试放在 app 测试根以验证两个功能的装配，生产 onboarding application 不导入其他 feature 实现。
- 首轮 /tmp/bdfun-onboarding-state.log（32s）359 项中 1 项新测试失败：新断言误将 colorStyle/interfaceStyle 当成不限制的整数；真实当前仓储与 main 671d264 原 SettingsStore 均明确 coerceIn(0,1)，因此修正该新断言为 1，保留 darkMode=9 的原始值验证，没有更改生产约束或旧测试断言。/tmp/bdfun-onboarding-ui.log 成功（35s），归并同页私有函数后最终 /tmp/bdfun-onboarding-final-checkpoint.log 成功（30s）：checkLayering、app:testDebugUnitTest（359，失败/错误 0）、app:assembleDebug 均通过。
- /tmp/bdfun-onboarding-fidelity.py：13 个未改行为函数体 token 一致；将新的渲染辅助函数展开、把展示状态成员映回原变量并映回设置写入后，WelcomePage、CompletePage、BdFunMark、NoticePage、BasicPage 五个原函数体同样与原 main 一致，共 18 项。归并文件后临时检查器最初仍按旧文件名查找 MarkPressure，修正为按真实声明查找后 18 项复验通过。此为源码与节点结构佐证，不替代像素/性能验收。
- presentation/onboarding 16 文件，最大 187 行；连同 application 的局部扫描为 17 文件、42 个块函数，最大块函数体 59 行，无超过 400 行文件、80 行块函数体或 12 项函数参数。另人工核查两个表达式绘制/手势函数也在 80 行内。git diff --check 通过，TeakUI 与 app/src/debug 无改动，无临时测试 Activity。
- 首次装新包后 2s 的截图仍在系统启动屏，自动下一步过早导致回放脚本找不到提醒条目；实际欢迎页显示后重跑正常，没有以该时序改应用行为。新旧 uiautomator 控件按文本/描述及出现次序对齐：notice 18/18、terms 6/6、viewed 18/18、accepted 18/18、basic 16/16 的 bounds/checked/enabled 全部相同，共 76/76；欢迎/完成页保留动画截图，不作动态背景像素相等断言。
- emulator-5580 的新回放完整走通：打开/关闭条款立即显示“已查看”，勾选后进入基础设置和完成页，返回保留勾选/已查看。基础设置“模糊效果”从 true→false→true，控件即时更新并恢复。临时切横/竖屏后仍保留状态；临时改系统 font_scale 触发 Activity 重建，仍停提醒步骤，checkbox=false、显示“使用前请完整阅读”，证明 step 恢复且临时状态按原规则重置。
- 系统原始值由实际设备读取得到 accelerometer_rotation=0、user_rotation=0、font_scale=1.0，finally 中已恢复；不能用早期记录的默认值覆盖它们。引导回放前后 settings.xml 均 17 项，规范化比较 changed keys=[]；仅通过 UI 往返切换既有 blur，没有直接改写偏好文件。/tmp/bdfun-onboarding-lifecycle-crash.log 为 0 字节。
- 最终归并 APK 已安装，欢迎页冷启动正常。最初释放后截图没有捕获中间脉冲，改在 2500ms 长按期间 750ms 取帧，final-mark-held.png 明确显示按压缩小和涟漪环，释放后截图 final-mark-released.png 留档；未因此修改生产动画。最终回放 settings.xml 规范化 changed keys=[]，未点击“开始使用”，未修改完成确认/版本字段。其余 7 个 app 超长文件、其余架构/作用域审计、全量 main 装机对照、完整播放器回归和 release/交付仍未完成；不将本轮引导局部验收标为全量完成。
- 最后按压回归后再次读取 crash 缓冲为空；已通过 activity dumpsys 核对回到 MainActivity。新包保留原 manifest 与默认 debug 包名，没有遗留临时入口或持久化测试数据。

## 2026-11-12 · 媒体库展示与个人页迁移检查点

- 媒体库数据控制器增加关闭/账号失效及请求返回后的协程检查，批量删除和移动/复制在每条请求前后校验，保留之前已确认成功的条目变更。交互与搜索改为纯 StateFlow 容器；本地稍后再看显示数读取当前数据，不捕获首次空快照。选全量过滤结果（包括本地尚未展开项）、按数量判断全选、关闭搜索不清批量选择等 main 规则保留并测试。
- LibraryScreen 925 行、LibrarySearchPage 357 行迁入 presentation/library，状态工厂、入场记录与播放队列显式注入，Activity 共用 LibraryScreenRoute。局部 21 文件/79 个块函数无 400/80/12 超标；最大的函数体 54 行。9 个顶栏/收藏夹叶子函数体及游客 Column 与搬迁前 token 一致。入场记录由静态无上限 HashSet 改为进程容器持有的 4096 项有界记录；超过上限淘汰最早记录，极久以前的条目可能再次播放原入场动画。
- /tmp/bdfun-library-final-checkpoint.log（1m43s）通过 checkLayering、app 测试与 debug 构建：app 383 项、失败/错误 0。新增 24 项离线测试覆盖交互、搜索、入场规则、历史视频投影、迟到结果、取消和账号切换；首次新测试构造 HistoryItem 时 viewedAt 重复指定导致编译失败，改为按真实模型使用命名实参，未修改生产字段或旧断言。
- 媒体库最终包安装后，正常 Main→我的，10 条游客文字/边界/checked/enabled 与安装前完全一致。游客个人页不显示媒体库入口；run-as am start 内部 LibraryActivity 被 Android assertPackageMatchesCallingUid 拒绝，未打开内部页，不能当作媒体库列表装机通过。登录后列表、搜索与批量菜单的装机/main 对照仍待最终验收，没有进行线上内容操作。
- MineScreen 545 行拆为 presentation/mine 6 文件和纯 application 状态/缓存/时间格式化。顺序保持 nav→relationStat→userLikes，逐字段失败回退到初始资料，缓存仍为一个账号、五分钟且从请求开始计时，不写回 Session；账号变更清缓存，页面关闭和取消拒收迟到资料。新增 11 项 JVM 测试（包括 8 种字段失败组合）。
- 本机 SentRecord 移到 core:model；SentHistoryStore 原实现移入 infra:storage，app/data 仅保留两个类型别名。类实现按行尾规范化后与 HEAD 逐字节一致，SharedPreferences 文件名、字段、上限和读取失败回退未改。
- /tmp/bdfun-mine-state-ui-fixed.log（59s）通过 checkLayering、core:model 构建、domain 50、bili 57、storage 9、app 394 项测试和 debug 构建，失败/错误均 0。首次个人页编译因 QuickAction 从文件私有扩大成 internal 而与播放器重载歧义，恢复原私有可见性后通过；没有改播放器调用或旧断言。
- /tmp/bdfun-mine-fidelity.py 验证拆分前 MineScreen 与 main 671d264 文件相同，4 个基础组件、时间策略、游客 Column、展开 helper 后的资料卡与快捷 Row token 一致。个人页局部 9 文件/21 个块函数无 400/80/12 超标，最大函数体 53 行、最大展示文件 140 行。源码比对不等于全部像素/行为验收。
- 个人页最终包再次安装，emulator-5580 Main→我的，与原版 10/10 条语义边界和状态一致；截图 /tmp/bdfun-mine-final.png，XML /tmp/bdfun-mine-final.xml。崩溃缓冲存在系统蓝牙 native 崩溃，/tmp/bdfun-mine-crash-buffer.log 无应用包名，不能描述为整个系统 crash 缓冲为空。登录资料卡、最近本机记录与宽屏仍待运行时验收。
- TeakUI 无本轮改动、无临时 debug 入口；未执行评论/弹幕发送、点赞、关注、收藏/删除或私信。其余大文件、依赖/全局对象审计、最终逐页 main 对照、完整播放回归、默认签名 release 与交付均未完成，全量目标继续进行。

## 2026-11-12 · 设置投影与弹幕设置面板检查点

- SettingsStore 缩为 Android 装配构造器，原字段投影迁入 SettingsBindings；/tmp/bdfun-danmaku-settings-fidelity.py 核对投影正文逐字节一致。ui/presentation 19 处设置类型改为 SettingsBindings，控件不依赖 Android 存储构造器。VideoActivity 与 TeakBareTheme 提供 LocalSettingsBindings，弹幕面板从作用域取投影。
- 旧 DanmakuSettingsContent 726 行迁入 presentation/settings/danmaku 8 个展示文件；关键词编辑、本地两项展开状态进入纯 DanmakuPanelStateHolder。5 项新 JVM 测试覆盖草稿生命周期、空白/trim/大小写/Set 去重、清空保留草稿、18 字段重置范围与重新读取持久化值。旧兼容 DanmakuSettings.resetDefaults 委托同一策略，无重复重置逻辑。
- /tmp/bdfun-danmaku-settings-ui-fixed.log（48s）与最终 danmaku-settings-clean-checkpoint.log（48s，同 /tmp/bdfun- 前缀）通过 checkLayering、app 399 项测试和 debug 构建，失败/错误 0。首次编译因拆分导入清理误删 Compose 委托 getValue，补回后通过，未改变旧断言。没有新增测试依赖。
- 搬迁前完整内容文件与 main 671d264 逐字节一致；8 个 UI 叶子函数体、完整 androidPointerSlider 表达式、展开 DrawScope helpers 后的 Canvas 命令与 main token 一致。局部 9 文件/29 个块函数无 400/80/12 超标，最大函数体 61 行，最大展示文件 172 行。表达式手势与 runtime 6 字段构造另行人工检查。
- 使用临时 debug Activity，在同一个 APK 中保留搬迁前副本与迁移后的正式内容入口，分别以 286dp 半屏内容宽度、360dp 全屏内容宽度运行。此为离线内容布局/输入验证，不是实际播放器全屏旋转、弹幕列表真实数据或生命周期回归。测试只打开关键词编辑器、输入未提交 draft、返回隐藏键盘，没有添加词、重置或改动持久化开关。
- 第一次 initial after 抓到旧 Activity 的草稿与滚动状态，未作为差异结论。改为 NEW_TASK|CLEAR_TASK 并确认顶部标题和空草稿后重测；单次滑动的不同停点仍造成整体偏移，统一重复滚到底部后重采。最终半屏 initial/editor/draft 为 20/24/23 条，全屏为 19/22/21 条，共 129/129 条文字或编辑器语义、bounds、checked、enabled 完全一致。JSON/XML/PNG 在 /tmp/bdfun-dm-probe-{inline,full}-{before,after}*；实际面板外壳、滑杆修改/纵横手势、播放器退出与旋转仍需最终真实播放器验收。
- 17 项 settings.xml 键值逐项前后比对无变化（/tmp/bdfun-dm-probe-settings-{before,after}.xml）；/tmp/bdfun-dm-probe-crash.log 无应用包名。系统蓝牙 native crash 仍可能在共享缓冲中，未声称系统缓冲为空。
- 两个临时 debug 源文件已删除，debug manifest 与测试前备份逐字节恢复；最终 clean-checkpoint APK 的 manifest 与全部 dex 均不含 DanmakuPanelProbeActivity / ProbeBeforeDanmaku 标记，app/src/debug 无 git 改动，git diff --check 通过。TeakUI 未改动。
- 剩余超过 400 行的 app 源文件为 SettingsScreen 648、BackgroundPlayDisc 563、VideoCacheManager 516、EmojiPanel 453；另有全局对象、函数参数/长度与完整依赖审计待办。全量目标、最终 main 同环境验收、完整播放回归、默认签名 release 和最终交付仍在进行，不能将本检查点当作全量完成。

- 最终 clean-checkpoint APK 已重新安装；正常 MainActivity 启动显示首页/动态/我的三个底栏标签与 52 个应用文字节点，/tmp/bdfun-dm-clean-main.png、dm-clean-main.xml（同 /tmp/bdfun- 前缀）保存实际主界面，dm-clean-main-crash.log 无应用包名。没有用临时页作为最终安装包。

## 2026-11-12 · 设置页展示与 CDN 观察检查点

- 648 行 SettingsScreen 移入 presentation/settings/page：9 个展示文件，根入口 22 行；外观 pending 和字号拖动临时状态在 UI StateHolder。SettingsScreenRoute 显式装配返回/退出/外链和关于页导航，照片选择和持久化 URI 权限仍由 UI 平台边界处理。
- 新增 CdnSettingsSource 与纯 CdnSettingsStateHolder，由 AppContainer 的 CdnSettingsAdapter 连接既有测速器。新增 8 项 JVM 测试覆盖节点顺序与自动节点映射、US 小数格式/严格阈值、首屏缓存、强制条件、相等 Map 不重采、关闭迟到、重复加载/取消重试与现有探测的轮询退出；不联网。测速下载实现仍是原 CdnAutoSelector，未宣称已迁完其网络/存储。
- /tmp/bdfun-settings-page-state-ui.log（40s）、settings-page-final-checkpoint.log（23s，同 /tmp/bdfun- 前缀）通过分层检查、领域层测试、app 407 项测试与 debug 构建，无失败。最终源代码整理后 app 测试重复通过；TeakUI 没有改动，git diff --check 通过。
- 搬迁前 SettingsScreen 与 main 671d264 对照，唯一差别是上一轮的 SettingsBindings 类型替换。/tmp/bdfun-settings-page-fidelity.py 展开 helpers 并仅替换绑定/导航回调后，10/10 个 TeakSettingsCard 正文 token 相同；字号行和开发者行布局也相同。最初 proof 脚本把 URL 内的 // 当注释截断，造成开发者卡片假差异；修正词法解析、保留字符串后比对通过，没有为通过比对而更改 UI。
- 局部 11 文件/35 个块函数（包含新 CDN 状态与 SettingsScreenRoute）无 400/80/12 超标；最大展示文件 158 行、最大函数体 61 行。源代码比对不替代逐个设置写入/重启/播放器实际生效的最终验收。
- emulator-5580 正式入口 Main→我的→设置：迁移前后首屏 27/27 项文字、控件语义、bounds、checked、enabled 完全一致；/tmp/bdfun-settings-page-{before,after}-initial.{xml,png} 留存。首轮脚本在冷启动 UIAutomator 无根节点、目标 XML 尚未生成时读取失败；将该临时工具错误纳入就绪重试后成功。settings-page-after-crash.log 无应用包名。未点击任何持久化设置、退出登录、外链或下载操作。
- 后续剩余超 400 行 app 文件为 BackgroundPlayDisc 563、VideoCacheManager 516、EmojiPanel 453；完整依赖/全局对象与函数审计、最终 main 同环境与播放回归、默认签名 release 和最终交付仍未完成，全量目标保持进行。

## 2026-11-12 · 后台唱片展示与原生手势检查点

- 563 行 BackgroundPlayDisc 移入 presentation/player/disc：显式播放端口/设置输入、UI StateHolder、pointer 采样与纯手势策略、封面效果、旋转/呼吸、静态玻璃与旋转脸分开；位置公式不再回查 AppGraph.settings。core/application 新增 BackgroundDiscPlayback，只扩展已有状态/视频/倍速动作，未在本检查点重写原生播放器交接与服务。
- 新增 6 项纯手势边界测试、2 项纯显示投影测试。后者用 100 次进度/缓冲/其它元数据变化确认只收 cover/playing 的投影；新订阅取得当前值，取消不再收到更新。根 runtime 复用同一组依赖，避免动画帧创建的新对象使所有效果失去跳过条件。
- 初版抽取无参数 SideEffect 时错误把内层 if 当成完整 block，首轮编译出现缺少右括号，修正抽取边界后通过。/tmp/bdfun-background-disc-ui-fixed.log（5m）、background-disc-final-checkpoint.log（59s）、background-disc-diagnostic-checkpoint.log（38s）均通过；最终 background-disc-projection-checkpoint.log（1m8s，同 /tmp/bdfun- 前缀）通过 checkLayering、app 415 项、core/application 34 项测试和 debug 构建，失败/错误 0。未改旧断言或添加依赖。
- 搬迁前唱片源码与 main 671d264 文件相同；/tmp/bdfun-disc-fidelity.py 比较位置、取色、两个独立封面任务、延迟退出、呼吸/旋转计时、诊断 SideEffect、光圈/纹路 Canvas、封面 Box、展开后的静态玻璃/旋转层兄弟树，以及尺寸/锚点/变换/阴影链，14/14 项 token 一致（仅替换绑定、允许 Kotlin 尾逗号）。原 SmoothAnimation/ManualAnimation 文件均无 git diff。局部大小扫描无 400/80/12 超标；完整表达式 pointer 入口 19 行，runtime 构造 11 字段人工检查。
- 初轮真实重放只确认第一下暂停；第二次下滑接近阈值没有恢复，长按发生在暂停状态，不能用来证明倍速。瞬时语义坐标曾移出原位置，未将该轮记为通过。原弹簧在 50ms 步长的数学演算有数值不稳定可能，但没有据此声称原版运行时一定有该问题，也未修改共享公式。
- 为区分迁移差异，创建只读验证用 detached worktree /tmp/bdfun-main-fidelity-671，固定 671d264、git 状态干净；/tmp/bdfun-main-baseline-build.log（2m17s）构建通过，原包保存 /tmp/bdfun-main-671-debug.apk。相同模拟器、相同输入序列中 main 成功上滑退出，日志 main-disc-gesture-*（同 /tmp/bdfun- 前缀）留存。未把目录名/意图当作基准证据，实际核对了 HEAD 与 clean status。
- 复查发现迁移版最初观察整个会话快照，包括进度/缓冲，原界面只观察封面和播放字段；已收窄并稳定 runtime 引用。修正后的两轮重放未再抓到语义位置异常；小距离下滑仍可能因采样/跟手使最终累计距离贴近阈值，原阈值与 pointer 只累计 pressed 采样的行为保持不变，没有为脚本改生产手势。
- 最终使用明显超过阈值的下滑后，/tmp/bdfun-disc-projection-wide-gesture-runtime.log 与分阶段 *.log 确认同一个原生 adapter id=237982387：正常 position 59825→61556 / playing=true / speed=1；暂停连续 62639 / playing=false；再次下滑恢复至 63194 / true；按住时 speed=3.0、position 64342→65821；松手恢复 speed=1.0、position 70333→70953。分阶段日志的同 id、暂停不推进、恢复推进、长按/释放速度用脚本断言通过。此为原生状态证据，不是音频听感或性能验收。
- 上滑日志 total=(0,-174)→exiting=true→等待 280ms→调用 exit→会话清理→teardown，之后控件消失、后台媒体会话不再注册；/tmp/bdfun-disc-projection-wide-gesture-freeze-later.log、wide-media-session-after.txt 留存（均带 /tmp/bdfun-disc-projection- 前缀）。crash 缓冲无应用包名。不能把服务构建通知时硬编码 1f 的速度当成真实倍速，故诊断只在 debug 的原 500ms refresh 处读取既有返回值，没有增加刷新请求。
- 原版对照结束后已装回最终重构包；本检查点未加入临时 debug Activity/manifest，app/src/debug 与 TeakUI 无 git 改动，分层检查与 git diff --check 通过。主界面初始唱片位置与基准一致；旋转图像的语义 AABB 会随角度取整，不用其宽高差一像素宣称所有帧像素验收完成。
- app 初步尺寸扫描为 622 文件/1367 块函数，仍有两个 >400 行文件（VideoCacheManager、EmojiPanel）、14 个 >80 行函数和 3 个超长参数列表；该扫描不完整覆盖表达式函数/构造，不作为最终全量尺寸证据。UI 直接 api.xxx() 调用还剩链接标题与 QR 登录共 4 处；扩展类型/静态工具边界扫描还有 25 条引用，见 /tmp/bdfun-ui-api-boundary-audit.txt，不能把“4 处”当成所有 UI 基础设施依赖的总数。
- 全量目标未完成：还需其余 UI/缓存/登录与链接状态迁移、完整尺寸/依赖/全局对象与作用域审计、main 最终刷新、全部页面及完整视频旋转/画中画/多 P/媒体键/后台/恢复进度回归、默认签名 release 与最终交付。

- 额外真实单击验证：首次同一 MainActivity 重发相同 VIEW URI 被 lastHandledLink 去重，读 main 源码确认同样规则，改用冷启动测试而未改路由行为。最终 /tmp/bdfun-disc-open-runtime.log、open-video.xml/png、open-media-session.txt（同 /tmp/bdfun-disc- 前缀）确认单击唱片返回原 BV 标题与简介/评论 tab；点击前后台 position=68561，进入后前台 TeakBili 媒体会话 playing、position=75276，符合继续推进且没有从 0 重播。本次未比较精确到毫秒的转场采样，也不代替进程重建恢复进度验收。随后正常返回唱片并上滑退出，crash 缓冲无应用包名。

## 2026-11-12 · 富文本与链接标题检查点

- /tmp/bdfun-rich-text-checkpoint.log：checkLayering、core/common 4、core/domain 54、core/application 52、infra/bili 66、app 415 项测试及 assembleDebug 通过，失败/错误 0，构建 1m9s。新增 31 项：domain token 4、区间/回退 8、标题缓存与批量状态 6、表情面板 4、重定向 4、标题仓储 5；未改既有断言。
- /tmp/bdfun-rich-text-fidelity.py 固定 clean main 671d264 比较 prefix/UP 徽标、表情图片/占位、尾部占位、面板布局算式、玻璃材质/变换、单元渲染与 pick，6/6 token 一致。七处调用原参数机械分组，保留原 remember 键（包括原本未包含 trailingWidth/回调身份的情况）。这不是全页面运行时像素验收。
- 离线覆盖重叠优先级、最长提及名/正则转义、UTF-16 偏移、重复裸 BV 只取首次 indexOf、短 URL 拒绝但仍走富文本、失败缓存/共享 LRU/账号失效/取消、缓存先发与缺失标题 awaitAll 后发布、已关闭正文迟到结果、表情首次/预加载快照与原双括号值。
- 短链测试以假 HttpURLConnection 验证最多四跳、相对 Location、空 Location、超时/请求头/关闭；仓储测试覆盖 IO 边界、裸 av 两请求、原大小写分支、空值、异常与取消，不连接真实账号。最终 UI、旋转/画中画/后台/恢复和默认签名 release 仍需完成。

## 2026-11-12 · 扫码登录检查点

- /tmp/bdfun-qr-login-checkpoint.log：checkLayering、infra/bili 69 项、app 424 项单测及 debug 构建通过（36s、失败/错误 0）。新增仓储 3 项与状态 9 项测试，覆盖原文/空串/null 错误、取消、轮询先等待、连续网络错误/Waiting、过期/失败、关闭/换 key 迟到与 null profile 成功。
- /tmp/bdfun-login-fidelity.py：固定 main 671d264 标题与间距、二维码/保存按钮、状态与刷新、位图和 MediaStore 4/4 token 对照通过，仅变量绑定/刷新回调改为显式输入。局部 text/login 18 文件/29 块函数无 400/80/12 超标（初步扫描，不替代完整审计）。
- emulator-5580 安装当前 debug，经 Main→我的→扫码登录，真实出现“登录二维码”和“请用哔哩哔哩手机客户端扫码”，正常返回；未扫码、未保存相册、未登录。/tmp/bdfun-qr-{main,mine,loaded}.xml、qr-login-runtime.log/qr-login-enter.log 留存。第一次读取 crash 缓冲包含先前运行/系统崩溃，不能把整段旧缓冲当作本轮新崩溃或零崩溃证据；后续需限定本次时间窗口核查。
- 此检查点后的 LoginStatus 仅把同行闭括号拆行，须在下次构建中复验。UI 中显式 api.xxx() 四处（标题/扫码）均已移出，但其它基础设施工具、全局状态和文件边界仍需完整审计。全量目标继续进行。

## 2026-11-12 · 自动 CDN 与展示边界检查点

- /tmp/bdfun-auto-cdn-checkpoint.log：checkLayering、infra/bili 76、infra/storage 11、app 424 项测试和 debug 构建通过，59s、失败/错误 0。新增 selector 7 项（原缓存/TTL、候选/同分、force/失败回退、无结果、无效 bestId、取消解锁、实际四路并发限制）及缓存 codec 2 项，不访问真实测速网络。
- 扫码页清空已保存的旧 crash 缓冲后重走正常入口：/tmp/bdfun-qr-login-enter-clean.log 再次出现真实二维码/扫码提示并正常返回，新增 crash 缓冲为空。未扫码、登录或写相册。不能把最初旧缓冲里的 app/SystemUI 历史崩溃当成本轮迁移证据。
- CDN 检查点之后继续清理视频展示 BiliApi 穿透参数，须在下一轮构建中复验；完整播放/main UI 与默认签名 release 未完成，全量目标保持进行。

## 2026-11-12 · 关注/粉丝页与视频边界检查点

- /tmp/bdfun-video-api-boundary-checkpoint.log：checkLayering、app 424 项及 debug 构建通过（22s）；视频 UI 不再接收/穿透 BiliApi，保留晚读取登录态与每次内容快照。
- /tmp/bdfun-cdn-fidelity.py：测速完整 probe/HTTP 请求/两轮慢值和客户端超时构造 2/2 源码 token 对照通过，仅包名和返回 Probe→Double 适配；不把离线分数替身当作真实网速验收。
- /tmp/bdfun-relations-checkpoint.log：checkLayering、infra/bili 78、app 432 项及 debug 构建通过（29s、失败/错误 0）。原分页 4 项断言保留，新状态测试 8 项与仓储结果 2 项覆盖未知关系只重查、null 关系、游客拒绝、并发点击/取消、账号/close 迟到、防止错误乐观更新与空串原消息。
- /tmp/bdfun-relations-fidelity.py 对固定 main 671d264：整行用户 UI、列表 padding/大标题/Teak 排序 tab/总数、页脚文案/样式/间距 3/3 token 一致，仅绑定/回调变化；局部 5 文件/9 块函数初扫无尺寸超标。
- 最初 root 用 key(controller) 包整个页面会扩大重建范围，复查后改为仅重建请求子作用域，保留 listState/TeakScaffold 的位置。该最终接线需在后续构建复验。未对真实账号执行关注/取消关注；完整页面运行时和全量验收仍未完成。

### 本批最终复验

- /tmp/bdfun-rich-login-cdn-relations-final.log：关注页仅重建子请求作用域的最终接线通过 checkLayering、app 432 项与 assembleDebug（20s）。当前累计 core/domain 54、core/application 52、infra/bili 78、infra/storage 11、app 432，已有结果文件失败/错误均 0；这里不声称最后这条命令重跑了全部模块。
- /tmp/bdfun-migration-latest-runtime.log：最终包 install -r -d 成功；真实 BV1orW46aEba 出现原视频标题与“简介/评论”，TeakBili 原生媒体会话 PLAYING、speed=1、position=83677，crash 新缓冲为空。标题文字本身没有空间点击语义，首次点文字未跳转，不把该步记录为空间通过。
- 随后从 XML 确认头像 clickable bounds 并正常点击：/tmp/bdfun-relations-runtime.log 与 /tmp/bdfun-qr-relations-{space,fans}.xml 确认 UID 2283296 空间四个 tab，点击“粉丝”进入新页，真实接口返回“请求被 B 站风控拦截，请稍后再试（-352）”，原“点击重试”与“刷新粉丝”均显示，正常返回，新 crash 缓冲为空。没有执行关注、私信、发送或登录；本轮不能认定粉丝列表成功加载或完整动作验收完成。
- 重新执行富文本 6、登录 4、CDN 2、关系页 3 组固定 main 源码对照全部通过。app/src/debug 与 TeakUI 无本批改动；没有临时测试 Activity/manifest。git diff --check 与 tools/check-layering.py 通过。
- 最新 /tmp/bdfun-app-size-audit-current.txt 初步为 app 640 文件/1384 块函数、13 项超标（下载管理文件/函数、CacheRow、弹幕输入/层、HomeTopBar 参数/函数、MySent、SwipeCardPager、TeakActivityScaffold 参数、MenuGlyph、TeakSplash、TvBuffering）。这仍不完整覆盖表达式函数/构造。
- 扩展到整个 app 的显式 api 点调用扫描剩 12 行，均为装配壳中的调用或登录态读取；展示基础设施类型扫描剩 7 行，为播放器 UA 三处与空间图像归一化工具。该数字不覆盖方法引用、别名和下载/持久化等其它接口，完整边界审计仍需继续。
- 全量目标未完成：下载队列/文件存储与离线列表、其余超大函数/长参数、全局对象/无界缓存/Activity 作用域及重复 UI 清单仍需处理；最终 main 刷新与逐页同环境对照、完整播放回归、默认签名 release、发布说明和最终授权交付仍待完成。

## 2026-11-12 · 离线缓存队列检查点

- 新增 25 项离线测试：core/application 15（发布/落盘边界、阶段上限/未知总长、音轨/重复限制、顺序/完成、失败重试与保留进度、删除、同 id 旧回调/清理/finally、关闭、初始化合并/删除、离线投影与文件条件）、infra:bili 5（真实 OkHttp 假响应写文件、HEAD/状态/错误、请求头、封面、取消）、infra:storage 5（旧 20 字段、顺序/消息、中断、缺文件、畸形/未知枚举回退）。均不联网。
- 首轮 download-queue-first.log（/tmp/bdfun- 前缀）发现抽取 CacheCover 漏了主题 colors，修复后继续。新增测试误用了 Android JSONObject 不提供的 keySet，改为 keys 迭代；新增字节样本断言错误地假设 InputStream 每次填满 256KiB，真实 Okio 按 8KiB 短读，修为验证累计递增/精确最终字节/单次上限；新增 JSON 文本断言误把 org.json 的 1.0→1 规范化当成数据变化，改为数值语义比较。没有修改既有断言或生产算法来迁就测试。
- /tmp/bdfun-download-cache-probe.log：checkLayering、core/application 67、infra:bili 83、infra/storage 16、app 432 项及 debug 构建成功（1m5s，失败/错误 0）。后续两个仅临时探针的构建（31s/22s）成功；最终无探针版本另做复验。
- /tmp/bdfun-cache-fidelity.py 对固定 main 671d264，空页/封面树与尺寸/标题状态进度树/播放删除控件/大小格式/全部字段写入/HTTP 字节循环 7/7 token 一致；迁移前 CacheScreen 与该 main 文件 cmp 相同。
- 临时 debug 验证入口仅注入内存样本，旧渲染直接复制固定 main 源码并仅替换测试命名/数据源。/tmp/bdfun-cache-ui-runtime.log：四状态 20 项语义/坐标、删除后 16 项、空页 4 项全部一致；before/after 的 items/deleted/empty 截图排除顶部/底部各 100px 系统区域后 ffmpeg PSNR 全部 inf，即该内容区域逐像素一致。图片为本地资源，样本删除不触及真实缓存、账号或外部服务。
- 真实链路使用 ffmpeg 生成 8 秒蓝色视频与静音音轨，127.0.0.1:18763 临时服务器提供给模拟器。两轮 HEAD/视频 GET/音轨 GET 顺序确认，真实缓存视频 8263、音频 4612，共 12875 字节与本地源 SHA-256/全字节相同；原 20 字段完成记录、progress=1、bytes/totalBytes=12875 已检查。
- 首轮 adb exec-out 对不存在文件返回 0，脚本把 cat 错误当成 XML，在任何写入前停止；随后修正存在性检查。run-as 缺外部存储组，第一轮下载验证读取/清理样本失败；改用有外部存储权限的 adb shell，及时清理并恢复备份证明原本不存在的 prefs。最初媒体会话 state=null 只代表创建，未记为成功起播；临时入口未初始化 LogCollector 导致读到旧日志，也没有当作本轮证据。
- 最终 /tmp/bdfun-cache-native-runtime-final.log 与 cache-native-app.log 确认本轮 source=offline、q=16、本地两轨路径、first-frame、ready；系统 TeakBili 媒体会话 PLAYING(3)、position=433、speed=1。新增 crash 缓冲为空。finally 验证缓存 prefs 恢复原不存在状态、样本目录移除，应用已停止；本地服务器随后停止。不把静音样本当作听感验收。
- 三个临时 debug Kotlin 文件已删除，debug manifest 从原始字节备份恢复；不将测试入口保留在最终包。当前 app 初步尺寸扫描 644 文件/1378 块函数，超标从 13 降至 10；>400 行 app 文件已消除。表达式函数/构造、完整依赖/作用域仍需最终审计。
- 全量目标未完成：其余大函数/长参数、全局对象/缓存/Activity 作用域、重复 UI 清单、最终 main 刷新、全页面及完整视频回归、默认签名 release 与最终交付仍待完成。

### 离线缓存最终无探针包复验

- /tmp/bdfun-download-cache-final-clean.log：checkLayering、core/application/infra:bili/infra:storage/app 测试任务与 assembleDebug 成功（37s）；生产与测试代码未再变化，累计结果 67/83/16/432，失败/错误 0。
- 检查最终 APK 的 AndroidManifest.xml 与全部 dex，CacheValidationEntryActivity/CacheProbeBefore/CacheProbeOldVideo/CACHE_PROBE_NATIVE（UTF-8/UTF-16）均不存在；APK 备份 /tmp/bdfun-download-cache-clean-debug.apk。debug manifest 与原备份逐字节相同，app/src/debug 和 TeakUI 无 git 改动。
- 正式 APK 已重新安装；旧探针启动返回 does not exist。首次启动检查脚本误只识别 mResumedActivity，而当前 Android 使用 topResumedActivity，读取实际 dumpsys 确认 MainActivity 正常后修正采样字段。/tmp/bdfun-cache-clean-apk-runtime-final.log 证明已安装无探针包主 Activity 正常 resumed、原两份缓存 prefs 均保持不存在、样本目录不存在、新 crash 缓冲为空；应用已停止。该错误不属于应用崩溃。
- 本批工作完成检查点，全量目标保持 active；下一步仍为剩余 10 项大函数/参数与完整边界/作用域审计，不能以缓存页局部像素通过替代所有页面验收。

## 2026-11-12 · 首页顶栏与弹幕控件检查点

- /tmp/bdfun-home-topbar-first.log（35s）、homebar-danmaku-layer.log（28s）、danmaku-input-state.log（31s）、homebar-danmaku-final.log（25s，均 /tmp/bdfun- 前缀）构建与对应测试通过。最终 checkLayering、core/domain 62、app 438、danmaku-engine 97 项及 assembleDebug 成功，失败/错误 0；弹幕引擎生产代码未改，未新增依赖。
- 新增 domain 8 项过滤测试：空白/模式范围、mode1 与 2/3 的原区别、双向固定、精确白色、大小写/空关键词、去重先于排序/保留首次、过滤后去重、原高级/count 标志语义。新增 app 6 项草稿测试：空值与缺发送器、trim/固定参数/成功清空、false/异常保留重试、重复 begin/complete 防重、关闭迟到、取消解锁。
- /tmp/bdfun-homebar-danmaku-fidelity.py 与同名 .log：25 组 token 校验通过，包括顶栏左右/背景/展开后的完整搜索面与焦点/下拉指示器、全部 15 个旧调用值、展开后半屏输入完整树、引擎 item 映射/配置/遮罩/时钟。HomeTopBar 搬前与固定 main 671d264 cmp 相同；remember 与 effect 键、200/700ms 时钟参数、8.5s/4s 时长公式未改。纯过滤策略新增逻辑由上述 JVM 测试覆盖。
- /tmp/bdfun-homebar-danmaku-runtime.log：在 emulator-5580 依次安装独立 main 671d264 基准 APK 和最终迁移包，经正常 Main/VIEW 视频入口验证。筛选 app 顶栏 y<=290 的语义/坐标，未搜索/输入草稿/清空分别 6/5/6 项相同；半屏草稿的 EditText/表情键 2 项、打开表情面板后同类 3 项相同。实际焦点输入、清空与表情按钮均可用；这不是全页面或所有动画帧像素验收，推荐/评论/玻璃背后的动态内容未作静态像素断言。
- 实际只输入 bdfun_probe/draft_probe 并清空或退出，没有提交搜索（不添加历史）、弹幕、评论或其它账号动作。两包新增 crash 缓冲均为空；finally 已重新装回最终迁移包并停止应用。XML/PNG 和筛选结果 JSON 保存在 /tmp/bdfun-homebar-*、danmaku-*、homebar-danmaku-runtime-rows.json（后两项同 /tmp/bdfun- 前缀）。没有临时 Activity、manifest 或测试探针。
- 原机械提取把右侧 Crossfade 与外层函数两个闭括号放在同行，源码校验时发现后仅拆行并清理多余导入；最终构建与源码校验均在该清理后完成，没有改变算法或既有断言。
- 当前 app 初步尺寸扫描 663 文件/1399 块函数，超标从 10 降至 6：MySentScreen、SwipeCardPager、TeakActivityScaffold 参数、MenuGlyph、TeakSplash、TvBuffering。仍未完整覆盖表达式函数/构造，不能据此宣称全量尺寸已达标。
- 全量目标仍未完成：其余大函数/长参数、全屏输入与所有展示业务/基础设施引用及作用域审计、全局对象/缓存/重复 UI 清单、main 最终刷新、全页面和完整视频回归、默认签名 release 与最终交付继续待办。

## 2026-11-12 · 六项 app 尺寸超标清理检查点

- 本批拆分 TeakActivityScaffold 参数、MenuGlyph、TeakSplash、TvBuffering、MySentScreen、SwipeCardPager。新增纯 JVM 测试 12 项：缓冲渐变 3、发送记录快照/筛选/跳转/格式 4、滑动加速度/严格停止阈值/阻力/预测/缩页 5。没有修改既有断言，没有新增依赖。
- 首次 scaffold-glyph-first.log（/tmp/bdfun- 前缀）因仅扫描 main，漏更新 debug LogActivity 的 title 参数，补入第 19 处接线。scaffold-splash-buffer.log 的失败为抽取开屏时漏搬共享 TeakOrange/TeakBiliPink 常量，已补回原定义。swipe-pager-checkpoint.log 因把 modifier 参数块的尾逗号带进独立函数而编译失败，删除遗留逗号后通过；未改手势/弹簧算法。位移测试用 4.13f 而非 4.12f，避免浮点表示使相对 4 的差小于 .12 的错误假设，生产阈值保持原值。
- sizes-sent-checkpoint.log（1m6s）、sizes-final-first.log（34s）、size-cleanup-final.log（27s，均 /tmp/bdfun- 前缀）通过相应构建/测试。最终 checkLayering、core/application 67、infra/storage 16、app 450 项和 assembleDebug 成功，失败/错误 0；最后仅清理格式/导入后也已复验。
- /tmp/bdfun-size-fidelity.py / .log：106 组源码 token 对照通过。包括 chrome/主题完整 body、全部 17 个分组默认值、19 处 main/debug 调用的全部表达式、七种图标路径、展开后的开屏完整树/时序/辉光、缓冲完整 Canvas/标签/三点公式与帧时钟、发送记录行/标题、滑动完整积分循环/指针键与回调/测量摆放/两个选页 effects。公式提取由新增 JVM 测试另行覆盖；仅格式、命名绑定、纯函数展开归一化。
- /tmp/bdfun-size-ui-runtime-final.log：同一 emulator-5580 依次安装固定 main 671d264 与最终迁移包，从正常 Main→我的进入本机发送记录，全部/评论/弹幕/重置四态每组 10 项文字、控件语义与坐标相同；随后设置页 41 项相同。两包新增 crash 缓冲为空。没有新增/删除记录或提交账号动作，finally 已装回最终迁移包并停止应用，未加入临时 Activity/manifest。
- 第一轮 am start Main 只带出了 Android 保留的旧粉丝页任务，不是 Main 入口失败；保留该轮 XML 后改用 NEW_TASK|CLEAR_TASK 清理该应用任务栈（不清应用数据），并等待主导航就绪，最终比较通过。XML 位于 /tmp/bdfun-qr-size-*.xml，筛选结果为 /tmp/bdfun-size-ui-rows.json；这里没有全页面/全动画帧像素一致的额外声明。
- 当前 app 初步扫描 692 Kotlin 文件/1420 个命名块函数，未发现 400/80/12 超标；最大命名块函数 76 行。扫描不完整覆盖表达式函数、构造器或匿名初始值，不能以此作为最终全量尺寸证明。
- 同时扩展扫描 core/infra 主源码（207 文件/276 命名块函数），仍发现 infra:bili/DynamicApi.kt 的 parseDynamicItem 147 行、infra:playback/PlaybackForegroundService.kt 的 buildNotification 104 行。下一轮继续处理，不把 app 局部清零当作所有模块完成。
- 全量目标仍待：上述两个基础设施长函数、全屏发送调度、完整依赖/全局对象/缓存/Activity 作用域与重复 UI 清单、全部任务卡逐项验收、main 最终刷新、全页面与完整视频回归、默认签名 release 及最终交付。

## 2026-11-12 · 动态/通知长函数、全屏输入与展示依赖检查点

- `checkLayering :core:common:test :infra:bili:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` 使用固定 Miuix 源码、JDK21、offline/串行、max-workers=2 通过（32s，`/tmp/bdfun-presentation-boundary-checkpoint.log`）。App 455、bili 91、common 8 项，失败/错误均 0；另有 Python 守卫 10 项正反例通过并作为 checkLayering 前置。全屏输入原 3 项行为断言保留，新增 5 项 false/关闭/迟到/取消测试；图片旧字符串规则新增 4 项测试。
- 动态解析 8 项新增测试仅为原解析字段的结构探针，不能作为新接口真实样本。匿名主机/设备采样 HTTP412，没有提交错误 HTML 为 JSON fixture，临时设备 dex 已删除。
- `/tmp/bdfun-infra-functions-fidelity.py` 的 17 组完整解析/通知展开 token 检查通过；全屏输入渲染段去除明确的状态读取/事件接线差异后完整 token 一致（Column/行/字体/颜色/边框/尺寸/chip/输入文案未变）。未改 TeakUI 或 danmaku-engine 生产源码。
- 通知原生验证 `/tmp/bdfun-notification-service-margin-runtime.log`：前台先 rewind 到视频前段，转后台 PLAYING position2630 → 系统 pause PAUSED3553 → play PLAYING4386 → stop 后后台 session 消失，crash 为空。此证据验证通知链路，不替代旋转/PiP/耳机/服务重建完整回归。
- 新 APK 安装及公开视频真实加载通过；本轮手动全屏入口探测没有进入输入窗，因此不计作全屏输入装机或 main 视觉对照验收。普通失败仍收起等发送分支只由纯 JVM 测试证明；没有发送真实弹幕。
- 已实际复现旧守卫对核心层完全限定 Android 类型与 app/presentation OkHttp import 的漏检，现隔离目录 CLI 反例均非零退出。展示模型导入直接指向同一个核心声明；banner 原 image 字符串规则共用 core/common。诊断 LogCollector/LiveCounters 仍是明确的临时例外，旧 ui 原生桥接、字符串插值内引用、Gradle 模块图和 CI 尚不完整。
- 初步命名块函数尺寸扫描无超标，但不覆盖表达式/构造/初始化 lambda；不能据此宣告全量完成。完整剩余任务见 `REFACTOR_COMPLETION_AUDIT.md` 的 44 行及最终门槛，feature Gradle 模块、全局对象 <8、作用域、完整 main UI/播放器与最终默认签名 release/飞书交付仍未完成。

## 2026-11-12 · 播放器平台端口检查点

- 新增 feature/video/presentation 的 VideoPlaybackPlatform、VideoOutput 和 PlaybackAudioEffect 接缝，以及 core/application 的 ForegroundMediaSession。app/playback 负责 Media3/PlayerView/音效/媒体会话装配；展示 ui/player 不再引用 Media3、infra 实现或 BiliApi。原设置/进度/队列兼容入口与 Activity/Service/广播原生桥仍未全部收敛，不计作全量展示分层完成。
- 工厂明确返回端口后 `checkLayering :core:application:test :app:testDebugUnitTest :app:assembleDebug` 通过（46s，`/tmp/bdfun-player-platform-checkpoint.log`）：App455、core/application67，失败/错误0。随后 Gradle `checkLayering` 的全部12个正反例通过（4s，`/tmp/bdfun-player-platform-guard.log`）。首次编译曾发现工厂链旧推断返回类型，现已在 AppContainer/AppGraph 显式约束接口返回，最终编译通过；未改测试断言或播放器参数。
- `/tmp/bdfun-player-platform-fidelity.py` 的27组核对通过：完整 Surface/变换、Layout/覆盖层、控件 effect/定时器/听视频、续播/resize、会话创建 key/队列/回调、来源/请求头、响度条件/增益/释放，以及新原生适配器的 XML/纹理截图等。基准是本批迁移前源码，不冒充全站 main 视觉验收。TeakUI 与 danmaku-engine 生产文件未改，无新依赖。
- 新 APK 原生媒体回归 `/tmp/bdfun-player-platform-media-runtime.log`：真实视频前台 TeakBili PLAYING → 返回后台 BdFunBackgroundPlayback PLAYING2457 → 系统 pause PAUSED3232 → play PLAYING4591 → stop 后后台会话消失，crash为空。
- 新 APK 原生 PiP 回归 `/tmp/bdfun-player-platform-pip-runtime.log`：通过原 Home/onUserLeaveHint 路径进入真实 `mode=pinned` 的 VideoActivity，TeakBili PLAYING1100→3197；返回 MainActivity 后 PiP仍保留且PLAYING8243，crash为空。此证据不包含从 PiP展开回视频页；展开/关闭/旋转组合仍待验收。只临时设置 debug 的 stop_on_video_page_exit，最终 settings.xml 原字节与原不存在的 .bak 均校验恢复，临时设备文件删除、app force-stop。
- PiP测试工具的 shell重定向/exec-out stdin曾失败或等待EOF；已终止确认的测试进程，按原始备份恢复，再改用有超时的 push→run-as cp。失败的工具尝试不记作应用结果。
- main 基线APK已实际进入2424×1080横屏全屏；自动化尚未成功打开其全屏输入窗，因而本轮没有全屏输入 main 界面对照通过的结论。每次尝试最后均恢复新APK并结束应用；没有发送真实弹幕或进行账号写操作。窗口/控件就绪与自动隐藏需要在后续验证器中继续处理。
- 初步生产 Kotlin 命名块函数尺寸扫描901文件/1696函数无400/80/12超标；仍不包含表达式、构造与初始化lambda的完整AST审计。全量目标保持，按44卡审计继续，不标完成。

## 2026-11-12 · 视频读取缓存/账号 revision 与自动清晰度检查点

- `checkLayering :core:domain:test :core:application:test :app:testDebugUnitTest :app:assembleDebug` 最终通过（27s，`/tmp/bdfun-read-cache-lifecycle-final.log`）：app469、domain70、application70，共609项 JVM，0失败/错误；另有既有12项架构规则正反例通过。
- 新增14项读取缓存测试覆盖6条插入顺序非LRU、原12条HashMap淘汰、空推荐、同账号跨页、六字段交互更新、旋转/真正退出、关闭页面/容器、容器实例隔离、旧账号迟到读写/清理、A→B→A与guest/login/logout。8项自动选档测试逐个固定原Kbps阈值及紧邻边界、116/74阶梯、重复档首项、缺候选取最小id原边缘规则、动态带宽与空输入不读平台；3项session测试证明身份刷新/失效推进revision、同账号/瞬时失败不推进、busy正常释放，原5项登录/取消竞态断言保留。
- `/tmp/bdfun-read-cache-fidelity.py` 的10组核对通过：整个VideoScreen布局/状态收集、详情离线→进度→内存→网络顺序、原remember keys、动作/回调/缓存六字段、全部自动选档阈值与阶梯、系统字段与失败0回退、Activity finishing清理条件及容器close接线。字符串字面量作为整体token核对，不靠忽略字符串掩盖文案差异。
- 最终APK装机日志 `/tmp/bdfun-read-cache-lifecycle-runtime.log`：公开真实视频BV1orW46aEba 首次source=network/qualities=2/audio=true → 首帧q32/PLAYING；同进程通过第二个公开入口新建页面，source=memory-cache/qualities=2/audio=true → 新首帧q32/PLAYING，crash为空；最终停止debug应用。账号切换隔离使用确定性的纯JVM测试，未进行真实账号写操作；此证据不替代全站main视觉/播放器组合验收。
- 最终APK所有DEX里确认旧 ui/RelatedMemory、ui/VideoCache、ui/VideoReadCacheKt、data/HttpClients 类型描述符均不存在。TeakUI/danmaku-engine生产文件未改，无临时debug入口/manifest改动；现有debug日志页改动仍是此前合法参数分组。
- 生产源码初步命名块函数扫描902文件/1701函数，无400/80/12超标；表达式/构造/初始化lambda完整AST审计仍待完成。28个具名object的同口径清点已写 GLOBAL_OBJECT_AUDIT.md，目标<8、所有作用域/模块图/最终UI/release交付仍未完成，保持全量目标。

## 2026-11-12 · 外部/剪贴板视频入口检查点

- 最终 `checkLayering :core:application:test :infra:bili:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` 通过（26s，`/tmp/bdfun-incoming-links-final.log`）：app469、application87、bili91，共647项 JVM，0失败/错误。架构正反例由12增至14并经Gradle执行，新增现有根Activity壳不得通过AppGraph.api或直接bili实现取得网络依赖。
- 17项 IncomingVideoLinkStateHolder 测试覆盖空/非支持原文、剪贴板先记录、紧邻原文去重、解析失败不重试、原裸av/URLav差别、Route默认字段、并行乱序完成仍分别打开、一次消费/外来请求、关闭前后/迟到/取消与不协作解析器、剪贴板和Intent独立最近键、重建后重置。既有 domain识别和infra原短链规则测试保持。
- `/tmp/bdfun-incoming-links-fidelity.py` 的13组检查通过，包括 main671d264实际短链实现完整展开、3个入口Regex、BV前缀、原IO边界/默认Route/Intent、MainActivity完整启动/CDN/60Hz/Compose树与intent precedence、剪贴板原生读取、VideoActivity完整组装，以及即时登录仍读原Cookie API getter。没有更改TeakUI/播放器参数、页面文案或布局。
- 最终APK原生回归 `/tmp/bdfun-incoming-links-final-runtime.log`：VIEW真实公开视频BV1orW46aEba→network/首帧q32/PLAYING；同原始VIEW紧邻重复不产生新video source；SEND EXTRA_TEXT裸BV是独立原始键→memory-cache/新首帧q32/PLAYING。crash为空，最终force-stop；未修改剪贴板、发送弹幕或进行账号写操作。真实剪贴板系统交互与全部路由入口仍待完整验收，纯规则测试不冒充这部分通过。
- 最终DEX里旧 BiliLink 类型描述符不存在。当前27个具名object，按同一范围更新 GLOBAL_OBJECT_AUDIT.md；P4-03其余入口/缺参错误、P4-05<8、P4-06作用域与最终模块/UI/播放/release/飞书交付尚未完成。

## 2026-11-12 · 视频设置契约与弹幕全局代理移除

- SettingsBindings全部81个字段/原操作显式实现核心契约，5组返回同一Compose投影，repository仍为纯控制器；无默认委托兜底。新增5项真实Snapshot观察/接口测试证明分组与扁平读取都参与观察、无关字段不误刷、回调晚读与同步写入、关闭时浮点持久化flush，原81字段往返与迁移测试断言保持。
- `checkLayering :app:testDebugUnitTest :danmaku-engine:test :app:assembleDebug` 通过（25s，`/tmp/bdfun-video-settings-final.log`），app474/engine97，0失败/错误；随后移除未使用的接口默认委托，`checkLayering :app:testDebugUnitTest :app:assembleDebug` 再通过（29s，`/tmp/bdfun-video-settings-contract-final.log`）。17个架构正反例通过；新增3例禁止视频/播放器回查全局设置。
- `/tmp/bdfun-video-settings-fidelity.py` 对25个改动文件去除明确设置接线差异后，完整原函数体/字符串/remember、effect、pointerInput keys及读取位置一致；包含全部81属性的key/read/write投影、页面初始化、手势DOWN读取、生命周期、来源、响度、进度、半屏/全屏菜单与Tabs。TeakUI和danmaku-engine生产文件未改，无新依赖。
- `/tmp/bdfun-video-settings-runtime.log`：main671d264与新APK的实际弹幕按钮开启→关闭→重启保持关闭→再次开启均通过；本次窗口为横屏分栏，4个标签/按钮语义和bounds逐项相同。每次有原生视频PLAYING、crash为空；最终恢复新APK和settings.xml/.bak原字节/原缺省状态。
- 最终取消默认委托的APK又通过 `/tmp/bdfun-video-settings-contract-runtime.log` 的实际开关、重启持久化、稳定标签/位置与原生播放，crash为空；设置精确恢复并force-stop。此处不是全部UI截图逐像素验收。
- `/tmp/bdfun-video-settings-background-runtime.log`：前台转后台、系统暂停PAUSED13054、播放PLAYING14145、停止后会话消失，距片尾充分，crash为空。`/tmp/bdfun-video-settings-pip-runtime.log`：真实Home→pinned，PLAYING1362→3899，回Main仍保留PiP且PLAYING9045；不冒充PiP展开返回视频页验证。stop_on_video_page_exit临时值已原字节恢复，临时设备文件删除。
- 已找到此前全屏输入验证的窗口/就绪前置问题：先临时固定竖屏，确认1080×2424，再点原全屏按钮，等UI完成切换后释放固定策略，确认2424×1080独占全屏。`/tmp/bdfun-settings-composer-runtime.log` main/new均实际打开输入窗，空态11项、草稿10项、较小字号/置顶样式10项标签与bounds相同。没有提交弹幕；最终恢复新APK、原fixed-to-user-rotation=default、user-rotation=lock0。失败尝试仍不计通过，当前证据不包含所有6个全屏面板、所有颜色选项或完整组合验收。
- DEX确认旧ui/DanmakuSettings类型不存在；具名object降至26，保持原统计口径，目标<8仍未完成。生产源码初步命名块函数903文件/1710函数无400/80/12超标；仍不是表达式/构造/初始化lambda的完整AST证明。其余全局兼容入口、实际feature模块、全部main UI/旋转组合/耳机/最终默认签名release与飞书交付继续按全量审计执行。

## 2026-11-12 · 实际feature:home模块检查点

- 首页状态/反馈、Compose投影、网格/下拉/分页/错误/操作面板移入真实feature/home模块。原两个测试文件连同16项断言原样移动；app测试现458项，合计474，与迁移前一致。共享卡片11文件仅迁到app/presentation/video/card保留原package与内容，由HomePresentationHost注入；TeakUI与danmaku-engine生产文件未改，无新外部依赖。
- 首次构建 `/tmp/bdfun-home-module-first.log` 成功3m12s；加入实际模块图守卫后，`checkLayering :feature:home:assembleDebug :feature:home:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` 成功33s，日志 `/tmp/bdfun-home-module-final.log`。首页16/app458项0失败/错误，17项源码守卫及6项模块图正反例通过。模块可独立构建，不依赖app/infra/其他feature。
- 根Gradle导出实际生产ProjectDependency图至build/reports/architecture/module-graph.tsv；checkModuleGraph校验方向与循环并接入checkLayering。临时init脚本注入首页debugImplementation→infra:bili，实际Gradle以禁止依赖失败（5s，`/tmp/bdfun-home-module-graph-negative.log`）；只注入testImplementation→app则通过（2s，`/tmp/bdfun-home-module-graph-test-only.log`），测试依赖未污染生产图。首次init脚本误作用于included build，修正findProject后才得到上述有效反例。无仓库违规依赖残留，CI尚未配置。
- `/tmp/bdfun-home-module-fidelity.py/.log` 核对完整原纯状态/投影/操作面板、11个共享卡片、8个首页UI文件，展开host回调后原card anchor/shape/click、加载、宽屏网格、诊断和提示接线保持；网格keys、padding、下拉时序保留。源码证明针对本批迁移前代码，不能替代全站main逐像素验收。
- 最终APK原生验证 `/tmp/bdfun-home-module-runtime.log`：正常Main入口真实推荐8张卡片→长按原操作面板→关闭→网格滚动→实际卡片点击→TeakBili PLAYING→返回仍显示首页信息流，crash为空。未点击稍后再看/不感兴趣等账号写动作，最后force-stop并删除设备XML临时文件。首轮脚本要求屏外UP菜单也可见而失败；第二轮脚本仅匹配state=3而漏认当前系统PLAYING(3)，原生日志实际已播放，修正工具后完成。没有把工具失败算作应用回归；未完成真实刷新/全部分页或main同数据截图验收。APK保留 `/tmp/bdfun-home-module-debug.apk`。
- P5-02前置P4-05仍未过，共享UI完整清单/下沉、所有作用域、实际feature:video、CI、main刷新、完整UI/视频回归、最终默认签名release与飞书交付仍待完成。此检查点不代表全量验收。

## 2026-11-12 · 首页/动态账号revision资源槽检查点

- 新SessionIdentity/SessionResourceSlot纯core实现接入首页/动态，容器关闭会显式关闭两槽。资源获取核对当前真实SessionState，而非由UI传入可能滞后的UID。登录Compose投影同时保留profile与identity；个人页/媒体库工厂捕获identity并在请求/remember key携带revision，阻止原A实例在A→B→A后重新有效。其余空间/消息/进程缓存作用域仍待后续完整审计。
- 新增22项测试：资源槽10项覆盖保留同identity、A→B→A、同账号重新登录、guest/login/logout、异步清理不误关新资源、重复close、失败工厂重试、工厂内改身份/close及容器隔离；1项实际SessionStateHolder→slot测试证明真实revision和失效登录接线；5项真实SnapshotStateObserver+排队dispatcher测试证明StateFlow合并A→B→A/guest往返仍使value读者失效、兼容setter同步发布、暂时错误/refreshing不误刷、同账号新资料保留identity；首页6项证明关闭后迟到首屏/刷新/分页/分类成功响应不发布、不滚动、不继续命令/提示/过滤。未修改既有断言。
- `checkLayering :core:application:test :feature:home:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` 首轮1m（`/tmp/bdfun-session-scope-first.log`），补强实际Session接线及分类成功响应测试后最终16s（`/tmp/bdfun-session-scope-final.log`）成功。最终core/application98、首页22、app463，共583项0失败/错误；另17项源码守卫/6项实际模块图规则测试通过。无新外部依赖。
- `/tmp/bdfun-session-scope-fidelity.py/.log` 17组完整token核对通过：除明确close guards外原首页加载/排序/分页/反馈算法一致；完整首页Compose投影/工厂、MainShell树/导航/chrome/媒体badge周期、AppGraph保留；容器全部api调用/引用名计数相同；11个共享卡片完整源码保持。没有更改TeakUI或弹幕引擎生产文件；上述证明不是全站main截图验收。
- `/tmp/bdfun-session-scope-runtime.log` 在最终APK验证真实推荐8卡→长按操作面板→关闭→网格滚动→实际卡片起播TeakBili PLAYING→返回仍有信息流，crash为空。没有执行账号写动作、伪造真实登录或修改设置；finally force-stop、删除设备临时XML。保留APK `/tmp/bdfun-session-scope-debug.apk`。实际账号切换尚未在授权测试账号上验收，确定性JVM竞态测试不冒充这部分通过。
- 尺寸初扫生产main根913 Kotlin/1735命名块函数无400/80/12超标；扩展debug后915文件/1740函数，发现LogActivity.LogScreen143行，已补入完成审计待拆。首次空参数执行扫描得到0文件，属无效工具调用，未作为通过证据。上述扫描仍不覆盖完整表达式/构造/初始化lambda AST。
- 全量目标保持：空间/消息/所有缓存与Activity作用域、共享UI清单/下沉、具名object<8、实际feature:video、CI、完整main视觉/播放组合与最终默认签名release/交付均尚未完成。

## 2026-11-12 · 空间Activity作用域与main动态失败语义检查点

- 删除AppContainer的spaceControllers进程Map及按mid批量释放接口。SpaceActivity的ViewModelStore持有SpacePageViewModel/SessionResourceSlot，每个页面独立，配置重建保留Store，onCleared关闭资源；同账号revision失效创建新控制器。session/controller显式传入SpaceScreen，presentation/space无AppGraph回查。空间请求前后和bootstrap await后检查取消/归属；close统一释放loading/busy并恢复未完成关注的乐观值。
- 显式声明现有传递依赖Lifecycle ViewModel 2.11.0（版本表/implementation），没有引入新版本或另一个框架；选择理由与替代方案见REFACTOR_DECISIONS。TeakUI/danmaku-engine生产源码未改，无临时Activity/manifest或探针。
- 新增14项纯JVM/生命周期Store测试：7项控制器测试覆盖bootstrap五await阶段、6类内容请求、关闭后所有命令、关注迟到成功/回滚、A→B→A、吞取消和正常重试去重；4项真实ViewModelProvider/ViewModelStore测试覆盖同Store Provider重建、同UP两Store互不共享/释放、revision替换、clear只关一次且拒绝再用；3项main失败规则测试覆盖首轮失败后非空动态恢复、保留已有items/cursor、分页失败仍停止。既有断言未修改。
- `checkLayering :app:testDebugUnitTest :app:assembleDebug` 首轮2m30s（`/tmp/bdfun-space-scope-first.log`，app474）；恢复main规则后36s（`/tmp/bdfun-space-scope-final.log`）；补强非空items测试后最终21s（`/tmp/bdfun-space-scope-contract-final.log`），app477项0失败/错误。源码守卫新增2例，现19项；模块图6项均经Gradle通过。首次显式依赖引发较广重编译，不能把这些非同条件数字当P5-05收益结论。
- `/tmp/bdfun-space-scope-fidelity.py/.log` 八组完整token核对：除生命周期guards与恢复main首轮失败语义外，完整五并发/兜底/分页/去重/关注算法保持；完整SpaceScreen树/动作/effects、runtime其余view state/keys、Activity状态栏/配置回调/route校验/TeakUI slots、AppGraph与容器其余代码保持；确认空间展示无全局获取、旧Map移除。尺寸初步局部扫描713文件/1504命名块函数无超标，非完整AST；debug LogScreen143行仍待拆。
- `/tmp/bdfun-space-scope-runtime.log`：从公开视频BV1orW46aEba的真实UP头像进入小猪P_空间，四Tab实际切换（各XML正文不同）、1080×2424竖屏/2424×1080横屏、退出到视频再进入均通过，crash为空，无关注动作。最初脚本误点可选择的UP名称而失败，改为原头像入口后通过。窗口fixed/default及rotation原lock0均恢复。
- `/tmp/bdfun-space-main-runtime.log` 依次安装固定main671d264和新包。冷启动dump最初曾复用旧XML，改为每次删除设备XML并核对视频标题后重试。重新取样的四个竖屏Tab状态和横屏帧中，页头/四Tab各5项文字、控件语义、bounds逐项相同，独立限于页头的结果为 `/tmp/bdfun-space-main-header-rows.json`。原整名匹配脚本把动态正文的同名UP也当页头，因main有正文/new空态而失败；没有将该整页比较记为通过。finally装回当前包并恢复窗口策略。
- 对照揭示了一个真实迁移差异：main bootstrap只在动态请求成功时改items/offset/end，而旧迁移把首轮Failure也标end=true。设备日志同时确认-352风控（`/tmp/bdfun-space-scope-device-current.log`），不能把不同响应误称布局变化。本批恢复main首轮失败仍可继续的规则，分页失败仍停；同响应/强制失败恢复由确定性纯测试验证。
- 最终产物再次装机 `/tmp/bdfun-space-retry-runtime.log`：正常头像入口→真实空间→动态Tab，bootstrap实际count=13/end=false/error=null（`/tmp/bdfun-space-retry-bootstrap.log`），XML实际显示与main相同的置顶提示“查看最新近况，请点评论按时间排序”；crash为空，无关注操作，最后force-stop并删除设备临时XML。最终APK保留 `/tmp/bdfun-space-scope-debug.apk`。该结果不替代动态所有13项同原始JSON/同窗口整页视觉验收，也没有把UI里“暂无动态”当作原接口真实空列表证明。
- 全量继续：空间全部分类/刷新/完整main同响应正文，消息/其他页面/进程缓存作用域、共享UI/全局对象<8、完整AST、feature:video/CI、完整播放器组合与默认签名release/飞书交付仍未完成。

## 2026-11-12 · Kotlin PSI尺寸基线与开发工具APK隔离

- 增加仅开发用的:tools:kotlin-audit，Java/JDK21+Kotlin 2.4.21 compiler-embeddable（与现有插件同版本，版本表显式锁定）；理由和替代方案见REFACTOR_DECISIONS及工具README。未修改app/UI/core/infra业务源码、TeakUI或弹幕引擎，也没有设备设置/账号写操作。新增仅工具依赖，不混入Android产物。
- 审计器首轮因2.4.21新增的Extensions storage初始化要求失败（`/tmp/bdfun-kotlin-psi-first.log`）；按本地实际API注册后可运行。随后发现IntelliJ PSI需要LF文档，直接传CRLF会误报13个既有文件2145处语法错；工具侧归一换行并增加CRLF测试，未改业务文件或降低断言。最终真实生产源码语法错误为0。
- 17项工具JVM测试通过，覆盖80行/12参数/400行边界、表达式、主次/数据类构造、lambda/lazy/匿名函数、getter/setter、init、字符串插值代码、泛型与默认lambda逗号、函数类型、显式context输入（含12边界）、CRLF与损坏语法。完整自有范围909文件/9511节点，19条违规（7个函数/lambda体，12个长构造参数），见docs/KOTLIN_SIZE_AUDIT.md与build/reports/kotlin-audit/report.json；快照 `/tmp/bdfun-kotlin-psi-report-909-9511.json`。没有将report退出0记作尺寸通过。
- 实际CLI/Gradle严格任务：合规源码成功（最终`/tmp/bdfun-kotlin-psi-positive-final.log`，2s）；81行函数非零（negative.log）；损坏语法非零（syntax.log）；空目录非零（empty-final.log），并验证预置假通过旧报告已移除；后三者均/tmp/bdfun-kotlin-psi-前缀。真实生产源码严格任务明确非零拒绝19项（`/tmp/bdfun-kotlin-psi-strict-final.log`，5s）。严格任务尚未接入checkLayering，待超标清零后作为最终硬门槛接线。
- `checkLayering :tools:kotlin-audit:reportKotlinSizes :app:testDebugUnitTest :app:assembleDebug` 成功2m16s（`/tmp/bdfun-kotlin-psi-app-final.log`）：app477/工具17项0失败/错误；报告仍明确19项超标。仅工具配置改变触发较广重编译，不作为P5-05冷/热收益证据。保存debug APK `/tmp/bdfun-kotlin-psi-debug.apk`，DEX确认既无KtFile/PSI编译器类，也无KotlinSizeAudit工具类。实际app debugRuntimeClasspath报告同样无compiler-embeddable（`/tmp/bdfun-kotlin-psi-strict.log`中的真实dependencies任务，不是脚本猜测）。
- 模块图规则新增app禁止依赖tools，7项正反例通过。临时init脚本注入app implementation→tools:kotlin-audit后，真实checkModuleGraph以禁止依赖失败（`/tmp/bdfun-kotlin-audit-apk-negative.log`，1s）。自动审批服务首次Bad Request未执行该命令，重新经同一审批流程后执行；不是绕过审批或仓库违规依赖残留。
- finally合法仓库配置已复验：`checkLayering :tools:kotlin-audit:reportKotlinSizes` 成功9s（`/tmp/bdfun-kotlin-psi-restored.log`），19项源码规则、7项模块图规则通过；生产模块图无app→tools边，实际PSI报告仍是909/9511/19。未新增临时Activity/manifest，无本批native运行需要；app业务/UI源码未改，未把工具测试冒充页面行为验收。
- 本批是把旧扫描未知项变为有依据的待办；19项须实际拆分，不能放宽阈值或排除构造器。共享UI/作用域/对象<8、feature:video/CI、main最终刷新、全UI/播放组合、默认签名release/飞书交付仍未完成，全量目标保持。

## 2026-11-12 · 空间资料mapper真实样本检查点

- 通过临时debug采样入口，复用当前BiliRequestGateway的Buvid/签名/客户端GET，对公开UP2283296取得info/stat/nav/upstat/App空间五份code=0响应；仅App所读分支摘录进入src/test/resources/json，字段值未改，来源/截取方式见SPACE_FIXTURES.md。网页vlist抛BiliException、动态code=-352，未记作成功样本；消息成功响应仍缺失。本批没有进行账号写操作。
- 临时入口只存在于采样构建（`/tmp/bdfun-space-raw-capture-build.log`，44s）；完成后debug manifest按备份原字节恢复，采样Activity删除，设备专用cache/refactor-space-raw目录清理。最终APK manifest及所有DEX确认不含RefactorSpaceCaptureActivity、其生成辅助类或CapturePlan；测试响应资源也不在APK中。
- SpaceApi保留完整五请求参数/签名/async及await顺序；条件获赞补查仍由API负责。SpacePayloadsDto/SpaceInfoMapper纯组装VIP、充电、横幅和原23个资料字段；旧parseSpaceVideo作为显式回调保留。新mapper不包含Context/HTTP/IO，不改UI和原接口字段/默认值。
- 新增10项mapper测试（真实资料/关系/导航/App VIP旧字段、横幅顺序/对齐、缺省勋章/经验、外部获赞传入、可选响应缺失，以及明确标注的现代VIP优先级/主题/充电/vlist结构探针）和5项获赞优先级/异常/取消测试。既有断言未改。首轮构建55s（`/tmp/bdfun-space-mapper-first.log`），最终`checkLayering :infra:bili:testDebugUnitTest :app:testDebugUnitTest :tools:kotlin-audit:reportKotlinSizes :app:assembleDebug`成功25s（`/tmp/bdfun-space-mapper-final.log`），bili106/app477项0失败/错误；工具17项及既有源码19/模块图7项通过。
- `/tmp/bdfun-space-mapper-fidelity.py/.log` 11组完整源码token核对通过：五请求/全部参数/async-await、条件获赞完整表达式/回调、全部23字段、VIP优先级、充电回退、SpaceInfo原始vlist数量/顺序/关注默认值、两个横幅函数、mapper无IO、临时入口清理。脚本首轮将mapSpaceInfo误当SpaceInfo构造器，修正为整标识符匹配后通过，没有改业务代码来迎合验证器。
- 最终APK `/tmp/bdfun-space-mapper-debug.apk` 实际装机（`/tmp/bdfun-space-mapper-runtime.log`）：经原视频头像进入公开空间，显示小猪P_/LV6/十年大会员，动态Tab可进入，crash为空，无关注动作；最终force-stop。本轮动态bootstrap count=0/end=false/error=null，符合此前恢复的首轮失败保留可重试规则，不能解释为真实接口空列表或动态成功验收。
- PSI现910文件/9519节点/17条超标，space()原119行函数及其lambda两项已消除；真实严格任务仍以17项失败（`/tmp/bdfun-space-mapper-strict.log`，6s），不是尺寸全量通过。剩余消息长解析、debug日志长函数、12个长构造、其余模块/作用域/共享UI/全局对象与最终交付继续按完成审计。

## 2026-11-12 · main直播集成、release与暂停检查点

- 集成main 4c4e03b的直播页面、播放器、弹幕长连接与设置入口。原始文件基准保留于/tmp/bdfun-live-main-4c4e03b。直播HTTP/JSON、WebSocket和Brotli移入infra:bili，纯模型和读取/解码/连接/播放端口进入core，页面业务状态接入LiveRoomStateHolder，Media3/PlayerView进入infra:playback；Activity仅装配，UI按原树拆分。TeakUI和danmaku-engine生产文件未改。
- `:app:testDebugUnitTest --tests '*LiveRoomStateHolderTest'`成功（/tmp/bdfun-live-targeted.log，1m），5项/0失败/0错误/0跳过：房间回退与请求顺序；同URL重载即使prepare失败仍清消息/恢复标记；重载失败保留旧流/清晰度/消息；首个不支持业务包清占位、忽略非业务包与300条窗口；关闭后迟到响应不能发布且只释放一次。没有改旧断言或重跑此前917项全套记录。
- `:tools:kotlin-audit:reportKotlinSizes checkLayering :app:assembleDebug :app:assembleRelease`成功（/tmp/bdfun-live-release.log，9m54s）。源码规则19项、实际模块图规则7项通过；PSI为952文件/10174节点，直播新增违规0，全仓原17项仍在，report退出0不代表严格尺寸通过。初编因infra缺media3.ui失败，显式补齐项目已有PlayerView依赖后通过。
- /tmp/bdfun-live-fidelity.py与.log核对22个原始/展开UI完整函数体token，包含文案、修饰、聊天行、输入装饰、手势图标、头部与两个设置面板，均与main 4c4e03b保持。布局/控件参数与remember/effect/pointerInput keys按原源拆分；此处不宣称全部直播截图逐像素或完整行为验收。
- 最终debug APK安装成功。R8并行期间首次启动被系统记为INITIALIZATION FAILURE/start timeout，crash buffer为空；构建结束后正常冷启动6106ms，显示真实推荐信息流，并经“我的”进入设置。私有直播Activity不能从shell直接启动，未为测试暴露组件或添加临时入口。直播实际推流/弹幕与所有面板的装机验收留在后续完整验收，本次不冒充已通过；未进行账号写入、未修改设置，最后force-stop。
- release APK：app/build/outputs/apk/release/app-release.apk；包名com.aritxonly.bdfun，versionCode500，13,028,097字节；SHA-256 `8d559893b268fc154493c505a06a1fdc3aecd663aa40f9a4e7369184a623b903`。R8/lint/validateSigningRelease成功，apksigner verify验证APK v2签名通过。无keystore.properties，沿用默认Android Debug证书回退，未覆写包名/签名；不是正式发布签名。本次仅要求release编译与阶段暂停，不宣称release完整装机验收或最终交付完成。
- 全量待办仍有17个尺寸节点、28个具名object、其余作用域/共享UI/feature:video/CI及全部视觉/播放器/评论回归。保存检查点后按用户明确要求暂停目标，不继续其余迁移；PR保持草稿。

## 2026-11-12 · 恢复全量重构与宽屏滚动视口

- 用户恢复全量目标，并授权修复宽屏内外同轴滚动手感；测试仅针对本批必要逻辑。centeredListWidth原祖先scrollable删除，变为仅宽度测量；TeakUI自有TeakScrollContainer让左右留白与内容成为兄弟手势区、共享原ScrollState，不再让内容手势经过祖先第二层scrollable。通用页面外壳覆盖设置/帮助/捐赠/致谢/缓存/日志及相关列表；搜索落地/结果与普通说明页显式接入。空间头部、独立面板和合集的有意嵌套不改。
- MessageApi原逐条解析迁至MessageFeedItemDto/MessageFeedMapper，字段/请求/点赞分桶/补充视频与游标规则保持；LogScreen按原item key提取组件，并把本地状态放入StateHolder，不改TeakUI节点/文案/回调。PSI原5个长体节点消除，只剩12个长构造器，未放宽阈值。
- `/tmp/bdfun-scroll-message.log`构建成功2m58s，只执行MessageFeedMapperTest三项与TeakContentWidthTest两项，共5项/0失败/错误/跳过；debug构建通过。新增JSON明确为旧字段结构用例，非真实线上样本。宽度测试覆盖599/600断点、70%/680上限、显式宽度和无界约束。未重跑其他既有套件。
- 最终debug安装/冷启动/真实推荐/“我的”→设置通过；2424×1080宽屏实际内容拖动后主题行y424→229，留白拖动后列表继续向上、内容区反向接续后y164返回顶部方向，crash buffer为空。首脚本把“必须首项完全离屏”当作成功条件而误失败，未改生产代码迎合；保留已经成功的中心拖动结果，只补完留白/反向两步。日志`/tmp/bdfun-scroll-runtime.log`与`/tmp/bdfun-scroll-runtime-followup.log`，实际截图`/tmp/bdfun-scroll-runtime-landscape.png`已查看，样式/横向位置保持；不宣称所有设备手感或完整视觉验收。未修改设置或执行账号写入，finally停止应用并清理远端XML。
- 源码分层检查通过；容器没有新增依赖。此阶段仍继续全量重构；上一阶段ed67f02的release不是本批最终产物，不重复构建release，待最终源码收尾后再构建。

## 2026-11-12 · 十二个长构造器与严格尺寸门槛收尾

- 视频/用户/动态/评论/消息与全屏/页面状态按职责分组，保留原flat只读属性、默认值与手写JSON协议，更新构造与copy接线。界面树、控件参数、回调对象及remember/effect keys保持；没有新依赖，没有为每个数据类添加样板测试。
- 第一批视频列表扩展、会话偏好与收藏选择：`/tmp/bdfun-record-targeted.log`完成domain推荐过滤、core缓存恢复、路由往返、视频/动态mapper与首页22项；随后只补跑未执行的app交互/会话/装配测试及debug（`/tmp/bdfun-record-app-final.log`44s成功）。首次编译遗漏测试工厂item().copy(pinned)，按新签名改样本构造，未改预期值。
- 剩余九组编译检查发现新动态作者类型与现有portal作者同名，已命名为DynamicItemAuthor；另补齐通知、楼中楼预览、视频缓存同步及测试样本的copy。编译错误不算验证通过，未绕过类型检查。
- 最终`/tmp/bdfun-record-final-targeted.log`成功1m3s：严格PSI检查0违规，953个生产文件/10421节点；domain VideoPagePolicy3、core/application VideoPageStateHolder4、storage RouteJsonCodec7、bili mapper27、app相关状态/目标/缓存/装配106，共147项既有定向测试，0失败/错误。不是全量917项重跑；测试构造/copy随API更新，断言预期值未减弱。debug构建通过。
- 严格checkKotlinSizes接入checkLayering，17项工具规则不变；不把report退出0或历史扫描结果当成最终验收。全局对象与作用域、模块化和最终交付仍继续。

## 2026-11-13 · 全局对象收口至7

- 同口径28→7，保留清单与生命周期见GLOBAL_OBJECT_AUDIT.md。十二个core规则/常量对象先改纯函数，之后完成infra参数/mapper/协议、路由动作、直播默认值与主题拥有关系；未移动到排除目录或用companion规避。
- core规则相关定向测试与严格守卫通过；首次app编译发现两个旧object类型别名已无目标，删除无用别名。随后只补跑PlaybackSessionStateHolderTest与DynamicFeedStateTest及debug，`/tmp/bdfun-pure-core-final.log`成功2m17s，没有重复之前已通过规则套件。
- 最终`/tmp/bdfun-global-final3.log`成功1m：checkLayering（含严格尺寸）、HttpRetryPolicyTest3、LiveProtocolTest2、AppThemePaletteTest2与debug均过，0失败/错误/跳过。新开发仅协议固定字节/压缩损坏兄弟包2项、主题独立来源/真实Snapshot观察2项；未重新跑147个数据分组或历史全套。协议样本明确是main格式结构用例，不冒充线上弹幕。
- 编译中定位并修正BiliBackend薄转发与新换算函数同名造成的递归接线（明确调用biliAidToBvid），及`..PROTO_VER_BROTLI`范围上界符号、导入别名遮蔽；未用显式返回类型掩盖递归。错误构建不计通过。
- 原导航Intent键/路由JSON、HTTP路径/参数/常量、回退房间字段/文字、主题ARGB与UI树保持。删掉无调用的随机LiveMock样本；native播放器/弹幕渲染生产实现没有逻辑变更。APK最终原生链路回归留到模块收尾后合并执行，避免每个命名空间搬迁都重复装机。

### 2026-11-13 视频实际模块

- 新建`:feature:video`，移动原11个application文件、平台端口及展示值对象/纯装配函数；原测试跟随移动。app只改依赖及四个StateFlow的等价观察接线，未改布局、tab、手势及生命周期effect。
- `/tmp/bdfun-video-module.log`：独立assembleDebug、checkLayering（含严格尺寸）、app assembleDebug通过，3m03s。仅执行原VideoDetailStateHolder6、VideoReadCache14、VideoInteractionStateHolder10、VideoPlayerAssembly2项测试，共32项，无失败。没有新增重复测试。

### 2026-11-13 滚动绑定与依赖负例

- 反向LazyColumn/Grid的留白拖动方向跟随原layoutInfo.reverseLayout；聊天只绑定滚动状态，不写入固定顶栏进度。绑定随composition释放，旧页面不会清除新页面的状态。
- 消息/私信控制器remember键改用已有SessionIdentity（UID+revision）；关系页面按同一identity重建，沿用原关闭和子协程取消流程。现有SessionPresentationStateHolder测试覆盖合并通知下的账号往返。
- `/tmp/bdfun-scroll-bind.log`：checkLayering与app assembleDebug通过，28s。设置宽屏中心/两侧拖动装机证据沿用本批此前记录；反向聊天尚无真实账号装机验证。
- `.github/workflows/architecture.yml`加入Java21、SDK37、固定Miuix源码、严格架构/尺寸、debug及必要纯逻辑测试。Action提交由官方GitHub API查询后固定。
- `tools/check-module-graph-negative.sh`在独立Gradle init配置注入feature:home→infra:bili的debugImplementation，只接受指定违规信息，其他构建错误仍失败。首次发现init脚本也会应用到Miuix的build-logic，已限定只含目标feature的主构建。`/tmp/bdfun-module-negative.log`重跑确实输出指定错误并PASS。远端CI尚未运行，不将本地证据写成CI已通过。
- 源码守卫补齐字符串模板表达式检查，原字面文本仍忽略；仅新增两个针对遗漏的测试方法，21项守卫反例通过，生产扫描通过。

`/tmp/bdfun-scope-key.log`：5项既有SessionPresentationStateHolder测试和checkLayering通过，34s；最新严格PSI报告954文件/10428节点/0违规。

### 2026-11-13 消息即时账号隔离与作用域清单

- 在消息会话/通知/私信/关系工厂捕获SessionIdentity，控制器在请求前后及会话姓名补全回调核对当前身份，不依赖Compose及时Dispose。普通账号内算法、布局和网络参数保持。
- 只新增4项竞态用例：旧revision的消息页结果、会话姓名/分页、私信上传后禁止继续发送、同UID关系结果；既有断言未改。
- `/tmp/bdfun-message-owner.log`：checkLayering、debug及4组48项定向测试全部通过，36s。分别私信19/会话10/消息流10/关系9。
- SCOPE_AND_CACHE_AUDIT列明21个Activity及旧六类Memory补丁去向，没有用静态Map替代object；公开只读元数据缓存与账号/页面状态分开保留原语义。
- CI第二次在平台安装失败：实际SDK包名为platforms;android-37.0，本机source.properties/package.xml确认；workflow已按真实名称修正，使用runner已有新版sdkmanager。尚待该版本远端结果。

### 2026-11-13 当前源码release与远端CI

- main已fetch核对，仍为4c4e03b，无新遗漏提交。当前生产源码dc80049，构建所在提交5c61d91（其间仅CI修正）。
- `/tmp/bdfun-final-release.log`：默认`:app:assembleRelease`含R8/lint通过，5m08s；499 tasks（233 executed/74 cache/192 up-to-date）。未覆盖包名或签名。
- APK：`app/build/outputs/apk/release/app-release.apk`，13,044,481 bytes，包名com.aritxonly.bdfun，versionCode500/versionName preview-5.0，min33/target37。SHA256 `38743b8c0a9461444126d06db0390b64abff0a245f8421eb7ea3336a3ddcd62a`。
- apksigner verify通过，v2=true；默认Android Debug证书，SHA256 `9d6aabd27c61c797ff68293e229707d82083f5f4108b091ded2976bea7c38db1`。没有正式keystore，不称作正式发布签名。
- 远端[Actions 43269829285](https://github.com/AritxOnly/bdFun/actions/runs/43269829285)已全部成功：架构/严格尺寸、debug、故意注入feature→infra生产依赖并拒绝指定错误、core政策与视频定向测试。环境准备修复依据实际日志和本机SDK元数据；不是绕开检查。
- 最新严格PSI：954文件/10443节点/0违规。MODULARIZATION_REVIEW提供一页实际数据与结论；未为性能报告追加clean构建，也不编造冷基线或因果收益。
- 最终集中装机脚本已确认真实起播，但进入视频后窗口变为横屏，未满足脚本的竖屏坐标前提。等待wm锁、关闭应用自动转向及对齐系统转向设置后，该前置条件仍不成立；停止重复该脚本，不计全屏/PiP/恢复组合通过，不为此修改生产逻辑。各次finally均force-stop，恢复原settings字节及窗口/系统转向设置；完整组合保留待验收。

`/tmp/bdfun-release-startup.log`：当前R8 release安装成功，COLD启动2599ms，真实推荐首页渲染，crash缓冲为空；截图`/tmp/bdfun-release-startup.png`已人工查看。结束force-stop，不改账号/设置。此记录不代替旋转/PiP等完整组合。

### 2026-11-13 视频/评论显式依赖与全屏展示叶组件

- `/tmp/bdfun-video-explicit.log`：显式视频依赖首轮checkLayering+debug通过，1m；`/tmp/bdfun-comment-explicit.log`：评论依赖接线后通过，36s；删除最后无调用进度/队列别名后`/tmp/bdfun-explicit-final-compile.log`通过，32s。无业务算法变更，没有为工厂构造器新增镜像单测；只新增2项守卫测试方法，现23项源码守卫反例，原模块图/PSI工具测试保持。
- `/tmp/bdfun-explicit-page-runtime.log`：安装当前debug，核对media_session属于com.aritxonly.bdfun.debug且PLAYING，进入评论tab后真实253条评论数量/列表渲染，crash为空，最后force-stop。截图`/tmp/bdfun-explicit-comment.png`已查看；没有改设置、发送或账号写操作。该记录不声称旋转/PiP组合通过。
- 全屏五个展示文件与迁移前HEAD逐文件对照，除import与跨模块internal可见性外完全一致；新增拆出的状态类体原样。`/tmp/bdfun-video-render-leaves.log`：独立feature:video assembleDebug、app debug及严格架构/尺寸通过，28s，最新957文件/10436节点/0违规。
- 本批生产源码已变化，上一阶段5c61d91的release及CI成功记录仅覆盖其对应提交；不作为当前源码最终release/CI证据。最终源码确定后再做默认release、R8及交付，避免每个小搬迁重复release。

### 2026-11-13 全屏根组合与七面板迁移

- 首轮编译发现选集还依赖旧app formatDuration兼容转发，改为导入同一core:common函数；没有改变格式化逻辑。随后`/tmp/bdfun-fullscreen-panels-final.log`31s、`/tmp/bdfun-fullscreen-chrome.log`22s、共用绘制下沉后的`/tmp/bdfun-fullscreen-shared-final.log`28s、internal可见性收尾`/tmp/bdfun-fullscreen-final-check.log`21s均通过独立视频模块、app debug与严格checkLayering。没有给无逻辑搬迁追加镜像单测，既有守卫保持。
- 最新PSI：961文件/10442节点/0违规。7个面板显式强调色接线外的布局/动作token对照一致，PanelHeader/Card体逐字一致；TeakUI与旧app tween体逐字一致。
- `/tmp/bdfun-fullscreen-module-runtime.log`最终通过：核对debug包PLAYING→按实际2424x1080分栏控制条进入全屏→再次单击显示控件→打开倍速面板全部6个原选项→关闭面板/返回详情tab→crash空。没有选择倍速、改设置或账号写操作，结束force-stop。截图`/tmp/bdfun-module-speed.png`已查看。
- 驱动失败记录不能算产品通过：首次150ms小于现有260ms单击判定；之后截图证明已进入全屏，但原实现会主动收起控件；补齐再次单击后，点击前截图又耗尽原4秒收起时间。已按原交互调整驱动并移除点击前截图，未修改生产时序、控件行为或断言，最终上述整条用例通过。没有再次运行此前的竖屏前提脚本。
- 上一批12441f2的[远端CI 43275116367](https://github.com/AritxOnly/bdFun/actions/runs/43275116367)已成功；本批新源码由后续push另行触发CI，不复用旧结果。旧5c61d91 release不覆盖本批，完整生命周期、剩余展示和最终release/交付继续。

### 2026-11-13 音频/弹幕剩余展示

- `/tmp/bdfun-fullscreen-remaining.log`：独立feature/app及严格架构通过，37s。随后测试筛选曾使用不存在的StateHolderTest类名，Gradle明确No tests found，未当通过；找到实际app/FullscreenDanmakuComposerStateTest后原样迁移并正确筛选。
- `/tmp/bdfun-fullscreen-remaining-verified.log`：checkLayering、独立模块/app debug、8项既有FullscreenDanmakuComposerStateTest全部通过（0失败/错误），22s。CI定向列表加入同一8项测试；没有新增镜像测试。最新961文件/10438 PSI节点/0违规。
- 静态对照：Composer除core模型import/可见性/最新sender getter外全部一致；Audio全部数值、颜色、字号、尺寸、跳转量和文案字面量序列一致，图片/设置读取仅替换为显式接缝。
- `/tmp/bdfun-audio-composer-runtime.log`最终通过：当前debug包PLAYING→真实全屏→更多/听视频→原封面、标题、-10s/+10s和退出控制→退出音频→弹幕输入→draft_refactor_probe文本及全部原样式项→crash空，结束force-stop。没有点击发送或更改设置。首次输入后颜色行被IME裁切；增加收起键盘步骤后完整样式断言通过，没有改产品布局或降低断言。截图`/tmp/bdfun-module-audio.png`、`/tmp/bdfun-module-composer.png`已查看。
- 上一批9cb8185的[CI 43280861557](https://github.com/AritxOnly/bdFun/actions/runs/43280861557)已成功，不替代本批新源码的后续CI与最终release。其余视频详情/平台接线及完整生命周期/最终交付继续。

### 2026-11-13 视频详情正文迁移

- `/tmp/bdfun-video-metadata.log`：checkLayering、独立feature:video与app debug通过，39s；963文件/10451 PSI节点/0违规。首轮只有一个旧导航import残留导致编译失败，删除并在app装配正确导入原导航后通过，没有改实现或断言。原无业务逻辑搬迁不追加镜像单测。
- 14文件数值/中文文案字面量对照，除简介复制的原两句文案随操作转入app装配外一致；布局、手势与动画保持。`/tmp/bdfun-metadata-runtime.log`：当前debug真实起播、作者/标题/简介/互动/原简介tab通过，crash空并force-stop，无账号写操作。`/tmp/bdfun-metadata-runtime.png`已查看，确认BV1orW46aEba、原五图标和正文均渲染。
- 自动化文字导出不含SelectionContainer中的编号；前两次编号文字断言失败，不算通过。等待动画后仍缺失导出，最终截图明确显示编号；保留其余文字断言。未修改产品选择语义，也未声称编号自动化断言通过。计数型分享按钮原本显示679而非“分享”，驱动改为核对实际原UI并截图。
- 此次源码使旧release过期；剩余详情/平台接线、完整生命周期及最终release/交付继续，不把当前截图当全站与main逐像素验收。

### 2026-11-13 详情分P/合集/相关列表骨架

- `/tmp/bdfun-video-tail.log`：checkLayering、独立视频模块和app debug通过，34s；965文件/10459 PSI节点/0违规。9文件全部数值及中文文案字面量序列一致；VideoCollectionState与原HEAD逐字一致（仅core模型类型路径变化）。没有改变纯逻辑或追加重复单测。
- `/tmp/bdfun-collection-runtime.log`首轮通过：安装当前debug/真实起播→详情滚动→合集展开后原单集标题渲染→收起恢复尾部→crash空/force-stop；没有点击单集、更改设置或账号写操作。截图`/tmp/bdfun-collection-open.png`已查看，原封面、时长、元信息和198dp局部列表保留。此用例没有覆盖选集/分P切换或完整生命周期。
- 视频主布局/原生装配边界仍继续迁移，不把正文与尾部迁入模块等同整条视频链路或全量重构完成。最终release与交付仍待最终源码后执行。
