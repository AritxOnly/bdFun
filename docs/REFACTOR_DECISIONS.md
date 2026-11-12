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

P1 HTTP/请求准备与签名抽离已通过集成验证；尚余拒签后的自动刷新重试、账号与业务接口分片、DTO/mapper 与业务策略；P2 页面状态迁移；P3 大文件拆分；P4 全局 Bus 与隐式参数清除；P5 可选模块化。不得因骨架可编译将以上项目勾选完成。

## main 并行开发的合入策略（2026-11-11）

- 先将已验证的重构保存为 `a323145`，再用 merge 合入 `origin/main`，不改写 main，也不覆盖双方历史。
- 当前纳入的 main 为 `671d264`，合并提交为 `e63e590`；包含个人主页竖屏头部/分段栏回归、小标题，以及动态/视频关注去描边。四个上游 UI 文件均已合入。
- 保留原分支已有的首页长按与收藏夹改动；不能把 main 尚未包含的本地功能误删。
- 每阶段收尾前 fetch origin/main，检查新提交与迁移前后的调用点；有新 main 则先合并，再重新跑受影响测试和 App 编译。交付前再检查一次；不把“未来可能有更新”理解为无限等待。
- WBI 密钥与时间戳使用单个不可变缓存快照，避免分开的 volatile 字段出现不一致；六小时 TTL、刷新失败使用旧 key、退出清空、协程取消原样保留。
- App/TV 参数签名抽到 AppRequestSigner；沿用原编码器、appkey、access_key 选择和时间戳规则，没有新增密钥或猜测服务端错误码。
- URL/请求头/表单与响应 code/data 提取移到 BiliRequestGateway；旧 raw/data/post 方法保留签名并转发。buvid 身份准备仍待账号分片迁移。
- 签名新增 11 项 JVM 测试（WBI 8、App/TV 3）通过；HTTP 3 项原测试继续通过。main 合入后的 App 单测、debug 构建与架构检查通过（共 62 项测试）；上一轮签名 release 只对应旧检查点。

- 阶段收尾再次 fetch，origin/main 仍为 `671d264`，已是当前分支祖先。
- 使用 Pixel_9 只读临时 AVD 完成安装、冷启动、首页真实推荐加载、视频详情与实际起播；进入作者空间并滚动，确认 main 的头部与顶栏小标题可渲染，crash 缓冲为空。未据此勾选完整播放/登录回归。
- 临时模拟器已关闭，原 AVD 快照未保存；截图位于被忽略的 `build/reports/refactor/`。

## 网络分片与核心类型接线

- 旧 Models/DynamicFeedSource 改为核心类型的兼容别名；新旧调用共享一套模型，不再双份维护。
- 原 BiliApi 的 139 个方法迁入 infra:bili；102 个公开方法通过 BiliBackend 转发，app 的兼容入口仅保留构造和静态工具转发。各网络分片文件不超过 400 行；没有增加接口或猜测字段。
- Cookie 持久化仍为原文件/原 key/原格式；HTTP 连接池共享；自动 CDN 选择通过启动层回调接入，不让 infra 反向依赖 app。
- 消息用户缓存和空降助手缓存从全局 object 改为 API 上下文实例，随容器持有；没有新建全局缓存。
- 分页、推荐过滤、横幅策略及原 14 项断言迁入 core:domain，原断言未修改。
- VideoDetailDto/VideoDetailMapper/FeedVideoMapper 用三份真实匿名 view 响应和一份 related 响应验证；测试样本目录记录来源。缺字段默认值、合集、长用户 ID、标题转换边界均有测试。
- 新增 org.json:json 20250517 仅为 JVM 测试替代 Android stub；比引入完整 Robolectric 更小，生产依赖未改变。
- PlaybackPolicy 与 CdnPolicy 用纯 JVM 测试固化兼容性、音轨优先级、mcdn 不换 host、备用 URL 与查询串保留规则；平台 Uri 编码仍由原 Android 适配器执行。
- Video/Comment/Feed/Account 仓储实现已在 AppContainer 装配；失败显式转成 Outcome，取消继续传播。现有页面仍可走旧入口，后续状态迁移使用仓储接口。
- 跨模块后 InlineVideoPlayer 只增加 audioUrl 的局部稳定值，保持原空值分支和 MediaSource 行为。

## 设置分组、StateFlow 与旧格式升级

- 新建 infra:storage，继续读写同一个 settings SharedPreferences 文件；原偏好键、迁移标记、默认值和范围限制保留。
- Appearance/Playback/Danmaku/Message/General 五组纯应用层 StateHolder 实现领域设置协议，以不可变 SettingsUiState + StateFlow 发布变化，应用层没有 Android、Compose 或 JSON 依赖。
- 旧 SettingsStore 从 913 行缩至 110 行，仅负责启动层装配和逐字段 Compose 投影。投影按变更键刷新，避免滑杆每帧重读所有设置；可写设置仍立即生效。
- 浮点设置保持最后一次修改后 250ms 合并写入；作用域关闭前可显式 flush。没有更换存储格式或一次性搬用户数据。
- 使用须知页改为共用 AppGraph 的设置实例，修复它单独写入后主界面内存状态不同步的问题。
- 81 个可写字段均通过默认值与读写往返测试；协议反射覆盖检查保证新增字段不能漏测。另有旧迁移、范围限制、浮点合并、搜索历史顺序/去重/上限测试。
- 首次新测试中 boostDefaultSpeed 的替代输入误用了原默认 3f，已改成不同的合法输入 4f；原默认值和测试断言均未修改。
- 用迁移前 APK 在只读 AVD 写入全部 81 个旧格式设置，保存实际落盘基线后覆盖安装新 APK；82 项（含搜索历史）逐项比对：missing=[]、changed=[]。冷启动成功，crash 缓冲为空。
- 架构守卫已覆盖 app 内的 feature 目录，并对 application 平台依赖、presentation 直接访问网络、跨 feature 引用报错；三个临时反例均被拒绝。
- main 收尾检查仍为 671d264，无新提交。

## 首页、动态、消息、媒体库与空间状态

- 五类页面的数据与写操作迁入各 feature/application 的不可变 StateFlow 容器；旧 UI 暂保留视图渲染入口，尚未完成 P3 的大文件拆分，不能据此认定全量重构完成。
- 首页保留返回时数据/滚动状态、刷新前插和本地推荐过滤；动态保留 UP 切换、游标与资料缓存。分页与刷新版本隔离，点赞/关注加重复提交保护和取消回滚。
- 消息仓储保留原消息游标、关注筛选与异步姓名补全；写操作后使过期的补全回调失效。通知页按原类型与 cursor 请求，不猜测服务端字段。
- 媒体库修复删除失败仍从页面移除、移动失败仍清掉源条目、移动到原收藏夹可能误删的问题；只有服务端成功的条目才移除。翻页传递全部历史游标字段，并隔离旧收藏夹响应。
- 空间页将资料/分页/合集请求移入状态容器；竖屏资料头与分段栏、主分支顶栏小标题逻辑保持。头图解码与亮度仍属于视图层。合集连续切换用版本号防止标题与内容错配。
- SessionStateHolder 收拢登录态：旧请求不能覆盖新登录/退出，瞬时网络错误不清空已有资料。首页、动态与空间按账号隔离，账号变化取消旧的视图投影作用域。
- 网络分片用 cancellableCatching 明确传播取消。签名 GET 在原 HTTP 403 或响应 -403 时最多强刷密钥再请求一次；不重放 POST，不重试 -412 风控，刷新失败保留原拒绝结果。-403 不被解释为已证实密钥过期。
- 弹幕原有测试配置缺少 kotlin-test，补齐后发现 Android Color/Log/JSON stub 导致失败。按 Robolectric 官方配置使用 4.17，仅在需要 Android 的九个测试类加 runner 与 SDK33；没有修改断言、没有修改引擎生产代码，也没有把 Android API 默认返回值设为 0。该测试依赖比自造 Android shim 更重，但能执行真实框架行为，纯业务测试继续使用普通 JVM。
- 当前检查点：app 157、infra:bili 35、infra:storage 2、core:domain 28、core:common 4、core:application 5、danmaku-engine 97 项均通过；checkLayering 与 debug 构建通过。此处只记录本阶段，不替代最终 release 与完整播放回归。
- 2026-11-11 再次 fetch origin/main 仍为 671d264；已合入的主分支变更保留。

## P2-07 详情只读迁移：详情与相关推荐检查点

- 视觉基准由用户确认为 main（当前已合入 671d264）；TeakUI 和 VideoActivity 相对该提交无差异。后续逐页验收视觉与交互，目前不宣称运行效果完全一致。
- VideoDetailStateHolder 接管 detail / related 的不可变 StateFlow；详情与相关推荐读取走既有 VideoRepository，取消继续传播。离线优先、缓存返回、并行播放地址/弹幕/相关推荐及首屏推荐失败时保留旧推荐的顺序保持。
- 写缓存时直接取容器最新推荐值，避免 StateFlow 尚未触发重组时读到旧投影。页面状态容器仍由页面 remember 持有，不宣称已解决进程恢复。
- 本检查点只是 P2-07 第一步的子集；playInfo / danmaku / error / loading 的状态与请求编排仍待迁入，交互动作、播放器、全局 Bus 和导航也未完成。没有勾选 P2-07 完成。
- 复用仓储后详情错误使用现有 AppError 文案映射（例如登录过期显示“请先登录”）；错误场景文案需要在后续行为验收时比对。

## P2-07 后续、评论与页面作用域检查点（覆盖上方“只读子集”进度）

- 视频六个只读状态与并行请求编排现已迁入 VideoDetailStateHolder，播放源/弹幕/云端进度走 VideoPlaybackRepository；清晰度与恢复进度位于 VideoPlaybackSelectionStateHolder。流版本与弹幕版本分离：切清晰度不丢初始弹幕，切分 P 不接收旧分 P 弹幕。
- 视频互动迁入 VideoInteractionStateHolder / VideoActionsRepository，字幕和作者数据迁入 VideoMetadataStateHolder / VideoMetadataRepository。应用层不依赖 Android、Compose 或网络实现；界面保留登录提示、面板开关、日志与历史记录适配。
- 原样拆出操作统计、简介、分集、合集、相关推荐行与读取缓存，拆出的七个文件均不超过 400 行；VideoScreen 主体、VideoPlayer 与评论主函数仍超过完成标准，P3 尚未完成，不把“已拆部分文件”记为整体完成。
- P4-03：Route 携带视频/动态快照、空间 mid 与评论定位参数；RouteJsonCodec 保存在 Intent extra 中的 JSON 使用已有 org.json，无新增第三方依赖。模型字段全部逐项编码，测试包括嵌套转发、直播卡、推荐反馈字段、Unicode、64 位 ID 与独立页面快照。删除全部 AppGraph.pendingXxx 和全局评论定位槽。VideoActivity BV/AV 解析改为页面 LaunchedEffect，取消不再继续写入其他页面；不再线程轮询全局槽。
- 评论主列表与楼中楼分页迁入 CommentListStateHolder，查询包含评论类型、UP、排序与根楼参数；请求隔离排序与根楼切换，取消在 finally 释放 loading。写操作走 CommentActionsStateHolder / CommentContentRepository。CommentLikeStore 替代 CommentLikeMemory，由 AppContainer 持有并在账号变化时清理，容量仍为 300；Compose 使用按 rpid 的逐字段投影，保留原列表重组粒度。
- P4-01 的页面状态部分：删除 VideoPageBus / PlayerUiState / ReplyTarget / VideoOrientationLock 全局 object，以 core/application/VideoPageStateHolder 和页面 VideoPagePresentationState 投影取代。VideoActivity 与 DynamicDetailActivity 各自提供 CompositionLocal；图片、滚动回调与发送器留在页面表现层，不进入纯核心层。局部变量暂保留旧名称以控制 UI diff，它们已不是全局实例。
- 跨所有仍存活播放器的暂停行为迁入 AppContainer 的 PlaybackPauseRegistry。保留旧页面退出只能暂停自身、新页面不被误停、画中画退出不自动恢复等断言；VideoExitOwnershipTest 只更换调用入口，没有更改断言。
- 本检查点没有完成 P2-08 / P4-04：ExoPlayer 与 BackgroundPlayback 尚未统一到 PlaybackController。其他 Bus、剩余缓存、剩余大页面和独立 feature 模块仍待处理；不能标为全量完成。

## 播放控制、系统媒体与页面壳后续检查点

- 建立 core/application/PlaybackController、PlaybackSnapshot 与 PlaybackSource，Media3PlaybackController 位于新 :infra:playback。旧解码器选择、音频焦点、缓冲参数、Referer/UA、DASH 音视频合并和恢复进度顺序按原实现迁入；VideoPlayer 的传输命令、系统媒体控件和后台接管共用同一控制器。
- 播放进度/缓冲/实际播放状态由控制器发布，原 UI 投影使用只读 Compose delegate，长生命周期弹幕/历史协程仍能读到最新值。PlaybackSnapshotReducer 保留“播放结束已处理”的确认，轮询不得重放自动连播。
- PlaybackSessionStateHolder 持有进程会话、所有者和交接状态，BackgroundPlayback 改为 AppContainer 所持适配实例的兼容 getter，不再是全局可变 object。后台 metadata 与进度订阅同一控制器；BackgroundQueueLoader 通过仓储加载下一分P/合集，退出或新选择使旧回包失效。
- MediaPlaybackSession 与 PlaybackForegroundService 迁入 infra:playback。app 的 Service 和 BroadcastReceiver 只负责取得 AppContainer 依赖；MediaControlsRegistry 替代 MediaPlaybackSession.current，旧会话释放不注销新会话。Media3 释放从后台清理入口转回 applicationLooper，避免错误线程调用先抛异常又被 runCatching 吞掉；仍需装机评估释放耗时。
- 新模块直接声明原 app 已使用的 Media3、media-compat、Coil 引擎与 AndroidX core；没有引入新的第三方技术栈。另一种组织方式是保留 app 实现并仅抽接口，但不能达到本任务要求的 infra 边界。AndroidX 的传递依赖用版本目录约束对齐 app 已解析版本，避免新模块重新选择 media-compat 的过旧默认版本；没有为了离线构建改变 API 逻辑。
- 原样拆出播放器手势/半屏控制/seekbar，以及全屏面板与控件；全屏原 2515 行文件已删除，新增展示文件均不超过 400 行。VideoPlayer/VideoScreen 主体仍超长，不能据此勾选 P3 整体完成；函数长度/参数分组也仍需继续处理。
- LibraryPageBus、MessagePageBus、ScrollTopBus 改为 PageChromeStateHolder + 页面壳的 CompositionLocal 投影。媒体库顶栏/批量浮层、消息动作与回顶部入口在同一壳内共享，多 Activity 各自持有；回顶部滚动回调留在表现层，不放纯核心状态。
- 评论行/预览/附图展示拆出；CommentImageViewer 改为评论页面的局部 StateHolder，楼中楼 Dialog 继承该页面的图片浏览上下文。
- 本阶段仅证明控制契约与部分运行路径；旋转、画中画、系统媒体按钮、后台退出与恢复进度仍需按完整清单逐项验收，不能以 JVM fake 或成功起播替代。

## 队列、窗口刷新率、表情缓存与评论入口后续

- VideoPlayQueue 改为 AppContainer 的 PlaybackQueueStateHolder；旧导入只是无存储 getter。队列复制调用方列表，顺序、缺失锚点与末尾不循环规则保持，账号切换清空。
- SmartRefresh 全局 object 改为每个窗口 remember 的 WindowRefreshStateHolder，纯 RefreshRatePolicy 保留 1500ms 与 0/1/60Hz。播放提示来自同一会话，fullscreen 更新按 owner 检查，旧页面退场不得覆盖新页面提示；未改 MainActivity 固定 60Hz 逻辑。
- EmoteCache 改为 EmoteStore + EmoteRepository + Compose 投影，共享评论/动态/私信/输入框的官方表情。请求取消在 finally 解锁，账号变化失效旧响应，保留空结果可重试及未知表情的原文本回退。
- VideoPlayer 长参数列表按媒体、展示、传输、互动、导航分组，各新增契约均不超过 12 参数；保持原事件 lambda 与入参表达式，主播放器函数仍需继续分解。
- 主评论入口已降至 295 行，列表展示拆为标题/排序/卡片/尾部，菜单复制分享与延时关闭放在页面 CommentMenuStateHolder；仍需处理 CommentScreen 主函数长度、楼中楼及其他超长函数，不能据此标记 P3 完成。

## 楼中楼与详情元信息继续拆分

- CommentThreadStateHolder 接管当前根楼新发送的回复投影和删除集合；发送仍走已有 CommentActionsStateHolder，只有成功并返回非 null 结果才增加条目。ID 为 0 的时间戳回退保留，时钟在请求返回后读取，与原时序相同；UI 不再构造业务回复实体。
- 楼中楼编辑/菜单/高亮由根楼范围的 CommentThreadPresentationStateHolder 持有；mounted 单独按窗口生命期保留，避免切根楼重置入场进度。分页与定位 effect 加根楼键，防止相同条数的另一根楼复用旧查询捕获。
- 楼中楼入口已为 322 行，窗口几何 73 行、内容 332 行、输入区 116 行；参数按输入/视口/导航分组。窗口的位置计算、2dp 重叠、非模态标志、动画参数和输入区控件树保持。文件达标仍不代表所有函数长度达标。
- VideoScreen 的作者/标题/统计/简介/互动展示整段搬入 VideoMetadataSection（349 行），简介展开状态移入纯 UI StateHolder，reset 仍在 detail.bvid effect 执行，所有动作回调与参数保持。VideoScreen 目前 1201 行，仍需继续拆分；不勾选视频详情 P3 完成。

## 详情布局、列表尾部与播放入参继续分解

- VideoInlineLayout 抽离播放器收起几何，并继续拆为几何/自动暂停/滚动连接/回复页桥接的多个小函数，保持原阈值、位移模型与动画参数。自动暂停的瞬时标记移入 UI StateHolder，remember 的 bvid 键保留。
- 分P/合集、错误与推荐列表的 LazyList DSL 搬至 VideoDetailTail：item key、7/8/9 阶段入场顺序、间距与形状保持，相关推荐先暂停再显式开新页的动作由入口传入。
- VideoPlayerAssembly 统一投影媒体及全屏入参，直接使用现有只读、互动、元数据与选择状态；回调仍由页面入口显式组装。默认标题封面、自动清晰度文案、cid 与进度均保留。
- 前后台重复的选集纯函数统一为 core/domain/VideoPagePolicy；保持多P优先、后接合集并按cid去重，单P仅在合集超过一集时切换为合集。删除两个旧函数副本。
- VideoScreen 现为 899 行，VideoPlayer 1742 行；仍超完成标准，后续还须拆生命周期/渲染装配与剩余业务入口，未将本检查点当作 P3 全部完成。

## 播放跟踪、进度存储与详情 effect 拆分

- VideoPlayer 内最后两类直接网络请求迁入 PlaybackHistoryRepository / SponsorRepository 及 PlaybackTrackingStateHolder。原历史起报/周期/结束补报规则在 PlaybackHistoryPolicy；请求保持独立协程，busy 在 finally 释放；片段按 bvid/开关版本隔离。
- PlaybackProgressStore 原有可变单例删除，旧名字仅委托 AppContainer 的 PlaybackProgressRepository；Android 实现在 infra:storage，旧 SharedPreferences 文件 playback_progress、p_ 前缀和 position,duration,timestamp CSV 不变。保留低于 3000ms 跳过、严格大于 duration-5000ms 清除、每 100 写检查、超过 500 条裁至 400 条的原边界。
- 播放进度校验/淘汰纯策略进入 core/domain/PlaybackProgressPolicy。没有清除原用户数据、改变文件格式或添加迁移双写。
- VideoDetailPager 独立承接两页保持组合、全屏/PiP 冻结竖屏尺寸及原 padding；元信息/列表尾部/评论仍在原插槽位置调用，portrait 尺寸是专用 UI StateHolder 的瞬时状态。
- 详情全屏同步、弹幕发送器、后台元数据和 pager 吸附拆为单独 effect 函数；全屏/播放/元信息入场和币/收藏面板开关由页面 UI StateHolder 持有，仍保留原 remember 生命周期。VideoScreen 现为 745 行，主动作和加载装配尚待继续拆，不宣称达标或全量完成。

### 2026-11-12 · 详情组装与播放器 effect 的生命周期边界

- 详情页的初始化/作者/页面元数据 effect、用户动作适配、弹层与折叠遮罩按原执行顺序拆出；VideoScreen 缩至 375 行，TeakUI 布局参数与组件树沿用原实现。函数长度及其余大页面仍需继续收敛。
- 播放器原 remember(Unit) 瞬时状态与资源引用收进同一组合生命周期的 VideoPlayerUiStateHolder；保留按 bvid 重置的独立状态，避免把不同 key 的生命周期合并。
- 生命周期暂停恢复、系统媒体队列、后台唱片所有权、控件显隐、错误回退与播放源/进度上报各自绑定。长期运行协程仍通过 MutableState 委托读取更新后的速度/媒体会话，不捕获初次组合的值。
- 本次是搬迁。原地址重试→兼容流回退、后台交接条件、进度保存门槛和历史上报周期均保持；最终必须用对应 APK 做播放、旋转、画中画、后台及 main 视觉对照后才能验收。

- 播放器展示按原绘制顺序分为 PlayerSurface、常显进度/热门弹幕/赞助提示、状态提示、缩放复位、全屏控制/锁/拖动预览/字幕和弹幕输入。无新增布局包裹层；图层 modifier、尺寸与颜色原样保留。
- VideoFullscreenActionBindings 与 VideoPlayerGestureBindings 只组装原回调：UI 字段通过同一个状态对象委托读取，playing 从同一 PlaybackController 状态读取。手势入口改为一个 GestureCallbacks 参数，仍以 rememberUpdatedState 更新 pointerInput(Unit) 的回调，避免重新引入陈旧闭包。
- VideoPlayer 文件降到 399 行并不等于 P3 或全量完成：长组装函数、控件参数和其余大页面仍需收敛，最终 UI/播放链路验收继续保留。

### 2026-11-12 · 播放器主组装函数与半屏契约

- VideoPlayer 收敛到 29 行组装函数（文件 51 行）；初始化、UI 初始化、媒体/进度绑定、图层组装分别位于 PlayerCompositionEffects 与 PlayerLayout，继续保持原 effect 顺序和绘制层级。
- PlayerObservedStateHolder 共用原播放快照的只读 derivedStateOf 投影，半屏播放、相对跳转等闭包通过同一个 State 委托读取最新值，避免提取函数时把 playing/position/duration 固定在旧一帧。
- InlineCompactChrome 入参收为状态和动作两个契约（状态 7 字段，动作 12 字段）；HalfScreenMoreSheet 收为可见性、关闭回调、状态、快捷动作和设置动作五项。仅改变参数组织，原布局树、间距、颜色、回调顺序保留；半屏控件、听视频和更多面板按原区块继续拆分。
- 空降助手的首次命中、左闭右开与已关闭过滤进入 core/domain 的 SponsorSegmentPolicy，由播放跟踪状态容器暴露；单视频提示关闭集合改为相同 bvid key 的 UI StateHolder，切视频仍重置。
- 新增领域用例覆盖片段起止、重叠原顺序、关闭后下一片段、空/零长/反向区间；新增状态容器用例验证另一视频的片段不投影。本检查点不等于其余长函数、全局对象审计、最终 main 对照与发布验收完成。

### 2026-11-12 · 全屏展示、输入与手势分块

- 全屏听视频按原绘制顺序拆背景、元信息、时间进度、播放动作、顶部栏；全屏主控件拆遮罩、标题动作、作者信息与时间进度，未新增布局包裹。右侧面板 StateHolder 仍在非听视频的 BoxWithConstraints 分支内创建，保留切入听视频时原来的销毁/重建边界与 264ms 收起延迟。
- 全屏弹幕输入的五项瞬时状态进入 FullscreenDanmakuComposerStateHolder，保留 80ms 焦点请求、发送时 trim、busy 防重复、缺少发送器直接关闭，以及请求结束后清焦点/隐藏键盘/关闭的原顺序。搬迁未改变原 runCatching 的结果与取消处理，不把发送失败改为新文案或保留输入窗。
- PlayerGestureSession 仅持有一次按下期间的状态，PlayerTapStateHolder 保留跨手势点击窗口；回调、宽度和 touchSlop 通过原时机的动态读取，继续使用 24dp 系统边缘、260ms 点击窗口、300ms 短按、380ms 长按及严格超过 70dp 的中心上滑。滑动扩展回调分组后 GestureCallbacks 入参收至 10 项。
- 两种进度条分别拆 UI StateHolder、原 pointerInput(durationMs) 和展示：半屏条仍要求明确 released 才提交并结束预览；全屏条保留原最终 seek 和 scrub/look/preview 的复位顺序。未借搬迁改变取消处理或回调捕获行为。
- 画质自动/手动行、收藏夹行、更多快捷/设置及亮度音量绘制仅搬出原区块；原过滤、排序、文案、颜色和点击顺序均保留。

### 2026-11-12 · 详情入口与 Activity 页面壳

- VideoScreen 收为请求、展示配置、事件和 pager 的显式契约；主组装函数 32 行（文件 45 行）。视频读取/选择/互动/元数据容器仍按 video.bvid 记忆，页面瞬时状态、描述与推荐入场进度仍按原页面组合生命周期记忆；没有把推荐动画移入懒列表或全屏条件分支。
- 元信息、尾部、评论窗格与播放器窗口分别接收不可变内容投影；播放器动作继续通过同一个 UI StateHolder 委托更新 playing/fullscreen。窗口顶部避让与收起条共用 VideoPlayerViewport 的原计算，新增边界测试。
- BV/AV 入口读取从 Activity 的直接 api 调用迁到 VideoEntryRepository / BiliVideoEntryRepository / VideoEntryStateHolder。保留 route 快照优先、非空 BV 优先于正数 AV、映射原 VideoItem 字段、失败/缺失标识关闭页面；取消继续传播，finally 释放 loading，旧响应不再覆盖新入口或关闭新页面。仅 BV 的导航入口也走 Route.Video，兼容 extras 继续由 RouteIntents 填写。
- VideoActivity 保留方向监听、系统栏、画中画、退出和生命周期判断，文件 305 行；Compose 内容入口移到 VideoActivityContent（47 行），tab/回复栏/输入/弹幕设置、渐变与独立评论分别拆出。通过 HostActions 传原生操作，未变更条件、尺寸、组件、动画参数及 overlay 顺序。
- Activity 的 pipMode 与页面的 comments/danmakuInput/auto-next 消费计数分别进入原生命周期对应的 StateHolder；TeakPagerTabs 继续沿用原选项、宽度、分页切换和玻璃采样提供层。

### 2026-11-12 · 详情展示短函数与主壳拆分

- 元信息区按作者、标题/统计、标识/简介/分区及互动行拆分，原 StaggerLine 0..6、间距、420ms 简介展开和绘制层级保持。简介测量仍使用同一 TextMeasurer/宽度/两行布局，尾部模糊与清晰前两行未换实现。
- ActionStat 的五项按压/水波瞬时状态进入同生命周期的 VideoActionPressStateHolder；按压回弹、长按计时、震动和 Unit 键手势分别搬出，保留投币 230ms/其它 160ms、长按预备 280ms 与 1280ms 完成阈值，以及原回调捕获方式。
- 合集展开状态仍按 title/episodes.size 创建，待定位目标、条目坐标和滚动状态仍随合集组合存活；NestedScrollConnection 继续在列表可见性分支之外记忆。展开/定位保留 40ms 延迟，内部高度 198dp、边缘渐变和卡片布局不变。分P、合集、错误、相关推荐的 LazyListScope 项按原 key 和顺序追加。
- 首页消息角标通过 MessageBadgeRepository / BiliMessageBadgeRepository / MessageBadgeStateHolder 读取。主界面仍按账号重建状态，仅在 STARTED 轮询，每次完成后等 60 秒；失败保留上次计数、取消传播。新增 JVM 测试覆盖游客/清零/失败/取消/不同账号实例隔离。
- App 主题色板与经典底栏搬到 presentation/shell，包名与现有访问方式不变；主底栏按宽屏、经典、悬浮与唱片分块。底栏透明度和唱片弹簧仍在可见性判断之外组合，几何、颜色、触摸拦截和导航回调保持；原 ThemePalette 兼容投影仍待全局对象审计。

### 2026-11-12 · 主界面短组装与搜索状态迁移

- BdFunApp 收为主题、页面状态、TeakTheme/Backdrop 提供层和 MainScaffold；主壳显式分组导航、滚动材质、数据投影与两块采样源。搜索文本/提交词/显隐和动态刷新计数进入原页面生命周期的 MainShellUiStateHolder；关闭搜索保留原清空顺序，底栏回调仍捕获本次组合的 selected 值。原未被读取的 splashVisible 记忆项已删除，未删除任何可见开屏，启动开屏仍由原生启动路径负责。
- 首页/动态顶栏、搜索层、底栏导航与返回处理分别拆出，原条件与布局节点不变。主壳和 SearchOverlay 已移除 BiliApi 入参，MainActivity 改为只传 settings；其他 Activity 尚未据此推定全部完成依赖收敛。
- SearchRepository / BiliSearchRepository 复用现有四个搜索入口；SearchStateHolder 管理热搜、联想、视频和用户结果，保留联想 180ms、首次关键词 90ms/同词筛选 180ms、用户先于视频、同词复用成功用户结果、用户失败不阻断视频、视频失败保留旧结果及 loadedOnce 防空态闪烁。请求代数使过期结果失效，取消继续传播并在 finally 释放 loading。
- SearchFilterState 的原 order/duration/1天/7天/183天与 quality 仅记录规则移入 core/domain，时间读取顺序不变。搜索仓储单独保存原异常 message（含空字符串，缺失时“搜索失败”），避免通用登录/空文案转换悄悄改变错误显示。
- 搜索展示按展开框、落地页 lazy 项、结果 pager/列表/行、tab/筛选菜单拆分，仍使用原 TeakPagerTabs（“视频/用户”，64dp）、采样兄弟层、列表 key、间距和两套滚动状态。筛选锚点进入同一 header 生命周期的 StateHolder。
- 迁移前搜索菜单 onFiltersChange 与 onApplyFilters 都只写 filters，appliedFilters 从未写回；此轮原样保留，不能把新状态容器可接收筛选条件的 JVM 测试解释为菜单筛选生效验收。该现有行为留待 main 对照和独立行为修复。

### 2026-11-12 · 空间展示拆分与分页动态读取

- SpaceActivity 保留路由解析、状态栏对比度、配置变化和销毁释放，文件 60 行；SpaceScreen 为 26 行短入口。展示组件在 presentation/space，按运行时、内容动作、滚动动作、材质/顶栏绑定、分页 effect 和具体 lazy 内容区块拆分。
- banner 的 ready/bitmap 仍按 mid 创建，分类选择与头部高度/位移仍按原页面组合存活；compact/wide 各四份列表状态始终在分支之外创建，宽屏资料另有列表。保持 736dp 断点、40%/280..440dp 资料宽度、原头图比例、pointerInput key、tab settledPage 同步以及收头/列表惯性消费顺序。
- 资料头像/统计/姓名动作/等级会员/认证/签名/充电、合集和内容卡片只搬出原节点。竖屏内容中的 TeakPagerTabs 仍 backdrop=null，顶栏仍在采样层外，宽屏 tabs/动作使用原提供层；没有新增包裹布局、换控件或调整尺寸/颜色/动画。签名 expanded 进入同 text key 的 StateHolder，260ms 展开和箭头绘制保持。
- 对照已合入 main `671d264`，此前 P2 把 videoPage/loading/end 等 Compose 委托变成不可变投影，长期 LaunchedEffect 捕获旧页码；本轮新增 application 的 SpacePageRequest/nextPage，让每次原滚动事件按当前 StateFlow 决定后续页，再沿原 scope.launch 发起请求。关注 busy 与按 tab 补拉也恢复事件时读取；账号控制器变化重新绑定相应 effect。新增 6 项 JVM 回归覆盖最新页码、busy/取消、结束状态、分类与原分支。
- main `671d264` 第四主 tab 的现有翻页分支仍为 bangumi（界面是“小店”），此轮没有借分页修正改变该分支。实际主 tab 为“主页/动态/投稿/小店”四个，投稿内有视频/图文/合集；任务卡早期“六个 tab”更正为实际界面，不新增 tab。
- 本地 main 指针仍是 f0330f0；本轮源码对照使用已合入的 671d264，不把本地旧指针当作最终验收基准。最终 main 刷新、逐页对照和发布仍在总目标内。

### 2026-11-12 · 私信状态、展示和显式路由迁移

- PrivateChatRepository 与 BiliPrivateChatRepository 包装原分页/已读/头像/上传/发送入口；上传和分享视频 JSON 解码搬入 infra，沿用真实旧字段与默认值。PrivateChatStateHolder 持有不可变状态，保留三次加载与 400ms 重试、8 秒前台轮询、旧历史副本优先去重、最新消息替换以及原 cursor 规则。
- 发送保留图片先于文字、图片成功后清附件、文字成功且草稿仍相等才清草稿，以及后续刷新失败的“已发送，消息列表稍后更新”提示。上传/发送 busy 在 finally 释放，取消继续传播；页面与账号各自的控制器/子任务及代数阻止退出后的旧请求回写或继续发送。
- PrivateChatActivity 只保留原生壳与入参兼容；presentation/message/chat 原样搬出倒序列表、气泡、时间分隔、共享视频、输入栏和表情。无新增包裹节点，原玻璃材质、尺寸、偏移和图标保留。EmojiPanel 仅去掉未使用的 BiliApi 参数。迁移前文件与 671d264 的唯一已有差别为 EmoteCache.ensure 不再接收 api。
- Route.PrivateChat 与 RouteJsonCodec 接入六项原会话参数；RouteIntents 同时写原 Intent extras，Activity 保留旧字段回退和原 session id 构造规则，空间及消息入口统一调用显式路由。

### 2026-11-12 · 消息中心与首页短函数拆分

- MessageScreen 原 986 行入口按会话列表/快捷入口/tab/游客提示、回复/喜欢/粉丝/系统卡片、共享材质、菜单与导航拆入 presentation/message，保留原包名以兼容迁移期调用；旧文件已删除。会话和通知入口不再接收 BiliApi，原未使用的 onOpenVideo 入参也已移除。13 个叶子函数体与拆分前逐字比较一致，日期格式化同样沿用原 Calendar/SimpleDateFormat 规则。
- 页面 tab/进度与已消费 chrome 请求计数留在原组合范围的 StateHolder；菜单按控制器隔离。控制器按账号创建，子任务随控制器离开取消，resume observer 用对应任务域发起请求；会话 revision 和通知 closed 守卫阻止已退出页面的旧结果回写、继续请求或弹完成提示。其余页面的生命周期 helper 保留原兼容入口。
- 两种消息页脚的原 more/重试选择进入 application，在事件时读取当前 StateFlow，保留“我关注的人”复用全部页、@我独立页及粉丝页码规则。MessageOpenTarget 仅搬出原业务分支和 root/target/source 优先级；Android nativeUri 解析、动态加载与实际导航仍在适配层，未猜接口字段或更改通知打开行为。
- HomeScreen 原 675 行入口、两种复用视频卡片和指标图标拆入 presentation/home，原包名和调用契约保持。下拉位移/已消费刷新请求/长按目标在原组合生命周期的 StateHolder；HomePullSession 保留 remember(state) 时捕获几何的规则、88dp/260dp、0.6 阻尼、8ms 单调手动缓动及原 140/160/200/220/260ms 时序。
- 首页 lazy key、首次索引 drop(1)、600dp 断点、宽屏有界列、底部 104dp、OneTake 每卡锚点和卡片两种原渐变/圆角/字号/UP 点击区域保持。封面信息带、进度条和选择圈以 BoxScope 函数搬出，未新增布局节点；没有借复用卡片迁移合并两种排版，也未改 TeakUI 源码。

### 2026-11-12 · 动态展示、图片查看器与详情状态迁移

- 原 ui/DynamicScreen.kt 的 1715 行内容拆入 presentation/dynamic，保留原包名和外部入口；旧文件已删除。DynamicScreen 42 行、DynamicCard 65 行、大图入口 56 行。卡片改为 DynamicCardActions / DynamicCardPresentation 显式分组，列表、空间和详情调用同步更新；没有保留超参数兼容副本。
- 列表下拉状态按 state 记忆，已消费刷新计数仍无 key；自动播放状态仍只在登录内容分支存活。保留 80dp/240dp、0.6 阻尼、8ms 手动缓动、原 140/160/200/220/260ms 时序，736dp 断点、原作者侧栏宽度、FullLine 项、稳定 key 和最近居中视频的静止后播放条件。
- 主卡与转发卡各自持有图片选择状态；图片格 bounds、每个 pager 页的 scale/pan 与查看器 shown 都放入同组合生命周期的 StateHolder。缩放/平移决策搬入纯 application 函数，保留 1..4 倍、严格 1.01 单指阈值、双指消费、回到原尺寸清平移及释放事件的原处理；点击判定仍保留逐事件 12px 与最多按下指针计数。220ms 放大、180ms 透明、200ms 关闭延迟、原 Rect 起点和 Dialog/window 参数不变。
- 作者/标题正文、PGC、视频、转发、IP 与操作行从原节点搬出；10 个未改造叶子函数体与拆分前逐字比较一致，未新增布局节点或换控件。TeakUI 源码未改，最终 main 像素和行为验收仍须独立进行。
- DynamicDetailRepository / BiliDynamicDetailRepository 包装原详情、IP 属地与点赞入口；DynamicDetailStateHolder 先显示传入摘要，再补全正文/图片/转发。详情成功仍覆盖 liked/count，失败/缺失保留摘要，IP 异步结果只补当前同作者的空位置。点击时立即乐观更新，发送由原 UI 协程执行；原详情无 busy 防重、取消不回滚、计数不 clamp、失败按当时当前计数加反向增量，均未借迁移改变。它与列表的串行/取消回滚/计数 clamp 规则不同，不应为了复用合并。
- 动态详情按账号与入口创建控制器及子任务，退出后旧详情/IP/点赞响应失效。DynamicDetailActivity 62 行只保留原生状态栏/生命周期/显式路由壳；内容、原三项 lazy header、评论外壳仍在 presentation/dynamic。CommentScreen 的未使用 BiliApi 参数已删除并更新三个入口，评论其余长函数/超参数契约仍未完成。
- 动态列表原容器只在账号替换时取消状态投影任务，没有显式关闭旧 application 控制器；本轮补 close 代数失效及列表/作者分页/点赞拒收，AppContainer 在替换账号 slot 时调用，页面动作子任务同时按 state 隔离。保留当前账号分类缓存的原生存期，不因 pager 暂离销毁它。InlineVideoPlayer 仍直接请求/创建 ExoPlayer，12 项 InlineVideoCache 与全局作用域审计留在后续迁移。

- 运行复查期间公开空间曾两次出现空动态而后续重入正常；SpaceContentEffects 仅在 debug 的首次加载结束后记录 dynamics count/end/error 摘要，便于区分空数据与领域错误。未更改空间空态渲染、重试策略或接口字段；具体原因仍需最终 main 对照确认。

### 2026-11-12 · 内嵌视频预览分层与原生适配

- 旧 ui/InlineVideoPlayer.kt 与 InlineVideoCache object 已删除。预览请求/缓存经 core/domain/InlineVideoRepository 与 BiliInlineVideoRepository 接入，app/feature/dynamic/application/InlineVideoStateHolder 持有不可变 StateFlow；展示分为 presentation/dynamic 的短入口、内容、effect 与 TextureView 布局。
- 仓储由进程容器持有两份各 12 项的 ConcurrentHashMap，保留原 BV 单键及原 trim 的第一键淘汰方式，保留详情成功后即缓存、播放失败仍留详情、并发请求不合并和详情结束后再次命中播放缓存的规则。这里用正式仓储替代静态 object，没有顺手切成 LRU 或按质量/账号扩缓存键，以免改变原预览的命中与请求行为。设置仍在详情结束且播放缓存未命中时读取。
- 原 preferred→64→服务端最后一项→无质量时 fallback 的顺序位于纯 selectInlineVideoMedia；空字符串视频地址和空白音频值保留原样，无地址时只跳过 prepare，不新增回退 UI。网络失败后原组合维持封面且不重试，重新进入可再次请求；取消传播，离开的旧状态不接收结果。
- core/application/InlinePlaybackController 只表达预览命令与比例 StateFlow，数据 InlineVideoMedia 位于 core/model；infra/playback/Media3InlinePlaybackController 独立保留原 ExoPlayer 默认 Builder、静音初始值、REPEAT_MODE_ONE、playWhenReady、DefaultHttpDataSource、Referer/UA、独立视频源及可选合并音轨。没有使用详情播放器的自定义解码、缓冲或音频焦点配置。infra/playback 增加 core/model 项目依赖以实现该契约，无新第三方依赖。
- 原生适配器及 TextureView 绑定由 AppContainer 装配，展示层不再导入 Media3/ExoPlayer 或 BiliApi。比例沿用正尺寸、有限值且严格大于 0.05 的校验；布局仍为原两层 Box/BoxWithConstraints，按容器与视频比例选择 fillMaxWidth/Height + aspectRatio。ON_STOP pause、ON_START play、关闭时 clearVideoSurface/release 的规则保持。
- 进一步核查发现账号监听会先把旧 dynamicSlot 清空，可能绕过此前按需重建分支的 close；本轮统一 releaseDynamicSlot，账号监听与按需替换都先关闭 application 状态，再取消投影任务并清 slot。真实账号切换仍须最终装机验收。

### 2026-11-12 · 评论回复条与评论页入口拆分

- 评论图片上传通过 CommentImageRepository / BiliCommentImageRepository 包装原 URI 上传入口；原 multipart、CSRF、bitmap 尺寸和 JSON 编码留在 infra，不重新猜字段，也不重写图片载荷。新增纯 CommentComposerStateHolder 持有草稿、表情、发送和上传状态，展示保留原 46dp 输入条、180ms 图标渐隐、玻璃材质、错误文案及 IME/底部避让。
- 明确保留 main 的已有时序：发送捕获裁剪后的文字与发送回调，执行协程时读取图片；成功即清当前草稿，不采用私信的草稿相等判断。新图片上传失败仍保留旧 pictures，旧上传可以晚完成并覆盖载荷，空但非 null 的上传结果依然视为成功。收起时先清焦点以允许输入法提交，再判断空草稿是否清回复目标/图片。取消继续传播，页面关闭后不接受旧结果。实际发送与上传未在模拟器触发。
- ui/CommentReplyBar.kt、ui/CommentScreen.kt 已删除，分别迁到 presentation/comment/composer 和 page，保留原包名。评论入口改为 CommentScreenRequest（8 项）、CommentScreenLayout（8 项）及 onBack/header，共 4 项参数；视频独立评论、视频 tab 与动态详情三个入口同步接入。菜单、楼中楼可见性/目标、短暂高亮及不喜欢快照留在原组合生命周期的展示 StateHolder。
- 评论根入口 25 行以内的函数体负责装配；列表、thread、分页/发送/焦点 effects 分开，列表、菜单、图片窗口和楼中楼渲染组件继续沿用，没有新增布局包裹节点。保留 320ms 菜单抑制输入、90ms 定位高亮、最多自动翻 5 页、楼中楼保留 target 退场及排序快照捕获时序。
- 对照 671d264 原 Compose 委托后发现分页长 effect 在迁移中捕获了旧 loading 快照；shouldLoadMore 现从当前 StateFlow 读取相同条件。包含 items 非空、各 busy/end 条件以及原 count=0 的边界，不添加新阈值或预加载策略。

### 2026-11-12 · 评论控制器作用域与楼中楼窗口拆分

- CommentListStateHolder 增加幂等 close：递增查询版本、释放 busy，拒收晚到页面及未来 reload/reset/loadMore/本地更新；网络返回后显式检查取消。CommentActionsStateHolder 固定账号清理代数，旧账号控制器不能再发起写操作；post/report/delete/block/like 在返回后检查取消与控制器有效性。
- 评论点赞仍使用账号内共享 CommentLikeStore，保留原乐观更新与失败/取消回退。关闭时每项在途回退只执行一次，并先删除回退记录；晚到的取消处理不能覆盖同账号新页面已写入的快照，旧账号回退也不能越过代数。此处没有把点赞状态改为每页面独立存储。
- CommentLikeStore 的清理代数另行暴露只读 StateFlow，空快照清理也会通知。评论区与楼中楼以账号、代数、页面和原查询/根楼作为控制器 key；这样即使会话投影与容器清理先后到达，展示也会重建到当前代数。菜单、动作使用对应 SupervisorJob，根楼/评论区替换或页面离开时先 close 再 cancel。
- 评论发送回调使用引用相等比较释放，旧入口退出不能清掉新入口绑定。长时间分页/定位 effect 及 collectAsState 按控制器区分；发送后重载/滚动在继续展示结果前再次检查作用域。菜单延迟回复、举报/删除/屏蔽结果不会在控制器失效后继续更改展示或弹完成提示。
- ui/CommentThreadSheet.kt（322 行单函数）与 ui/comment/CommentThreadDialog.kt 已删除；窗口装配、手势连接、focus/paging effects、输入动作、基本菜单/管理菜单、列表及卡片/渐变拆到 presentation/comment/thread。保留原包名与既有渲染叶子行为，不新增布局节点：220ms EaseOutQuint、2dp 窗口重叠、46dp 标题渐变、54dp 底部渐变、78/16dp 列表底部、700ms 回复定位高亮及 360ms 菜单延迟保持。面板保留 target 以完成退场的原行为不变。

### 2026-11-12 · 评论行、折叠回复与图片展示拆分

- 原 ui/comment/CommentRow.kt（287 行）和 CommentReplyPreview.kt（208 行）删除，展示迁到 presentation/comment/row，沿用原包名。CommentRowActions（9 个回调）与 CommentPreviewActions（4 个回调）显式分组，三个列表/楼中楼调用点同步更新；回调契约为不可变 data class，使相同回调可比较；本轮未做重组性能基准。
- 每条主评论的 liked/count 仍在原行组合按 rpid + 原始值 remember/derivedStateOf，仅订阅自己的点赞快照。折叠回复仍只取前两项，每行按 reply.rpid 记忆 bounds，保留行内独立点赞订阅；bounds 的 mutableState 只在展示 StateHolder 内。头像/人名导航、正文 @ 点击、图片查看、长按与更多使用原坐标范围，未扩大点击区域或替换图标。
- 原 Row/Column/Spacer 层级通过无布局节点的 Composable 函数搬出；经典/玻璃模式的 6/12dp 外边距、2/10dp 折叠回复内边距、32dp 头像、9dp 间隔、正文 14.85sp/21.8sp、折叠 13.5sp/20sp、操作图标上下偏移与 trailingWidth 保持。AppGraph 的逐行点赞/设置兼容投影仍需后续整体依赖边界审计，本轮不趁搬迁改业务行为。
- 原 ui/comment/CommentImages.kt 删除，图片缩略图/窗口/每页缩放展示状态/手势搬到 presentation/comment/images。每个 pager 页的 scale/pan 生命周期、pointerInput(Unit) 捕获、1..4 倍缩放、严格 >1.01 单指消费、逐事件 12px moved 阈值、未放大轻点关闭和黑底计数保持。没有把图片手势改成新的算法或与动态页面跨 feature 依赖。
- 原 ui/comment/CommentThreadComposer.kt 删除，44dp 胶囊输入面板与 BasicTextField 分别搬到 presentation/comment/thread；焦点、占位文案、输入样式、末尾 44dp 留白、键盘/nav 底部避让与发送图标颜色不改。TeakUI 源码未修改；本轮没有下沉或合并视觉相似组件。

### 2026-11-12 · 引导页状态、保存规则与展示拆分

- ui/OnboardingScreen.kt 的 1034 行内容迁到 presentation/onboarding，旧文件删除；同页私有渲染函数归并到各页面，最终展示目录 16 文件，最大 NoticePage.kt 187 行。步骤、提醒、六项基础设置使用纯 OnboardingStateHolder/不可变 UiState/StateFlow；AppContainer 注入现有领域设置接口及变更信号，生产代码不跨 feature 导入设置实现，也不再把 SettingsStore 传给引导展示。
- Saver 只写 step 整数。原 rememberSaveable(step) 之外的 backwards/accepted/termsViewed/showTerms 继续在重建时重置；没有将它们一并持久化。TermsActivity 原 manifest 处理 orientation/uiMode/density 等配置，未改 manifest；普通旋转保留会话，fontScale 触发 Activity 重建后只恢复步骤。
- 保留打开条款立即标记已查看、关闭不清标记、前进/后退保留本次勾选、同一步 go 也重置 backwards、未知保存步骤不 clamp 的行为。继续按钮只由 accepted enabled 控制，controller.go 不增加条款阅读/滚动校验。acknowledge 不禁止重复完成；页面写 onboardingAcked 后 Activity 仍沿用原第二次确认与 termsAckedVersion 更新/回放退出或进入首页的逻辑。
- 六项设置复用 AppearanceSettings；色彩和界面风格的 0..1 限制由既有仓储保留，darkMode 的原始读写及 UI coerceIn 保留。ThemePalette / TeakUiStyle 在展示 actions 中仍先写设置，再按原值更新主题投影；应用层无 Compose、Android 或主题依赖。controller.close 取消自身观察，拒收后续操作。
- 欢迎/完成的 Hero、品牌按压与涟漪状态为原组合生命周期的展示 StateHolder；没有共享两页实例。呼吸动画把 State 传入图层辅助函数，在 graphicsLayer 内读 value，保留原图层读取范围。360ms 水平步骤切换、200/160ms 渐变、1150ms 涟漪、4200ms 呼吸、原 spring、界面文案与 TeakUI/玻璃宿主均保持；没有借拆分下沉或合并 TeakUI 组件。

## 2026-11-12 · 媒体库与个人页

- 媒体库展示只接收显式依赖；交互和搜索维护不可变 StateFlow，数据控制器在请求前后检查页面与账号归属。保留 main 的全选计数规则和未展开项选择，不顺手调整产品交互。旧状态迁移中分页闭包捕获不可变空快照，已改为从当前控制器读取稍后再看条目数，恢复 main 委托状态的晚读取行为。
- LibraryEntranceMemory 按 P3-08 改为 AppContainer 的 4096 项有界顺序记录；重复命中不改变淘汰顺序，超上限最旧卡片可能重播原有 140f/70ms 入场动画。该容量边界是有界缓存的取舍，最终验收需包含返回已看条目。
- MineProfileCache 改为容器持有的单账号 MineProfileStore，保留五分钟/请求开始时间、三个请求顺序及逐字段回退，不扩大成多账号缓存。账号改变清理；关闭后的旧请求不得写缓存或发布资料。
- SentHistoryStore 连同原 SharedPreferences 实现迁入 infra:storage，SentRecord 成为 core:model 值对象；旧包名仅为类型别名，迁移不改格式、不双写。

## 2026-11-12 · 设置投影与弹幕设置面板

- SettingsStore 缩为 Android 存储与作用域构造入口，原 81 字段 Compose 投影原样迁入 feature/settings/presentation/SettingsBindings；逐字段观察与 setter 时机不变。Activity/主题层通过 LocalSettingsBindings 提供投影，弹幕面板不再自行查 AppGraph 或全局 DanmakuSettings。
- 弹幕面板关键词草稿、关键词编辑展开与当前弹幕列表展开由纯 StateFlow 管理；添加仍 trim、空白不清草稿、Set 去重且不自动启用关键词过滤。重置仅触及旧面板的 18 个字段与本地展开/草稿，不扩大到热弹幕等其它设置；旧兼容入口委托同一重置策略。
- 旧 726 行内容按滑杆、过滤、工具、行组件、图标与手势拆分；Canvas 命令和纵向放行/横拖/点击的原 pointerInput 表达式不改。绘图辅助函数使用原 Canvas DrawScope，不新增布局节点。

## 2026-11-12 · 设置页面拆分

- 648 行 SettingsScreen 拆为 presentation/settings/page 的外观、播放器、业务开关和信息区块；所有 TeakSettingsCard、条目顺序、条件、文案与字段回写不改。原主题/色彩 260ms 延迟仍由同一个页面 scope 执行，pending 值属于外观 lazy item 的 UI StateHolder；字号拖动保持临时值/结束提交与默认值吸附，不改为每帧持久化。
- 设置页通过纯 CdnSettingsSource 读取既有进程测速器；CdnSettingsStateHolder 保留空缓存强制刷新、旧缓存刷新与 400ms 观察，速度 Map 相等时不重采自动节点文案（原 remember key 的行为）。页面关闭/取消不能发布迟到快照，重复启动不创建第二个测速等待。CdnAutoSelector 的网络/存储实现尚未在本检查点继续迁移，不把观察端口提取写成网络实现已完成。
- 关于、开发者外链与退出动作由 SettingsScreenRoute 显式装配；Telegram 仍先定向客户端、无客户端再用系统，失败提示不变。系统照片选择与持久化读取权限继续留在 UI 平台边界，只把 URI 字符串提交到设置控制器。

## 2026-11-12 · 后台唱片展示

- BackgroundPlayDisc 拆为显式依赖、Compose UI StateHolder、封面效果、动画效果、绘制与 pointer 事件接入，消费 BackgroundDiscPlayback 的不可变会话 StateFlow。该端口扩展既有 BackgroundPlaybackPort，只暴露视频与长按动作；不移动或重写 native adopt/保活/通知逻辑。
- 手势决策纯化但保持原边界：长按结束优先；横向 >=32dp 且同幅对角时横向优先；竖向必须严格更大；点击距离严格小于 8dp；超过阈值后保持 armed，拖动最多 8dp。原 pointer 事件只累计仍 pressed 的采样、不累加抬手位置增量，按住不动仍由页面 scope 的 380ms 协程触发，不增加手势重试或不同产品动作。
- 保留静态 TeakGlassSurface 和旋转内容的兄弟结构，避免把旋转重新纳入玻璃采样。手动拖动/退出进度效果在 deferred exit 前注册，与原顺序一致；退出仍淡出 240f 并等待 280ms 才停后台，封面 120/180ms 模糊过渡、33ms 呼吸/旋转与 55°/s 旋转公式不改。
- 原 500ms detachedPlayer.refresh 仅把已有返回值写入 debug 的 BGP_STATE 日志，以验证原生 playing/speed/position；不增加 refresh 调用，不改变 release 行为。该诊断避免把前台服务硬编码 1f 的系统通知速度当成倍速生效证据。

- 唱片 UI 必须投影 cover/playing 后 distinctUntilChanged，再交给 Compose；不得直接收整份包含 position/bufferedMs 的会话快照。当前 UI 仍在点击时从端口晚读取最新 video/position，投影过滤不会把打开目标冻结在旧视频。
- 真实手势回归出现异常时先构建独立 clean main 包对照，不能把低帧率数学推演等同于主分支运行时证据。此次未改共享弹簧公式；修正了显示订阅与 runtime 引用复用差异，最终以原生 playing/speed/position 及退出媒体会话消失为证据。短距离 ADB 手势若最终采样贴近阈值，调整测试输入，禁止反向修改生产阈值迁就脚本。

## 2026-11-12 · 富文本、表情与链接标题分层（P3-08）

- 删除旧 EmojiPanel/BiliLinkTitles/LinkText 文件；新展示位于 presentation/text，七处 EmoteText 调用用 content/style/decorations 分组，原参数值不改。保留 main 的 Text 快捷路径、remember 键、图片/徽标/尾部占位尺寸、面板玻璃/阴影/缩放/四行布局。
- 通用 URL 裁剪在 core/common，视频 token/识别分支在 core/domain，区间与标题/面板状态在 core/application。短链重定向与标题查找在 infra:bili，通过既有真实 view/viewByAid 请求，不猜新字段。av 保留两次请求，完整 av URL/大小写短链原来的不解析情形保留。
- 标题正负缓存合并为 4096 项 LRU（长期滚动内存有界），淘汰后允许重新请求；账号切换显式清空并使在途结果失效。这是缓存寿命差异，不声称无限时间内请求次数完全不变。正文仍先显示原文，命中缓存先发布，缺失标题并行取得后整批发布；取消不写负缓存。
- 面板保留一次 ensure 后取快照；已在预加载的面板不自动追踪随后官方包更新。表情内容仍直接加一对括号，不改旧数据语义。未修改 TeakUI 或原弹簧公式，未引入依赖。

## 2026-11-12 · 扫码登录请求与展示分层

- 删除 ui/LoginScreen.kt，拆为 presentation/login 的根、标题、二维码/保存、状态/刷新、位图/MediaStore 适配。Activity 显式传页面控制器与回调；控制器关闭后不再发布旧请求。
- QrLoginRepository 使用明确的 Success/Failure(message: String?)，避免通用账号错误映射丢失服务端原文或把空串换成默认值。infra:bili 只包装既有真实二维码 API，不改字段、Cookie 或成功后 Session 写入流程；取消传播。
- 保留 1500ms 先延迟后轮询、网络错误继续、Waiting 不覆盖已扫码提示、成功 profile 可空、过期固定文案、Error 空白文案回退。刷新隐藏旧图片而保留旧 key，不能为了“修复”改掉主分支原轮询规则；更换 key/关闭后丢弃迟到结果。

## 2026-11-12 · 自动 CDN 测速边界

- 删除 app/data/CdnAutoSelector 与 CdnSettingsAdapter，AppContainer 创建单个 AutoCdnSelector，设置控制器只观察 CdnSettingsSource，启动和播放 CDN 解析共享这个实例。HTTP 测速独立 CdnSpeedProbe，旧 1MiB Range、两轮取较慢、6 秒/轮、5/8/10 秒超时均不改。
- cdn_auto/ best_id / best_speed(float) / measured_at / speeds 仍由相同键读写；逗号/最后冒号编解码移到 infra:storage。未迁移或覆写原存储格式。保持失败缓存/候选过滤/并行 4/同分首个节点/严格一小时新鲜度，包括无效 bestId 的原新鲜度规则。
- 视频展示去掉 BiliApi 参数穿透：只从 Activity 注入晚读取 isLoggedIn 回调，供交互前检查；纯渲染仍使用本次内容快照 loggedIn。评论/分页中原本完全未使用的 API 参数直接移除，不新增账号状态副本。

## 2026-11-12 · 关注/粉丝页状态与动作

- 全 app 扩展边界扫描发现 RelationActivity 内仍有 Composable 网络调用，不能只扫描 ui/presentation 目录认定清零。现 Activity 只开屏，RelationScreenRoute 组装设置/账号/工厂/导航，展示和分页/关注动作分别位于 presentation/relations 与 feature/relations/application，infra:bili 实现 RelationRepository。
- 保留失败刷新不清行、重试原 reset/cursor、mid 去重、重复页面停止自动分页、关系未知时只补查下一次才执行关注、服务端返回 false 原逻辑仍按请求完成处理。原分页测试仅适配新仓储构造，断言未改。
- 页面 close/账号变化阻止未来请求与迟到状态/提示。控制器变化只取消对应子协程，保持原 TeakScaffold 与 listState 的组合位置，避免切排序重启整个玻璃页面/动画。frequent 保存恢复仍是单 Boolean；busyUsers 属于控制器。

## 2026-11-12 · 离线缓存队列与文件/传输边界

- 删除全局 VideoCacheManager 和旧 ui/CacheScreen，进程队列由 AppContainer 持有 DownloadQueueStateHolder；不可变 StateFlow 位于 core/application，视频资料/文件/进度分组模型在 core/model。展示在 presentation/cache，显式取得队列和导航回调；离线优先起播、视频菜单与前台服务共享同一队列。
- domain 仅定义 VideoCacheStorage/VideoCacheTransfer。HTTP/HEAD/封面传输在 infra:bili，旧 video_cache/items JSON、外部私有目录优先回退 filesDir、video.part/audio.part→video.m4s/audio.m4s 和 cover.jpg 在 infra:storage。20 个平铺字段、枚举名、默认值、失败/中断/文件丢失提示、float 进度和 URI 行为均保留。
- 同 id 仅 Failed 可重新入队，音轨必填；视频后音频，HEAD 两轨长度相加，未知总量使用已收字节；220ms 共用发布时钟、5000ms 共用落盘时钟、阶段 0.98/0.99/完成 1 的原规则保留。文件失败保留最近发布进度，完成以实际两文件长度为准。
- 任务使用独立 token 和 Job 身份，删除后迟到更新不得复活条目；同 id 重下时旧 cleanup 不删除新 part，旧 finally 不清新 Job。初始存储读取期间删除的条目也不能迟到恢复，并在读取结束补持久化删除结果。这是原竞态的修复，不改正常下载和展示规则；新增竞态测试证明隔离。
- CacheService 仅负责原通知/保活，进程依赖显式初始化；通知频道、ID、进度文案和一秒 tick 未改。没有新依赖、服务器字段或 TeakUI 变更。

## 2026-11-12 · 首页顶栏与弹幕层/半屏输入拆分

- 首页顶栏从 15 个参数收敛为 content/search/actions/modifier，头像/两端 Crossfade、背景、玻璃搜索面、输入焦点和结果覆盖层分别提取到 presentation/shell/homebar。不增加布局节点；保持 wide 判定、尺寸、材质参数、160ms Crossfade 与原 LaunchedEffect(searchResultsVisible)。searchReveal 原本无渲染用途，分组后仍不新增用途。
- 弹幕过滤提到 core/domain，不再在 Composable 内执行业务过滤；保留 mode 1..5、只过滤 mode1 的滚动开关、原固定/颜色/关键词规则、先按输入去重再排序。引擎窗口/配置/遮罩/时钟是原代码片段，位于 presentation/player/danmaku，未修改 danmaku-engine 生产文件。设置从 LocalSettingsBindings 投影，原人像防挡固定 false 的规则保留。
- 半屏草稿/发送/失败改为纯 StateFlow 状态容器，发送器仍由当前页已有元数据动作提供；固定参数 1/25/0xFFFFFF、trim、失败保留草稿、成功清空并关闭均不改。同步 begin 保持忙碌标记立即生效，再由页面协程完成发送；请求身份防止重复完成，close 不再弹迟到成功。取消按项目规则传播并释放 busy，不再被旧 runCatching 当成失败。
- shown/showEmoji/frozenBottom 仅在展示 StateHolder。保持首帧后显示、280f 平滑进度、80ms 聚焦、IME 与导航栏最大 bottom、关闭冻结 bottom 后 200ms 移除及原 close token 初值。全屏输入原“发送结果不改变收起顺序”的行为未在本批修改，后续收敛必须单独保留该差别。

## 2026-11-12 · 外壳、图标、开屏/缓冲与记录/卡片滑动尺寸清理

- TeakActivityScaffold 按 options/disc/slots 分组，content 仍为最后参数。root chrome/主题/overlay/menuOverlay 顺序完整保留，参数默认值不变；主源码 18 处与 debug 日志页 1 处调用原表达式无损展平比对。TeakUI 模块未改。
- MenuGlyph 七种 DrawScope 分支各自成函数，仍只有原 Canvas；坐标/描边/Path/默认中心均不改。TeakSplash 的八个 Animatable 合入展示 StateHolder，时序与色板独立；TvBuffering 的三段纯渐变函数保留 2700ms 周期/相位/阈值，Canvas 内仍读取状态，未把每帧点值改为整页组合输入。
- SentHistoryStore 实现 core/domain 的 SentHistoryRepository，AppGraph 暴露接口；旧 JSON、500 条上限与同步快照/写入实现未变。MySentScreen 拆标题/筛选/空态/行，记录与筛选为纯 StateFlow；保持首次进入快照、未知 kind/未知 filter 的原行为、原时间格式和视频跳转默认字段。移除原本未使用的页面 onBack 入参，外壳仍负责返回。
- SwipeCardPager 的状态/弹簧/指针/测量分开，保留 400/30、dt=.001..05、8ms、停止阈值、0.32 边缘阻力、0.16 预测。callback/selected/sharedProgress 通过每次调用的 runtime 捕获，未引入 rememberUpdatedState 或扩大 pointerInput key 来悄悄改变原捕获规则。pageCount 缩小时小位移可能回到原越界 selected 的规则也保留，测试只固化本次迁移，不夹带修复。

## 2026-11-12 · 动态解析、通知和全屏输入边界

- DynamicApi 的旧解析拆成 major 引用、video、content、live、根 mapper，仍按 archive 存在优先于 ugcSeason、draw→opus→article→common→live 等旧规则读取。原始 JSON 字段没有新增；8 个新测试是旧字段的结构探针，不冒充真实 API 样本。匿名主机/设备采样均被 412 拒绝。
- 播放通知只提取 metadata/playbackState 更新函数，title/artist/playing/art 读取顺序、速度 1f、按钮 action code、频道与 artwork 生命周期均保持。真实视频前段前台→后台后，系统暂停/播放/停止会话清理验证通过，避免临近片尾自然结束混淆停止证据。
- 全屏输入迁到纯 StateFlow，仍同步置 busy、trim 后文本点击时捕获、样式在协程开始时读取；成功/false/普通异常均收起且不清原草稿，与半屏失败保持输入的规则不同。仅按项目取消/生命周期约定传播取消并释放 busy，close 后不发起排队请求、不处理迟到收起；无发送副作用的装机对照独立记录。
- app/presentation 的模型兼容别名改为直接导入同一 core/model 声明，空间横幅原 image(url) 字符串规则提到 core/common，infra 入口仍薄转发；不改变 URL 格式、空白/大小写/@/整数裁切行为。
- 守卫补查完全限定名和 app/presentation，屏蔽字面量/注释后保留行号；Gradle checkLayering 先运行 10 个隔离目录正反例。只暂留两种明确的旧诊断钩子，不放行 data 包；旧 ui/模块图/字符串插值内引用仍列为未完成，绿灯不代表完整架构证明。

## 2026-11-12 · 播放器展示与原生输出/媒体会话接缝

- VideoPlaybackPlatform 继承纯 PlaybackController，展示仅使用端口；AppContainer/AppGraph 工厂显式返回端口，app/playback 的 AndroidVideoPlaybackPlatform 委托原单个 Media3PlaybackController。没有新增播放器实例、进程缓存或输出包装 View。
- 原 PlayerView XML inflation、player 绑定、useController=false、黑色 shutter、三分之一 TextureView 截图与 FIT/FILL/ZOOM 映射移入 Media3VideoOutput。展示保留同一 AndroidView factory 位置、变换/手势/弹幕/覆盖层顺序，resize effect 仍只以 fsResizeMode 为 key，不借机改变原 playerViewRef 晚到时的规则。
- 响度音效创建保留 audioSessionId>0、600mB 增益和原 runCatching，展示仍按 ready/settings 先释放旧引用、再置空/创建，组合退出释放；听视频按原时机 setTrackTypeDisabled(VIDEO, fsAudioOnly)，只把实现调用移到适配器。
- ForegroundMediaSession 是 core/application 的纯会话端口，infra 的实现逻辑不改。系统会话仍以 title/author 为创建 key，cover 不新增 key；队列/媒体状态/释放原顺序保留，回调捕获规则不变。用户代理由装配层提供原 BiliBackend.UA，不再从展示引用网络入口。
- 守卫新增 ui/player 下 Media3、infra、BiliApi 禁入与 2 个反例；其余旧设置/进度/队列兼容入口和原生 Activity/Service 广播桥仍待后续收口，不能据本批宣告完整展示分层完成。

## 2026-11-12 · 视频读取缓存与自动选档作用域

- 删除 ui/video/VideoReadCache.kt 中两个全局对象 RelatedMemory/VideoCache。AppContainer 持有纯 VideoReadCache，VideoPagePresentationState 持有绑定 mid/revision 的页面句柄，控制器接线显式传入。真正退出仍仅清当前 bvid 的推荐，旋转只关闭旧句柄、不清条目，详情缓存仍保留；容器关闭清空并拒绝未来访问。
- 详情仍是6条插入顺序 LinkedHashMap，读取、覆盖、交互更新都不刷新淘汰顺序；推荐仍是12条 HashMap，保持原 keys.first 的规则，不改为 LRU，空推荐不能覆盖旧列表。liked/like/coined/coin/favorited/favorite 六字段缓存更新及离线→指定进度→内存→网络顺序保留。
- 账号变化/登录失效/快速A→B→A使旧页面句柄失效，旧请求不能写入或删除新账号缓存；关闭页面不能重新写缓存。SessionStateHolder 原 refresh 成功换身份、成功返回null或LoginExpired时未推进revision，现仅身份mid变化时递增并释放busy；同mid资料更新、瞬时网络失败不递增，登录/退出update原递增规则保持。此项是账号作用域隔离修正，不改普通页面布局或同账号缓存规则。
- automaticQualityFor 的原平台带宽读取在 infra/playback/AndroidNetworkBandwidthSource，Kbps→/1024.0/阈值/SDR阶梯在纯 AutomaticVideoQualitySelector；空候选不读取系统，仍按原阶梯顺序选首个对应原输入项，阶梯没有候选时仍取最小id（包含原可能回退HDR/高档的边缘行为，不夹带修正）。infra:playback 显式依赖既有 core:domain，无新增第三方依赖。
- 全仓无调用的 app/data/HttpClients 薄代理已删除；infra 的唯一共享连接池/线程池不变。具名 object 从31减少到28，逐一状态/生命周期记录于 GLOBAL_OBJECT_AUDIT.md；目标<8与剩余依赖收口仍未完成，不通过排除纯工具/debug或改成companion缩小统计口径。

## 2026-11-12 · 外部/剪贴板视频入口的类型路由与生命周期

- 删除 BiliLink 全局代理。MainActivity 的最近原始链接/剪贴板和请求消费迁到 core/application 的纯 StateFlow IncomingVideoLinkStateHolder，解析依赖 domain/VideoLinkResolver，HTTP 与 IO 在 infra/bili。应用层返回 Route.Video，Activity 仍用原 RouteIntents 创建相同 Intent；core:application 显式依赖已有 core:navigation，只使用纯路由契约，没有新第三方依赖。
- 保留 URI 优先于 EXTRA_TEXT、clipboard 原 coerceToText、空白不记录、非链接剪贴板先记录、原始文本紧邻去重、失败也记 lastHandledLink、不同请求并行按完成顺序打开、不扩展裸 av/时间点/评论参数识别。关闭/取消后不迟到导航，Pending 请求只能由原控制器消费一次；仍不跨旋转/进程保存最近文本。
- 短链四跳/相对Location/空Location继续循环/5秒connect与read/UA Mozilla/5.0/disconnect顺序与BV前缀规范化，展开后与 main671d264原实现一致；保持不支持/失败静默，使用显式 Unresolved 结果表达该既有语义。
- MainActivity 不再保留未使用 BiliApi 字段；VideoActivity 仅为 isLoggedIn 读取 API 的依赖改为 LoginStatus。装配 lambda 仍实时读取原 api.isLoggedIn，不缓存 Boolean，也不以 session资料是否到位代替凭据判断。现有根 Activity 壳禁止 AppGraph.api/直接 bili 实现的新守卫已接入，两个反例均失败退出；更深层展示/模块图审计继续执行。

## 2026-11-12 · 视频/播放器设置的显式契约与逐字段观察

- 删除 ui/DanmakuSettings 全局代理。原仅有调用的 enabled 映射仍为 dm_on/danmakuOn；无调用的代理字段直接删除，不复制一份新全局对象。人像防挡固定false和重置字段范围由既有渲染/应用层规则保持。
- VideoPagePresentationState 显式接收核心 SettingsRepository；视频详情、半屏/全屏播放器、生命周期、手势、进度、响度、来源与赞助片段等原 AppGraph.settings 读取改从页面契约取得。回调仍晚读属性，效果/remember/pointerInput keys、原读取点与所有布局表达式保持。DOWN时的亮度/音量手势开关经当前设置回调读取，pointerInput仍为Unit，不把设置变成新的重启key。
- SettingsBindings 的81个字段及原6个操作显式实现核心契约，全部保持原逐字段 Compose 投影/get/set/存储键；5组接口返回同一投影。repository属性仍返回纯 SettingsStateHolder，应用控制器继续注入纯仓储，禁止把Compose投影当业务层依赖。没有使用默认接口委托兜底，新增契约字段漏投影应让编译器报错。
- 主体迁移25个文件去除明确接线差异后完整token一致（保留字符串字面量），不以简单grep替代布局/时序证明。新规则禁止ui/player、ui/video与VideoPlayer回查AppGraph.settings或旧DanmakuSettings；仍有其他全局兼容入口/模块边界需要后续处理。

## 2026-11-12 · 实际首页模块与共享卡片边界

- 首页纯状态/反馈、Compose投影、网格、下拉分页和操作面板迁入真实feature:home；原16项测试随代码移动，未修改断言。app提供原仓储/CoroutineScope，公开工厂组装内部状态，模块不反向import app或infra。
- 共享卡片仍被空间/媒体库使用，先在app/presentation/video/card保留原实现，通过HomePresentationHost注入卡片、一镜到底anchor、原加载与宽屏网格；不是复制组件或向TeakUI加入业务模型。P3-07完整共享UI清单与下沉仍待完成。
- checkLayering增加checkModuleGraph：读取Gradle配置中的真实ProjectDependency，排除测试配置，校验核心依赖次序、feature隔离、infra不可依赖feature/app、共享UI/引擎不可回指业务及模块环。当前无新的外部依赖。CI尚未接线，不能据此验收P5-04。

## 2026-11-12 · 首页/动态账号资源槽与登录投影

- UID不能区分A→B→A或同账号重新登录；StateFlow可合并中间值。账号资源使用SessionIdentity(mid,revision)，core/application的SessionResourceSlot获取时同步核对，不只靠异步collector清理。切换/关闭只释放一次；工厂内身份变化/关闭会释放新建结果并取消发布。当前接入首页和动态，保留同一identity进入详情返回的缓存/滚动状态。
- 登录Compose投影保留user与identity，而不是仅投影user。读取兼容value也会观察revision；refreshing/暂时错误不加入投影，避免原资料未变时额外刷新。个人页/媒体库状态工厂捕获identity，显式请求携带revision作为remember key；不更改网格、卡片、文案或正常加载顺序。
- 首页释放时关闭纯控制器/反馈状态并取消原scope：迟到首屏、刷新、分页、分类补全不更新旧页面，迟到watch-later不弹成功提示，迟到dislike不再过滤旧信息流。此处是账号/关闭竞态修复，正常账号生命周期内原操作结果/排序/过滤算法保持。尚未收口空间/全部消息/进程缓存作用域，P4-06不验收完成。

## 2026-11-12 · 空间按Activity实例持有状态

- 删除AppContainer中按(ownerMid,spaceMid)缓存控制器的进程Map及按mid批量释放入口。每个SpaceActivity通过自己的ViewModelStore持有SpacePageViewModel；同UP的两个页面独立，配置重建复用同一个Store，真正退出onCleared关闭账号资源槽。账号revision变化时旧控制器关闭，新页面状态由同一页面槽创建。AppContainer关闭也使旧空间控制器的归属检查失效。
- Activity显式传入session/controller，整个presentation/space不再回查AppGraph；新增守卫反例阻止重新引入。界面仍使用原TeakUI、chrome、4个Tab、手势/分页与五个并发请求，原success/error兜底不变。每次仓储调用和bootstrap await后检查账号/取消，close统一释放loading/followBusy并回滚未完成的乐观关注，拒绝迟到成功提示/列表结果。
- 在libs.versions.toml显式声明androidx.lifecycle:lifecycle-viewmodel，沿用已有2.11.0锁定版本；该组件原已由Activity依赖传递引入，本批不是引入新版本或另一个状态框架。采用现有ViewModelStore生命周期，替代进程mid缓存或手动保留非配置实例补丁；后两者无法可靠隔离页面实例/释放资源。

### 空间首轮动态失败语义恢复

main671d264的SpaceActivity bootstrap只在fetchedDynamics!=null时替换动态/offset/end；首次失败不会把初始dynamicEnd=false改成true，选中动态Tab后仍能触发原分页加载。旧迁移的applyDynamics(Failure)同时用于bootstrap/分页，导致首次暂时失败便终止重试。本批恢复bootstrap只应用Success，分页失败仍保持end=true原规则。新增三项纯测试覆盖失败后非空恢复、刷新失败保留已有items/cursor、真正分页失败停止自动请求。原生main/new对照发现正文不同，设备日志确认新包曾收到-352；这里修复的是已由main源码证实的语义差异，不据此宣称所有服务端响应或动态正文均已对齐。

## 2026-11-12 · Kotlin PSI尺寸门槛

- 新开发模块tools/kotlin-audit使用Java/JDK21直接调用已锁定Kotlin 2.4.21的compiler-embeddable PSI；版本进入libs.versions.toml。只属于开发工具，不作为Android依赖。实际debugRuntimeClasspath、最终debug APK DEX均确认不含编译器/审计器；模块图守卫新增app不得依赖tools，临时真实Gradle依赖注入也被拒绝。
- 正则只数具名块函数不能证明表达式/构造/初始化lambda等达标；改用逐PSI节点定位，17项正反例验证文件/体/参数边界、匿名函数/访问器/init/函数类型/context/字符串插值、语法失败与CRLF。保留原400/80/12阈值，不给数据类/长构造/debug设置豁免。
- reportKotlinSizes返回0仅用于诊断；checkKotlinSizes发现超标或语法失败实际非零，空扫描也拒绝且清理旧报告。当前909文件/9511节点有19条真实超标，清单见KOTLIN_SIZE_AUDIT.md；严格任务还未并入checkLayering，不能用report成功当验收通过。

## 2026-11-12 · 空间资料mapper与真实公开响应

SpaceApi仅组织原五并发请求、原await顺序和条件获赞补查，JSON字段组装移到SpacePayloadsDto/SpaceInfoMapper；保持全部23字段表达式、VIP现代/旧字段优先级、横幅顺序和充电回退。HTML/网页vlist沿用原parseSpaceVideo回调边界，未在缺少真实网页投稿成功响应时臆造字段。五份公开接口成功响应按来源说明进入纯JVM测试，额外变体明确为结构探针；临时采样入口/manifest和设备缓存均清理，最终APK不含入口或样本。

## 2026-11-12 · main直播集成与阶段暂停

以main 4c4e03b为本批直播基准，迁移不改HTTP参数/返回字段、WebSocket Cookie/鉴权/心跳/压缩限制，播放器继续使用原teak_player_view布局。主线程UI只接收不可变状态/回调/原生输出端口，业务顺序、同URL重载、失败保留旧流、首个业务包清占位和300条窗口按main保留。离开页面取消持有作用域并拒绝迟到结果，新增纯JVM测试覆盖此生命周期边界。

LivePlayerChrome/布局/聊天/设置/手势/弹幕渲染按原节点拆分；remember与effect/pointerInput keys、阈值、尺寸、文案保持。设置读取改为同一SettingsBindings投影；liveMode仅隐藏main已隐藏的非直播项。保留main的本地发送回显，不新增账号写入接口。

Brotli 0.1.2沿用main选择，集中版本表并仅由infra:bili声明，用于服务端v3压缩包；不用自写解码或移除支持，以免改变协议行为。infra:playback显式声明项目已有media3.ui（原PlayerView），不引入新版本或替换原生播放器。直播Mock和协议工具两个main对象进入全局审计，mapper改为容器持有实例，不伪造全局对象目标达标。

用户要求本步结束后编译默认签名release并暂停；只跑直播定向测试和必需构建/守卫，不重复全套917项历史测试，也不把本阶段产物当作全量完成或最终飞书交付。

## 2026-11-12 · 不可变字段分组与严格结构门槛

十二个长构造器按职责拆为不可变值对象，主记录保留全部原只读属性。所有构造/copy调用显式接入分组，不新增长参数工厂，不用单行压缩或排除构造器来规避尺寸。类型相等仍覆盖全部原字段；手写JSON的键、值、顺序与默认值不变。动态commentOid保留在主记录的id默认值，复制时沿用原值；各分组的copy只变更指定字段，其余分组/字段保留。

全屏回调按导航、社交、播放、偏好分组，传入原函数对象，getter不包一层新回调；UI节点/手势/effect keys未改变。Kotlin PSI严格任务现0违规，正式接入checkLayering；工具报告成功不作为通过的替代。复用受影响的既有mapper/路由/状态用例，没有逐数据类堆重复测试或减弱预期。

## 2026-11-13 · 纯函数与容器拥有的主题

21个原对象按职责删除：纯规则/参数/mapper/协议改为具名顶层函数与不可变常量，路由动作显式接收Context，回退房间按页面创建。协议Packet仍保留内容相等与原压缩/分包限制；HTTP池与调试诊断的真实进程生命周期保留。没有改用companion或隐藏可变全局值来制造数字达标。

主题只读适配器由AppContainer持有，读取现有SettingsBindings的colorStyle，不再把设置值复制进ThemePalette.style。移除设置/引导中的冗余双写，原ARGB颜色与未初始化时AC粉回退保持；SnapshotStateObserver验证同源观察，两个独立适配器验证不串状态。
