# 全局 object 审计

> 2026-11-13：按原口径从28个降为7个，数量目标<8已达到；全量作用域审计与最终验收仍独立执行。

范围保持app的com/aritxonly/bdfun（含debug）与core/infra/feature生产Kotlin；包括无状态工具，不计companion、sealed data object、匿名表达式、TeakUI/内联供应商、测试和生成源码。没有移动代码到排除目录、改成companion或新建隐藏可变单例来降低数字。

| 保留对象 | 生命周期与理由 | 位置 |
|---|---|---|
| AppGraph | 进程装配入口，只委托AppContainer；不保存pending导航槽或Activity | app/AppGraph.kt |
| TeakOneTake | 进程级反射Method/resolved缓存，不持有Activity；保留原窗口动画 | app/TeakOneTake.kt |
| TeakIcon | 只读资源ID目录，没有业务/账号状态 | app/ui/TeakIcons.kt |
| LiveCounters | debug进程的三个实例计数，固定体积 | app/data/FreezeWatchdog.kt |
| FreezeWatchdog | debug进程心跳/文件/队列；release不启动，文件上限2MB | app/data/FreezeWatchdog.kt |
| LogCollector | debug进程日志，内存3000行/文件2MB；release不初始化与写文件 | app/data/LogCollector.kt |
| HttpClients | 进程共享OkHttp连接池/dispatcher；账号Cookie仍由具体BiliApiContext客户端提供 | infra/bili/HttpClients.kt |

已收口：八个领域规则/请求契约、PlaybackSnapshotReducer、VideoLinkText、SettingsDefaults、ScrollTopPages均为纯函数/不可变常量；BiliParameters、HttpRetryPolicy、FeedVideoMapper、VideoDetailMapper与直播协议改为包内函数/常量/Packet值对象。算法、默认值、字段、压缩限制和重试语义保持，旧命名空间删除。

RouteIntents改为createRouteIntent/readRouteIntent，每次创建无状态codec；TeakNavigator改为接收Context的顶层路由动作，不存放Context。LiveMock的未用随机消息样本删除，唯一使用的main房间回退由页面工厂创建。ThemePalette的第二份可变全局style删除；AppContainer持有AppThemePalette适配器，读取同一SettingsBindings.colorStyle，进程重建/独立容器不共享另一份主题状态。

不可变Regex、协议参数Map与Color常量可以在文件中共享，但不能借此保存可变账号/页面状态。P4-06仍需核对全部页面工厂、账号revision与缓存；数量达标不代表所有作用域和视觉/播放验收完成。
