package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import org.json.JSONObject

internal fun mapSpaceArticles(data: JSONObject): List<SpaceArticle> =
    data.optJSONArray("articles").objects().mapNotNull { article ->
        val id = article.l("id")
        if (id <= 0L) return@mapNotNull null
        val covers = article.optJSONArray("covers")
        val cover = when {
            covers != null && covers.length() > 0 -> covers.optString(0)
            else -> article.s("cover")
        }
        SpaceArticle(
            id = id,
            title = article.s("title"),
            cover = image(cover, 480),
            view = article.optJSONObject("stats")?.l("view")
                ?: article.l("view"),
            publishTime = article.l("publish_time"),
        )
    }

internal fun mapSpaceBangumi(data: JSONObject): List<SpaceBangumi> =
    data.optJSONArray("list").objects().mapNotNull { item ->
        val seasonId = item.l("season_id")
        val title = item.s("title")
        if (seasonId <= 0L || title.isBlank()) return@mapNotNull null
        SpaceBangumi(
            seasonId = seasonId,
            title = title,
            cover = image(item.s("cover"), 480),
            progress = item.s("progress"),
            total = item.optInt("total_count", 0),
        )
    }

internal fun mapSpaceShop(data: JSONObject): List<SpaceShopItem> =
    data.optJSONArray("data").objects().mapNotNull { item ->
        val title = item.s("title")
        if (title.isBlank()) return@mapNotNull null
        val coverObj = item.optJSONObject("cover")
        val priceObj = item.optJSONObject("netPrice")
        val price = if (priceObj != null) {
            buildString {
                priceObj.s("pricePrefix").takeIf { it.isNotBlank() }?.let {
                    append(it); append(' ')
                }
                append(priceObj.s("priceSymbol"))
                append(priceObj.s("netPrice"))
            }
        } else {
            ""
        }
        val labels = item.optJSONArray("belowLabels").objects()
            .mapNotNull { it.s("title").takeIf(String::isNotBlank) }
            .joinToString("|")
        SpaceShopItem(
            title = title,
            cover = image(coverObj?.s("url").orEmpty(), 480),
            price = price,
            source = labels.ifBlank { item.s("itemSourceName") },
            url = item.s("cardUrl"),
        )
    }
