# 重构验证记录

## 自动验证

本轮使用 JDK 21，Miuix 固定到 `45645854cd46c32275c5666e3aafa9980d8f104e`。准备源码后运行：

```bash
python3 tools/setup-miuix.py # 已准备则无需重复运行
python3 tools/check-layering.py
bash ./gradlew :infra:bili:testDebugUnitTest :core:common:test :app:testDebugUnitTest :app:assembleDebug --no-parallel --max-workers=2
bash ./gradlew :app:assembleRelease --no-parallel --max-workers=2
```

- App：45 项通过，0 失败。
- core:common：3 项通过，覆盖格式化边界和跨年。
- infra:bili：3 项通过，覆盖只对 GET 重试、永久错误、退避抖动边界。
- debug APK：构建通过。
- 核心层依赖检查：通过；临时 Android import 反例验证非零退出。
- release：固定源码后串行构建通过，lintVital 与 R8 全部通过（4m 8s）。
- `apksigner verify --verbose --print-certs`：APK v2 签名校验通过，使用用户指定的项目默认 Android Debug 证书。
- APK：`app/build/outputs/apk/release/app-release.apk`。
- APK SHA-256：`d4b8993518b2b732237824341be04205289c3080720202bec7740e4c55b023b6`。

## 手动验证清单（未执行）

`adb devices -l` 未检测到连接设备。本清单不能用自动单测结果替代：

- [ ] 冷启动首页、刷新与翻页、进入视频再返回保持列表。
- [ ] 扫码登录、退出、重启保持登录。
- [ ] 视频进入播放、切清晰度、切分 P、全屏旋转。
- [ ] 画中画、后台播放、通知栏与耳机按键、退出再进恢复进度。
- [ ] 评论、收藏、投币等写操作不重复提交。
- [ ] 消息页各 tab、私信、系统消息删除。
- [ ] 空间页小店请求、空间投稿与合集。
- [ ] 断网、取消页面加载、恢复网络。
- [ ] 冷启动链接跳转、解析过程中退出 Activity。

## 交付条件

当前是 P0 与 HTTP 执行层抽离的阶段结果，不等同于全量重构完成。release 验证产物不作为全量完成版发送飞书；全量任务、回归与发布说明完成后，再按用户授权交付。

## 合入 main 后的请求与签名迁移验证

- main 基线：`671d264`；合并提交：`e63e590`。阶段收尾 fetch 无新增提交。
- 使用仓库 `.local/` 自动发现的 Miuix 快照，无需临时 MIUIX_SOURCE 路径。
- `checkLayering :core:common:test :infra:bili:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug --no-parallel --max-workers=2 --offline`：通过。
- App 45 项、common 3 项、infra:bili 14 项，共 62 项，0 失败。
- Pixel_9 只读临时实例：安装成功，MainActivity COLD 启动成功；首页推荐加载真实数据，视频详情与起播成功；个人主页头部和滚动小标题可见；crash 缓冲为空。临时实例已关闭，未保存原 AVD 快照。
- 截图：`build/reports/refactor/main-home.png`、`main-video.png`、`main-space.png`、`main-space-scrolled.png`（构建目录，不提交）。
- 上述验证没有覆盖切清晰度、切分 P、横屏、系统画中画、后台控制、扫码登录或完整消息流程。此前的 release 产物不对应本阶段源码，最终交付前须重新构建签名 release。

## 设置升级验证

- infra:storage 的 JSON 编码、格式错误回退测试通过。
- 81 项可写设置逐项默认值及往返测试通过；4 项迁移/范围/浮点合并/搜索历史测试通过。App 全量单测合计 116 项通过；设置覆盖协议的筛选重跑也通过。
- 临时 Pixel_9 AVD：安装迁移前 APK，写入全部旧偏好键，冷启动记录落盘基线；覆盖安装新 APK，冷启动成功。81 个设置键 + 搜索历史共 82 项比对，0 缺失、0 变化，crash 缓冲为空。
- 结果文件：build/reports/refactor/settings-upgrade-result.json（构建目录，不提交偏好值）。
- debug 构建通过。该检查点不代表尚未完成的页面状态和播放器迁移已经验收。
