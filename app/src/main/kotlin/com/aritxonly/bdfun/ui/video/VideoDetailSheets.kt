package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun VideoDetailSheets(
    ui: VideoDetailUiStateHolder,
    interaction: com.aritxonly.bdfun.feature.video.application.VideoInteractionState,
    actions: VideoDetailViewActions,
) {
    TeakActionSheet(
        visible = ui.coinSheet.value,
        onDismiss = { ui.coinSheet.value = false },
        title = "请选择投币数量",
    ) {
        SheetActionRow("投 1 枚硬币", MenuGlyphKind.Coin, MiuixTheme.colorScheme.onBackground) {
            actions.doCoin(1)
        }
        SheetActionRow("投 2 枚硬币", MenuGlyphKind.Coin, MiuixTheme.colorScheme.onBackground) {
            actions.doCoin(2)
        }
    }

    com.aritxonly.bdfun.feature.library.presentation.FavoriteFolderSheet(
        show = ui.favSheet.value,
        state = com.aritxonly.bdfun.feature.library.presentation.FavoriteFolderSheetUiState(
            title = "收藏到", description = "可选择多个收藏夹，点选后立即保存",
            folders = interaction.folders.map { folder ->
                com.aritxonly.bdfun.feature.library.presentation.FavoriteFolderOption(
                    folder.id, folder.title, folder.count, folder.id in interaction.folderIds,
                )
            },
            loading = interaction.favoritesLoading, pendingFolderId = interaction.pendingFolder,
        ),
        onDismiss = { ui.favSheet.value = false },
        onSelect = actions::toggleFolder,
    )
}
