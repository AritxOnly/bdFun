# MY-01 我的页布局与预览大卡

采用个人空间头图与叠层头像；不足600dp时头图从页面/状态栏顶部通栏铺开，不套卡片、不留外边距。资料卡左右内缩14dp，上压头图30dp，76dp头像再上压24dp。600dp起按原型使用左侧名片/右侧功能双栏。原四个快捷入口保留，补齐常用功能区标题。设置入口仅位于头图右上角，复用AdaptiveMiuixTopAppBar/TeakGlassIconButton，不在页尾重复。头图绘制/解码/Referer复用空间页现有组件与工具。

设置 → 我的页新增「展开历史记录」「展开我的收藏」「展开稍后再看」，默认关闭，立即生效且持久保存。页面内不提供展开/收起操作。该设备各账号共用布局偏好，内容请求按账号隔离。

历史/待看各预览两条视频，收藏预览两个收藏夹；点击收藏夹进入现有收藏页，查看全部进入对应完整页面。历史正数进度按原HistoryItem秒单位续播，已完成的负值不传续播位置。加载/空/错误重试均有展示；重新进入前台刷新，关闭或切号拒收旧响应。

分层：MineLayoutPreferences为领域存储端口，AndroidMineLayoutPreferences保存独立mine_layout文件，core/application/MineLayoutStateHolder提供共享不可变StateFlow。MinePreviewStateHolder复用LibraryRepository，不在UI请求API。头图从已对齐的空间响应读取，失败使用原资料/默认头图。

验证：新增布局偏好重建恢复、按需加载、失败重试、两项截断、账号替换、关闭配置后的迟到响应、取消解锁、续播位置等JVM测试。运行app全部单测、core/application单测、assembleDebug及checkLayering。

登录账号手动验收待办：个人头图/头像/等级/会员与统计；三个开关分别启停以及重启恢复；真实历史续播；收藏完整页；待看播放；空数据与断网重试；横竖屏及大字体；登录/退出/换号后不残留旧内容。
