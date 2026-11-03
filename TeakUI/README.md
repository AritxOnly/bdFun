# TeakUI

Gradle：

```kotlin
include(":TeakUI")
```

```kotlin
implementation(project(":TeakUI"))
```

包名：`com.aritxonly.teak.common`

---

## 文件对应表

### chrome：页面外壳与顶部交互

| 文件 | 作用 |
| --- | --- |
| `TeakPageScaffold.kt` | 页面外壳：背景、玻璃采样层、固定顶栏、大标题、返回键定位，以及所有位置常量 `TeakChromeDefaults` |
| `TeakPageChrome.kt` | 页面滚动状态：大标题折叠进度、返回键是否露出；提供 LazyColumn / LazyGrid / ScrollState 的滚动跟踪 |
| `TeakTopOverlay.kt` | 顶部整层：返回键 + 左侧内容 + 中间内容 + 右侧操作，所有玻璃按钮共享渐显 |
| `TeakGlassBackButton.kt` | 单独的玻璃返回键：顶部只显示箭头，下滑后 300ms 淡入玻璃底 |
| `TeakProgressiveTopBar.kt` | 顶栏渐变模糊 + 小标题的现成实现 |
| `TeakGlass.kt` | 玻璃图标颜色等小工具 |
| `TeakManualProgress.kt` | 手动补间进度，统一动画开关 |
| `TeakMotion.kt` | 全局动效开关 |
| `TeakContinuousCornerShape.kt` | 连续曲率圆角 |

### material：玻璃材质

| 文件 | 作用 |
| --- | --- |
| `GlassMaterialSpec.kt` | 玻璃材质参数定义：模糊、染色、折射、阴影等 |
| `GlassMaterialModifier.kt` | 把材质参数应用到 Compose Modifier |
| `TeakGlassSurface.kt` | 玻璃表面容器 |
| `TeakGlassPanel.kt` | 玻璃面板 |
| `TeakMaterial.kt` | `advancedTextureBlur` 等基础扩展 |
| `TeakGlassShadow.kt` | 玻璃阴影 |
| `TeakGlassPressFeedback.kt` | 玻璃按钮按压反馈 |
| `GlassRefractionShader.kt` | 边缘折射/色散 shader |
| `TeakMaterialFineTuning.kt` | 材质细节调参 |
| `TeakMaterialLevel.kt` / `TeakMaterialTuning.kt` / `TeakModalMaterialTuning.kt` | 材质层级和弹窗材质 |

### navigation：底栏与 Tab

| 文件 | 作用 |
| --- | --- |
| `TeakFloatingTabBar.kt` | 首页悬浮胶囊底栏、布局、默认尺寸 |
| `TeakFloatingTabMotion.kt` | 选中块弹簧、拉伸、按压、拖拽动画 |
| `TeakFloatingNavigationShadow.kt` | 底栏阴影 |
| `TeakPagerTabs.kt` | 视频页/二级页的玻璃 Tab：玻璃底座、滑动指示器、弹簧参数 |

### settings：设置页规范

| 文件 | 作用 |
| --- | --- |
| `TeakSettings.kt` | 标准设置页 `TeakSettingsPage`，以及卡片、开关行、下拉行、值行 |

---

## 新 App 怎么接

TeakUI 不绑定 bdFun 的主题色，但要求宿主 App 提供：

1. `MiuixTheme` 的 `ThemeController` 或 color scheme；
2. `MaterialTheme` 的颜色；
3. `LocalTeakMaterialSpec`，决定玻璃是否开启、模糊/折射参数；
4. 页面内容通过 `TeakPageScaffold` 建立 backdrop。

bdFun 里的 `TeakActivityScaffold` 是“主题 + TeakPageScaffold”的适配层，属于 App 自己的主题代码，不在 TeakUI 里。新 App 可以照着写一个自己的 `ActivityScaffold`。

推荐接入顺序：

```text
TeakActivityScaffold（宿主主题适配）
  └─ TeakPageScaffold（页面外壳 / backdrop / 顶栏 / 返回键）
      ├─ TeakTopOverlay（返回键 + 顶部操作）
      ├─ TeakLargeTitle（内容大标题）
      └─ 页面内容
```

底栏：

```text
TeakFloatingTabBar + TeakFloatingTabMotion + TeakFloatingNavigationShadow
```

视频/二级页 Tab：

```text
TeakPagerTabs
```

---

## 规范文档

- [页面 Chrome 规范](docs/PAGE_CHROME_SPEC.md)
- [逐文件来源清单](docs/PROVENANCE.md)
- [来源与致谢](docs/CREDITS.md)

---

## 来源说明

- HyperPiliPlus：有直接移植（玻璃材质参数、折射、阴影、底栏弹簧、ProgressiveTopBar 等），也有同源重构（玻璃表面、底栏、材质入口）；逐文件见 `docs/PROVENANCE.md`。
- HyperIsland：参考渐进式模糊的部分参数/观感；`TeakProgressiveTopBar` 来自 HyperPiliPlus，不是 HyperIsland。
- Miuix：第三方依赖，提供 backdrop / textureBlur / progressiveTextureBlur；没有复制源码。
- Teak 自己：页面外壳、顶部 overlay、设置页、PagerTabs 组件代码、TeakTheme 等。

详细记录见 `docs/PROVENANCE.md` 和 `docs/CREDITS.md`。
