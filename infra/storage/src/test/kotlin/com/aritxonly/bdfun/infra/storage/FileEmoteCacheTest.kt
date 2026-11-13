package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.CachedEmotes
import com.aritxonly.bdfun.core.model.comment.EmoteItem
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileEmoteCacheTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun diskRoundTripPreservesPackageMetadataAndSeparatesAccounts() = runBlocking {
        val folder = File(temporary.root, "panels")
        val snapshot = CachedEmotes(listOf(EmoteItem("[包_笑]", "https://image", 9, "icon", 1, 2),
            EmoteItem("(笑)", "", 10, "", 4, 1)), 12345)
        assertTrue(FileEmoteCache(folder).write(42, snapshot) is Outcome.Success)
        val restarted = FileEmoteCache(folder)
        assertEquals(snapshot, (restarted.read(42) as Outcome.Success).value)
        assertNull((restarted.read(43) as Outcome.Success).value)
        assertFalse(File(folder, "42.json.tmp").exists())
    }
    @Test fun brokenOrUnsupportedFilesReturnFailureInsteadOfThrowing() = runBlocking {
        File(temporary.root, "42.json").writeText("incomplete{")
        val cache = FileEmoteCache(temporary.root)
        assertTrue(cache.read(42) is Outcome.Failure)
        File(temporary.root, "42.json").writeText("""{"version":999}""")
        assertTrue(cache.read(42) is Outcome.Failure)
    }
}
