package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.video.ResolvedVideoLink

interface VideoLinkTitleRepository { suspend fun resolve(token: String): Outcome<ResolvedVideoLink?> }
