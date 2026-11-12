# 模块化收益复盘

2026-11-13，P5-05。结论：继续业务分层与当前模块边界，暂不批量创建其余物理feature模块。当前证据支持依赖隔离与可维护性，不能据此声称冷构建更快或APK更小。

| 指标 | 已有记录 | 解读 |
|---|---|---|
| main 671d264定向对照debug构建 | 2m17s；176 tasks，44执行/41缓存/91up-to-date；`/tmp/bdfun-main-baseline-build.log` | 非clean冷构建，不能当作冷基线 |
| 当前视频模块首次接线独立模块+app debug | 3m03s；313 tasks，167执行/38缓存/108up-to-date；`/tmp/bdfun-video-module.log` | 包含32项定向测试与架构工具，不与上行直接作速度比值 |
| 当前滚动绑定增量debug+架构 | 28s；285 tasks，27执行/258up-to-date；`/tmp/bdfun-scroll-bind.log` | 是实际增量观测；任务/缓存条件不同，不代表同条件收益 |
| 当前账号隔离定向测试+debug+架构 | 36s；308 tasks，25执行/283up-to-date；`/tmp/bdfun-message-owner.log` | 48项必要测试；没有重复全量app测试 |
| 上一直播阶段release | 9m54s；`/tmp/bdfun-live-release.log` | 历史阶段构建，不是重构前clean基线 |
| 当前默认release含R8/lint | 5m08s；499 tasks，233执行/74缓存/192up-to-date；`/tmp/bdfun-final-release.log` | 缓存与源码均不同，不能把耗时差全部归因于模块化 |
| release APK | 直播阶段13,028,097 bytes；当前13,044,481 bytes；增加16,384 bytes（约0.126%） | 分组值对象、模块接线及守卫修复后的真实产物；没有体积收益，不将正常构建差异夸大为回归/优化 |
| 严格结构门槛 | 954个自有生产文件、10443个PSI节点、0违规；具名object 7个 | 文件≤400行、函数≤80行、参数≤12，未通过改阈值/新增排除来清零 |
| CI | workflow含JDK21、SDK37.0、固定Miuix、架构/模块图/尺寸、debug、必要纯逻辑测试及真实违规依赖负例 | 首次环境接线遇到legacy tools、SDK包名和PATH问题，按实际日志修复；远端Actions 43269829285全部成功，6m42s（含首次环境/依赖准备），详见验证文档 |

没有同环境、同任务、同缓存状态的重构前后冷/热配对数据。按用户“不做冗杂测试”的要求，本次不为性能报告额外清缓存反复构建；保留实际任务统计和该限制，不编造冷构建数字。远端CI首轮还包含全新依赖下载，与本地热构建不可比较。

收益是可核验的结构边界：infra:bili/storage/playback不依赖app；home/video业务状态可独立编译测试；core无Android/网络/JSON；feature之间及feature→infra依赖被真实Gradle图拒绝；编译器审计工具和测试依赖不进入app生产图。app仍包含原TeakUI渲染和原生装配的兼容接线，物理模块不等于所有界面已完成像素/行为验收。

下一步优先完成兼容接线的最终清理与视频生命周期/同数据页面验收。若继续抽更多物理feature，应在独立任务中先建立同条件基线，再衡量配置成本、边界变化和增量重编译范围；本次不以模块数量作为收益。
