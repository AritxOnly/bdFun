# TeakUI 来源清单

更新时间：2026-11-02

这份清单按**文件**区分来源，避免把“直接移植”“同源重构”“参数参考”“自己新写”混在一起。

判定方法：

- 与 HyperPiliPlus 的 `com.aritxonly.deadliner` Kotlin 层逐文件比对；
- 去除注释和空白后计算代码相似度，再结合功能判断；
- 相似度只作辅助，不是法律结论。

## A. 直接移植 / 基本同源

这些文件和 HyperPiliPlus 的同功能文件高度一致，主要是改包名、改名、少量接口适配。

| TeakUI 文件 | HyperPiliPlus 对应文件 |
| --- | --- |
| `material/GlassMaterialSpec.kt` | `ui/material/glass/GlassMaterialSpec.kt` |
| `material/GlassRefractionShader.kt` | `ui/material/glass/GlassRefractionShader.kt` |
| `material/TeakGlassPressFeedback.kt` | `ui/material/glass/SoftGlassPressFeedback.kt` |
| `material/TeakGlassShadow.kt` | `ui/material/glass/SoftGlassShadow.kt` |
| `material/TeakMaterialFineTuning.kt` | `model/AdvancedMaterialFineTuning.kt` |
| `material/TeakMaterialLevel.kt` | `model/AdvancedMaterialLevel.kt` |
| `material/TeakMaterialTuning.kt` | `model/AdvancedMaterialTuning.kt` |
| `material/TeakModalMaterialTuning.kt` | `model/MiuixModalMaterialTuning.kt` |
| `navigation/TeakFloatingTabMotion.kt` | `ui/navigation/FloatingTabMotion.kt` |
| `chrome/TeakProgressiveTopBar.kt` | `ui/material/glass/ProgressiveTopBarMaterial.kt` |

## B. 同源，但已经过 Teak 重构 / 改名

这些不是原样复制，功能对应，但结构或接口已经改过。

| TeakUI 文件 | HyperPiliPlus 对应文件 | 说明 |
| --- | --- | --- |
| `material/GlassMaterialModifier.kt` | `GlassMaterialModifier.kt` | 适配 Teak 材质接口 |
| `material/TeakGlassSurface.kt` | `SoftGlassSurface.kt` | 改为 Teak 的玻璃表面封装 |
| `material/TeakMaterial.kt` | `ui/theme/AdvancedMaterial.kt` | 改为 Teak 材质入口 |
| `navigation/TeakFloatingTabBar.kt` | `MiuixFloatingTabBar.kt` | 改名、接入 Teak 玻璃，布局有调整 |
| `navigation/TeakFloatingNavigationShadow.kt` | `FloatingNavigationShadow.kt` | 改名、适配 Teak |

## C. Teak 自己新写 / 重新组合

这些没有可直接对应的 HyperPiliPlus 同名文件，代码是 Teak 这一层新写的。
其中有些使用了 HyperPiliPlus 的玻璃 recipe 或视觉参数，但组件本身不是复制。

| TeakUI 文件 | 说明 |
| --- | --- |
| `chrome/TeakPageScaffold.kt` | Teak 页面外壳、位置常量、顶栏/大标题/返回键组合 |
| `chrome/TeakPageChrome.kt` | Teak 的折叠/返回键状态与滚动跟踪 |
| `chrome/TeakTopOverlay.kt` | Teak 顶部 overlay，本次新写 |
| `chrome/TeakGlassBackButton.kt` | 按钮组件新写；玻璃背景/recipe 直接复用 HyperPiliPlus 材质 |
| `chrome/TeakManualProgress.kt` | Teak 的手动补间 |
| `chrome/TeakMotion.kt` | Teak 的全局动效开关 |
| `chrome/TeakContinuousCornerShape.kt` | Teak 连续曲率圆角 |
| `chrome/TeakGlass.kt` | Teak 玻璃图标颜色工具 |
| `material/TeakGlassPanel.kt` | Teak 玻璃面板封装 |
| `settings/TeakSettings.kt` | Teak 设置页规范 |
| `navigation/TeakPagerTabs.kt` | 早期 App 内实现，后移入 TeakUI；视觉/配方来自 HyperPiliPlus 体系，代码为新组合 |
| `theme/TeakTheme.kt` | 本次新写的主题适配层，不属于 HyperPiliPlus |

## D. Miuix

**没有复制 Miuix 的源码进 TeakUI。** Miuix 是第三方依赖，提供：

- `LayerBackdrop` / `rememberLayerBackdrop`
- `textureBlur`
- `progressiveTextureBlur`
- `ProgressiveBlur`
- `MiuixTheme` / `ThemeController`
- 底层 RuntimeShader / RenderEffect 封装

使用这些 API 的 TeakUI 文件：

- `chrome/TeakPageScaffold.kt`
- `chrome/TeakProgressiveTopBar.kt`
- `material/TeakGlassSurface.kt`
- `material/GlassMaterialModifier.kt`
- `navigation/TeakFloatingTabBar.kt`
- `navigation/TeakPagerTabs.kt`

## E. HyperIsland

**没有复制 HyperIsland 的源码进 TeakUI。** 只参考了渐进式模糊的部分观感/参数：

- `curve = 1.25`
- `blurRadius = 16`
- `tint = 0.66`
- 顶栏渐进模糊的分层方式

注意：

- `chrome/TeakProgressiveTopBar.kt` 是 **HyperPiliPlus 的直接移植**，不是 HyperIsland；
- `TeakPageScaffold` 里的 `teakProgressiveBarBlur` 是 Teak 自己的 Miuix 封装；
- 具体 `startFraction` 等参数是项目自己调过的，不应笼统说“全部来自 HyperIsland”。

## 一句话结论

- **HyperPiliPlus**：玻璃材质核心、底栏/Tab 动画、ProgressiveTopBar 等，有直接移植，也有同源重构；
- **Miuix**：模糊能力本身，第三方依赖，没有复制源码；
- **HyperIsland**：只参考渐进模糊的部分参数/观感，没有复制源码；
- **Teak 自己**：页面外壳、顶部 overlay、设置页、PagerTabs、主题适配层等。
