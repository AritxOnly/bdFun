# 真实匿名接口样本

2026-11-11 使用无 Cookie GET 采集，保留当次响应值，不对观看数等实时值作后续稳定性假设。

- video-detail-1.json: `/x/web-interface/view?bvid=BV1orW46aEba`（含 UGC 合集）。
- video-detail-2.json: `/x/web-interface/view?bvid=BV18cxv6SEe7`（无合集）。
- video-detail-3.json: `/x/web-interface/view?bvid=BV1ouxp6REjf`（无合集）。
- related.json: `/x/web-interface/archive/related?bvid=BV1orW46aEba`。

这些公开接口响应不含登录 Cookie 或 access_key。测试使用固定样本离线执行；没有凭空补字段。
新增 org.json:json 20250517 仅用于 JVM 测试，替代 Android 的不可执行 JSON stub；未增加生产依赖。相比引入 Robolectric，独立 JSON 实现足以验证这里的纯映射。

2026-11-13新增 `playurl-anonymous.json`：无Cookie/无access_key GET `/x/player/playurl?bvid=BV1orW46aEba&cid=43443290870&qn=127&fnval=4048&fnver=0&fourk=1`，使用公开浏览器UA/Referer。原始响应code=0，6个视频表示（id32/16各三种编码）、3个音频表示，timelength=98452；accept_quality包含64不代表响应实际提供64。原样保留匿名响应，播放URL签名自然过期，测试从不访问URL。测试中的空字段、snake_case、durl与非法accept元素属于显式构造的边界变体，不冒充额外真实响应。

2026-11-13新增 `comments-anonymous.json`：无Cookie GET `/x/v2/reply?type=1&oid=117584544403664&pn=1&ps=20&sort=2`，公开浏览器UA/Referer，原始响应code=0、67,942字节，3条根评论及一级preview。page.count/acount当次均为253；本次迁移保留旧page>=count语义，不把字段重新解释为页数。CommentMapperTest中置顶重复项、第四preview、空字段/表情/@人是显式边界变体，不冒充额外真实返回；固定样本离线运行。
