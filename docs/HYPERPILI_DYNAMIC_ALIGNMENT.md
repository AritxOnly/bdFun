# Hyper-PiliPlus 动态流对齐

参考 [AritxOnly/Hyper-PiliPlus](https://github.com/AritxOnly/Hyper-PiliPlus/tree/0d08dd25bdeaa9a55e5a0e333154b681fd8048fe)，核对日期 2026-11-11。按其服务端接口契约和动态页行为实现 Compose 版本，保留 bdFun / Deadliner 的主题、玻璃材质、刷新和自适应外壳。

## 数据与交互

- `lib/http/dynamics.dart`：分类与单 UP 统一使用 `/x/polymer/web-dynamic/v1/feed/all`。分类传 `type`，UP 传 `host_mid`，两者互斥；分页传服务端 `offset`，不再固定 `page=1`，不再用空间接口替代 UP 关注流。
- `lib/common/constants.dart`：动态 feed 的 `features` 与参考版本一致。
- `lib/pages/dynamics/controller.dart`、`lib/models/dynamics/up.dart`：首次加载/刷新请求 `/portal?up_list_more=1&web_location=333.1365`，UP 列表分页请求 `/uplist`，传 `offset/platform/web_location`。保留接口排序、更新标记和直播列表，不再从已加载动态中推断作者或按本地已读记录重排。
- `lib/models/common/dynamic/dynamics_type.dart`：全部、投稿、番剧、专栏、UP。分类/UP 分别缓存列表和游标；选择头像进入对应 UP，再点当前头像刷新，刷新作用于当前分类/UP。“我”入口使用当前账号。
- `lib/pages/dynamics/widgets/live_rcmd_panel.dart`、`lib/models/dynamics/result.dart`：渲染 `major.live_rcmd` 和 `subscription_new.live_rcmd` 的封面、标题、直播状态、分区与观看提示，保留服务端 `module_tag.text`。bdFun 尚无内置直播播放器，点击打开对应 B 站直播间链接。
- 不自行产生、替换或重排推荐内容；显示服务端在当前账号、分类和游标下返回的动态流。

## 状态与界面

- 删除本地每次截断 15 条的队列，按服务端分页展示和去重；重复/空游标终止分页，空内容页最多连续补取 5 页，仍可手动加载更多。
- 请求代次保护防止旧分类、旧 UP 或旧分页覆盖新结果；取消异常向上传递；刷新失败保留原内容，重试保留原请求类型；切账号重建动态状态。
- 宽屏左侧滚动 UP 列表、右侧限宽动态；窄屏横向头像列表。保留已有 Deadliner 玻璃分类栏和 APP 材质参数。
- 消息页“回复与@”使用 MIUIX `MiuixIcons.Messages`（与主页消息入口一致）；“新增粉丝”使用 `MiuixIcons.Contacts`。

## 验证

Debug / Release 构建和 23 项单元测试通过，其中 10 项新增覆盖接口参数、分类缓存/游标、UP 刷新、竞态、去重、空页、失败重试与 portal 排序。此次模拟器视觉验证尚未完成：Mac 锁定且模拟器安装/截图请求超时。未使用真实账号验证个性化动态和直播推荐内容，后续需登录后检查服务端实际返回结果。
