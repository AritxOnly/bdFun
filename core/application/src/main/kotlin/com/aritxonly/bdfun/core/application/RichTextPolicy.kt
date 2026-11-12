package com.aritxonly.bdfun.core.application

internal val EmoteToken = Regex("\\[([^\\]]{1,12})]")

data class RichTextHit(
    val start: Int, val end: Int, val emoteToken: String? = null, val emoteUrl: String? = null,
    val mentionName: String? = null, val mentionMid: Long = 0L, val linkUrl: String? = null, val video: Boolean = false,
)
data class RichTextAnalysis(
    val hits: List<RichTextHit>, val hasEmote: Boolean, val hasMention: Boolean, val hasUrl: Boolean,
    val mentionList: List<Map.Entry<String, Long>>,
) {
    fun plainText(trailing: Boolean) = !hasEmote && !hasMention && !hasUrl && !trailing
}

/** 优先级、去重与首次 indexOf 均保留原逻辑；这里只决定区间，不依赖 Compose。 */
fun analyzeRichText(text: String, emotes: Map<String, String>, mentions: Map<String, Long>, mentionClick: Boolean): RichTextAnalysis {
    val mentionList = mentions.entries.sortedByDescending { it.key.length }
    val mentionRegex = if (mentionList.isEmpty()) null
        else Regex("@(" + mentionList.joinToString("|") { Regex.escape(it.key) } + ")")
    val hits = collectRichLinks(text).toMutableList()
    addRichEmotes(hits, text, emotes)
    if (mentionRegex != null && mentionClick) addRichMentions(hits, text, mentionRegex, mentions)
    return RichTextAnalysis(hits.sortedBy { it.start }, emotes.isNotEmpty() && EmoteToken.containsMatchIn(text),
        mentionClick && mentionRegex?.containsMatchIn(text) == true,
        VideoLinkText.urls.containsMatchIn(text) || VideoLinkText.tokens(text).isNotEmpty(), mentionList)
}

fun collectRichLinks(text: String): List<RichTextHit> {
    val hits = mutableListOf<RichTextHit>()
    for (match in VideoLinkText.urls.findAll(text)) {
        val url = VideoLinkText.trim(match.value)
        if (url.length <= "https://".length) continue
        hits += RichTextHit(match.range.first, match.range.first + url.length, linkUrl = url)
    }
    for (token in VideoLinkText.tokens(text)) {
        val start = text.indexOf(token)
        if (start < 0) continue
        val end = start + token.length
        if (hits.any { it.start < end && start < it.end }) continue
        hits += RichTextHit(start, end, linkUrl = token, video = true)
    }
    return hits
}

private fun addRichEmotes(hits: MutableList<RichTextHit>, text: String, emotes: Map<String, String>) {
    for (match in EmoteToken.findAll(text)) {
        val token = match.groupValues[1]
        val url = emotes[token] ?: emotes["[$token]"] ?: continue
        val start = match.range.first; val end = match.range.last + 1
        if (hits.any { it.start < end && start < it.end }) continue
        hits += RichTextHit(start, end, emoteToken = token, emoteUrl = url)
    }
}

private fun addRichMentions(hits: MutableList<RichTextHit>, text: String, regex: Regex, mentions: Map<String, Long>) {
    for (match in regex.findAll(text)) {
        val name = match.groupValues[1]
        val mid = mentions[name] ?: continue
        val start = match.range.first; val end = match.range.last + 1
        if (hits.any { it.start < end && start < it.end }) continue
        hits += RichTextHit(start, end, mentionName = name, mentionMid = mid)
    }
}
