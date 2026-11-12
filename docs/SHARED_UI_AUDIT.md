# 重复 UI 清单与下沉决定

2026-11-13，P3-07。按production源码中的Avatar/Hint/Loading/Empty/Error/日期格式化及页面骨架调用点清点；先记录下列边界，再继续搬迁。目标`core:ui`仍指现有TeakUI，不新增模块，不统一原本不同的样式。

| 项目与调用点 | 决定 | 原因与行为约束 |
|---|---|---|
| TeakActivityScaffold/TeakPageScaffold；设置、消息、关系、历史、收藏等标题页 | 页面骨架已下沉TeakUI；新增TeakScrollContainer统一滚动视口 | 宽屏留白与内容为兄弟节点，共用原列表状态；内容不再套第二个scrollable。宽度、padding、标题及原列表均保持 |
| SearchLanding/SearchResultList、TermsScreen普通Column | 同一TeakScrollContainer | 搜索自带顶栏；条款使用ScrollState，不能另建LazyColumn改变测量/滚动语义 |
| PrivateChatRoot反向消息列表 | 复用容器，BindTeakPageScrollState只绑定状态 | 外侧方向读取原列表reverseLayout；固定顶栏不接入折叠观察；解绑只清理自己拥有的state |
| SpaceContentPager及空间折叠头 | 保留宽度modifier与原nestedScroll | 外层驱动头部，内层列表驱动正文，属于不同状态和职责，不是重复安装同一列表手势 |
| 播放面板、弹幕预览、日志文本窗口 | 保留独立滚动 | 固定高度局部视口，与页面滚动内容不同，不能把局部内容滚动交给全页 |
| TeakAvatar、MessageAvatar、ChatAvatar、CommentRowAvatar | 保留各自组合；图片请求共用rememberImageRequest | 分别有36dp细边框+未登录图标、44dp空占位、32dp文字占位、32dp无涟漪点击；强行统一会改变modifier顺序、语义或视觉；TeakUI不引入Coil/账号导航 |
| DynamicAvatarCard/AuthorAvatar/AllDynamicAvatar、SpaceProfileAvatar、LiveAvatar | 保留业务展示 | 动态更新标记/选中缩放、空间头像装扮、直播身份头部均不是同一种头像；不能下沉模型、关注和导航逻辑进TeakUI |
| EmptyHint、SpaceEmpty、SentHistoryEmpty、PrivateChatLoadError | 保留原布局 | 动态是全页22sp标题+操作；空间是60dp边距13sp单行；发送历史卡片与私信重试语义不同。复用页面骨架而不抹平布局 |
| TeakLoading、TeakTvLoading、SpaceLoadingRow、PrivateChatLoading | 保留现有行为 | 普通加载刻意为空实现；小电视动画只用于信息流下拉，其他wrapper保留各自占位边距。将空实现换spinner会违反main视觉基准 |
| TeakTopBar/TeakLargeTitle/TeakTopGlassButton、TeakPagerTabs等 | 已下沉TeakUI，页面继续复用 | tab不改组件、尺寸或动画，播放器/空间/设置保留原样 |
| formatCount/formatDuration/formatDate/formatDateTime等 | 已进入core:common；ui/Format仅兼容转发 | 纯格式化不属于设计系统；无Android或Compose依赖 |
| formatSessionTime/formatChatTime、全屏时钟、发送记录日期、日志时间 | 不合并 | 会话含昨天/同年、聊天含时间分隔、时钟只HH:mm、记录使用原Locale、日志含毫秒，各自时间单位/边界不同；统一会改文案 |
| PlayerSettingGlyphs与播放器专用控制按钮 | 保留播放器组合 | 尺寸、点击热区与原生播放动作绑定；通用图标目录仍由TeakUI提供，不搬业务动作 |

结论：本轮真正共同的宽屏滚动机制已经下沉；其余相似外观但不同规格的组合保留，并以上述差异作为回归约束。没有为凑复用数量新增泛型页面框架。
