# 公开空间响应样本

2026-11-12 从 emulator-5580 的 debug 客户端，使用现有 BiliRequestGateway 的签名、Cookie/Buvid准备与GET请求，读取公开UP `2283296`。未执行关注、消息或其它账号写操作。

- `space-info-2283296.json`：`/x/space/wbi/acc/info`，原 `mid/web_location/dm_img_*` 参数、WBI签名，code=0。
- `space-stat-2283296.json`：`/x/relation/stat?vmid=2283296`，code=0。
- `space-nav-2283296.json`：`/x/space/navnum?mid=2283296`，code=0。
- `space-upstat-2283296.json`：`/x/space/upstat?mid=2283296`，code=0；本次data为空对象，不推测其中不存在的字段。
- `space-app-2283296.json`：`app.bilibili.com/x/v2/space`，沿用原8430300/App签名/APP_HEADERS/APP_UA参数，code=0。仅摘录原root元字段与data.card.vip、data.elec，以及data.images被旧mapper读取的imgUrl/night_imgurl和第一个result的cover、item.image/animation的default_image/location/height；删除无关分支（含带临时签名的播放URL），没有改动保留字段的值。

原请求返回经现有网关解析为JSONObject，再格式化保存；除App无关分支裁剪外，未生成或改写响应字段。样本不含请求头、Cookie、access_key、csrf或签名参数。采样Activity/manifest仅临时使用，最终源码/DEX须确认已移除，设备临时缓存已清理。

网页vlist请求抛出BiliException，未取得成功样本；动态请求实际code=-352。二者不作为真实成功响应样本，也不据此证明接口应返回空列表。测试中额外边界变体为明确的结构探针，不将变体标为真实请求。
