# Deadliner Dialog / Sheet migration (MODAL-01)

Source: local Deadliner working tree at `3b561656e1ddca43220a39763f12aaa0c67fdaba`.
The source tree contains uncommitted modal improvements; exact input hashes below identify the copied snapshot.

The full modal dependency closure is retained: retained dialog host, separate-window recorder,
material resolver, button roles, form roles, Scaffold, sheet sizing, overlay adapter, complete
MIUIX-derived gesture/IME/predictive-back implementation and container expansion motion.
The upstream Apache-2.0 notices in the sheet fork are retained. See `TeakUI/docs/CREDITS.md` and `THIRD_PARTY_NOTICES.md` for attribution; full MIT and Apache-2.0 licenses are retained under `TeakUI/third_party`.

Integration adaptations: package names and existing Teak material/chrome locals; bdFun's existing
material tuning values are preserved. Two Deadliner-only top-bar observer/press-host locals have
no Teak equivalent and their reset providers are omitted. Teak's three material progress/host
locals are reset as in the source. Unused Deadliner resource imports are removed. A single
zero-inset root MIUIX Scaffold hosts page sheets, outside the page backdrop recorder; dialogs
sample the whole window only from their separate window. No per-screen replacement theme.

The large upstream `MiuixSheetContentLayout.kt` is deliberately kept intact for source comparison
instead of splitting/reimplementing its coordinated gestures. New application code uses small
presentation components. Canonical SheetHeight, responsive corners and material selection are covered by JVM tests;
real-device rendering/IME/drag verification is listed in the PR (no emulator run).

| Source path (under app/src/main/java/com/aritxonly/deadliner) | SHA-256 |
| --- | --- |
| `ui/base/AlertDialog.kt` | `8ac8b8581d16ce7b536d034cb497133a4ed9786779fa6067bb726ca6504ba9bd` |
| `ui/base/RetainedMiuixDialogHost.kt` | `7d2f3816223888b0ba64d704f836fa041031640128d867336d4ba993760375d3` |
| `ui/base/AdvancedMaterialDialogBlur.kt` | `0d8cded9d69dea03d485445af19d6648d742623dc397dcfa699e1fc29ffe7b12` |
| `ui/base/MiuixBottomSheet.kt` | `0fa3d25cd8837a209289dcc39e5b2e2626052358eba3d28b0ca68b25c97e76c7` |
| `ui/base/MiuixModalSurface.kt` | `91b9b243a1c307c524c300f251af8f60fc2b9a717abe177022de088977eec3ed` |
| `ui/base/SheetHeight.kt` | `e8ed0125aa0a7063f51e1e8492bd759f26c6bf4aa6eaebb1e28629488e713f23` |
| `ui/base/WindowGlassBackdropHost.kt` | `d87734971e4077136e326d7183f0e06f7619e2c8c547b9372fe2df20177daa17` |
| `ui/base/DeadlinerMiuixDefaults.kt` | `ec89cdc6e3939cf93cbf8c1b4515891d4ff70633ec3a3b8cbc29c372c9b87db1` |
| `ui/base/Scaffold.kt` | `0547b81f76a36ed7b8733530cf3ac27c12149a309272607ad67b9731b66ba09d` |
| `ui/base/Button.kt` | `a90ae0fd01c557f377285e5c222b226549dcfe944457e3890e6c937b62e9178f` |
| `ui/base/FormContainerStyle.kt` | `a2efe8f1d71c470fdc52912f5309d5aabaff300cc6ac505a83d751d02a6cef86` |
| `ui/base/sheet/SheetExpansionMotion.kt` | `f98de502b563603d33ae5b4460ce1460382e184f902849ed70303a86457b3b88` |
| `ui/base/sheet/MiuixSheetContentLayout.kt` | `4b23df98a749dbb325795b2fd27c51a7ba5097afbbef9e257360e104b1180121` |
| `ui/base/sheet/ScaffoldSheetOverlay.kt` | `4b26a5b91600805b3a915585b5fd5903737ebcfe46dda1306ecb280775307ed9` |
| `ui/theme/TextFieldContainerColors.kt` | `935c79126bf911e12f2c64b23cd4c3f142b490872ec984de9c9cc943c7bcad08` |
| `model/MiuixModalMaterialTuning.kt` (resolver adapted to existing Teak types/constants) | `36b61f470db58752c6677a343f08a93256cce5362c85e1e8e71ebc38d54911c4` |
