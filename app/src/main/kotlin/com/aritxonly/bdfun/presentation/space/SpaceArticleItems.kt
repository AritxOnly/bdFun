package com.aritxonly.bdfun

import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import com.aritxonly.bdfun.ui.showToast

internal fun androidx.compose.foundation.lazy.LazyListScope.spaceArticleItems(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val context = actions.context
    val articles = content.snapshot.articles.items
    val articleLoading = content.snapshot.articles.loading
    val tabEnterModifier = Modifier
    items(articles, key = { "article-${it.id}" }) { article ->
        SpaceArticleRow(
            article = article,
            modifier = tabEnterModifier,
            onClick = {
                runCatching {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(
                                "https://www.bilibili.com/read/cv${article.id}",
                            ),
                        ),
                    )
                }.onFailure { context.showToast("无法打开图文") }
            },
        )
    }
    if (articleLoading) {
        item(key = "article-loading") { SpaceLoadingRow() }
    }
}
