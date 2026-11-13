# 依赖作用域与内存补丁审计

2026-11-13，P3-08/P4-06。范围为app生产源码（21个Activity，含main新增直播）与core/feature/infra；TeakUI的设计系统状态不充当业务容器。

## Activity依赖取得

21个入口均在onCreate调用`AppGraph.ensure(applicationContext)`，统一委托AppContainer；没有Activity的`lateinit var api`或Activity自己创建BiliBackend。仓储、设置和网络连接池按application共享，页面控制器按工厂创建，不把Activity/Compose scope存入容器。

| Activity | 页面资源与释放 |
|---|---|
| MainActivity | 壳内各页面composition；首页/动态为账号SessionResourceSlot，换账号关闭控制器并取消投影/动作任务 |
| VideoActivity | 每实例VideoPagePresentationState/原生输出/页面回调；lifecycleScope跟随Activity；读取缓存只有有限进程数据，页面句柄关闭/账号revision失效 |
| SpaceActivity | 每Activity ViewModel持有SessionResourceSlot；onCleared关闭；旋转保留本实例Store，另一Activity不共享控制器 |
| LiveRoomActivity | 每页面LiveRoomStateHolder、播放输出和WebSocket连接；离开取消scope并关闭连接/输出，拒收迟到结果 |
| DynamicDetailActivity | 每详情composition控制器及子任务，Dispose关闭 |
| MessageActivity | 每composition消息控制器；SessionIdentity变化重建，Dispose关闭/取消任务 |
| MessageDetailActivity | 每kind及SessionIdentity控制器，独立分页游标，Dispose关闭/取消任务 |
| PrivateChatActivity | 每会话及SessionIdentity控制器与子scope；图片选择回调检查原controller归属；Dispose关闭 |
| RelationActivity | 每mid/fans/order/SessionIdentity控制器；Dispose关闭和取消scope |
| LibraryActivity、HistoryActivity、WatchLaterActivity | 每页面LibraryStateHolder与动作子scope；容器只共享明确的播放队列/页面入口事件注册器；账号identity检查拒收迟到结果 |
| CacheActivity | 页面展示订阅application下载队列；任务需跨页面/Service存活，不属于Activity状态共享 |
| LoginActivity | 每页面扫码控制器/轮询scope；离开取消，成功结果进入统一SessionStateHolder |
| SettingsActivity | 设置持久化及StateFlow属于application；页面临时弹窗/选择状态由本composition持有 |
| MySentActivity | 每页面读取持久化发送记录，列表/选择为页面状态；存储使用applicationContext |
| TermsActivity | 每页面入门流程控制器/临时状态；设置写入统一仓储 |
| ChangelogActivity、HelpActivity、DonationActivity、CreditsActivity | 静态内容与页面chrome，复用application设置，不持有跨Activity页面状态 |

消息列表、消息详情、私信、关系控制器的工厂现捕获SessionIdentity并注入即时有效性检查；即使Compose尚未重组，旧账号请求也不能发布新结果、头像姓名补全、后续私信发送或成功提示。账号A→B→A由revision区分；同账号资料刷新不无故重建。已有关闭/取消测试保留，新增4项只验证这个通知时序缺口。

## 原内存补丁去向

| 原名称/现资源 | 所有者及边界 |
|---|---|
| HomeFeedMemory | 已删除；账号资源槽持有正式HomeFeedState，换账号关闭 |
| SpaceCache | 已删除；Activity ViewModel及账号资源槽，页面销毁释放 |
| RelatedMemory/VideoCache | 已删除旧入口；VideoReadCache推荐最多12、视频最多6；按UID+revision隔离；关闭页面句柄不允许迟到访问 |
| MineProfileCache | MineProfileStore，单账号单条、原5分钟TTL；换账号清理 |
| EmoteCache | EmoteStore，只替换当前接口返回的一份快照，不跨请求追加；换账号clear推进代数，旧响应失效 |
| InlineVideoCache | 旧缓存及无调用HTTP代理已删除；每内联视频状态/输出随composition释放 |
| VideoLinkTitleStore | 容器持有4096项LRU；clear推进代数拒收旧响应 |
| CommentLikeStore | 容器持有300项快照；账号clear及写令牌检查 |
| MessageUserCache/videoStatCache/messageVideoCache/SponsorSegmentCache | BiliApiContext实例持有，分别200/300/64/100项；仅公开姓名头像/视频计数封面/广告分段，不保存账号关系或Activity |
| 动态分类缓存 | 正式账号控制器内部的分类/作者缓存，按原行为保留；不是静态Map，账号slot关闭后整实例释放 |
| 评论乐观回滚、富文本inline、请求参数/JSON mapper临时Map | 页面动作或单次函数内资源，不是静态缓存；不借重构改变原事务/文本/请求行为 |
| 设置属性投影与待提交浮点值 | SettingsBindings/控制器实例持有，键为已注册设置项；关闭注销或flush，不是无限用户数据缓存 |
| 下载队列/播放进度/发送历史 | 正式持久化业务记录，使用storage端口，不以删用户记录来满足内存缓存门槛 |

生产源码中旧六类Memory/Cache名称无残余；未发现无上限可变静态Map。剩余7个具名object生命周期见GLOBAL_OBJECT_AUDIT。此表是源码作用域结论，不冒充所有Activity的进程回收或账号装机验收。

### 2026-11-13 播放器展示状态物理迁移

VideoPlayerUiStateHolder/PlayerObservedStateHolder移入feature:video，创建点与remember键、Dispose/资源释放点未变。VideoOutput、ForegroundMediaSession、PlaybackAudioEffect、Job等引用仍属原播放器组合；迁移不把对象加入AppContainer或静态缓存。PlayerSurfaceSlots只传原DanmakuLayer组合，位置getter继续在读取时计算frameMs-btLatencyMs，不缓存首帧位置或页面状态。

### 2026-11-13 生命周期与页面端口

PlayerPageHost由原VideoPagePresentationState直接实现，四项控制状态仍委托原StateFlow；暂停钩子仍由同一PlaybackPauseRegistry按原Activity owner登记/注销，没有额外状态或进程引用。后台/自动播放/音效设置getter保持原事件读取时机。新增PlayerDiagnostics为页面组合内日志/Toast接线，不进入进程缓存。

### 2026-11-13 挂接协议与退出进度

ForegroundPlaybackAttachment仅抽出原attach/detach/fullscreen与Actions契约，旧token身份比较和状态变更体一致。PlayerExitHooks在页面组合remember(context,page)，adopt捕获当前Context并转发原后台实现；没有把Activity放入核心层或进程缓存。进度aid/cid/弹幕位置继续读写原页面投影，媒体会话仍按原title/author键创建释放，旧session释放只清理自己。正常退出保存后force-stop再重入已验证持久化读取，不代表未保存时杀进程、服务重建或所有退出组合都已验证。

### 2026-11-13 根绘制与手势会话

PlayerRenderPorts为当前组合装配，不加入AppContainer/静态缓存。手势callback及开关继续rememberUpdatedState，pointerInput(Unit)、PlayerTapStateHolder与coroutineScope仍属于原Modifier组合；识别会话与4项测试体逐字保留。发送器以getter取最新page.danmakuSender，图片/全屏资源仍在原条件分支调用槽，未提前冻结为初始值。

### 2026-11-13 入口状态创建归属

VideoPlayerEntry的UiState、playback、tracking仍沿用原无键remember，观察/事件Effect的原键和调用顺序保持；只是创建位置从app进入实际视频模块。PlayerEntryPorts的工厂/default值在原remember执行时读取，回调不存进程缓存；蓝牙观察继续由原rememberBluetoothAudioConnected管理。正常Back后当前debug媒体会话消失已实测，不代表所有异常销毁或服务交接已验收。

### 2026-11-13 视频详情入口迁移

VideoDetailEntry仍保留原bvid控制器/选择/元数据remember键和页面ui/description作用域，scope仍由同一页面rememberCoroutineScope持有。动作在原键下创建并捕获当时app平台host，onBack保持rememberUpdatedState；host只引用原Context和同一页面，不入进程容器。VideoDetailDanmakuHost由该页面直接实现，没有镜像state/sender；原DisposableEffect随aid/cid释放。app布局/跨feature槽不持久保存参数对象，VideoDetailLayoutContent仅传本次组合输入。

### 2026-11-13 视频Activity页面壳迁移

VideoActivityPageStateHolder仍在同一个Activity composition的remember创建，包含原consumedAutoNext/comments/danmakuInputOpen；没有改为进程状态或增加保存恢复。entry仍remember无键，初始化snapshot和解析Effect(Unit)保持；queue在原bvid与autoNext计数键下执行。OverlayHost由同一页面直接实现，panelBackdrop保持原mutableState引用；可组合槽仅提供原控件和本次布局，不入容器。

### 2026-11-13 后台持有与UI投影

进程容器仍lazy创建唯一BackgroundPlaybackStateHolder；它是AndroidBackgroundPlayback的Compose投影子类，原session/processScope实例、UNDISPATCHED收集及每个sync的同步更新保持。原token/actions/detachedPlayer/detachedScope、单线程teardownExecutor全部由infra实现持有，adopt仍新建原Main.immediate SupervisorJob并500ms刷新，停止仍先清UI/主线程pause再后台取消/释放/停服务。settings getter返回原SettingsStore投影实例；服务保持原组件及动作值。未改变容器close语义或额外启动服务。
