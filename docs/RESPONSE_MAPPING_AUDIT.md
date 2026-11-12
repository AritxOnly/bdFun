# 响应映射职责审计

2026-11-13检查点。这里记录字段读取的归属，不把mapper单测当作页面/账号完整验收。

| 业务 | 当前落点 | 尚需收口 |
|---|---|---|
| 视频详情/常规及App推荐项 | VideoDetailMapper/FeedVideoMapper/AppRecommendationMapper及视频DTO | 真实App/Web回退和账号行为验收独立保留 |
| 播放流/字幕/空降/弹幕转换 | PlayInfoMapper/VideoSupplementMapper/VideoDanmakuMapper及DTO | 真实字幕/分段/XML回退/空降开关和最终release回归独立保留；引擎生产代码未改 |
| 评论/楼中楼/动态评论/面板表情 | CommentItemMapper/CommentPageMapper/ReplyEmoteMapper，真实匿名样本+6项测试 | 真实写操作验收独立保留；上传pictures属于传输编码 |
| 消息feed/会话/私信/系统/粉丝/未读 | MessageFeedMapper/MessagePageMapper/MessageSessionMapper/PrivateMessageMapper/MessageSupplementMapper/MessageUnreadMapper | 账号收发/管理/ack装机验收独立保留；请求编码仍在传输适配器 |
| 动态正文/视频/直播卡片及page/portal/up元信息 | Dynamic*Mapper，入口新增2项回归 | 实际登录动态/入口装机验收独立保留 |
| 空间资料及正文/合集/brief | SpaceInfo/Video/Collection/Content/SupplementMapper，主资料真实样本，正文新增5项回归/原5项获赞回退 | 真实分类/分页/小店装机验收独立保留；原签名/聚合/回退仍在infra |
| 搜索 | SearchResponseMapper，匿名补全样本+3项回归 | 真实搜索/筛选装机验收独立保留；原HTML解码器显式输入 |
| 媒体库/关系 | LibraryHistory/Collection/RelationMapper，4项回归；云续播规则入domain并有4项测试 | 真实账号/批量操作装机验收独立保留；补资料/缓存/请求仍在infra |
| 账号资料/QR非成功状态 | AccountResponseMapper，原扫码仓储回归通过 | 成功saveTvLogin→nav编排、Cookie/WBI/设备指纹保留协议层；真实扫码验收独立保留 |
| 直播 | BiliLiveMessageDecoder/LiveResponseMapper | plain→signed/WBI重试仍在API，真实直播/旋转/弹幕验收独立保留 |

HTTP错误信封、签名/Cookie、请求JSON和WebSocket协议帧位于infra是传输职责；本表关注API分片内混入的业务模型字段读取。不得以只消除`org.json`导入数量代替职责检查，也不得为通过检查创建空DTO壳或把HTTP操作搬到mapper。

本表中的业务读模型字段已迁入mapper；这不等于全量功能验收通过。API剩余字段读取逐项归类：AccountApi/BiliApiContext的Cookie、access_key、buvid、WBI是认证协议；各API的code/message/data是错误信封；CommentApi的rpid与上传image_url→pictures是发送回执/请求编码。它们不进入presentation/domain。云续播字段、渐进式fallback和CDN测速字段复查后也归mapper。完整功能门槛仍见REFACTOR_COMPLETION_AUDIT.md；最终还要实际依赖/作用域复核、必要完整测试和release装机。
