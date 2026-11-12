# 响应映射职责审计

2026-11-13检查点。这里记录字段读取的归属，不把mapper单测当作页面/账号完整验收。

| 业务 | 当前落点 | 尚需收口 |
|---|---|---|
| 视频详情/常规推荐项 | VideoDetailMapper/FeedVideoMapper及视频DTO | app推荐专用结构仍在FeedApi |
| 播放流 | PlayInfoMapper/PlayInfoDto，真实匿名样本+6项测试 | VideoApi2字幕/VideoApi空降片段等附加读取 |
| 评论/楼中楼/动态评论/面板表情 | CommentItemMapper/CommentPageMapper/ReplyEmoteMapper，真实匿名样本+6项测试 | 真实写操作验收独立保留；上传pictures属于传输编码 |
| 消息feed/会话/私信/系统/粉丝/未读 | MessageFeedMapper/MessagePageMapper/MessageSessionMapper/PrivateMessageMapper/MessageSupplementMapper/MessageUnreadMapper | 账号收发/管理/ack装机验收独立保留；请求编码仍在传输适配器 |
| 动态正文/视频/直播卡片 | Dynamic*Mapper | DynamicApi的portal/up列表元信息 |
| 空间主资料 | SpaceInfoMapper及真实样本 | SpaceApi/SpaceApi2的合集/投稿/文章/番剧/小店/brief等 |
| 搜索 | SearchResponseMapper，匿名补全样本+3项回归 | 真实搜索/筛选装机验收独立保留；原HTML解码器显式输入 |
| 媒体库/关系 | LibraryHistory/Collection/RelationMapper，4项回归；云续播规则入domain并有4项测试 | 真实账号/批量操作装机验收独立保留；补资料/缓存/请求仍在infra |
| 账号 | AccountApi直接字段读取 | nav/QR响应业务模型映射；Cookie/签名仍为协议职责 |
| 直播 | Socket消息已有BiliLiveMessageDecoder | LiveApi房间/画质/表情/播放信息映射 |

HTTP错误信封、签名/Cookie、请求JSON和WebSocket协议帧位于infra是传输职责；本表关注API分片内混入的业务模型字段读取。不得以只消除`org.json`导入数量代替职责检查，也不得为通过检查创建空DTO壳或把HTTP操作搬到mapper。

其他API项目未审计通过前，本表不标全库映射完成。完整功能门槛仍见REFACTOR_COMPLETION_AUDIT.md；每批只运行必要验证。
