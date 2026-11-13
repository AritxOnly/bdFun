package com.aritxonly.bdfun

internal fun androidx.compose.foundation.lazy.LazyListScope.spaceSubmissionItems(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    spaceSubmissionChips(content, actions)
    when (content.ui.submissionMode) {
        1 -> spaceArticleItems(content, actions)
        3 -> spaceCollectionItems(content, actions)
        2 -> spaceCollectionVideoItems(content, actions)
        else -> spaceVideoItems(content, actions)
    }
}
