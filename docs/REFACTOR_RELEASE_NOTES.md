# bdFun 全量重构候选包 · 2026-11-13

生产提交：2b5cd245d6ccab60824194e57cd78b93c2af6b6c；已包含main 4c4e03b（新增直播）。
PR：https://github.com/QxTeak/bdFun/pull/33（草稿，待验收）。

## 迁移内容

- P0–P5实现已接入：core业务模型/仓储/纯策略/跨屏状态/路由，infra网络/存储/播放，独立home与video模块，其余页面业务状态按application/presentation拆分。
- 视频详情、播放器、评论及直播的状态与展示拆分；全局Bus、隐式pending参数和无界页面内存补丁已替换。原生播放、后台服务和媒体会话归infra:playback；app负责平台与跨业务组装。
- 设置保留旧格式/默认值，五组StateFlow接线；历史升级逐项比对82项一致。业务读模型响应映射归infra mapper，保留原协议、字段语义和请求时序。
- 使用原TeakUI设计。设置等标题页、搜索和条款页统一TeakScrollContainer：左右留白与内容共享滚动状态，去掉同一次手势经过两层滚动容器的问题；保留原布局、宽度和padding。

## 验证

- 当前源码1089项现有测试全部通过，无失败/错误/跳过；debug/release及R8、架构/实际模块依赖、严格尺寸检查通过。1031生产文件/10709函数等节点，0尺寸违规；具名全局object按相同口径28→7。
- 当前提交远端CI通过：https://github.com/AritxOnly/bdFun/actions/runs/43326151184 。
- 当前release装机、冷启动、真实推荐/视频起播、全屏/弹幕/更多面板、真实PiP及系统展开继续播放通过，无崩溃。前序已验证后台交接与系统暂停/播放/停止、退出后冷进程恢复进度、详情Tab位置及宽屏滚动。
- 按约定，全站同数据UI/完整行为留待后续人工验收；未进行真实点赞/关注/投币/发送等写操作，不能将单测或局部装机视为这些流程通过。耳机/服务销毁重建、账号切换等完整组合亦保留在验收清单。

## 安装包

- 文件：bdFun-refactor-2b5cd24-release.apk
- 包名：com.aritxonly.bdfun，版本preview-5.0(500)，minSdk33 / targetSdk37。
- 按用户要求使用默认签名：本机无正式keystore，项目默认回退Android Debug证书，APK v2验签成功。
- SHA256：2aadf2aa1b01117b38d4e6ff2e0e5c4044fe499fbd62fcc6cbfab49c7f02197f

回滚：main 4c4e03b为迁移基线，当前生产提交及逐步提交保留。签名不同的已安装正式包不能直接覆盖本候选包；不要为安装删除用户数据。当前PR保持草稿，合并和全面验收完成另行确认。

逐项证据见docs/REFACTOR_COMPLETION_AUDIT.md、REFACTOR_VALIDATION.md、RESPONSE_MAPPING_AUDIT.md及SHARED_UI_AUDIT.md。
