# Kotlin语法树尺寸审计

> 2026-11-12：尺寸门槛尚未通过。以下为真实PSI节点报告，不再以旧正则扫描的“具名块函数无超标”作为完成证据。

使用开发工具 `:tools:kotlin-audit`（Kotlin 2.4.21 PSI），扫描910个自有生产Kotlin文件，包含debug；共9519个可调用/函数类型节点，语法解析错误0。规则仍为文件≤400行、函数体/lambda/init≤80行、声明/构造/函数类型输入参数≤12。17项JVM正反例覆盖边界、表达式、主次构造、匿名函数、lambda、访问器、初始化块、字符串插值代码、函数类型、context参数、CRLF和语法失败。

完整机器报告：`build/reports/kotlin-audit/report.json`。严格任务 `./gradlew :tools:kotlin-audit:checkKotlinSizes` 必须为非零；只供定位的report任务为0，不是验收通过。严格任务尚未并入checkLayering，待清零后完成最终接线。

开发依赖理由：重用已锁定Kotlin版本的compiler-embeddable提供PSI，替代无法可靠处理表达式、构造和字符串插值的正则；手工括号扫描无法证明完整语法覆盖。该依赖只属于工具模块，实际app debugRuntimeClasspath及当前APK确认不含PSI/审计器，不引入Android运行时框架或编译器版本变化。

当前17条违规为5个体积节点与12个长参数构造器；同一物理函数及其内部lambda可能分别违规，不把节点数误称独立文件数。

| 文件 | 行 | 节点 | 当前 | 限制 |
|---|---:|---|---:|---:|
| [LogActivity.kt](../app/src/debug/kotlin/com/aritxonly/bdfun/LogActivity.kt#L72) | 72 | `body-lines` / `function:LogScreen` | 143 | 80 |
| [LogActivity.kt](../app/src/debug/kotlin/com/aritxonly/bdfun/LogActivity.kt#L90) | 90 | `body-lines` / `lambda:<anonymous>` | 124 | 80 |
| [VideoInteractionStateHolder.kt](../app/src/main/kotlin/com/aritxonly/bdfun/feature/video/application/VideoInteractionStateHolder.kt#L12) | 12 | `parameters` / `primary-constructor:VideoInteractionState` | 16 | 12 |
| [FullscreenPlayerModels.kt](../app/src/main/kotlin/com/aritxonly/bdfun/ui/player/fullscreen/FullscreenPlayerModels.kt#L24) | 24 | `parameters` / `primary-constructor:FullscreenPlayerInfo` | 23 | 12 |
| [FullscreenPlayerModels.kt](../app/src/main/kotlin/com/aritxonly/bdfun/ui/player/fullscreen/FullscreenPlayerModels.kt#L50) | 50 | `parameters` / `primary-constructor:FullscreenPlaybackState` | 19 | 12 |
| [FullscreenPlayerModels.kt](../app/src/main/kotlin/com/aritxonly/bdfun/ui/player/fullscreen/FullscreenPlayerModels.kt#L72) | 72 | `parameters` / `primary-constructor:FullscreenActions` | 31 | 12 |
| [VideoPageStateHolder.kt](../core/application/src/main/kotlin/com/aritxonly/bdfun/core/application/VideoPageStateHolder.kt#L7) | 7 | `parameters` / `primary-constructor:VideoPageState` | 24 | 12 |
| [CommentItem.kt](../core/model/src/main/kotlin/com/aritxonly/bdfun/core/model/comment/CommentItem.kt#L3) | 3 | `parameters` / `primary-constructor:CommentItem` | 21 | 12 |
| [DynamicItem.kt](../core/model/src/main/kotlin/com/aritxonly/bdfun/core/model/feed/DynamicItem.kt#L5) | 5 | `parameters` / `primary-constructor:DynamicItem` | 24 | 12 |
| [MessageItem.kt](../core/model/src/main/kotlin/com/aritxonly/bdfun/core/model/message/MessageItem.kt#L3) | 3 | `parameters` / `primary-constructor:MessageItem` | 29 | 12 |
| [MessageSession.kt](../core/model/src/main/kotlin/com/aritxonly/bdfun/core/model/message/MessageSession.kt#L4) | 4 | `parameters` / `primary-constructor:MessageSession` | 14 | 12 |
| [UserProfile.kt](../core/model/src/main/kotlin/com/aritxonly/bdfun/core/model/user/UserProfile.kt#L3) | 3 | `parameters` / `primary-constructor:UserProfile` | 23 | 12 |
| [VideoDetail.kt](../core/model/src/main/kotlin/com/aritxonly/bdfun/core/model/video/VideoDetail.kt#L3) | 3 | `parameters` / `primary-constructor:VideoDetail` | 27 | 12 |
| [VideoItem.kt](../core/model/src/main/kotlin/com/aritxonly/bdfun/core/model/video/VideoItem.kt#L3) | 3 | `parameters` / `primary-constructor:VideoItem` | 19 | 12 |
| [MessageApi.kt](../infra/bili/src/main/kotlin/com/aritxonly/bdfun/infra/bili/MessageApi.kt#L69) | 69 | `body-lines` / `function:messagesPage` | 170 | 80 |
| [MessageApi.kt](../infra/bili/src/main/kotlin/com/aritxonly/bdfun/infra/bili/MessageApi.kt#L69) | 69 | `body-lines` / `lambda:<anonymous>` | 170 | 80 |
| [MessageApi.kt](../infra/bili/src/main/kotlin/com/aritxonly/bdfun/infra/bili/MessageApi.kt#L89) | 89 | `body-lines` / `lambda:<anonymous>` | 138 | 80 |

2026-11-12空间资料mapper检查点消除了space()的119行函数/嵌套lambda两项；此前基线为909文件/9511节点/19条，当前以机器报告为准。没有放宽阈值或排除构造器。

范围排除沿用原重构边界：测试、生成源码、TeakUI/弹幕引擎及内联供应商源码；没有因超标临时排除数据类、构造器或debug日志页。分组构造参数时仍须保留全部原字段、默认值、copy/相等/存储协议与UI行为；不能直接删字段或放宽阈值。此处不是P3/P4/P5或全量验收完成声明。
