# 全局 object 审计

> 2026-11-12：P4-05 尚未完成。当前26个具名 object，目标仍为 <8，不能只排除纯工具或debug类型来制造达标。

统计范围为 app 的 com/aritxonly/bdfun 与 core/infra/feature 生产 Kotlin；包含无状态 helper、协议常量和 debug 代码里的具名 object。按与此前31个相同的口径，不计 companion、sealed data object、匿名 object 表达式、TeakUI/内联供应商片段、测试和生成源码。这是声明清点，不是完整 Kotlin AST 或线程安全证明。

本批删除 RelatedMemory/VideoCache 两个静态缓存，改为 AppContainer 持有、账号 generation 与页面 close 约束的 VideoReadCache；另删除全仓无调用的 app/data/HttpClients 薄代理，infra 共享 HTTP 客户端保持不变。本次又删除 BiliLink 代理，主入口按 Activity 生命周期创建 IncomingVideoLinkStateHolder，输出 Route.Video，并通过 domain/infra 解析端口接线。本批删除 DanmakuSettings 全局代理；视频/播放器通过页面提供的核心设置契约读写，实际观察与持久化仍由同一个逐字段投影/控制器承担。

| 对象 | 当前状态/生命周期 | 收尾要求 | 位置 |
|---|---|---|---|
| AppGraph | 进程装配入口，持有 AppContainer | 保留候选；仍需移出展示中的隐式获取，关闭/重建应使旧依赖失效 | [app/src/main/kotlin/com/aritxonly/bdfun/AppGraph.kt](../app/src/main/kotlin/com/aritxonly/bdfun/AppGraph.kt) |
| TeakNavigator | Activity导航、异步入口与提示；调用进程依赖 | 改由组装壳注入路由动作，避免 feature 互相/隐式依赖 | [app/src/main/kotlin/com/aritxonly/bdfun/TeakNavigator.kt](../app/src/main/kotlin/com/aritxonly/bdfun/TeakNavigator.kt) |
| TeakOneTake | 进程反射 Method/resolved 缓存；原窗口动画 | 明确进程生命周期保留候选；不得改变原窗口放大动画 | [app/src/main/kotlin/com/aritxonly/bdfun/TeakOneTake.kt](../app/src/main/kotlin/com/aritxonly/bdfun/TeakOneTake.kt) |
| FreezeWatchdog | debug 主线程心跳、队列、handler、文件、帧状态 | debug进程生命周期；保留限流/只抓主线程/禁止release启动 | [app/src/main/kotlin/com/aritxonly/bdfun/data/FreezeWatchdog.kt](../app/src/main/kotlin/com/aritxonly/bdfun/data/FreezeWatchdog.kt) |
| LiveCounters | debug 进程实例计数，多个展示仍直接读写 | debug生命周期明确；诊断端口注入仍待完成 | [app/src/main/kotlin/com/aritxonly/bdfun/data/FreezeWatchdog.kt](../app/src/main/kotlin/com/aritxonly/bdfun/data/FreezeWatchdog.kt) |
| LogCollector | debug 进程日志、写入队列/线程、文件、崩溃处理 | debug进程生命周期；保留容量/落盘上限，展示只应使用诊断端口 | [app/src/main/kotlin/com/aritxonly/bdfun/data/LogCollector.kt](../app/src/main/kotlin/com/aritxonly/bdfun/data/LogCollector.kt) |
| RouteIntents | Intent/Route 编解码工具，无页面状态 | 与路由壳一并收口；保留缺参数明确错误和系统返回栈 | [app/src/main/kotlin/com/aritxonly/bdfun/navigation/RouteIntents.kt](../app/src/main/kotlin/com/aritxonly/bdfun/navigation/RouteIntents.kt) |
| ThemePalette | Compose style 可变全局；决定主题颜色 | 改为主题/设置生命周期内显式投影；保持启动时赋值和所有窗口颜色 | [app/src/main/kotlin/com/aritxonly/bdfun/presentation/shell/AppTheme.kt](../app/src/main/kotlin/com/aritxonly/bdfun/presentation/shell/AppTheme.kt) |
| TeakIcon | 资源映射，无页面业务可变状态 | 无状态保留候选；不改图标资源、尺寸或来源 | [app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakIcons.kt](../app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakIcons.kt) |
| PlaybackSnapshotReducer | 纯播放快照归约 | 纯函数/策略依赖收口候选；保留 ready/ended/位置语义 | [core/application/src/main/kotlin/com/aritxonly/bdfun/core/application/PlaybackSnapshotReducer.kt](../core/application/src/main/kotlin/com/aritxonly/bdfun/core/application/PlaybackSnapshotReducer.kt) |
| VideoLinkText | 富文本URL识别工具，无缓存 | 纯函数收口候选；保留匹配优先级/边界 | [core/application/src/main/kotlin/com/aritxonly/bdfun/core/application/VideoLinkText.kt](../core/application/src/main/kotlin/com/aritxonly/bdfun/core/application/VideoLinkText.kt) |
| DynamicFeedContract | 动态接口参数/类型/文案只读契约 | 协议/模型值收口候选；不得更改原参数 | [core/domain/src/main/kotlin/com/aritxonly/bdfun/core/domain/DynamicFeedSource.kt](../core/domain/src/main/kotlin/com/aritxonly/bdfun/core/domain/DynamicFeedSource.kt) |
| PlaybackPolicy | 纯编码/音轨选择 | 纯函数/策略依赖收口候选；保留回退、排序及单测 | [core/domain/src/main/kotlin/com/aritxonly/bdfun/core/domain/PlaybackPolicy.kt](../core/domain/src/main/kotlin/com/aritxonly/bdfun/core/domain/PlaybackPolicy.kt) |
| PlaybackProgressPolicy | 纯进度CSV/保护区/淘汰规则 | 纯函数/策略依赖收口候选；保留3000/5000/500→400边界 | [core/domain/src/main/kotlin/com/aritxonly/bdfun/core/domain/PlaybackProgressRepository.kt](../core/domain/src/main/kotlin/com/aritxonly/bdfun/core/domain/PlaybackProgressRepository.kt) |
| PlaybackHistoryPolicy | 纯历史上报节流策略 | 纯函数/策略依赖收口候选；保留首次3秒/之后15秒/结束补报 | [core/domain/src/main/kotlin/com/aritxonly/bdfun/core/domain/PlaybackTrackingRepository.kt](../core/domain/src/main/kotlin/com/aritxonly/bdfun/core/domain/PlaybackTrackingRepository.kt) |
| RefreshRatePolicy | 纯刷新率决策 | 纯函数/策略依赖收口候选；主窗口60Hz约束不得删 | [core/domain/src/main/kotlin/com/aritxonly/bdfun/core/domain/RefreshRatePolicy.kt](../core/domain/src/main/kotlin/com/aritxonly/bdfun/core/domain/RefreshRatePolicy.kt) |
| SponsorSegmentPolicy | 纯片段筛选/跳过决策 | 纯函数/策略依赖收口候选；规则与单测保持 | [core/domain/src/main/kotlin/com/aritxonly/bdfun/core/domain/SponsorSegmentPolicy.kt](../core/domain/src/main/kotlin/com/aritxonly/bdfun/core/domain/SponsorSegmentPolicy.kt) |
| VideoLinkPolicy | 只读Regex与纯链接识别 | 纯函数/解析依赖收口候选；保留裸BV/大小写/短链规则 | [core/domain/src/main/kotlin/com/aritxonly/bdfun/core/domain/VideoLinkPolicy.kt](../core/domain/src/main/kotlin/com/aritxonly/bdfun/core/domain/VideoLinkPolicy.kt) |
| VideoPagePolicy | 纯视频页策略 | 纯函数/策略依赖收口候选；原生命周期/退出条件保持 | [core/domain/src/main/kotlin/com/aritxonly/bdfun/core/domain/VideoPagePolicy.kt](../core/domain/src/main/kotlin/com/aritxonly/bdfun/core/domain/VideoPagePolicy.kt) |
| ScrollTopPages | 页面键/文案的只读集合 | 模型值收口候选；原存储键/顺序/文案不变 | [core/model/src/main/kotlin/com/aritxonly/bdfun/core/model/settings/ScrollTopPages.kt](../core/model/src/main/kotlin/com/aritxonly/bdfun/core/model/settings/ScrollTopPages.kt) |
| SettingsDefaults | AUTO_CDN_ID 常量 | 模型常量收口候选；默认值仍唯一来源 | [core/model/src/main/kotlin/com/aritxonly/bdfun/core/model/settings/SettingsDefaults.kt](../core/model/src/main/kotlin/com/aritxonly/bdfun/core/model/settings/SettingsDefaults.kt) |
| BiliParameters | 协议常量、签名/图片/文本等工具，无账号可变状态 | 按职责收口为纯函数/协议依赖；禁止改字段含义及旧优先级 | [infra/bili/src/main/kotlin/com/aritxonly/bdfun/infra/bili/BiliParameters.kt](../infra/bili/src/main/kotlin/com/aritxonly/bdfun/infra/bili/BiliParameters.kt) |
| HttpClients | OkHttp单连接池/dispatcher，进程共享 | 进程基础设施保留候选；不得按页面创建重复线程/连接池 | [infra/bili/src/main/kotlin/com/aritxonly/bdfun/infra/bili/HttpClients.kt](../infra/bili/src/main/kotlin/com/aritxonly/bdfun/infra/bili/HttpClients.kt) |
| HttpRetryPolicy | 无状态重试决策 | 纯函数/策略依赖收口候选；保留次数、条件、退避 | [infra/bili/src/main/kotlin/com/aritxonly/bdfun/infra/bili/HttpRetryPolicy.kt](../infra/bili/src/main/kotlin/com/aritxonly/bdfun/infra/bili/HttpRetryPolicy.kt) |
| FeedVideoMapper | JSON→模型，无缓存 | 纯mapper收口候选；保留真实样本/缺字段测试 | [infra/bili/src/main/kotlin/com/aritxonly/bdfun/infra/bili/mapper/FeedVideoMapper.kt](../infra/bili/src/main/kotlin/com/aritxonly/bdfun/infra/bili/mapper/FeedVideoMapper.kt) |
| VideoDetailMapper | JSON→模型，无缓存 | 纯mapper收口候选；保留真实样本/缺字段测试 | [infra/bili/src/main/kotlin/com/aritxonly/bdfun/infra/bili/mapper/VideoDetailMapper.kt](../infra/bili/src/main/kotlin/com/aritxonly/bdfun/infra/bili/mapper/VideoDetailMapper.kt) |

减少声明数量必须伴随职责收口与依赖接线，不用改成 companion 或排除目录躲过统计。无状态工具本身不等于泄漏；有状态对象必须证明生命周期/账号隔离/迟到回调，符合最终数量和依赖验收后才能勾 P4-05。

后续同步核对 AppContainer Activity/页面/进程作用域、真实 Gradle 模块图和全部调用点；不要把本清点代替这些验收。
