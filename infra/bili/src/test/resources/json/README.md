# 真实匿名接口样本

2026-11-11 使用无 Cookie GET 采集，保留当次响应值，不对观看数等实时值作后续稳定性假设。

- video-detail-1.json: `/x/web-interface/view?bvid=BV1orW46aEba`（含 UGC 合集）。
- video-detail-2.json: `/x/web-interface/view?bvid=BV18cxv6SEe7`（无合集）。
- video-detail-3.json: `/x/web-interface/view?bvid=BV1ouxp6REjf`（无合集）。
- related.json: `/x/web-interface/archive/related?bvid=BV1orW46aEba`。

这些公开接口响应不含登录 Cookie 或 access_key。测试使用固定样本离线执行；没有凭空补字段。
新增 org.json:json 20250517 仅用于 JVM 测试，替代 Android 的不可执行 JSON stub；未增加生产依赖。相比引入 Robolectric，独立 JSON 实现足以验证这里的纯映射。
