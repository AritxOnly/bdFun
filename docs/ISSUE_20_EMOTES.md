# Issue #20：表情裁切与面板补齐

## 原因与修改

表情面板调用 `biliImage(url, 120)`，该通用函数实际生成视频封面的
`@120w_67h_1c.webp`，服务端已经把表情裁成 16:9。现在保留原图 URL，
仅规范协议；显示仍采用 Fit，完整保留表情内容。

对照 PiliPlus 主线 `lib/pages/emote/view.dart`（2026-11-13 获取）及
本地 Hyper-PiliPlus `0d08dd25bdeaa9a55e5a0e333154b681fd8048fe` 的
Package / Emote / Meta 契约，补齐表情包切换、type=4 文字表情、
meta.size 大表情网格和长按原图预览。插入时文字表情保留字面文本，
图片表情仅保留一层括号；正文取消 12 字符表情名限制。

未新增依赖、未修改 TeakUI、未改变其他图片的裁切规则。

## 验证

- core:application:test、infra:bili:testDebugUnitTest、app:testDebugUnitTest 通过。
- app:assembleDebug、tools/check-layering.py、git diff --check 通过。
- 新增 mapper 合成契约样本，覆盖图片 URL、分组、大小、文字表情与空包。
- 匿名真实 `/x/emote/user/panel/web?business=reply` 返回 code=0、packages=null，
  因此合成样本没有冒充登录接口的真实完整返回。
- 旧测试曾断言 URL 必须包含裁切参数、长名称不匹配和双层括号；这些断言
  对应本次修复的缺陷，已改为原图 URL、支持长名称及单层括号。

## 待装机验证

当前 adb devices 无设备，以下检查尚未完成：

- 登录后在评论、楼中楼、私信等既有表情面板入口切换多个表情包。
- 检查第一行/最后一行表情完整性、大小表情及文字表情的显示和插入。
- 长按图片表情预览并点击外部关闭；窄屏、横屏与大字体检查。
- 发布带长名称表情的评论，确认正文与服务端返回显示完整。

参考：https://github.com/bggRGjQaUbCoE/PiliPlus/blob/main/lib/pages/emote/view.dart

## 最新 main 与紧凑面板修订

已合入 main 的 `dc25332`。表情包切换栏移入玻璃容器内部；总高度固定
220dp（包含 40dp 切换栏），内容区内部滚动，切换包不再改变面板高度。
切换栏图标固定 20dp、文字 12sp；表情图片限制 28/36dp，行高 40/48dp，
文字表情固定 16sp、单行省略，避免列宽或内容撑大显示。

## 未下载表情包的评论解析

对照本地 Hyper-PiliPlus `0d08dd25bdeaa9a55e5a0e333154b681fd8048fe`
的 `lib/pages/video/reply/widgets/reply_item_grpc.dart`：以评论自带表情字典
构建转义后的完整名称匹配，不要求个人表情面板下载该包。现有字典输入保留，
匹配规则改为完整名称、长名称优先，允许名称内层括号及正则特殊字符；链接优先级
和旧带/不带外括号的字典兼容保持不变。

2026-11-13 匿名真实请求 `/x/v2/reply?type=1&oid=170001&pn=1&ps=20&sort=2`
成功；提取单条评论的 rpid/content.message/content.emote 为
`infra/bili/src/test/resources/json/comment-emotes-anonymous.json`，不保存其他资料。
真实样本覆盖无个人表情包的三种评论表情 URL；特殊名称另以合成用例覆盖。
core:application:test、infra:bili:testDebugUnitTest、app:assembleDebug 和分层检查通过。
尚待反馈具体未显示的表情名称/评论，以确认现场问题与这次修复完全对应。
