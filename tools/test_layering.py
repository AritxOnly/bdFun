"""守卫的反例必须实际非零退出；不依赖仓库现有源文件来证明规则。"""
import importlib.util
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name('check-layering.py')
spec = importlib.util.spec_from_file_location('layering', SCRIPT)
layering = importlib.util.module_from_spec(spec)
spec.loader.exec_module(layering)


class LayeringTest(unittest.TestCase):
    def check_source(self, relative, source, forbidden=True):
        with tempfile.TemporaryDirectory(prefix='bdfun-layering-') as directory:
            path = Path(directory) / relative
            path.parent.mkdir(parents=True)
            path.write_text(source)
            result = subprocess.run(['python3', str(SCRIPT), '--root', directory], capture_output=True, text=True)
            self.assertEqual(bool(result.returncode), forbidden, result.stdout + result.stderr)
            if forbidden: self.assertIn(str(path), result.stdout)

    def test_core_import(self):
        self.check_source('core/domain/src/main/kotlin/Test.kt', 'import android.content.Context')

    def test_core_fully_qualified(self):
        self.check_source('core/domain/src/main/kotlin/Test.kt', 'val x: android.content.Context? = null')

    def test_core_implementation(self):
        self.check_source('core/application/src/main/kotlin/Test.kt', 'val x = com.aritxonly.bdfun.infra.bili.BiliBackend()')

    def test_app_presentation_import(self):
        self.check_source('app/src/main/kotlin/com/aritxonly/bdfun/presentation/Test.kt', 'import okhttp3.Call')

    def test_app_presentation_fully_qualified(self):
        self.check_source('app/src/main/kotlin/com/aritxonly/bdfun/presentation/Test.kt', 'val x = com.aritxonly.bdfun.data.BiliApi.image(url)')

    def test_feature_cross_reference(self):
        self.check_source('feature/home/src/main/kotlin/Test.kt', 'val x: com.aritxonly.bdfun.feature.video.State? = null')

    def test_space_presentation_cannot_lookup_global_page_state(self):
        self.check_source('app/src/main/kotlin/com/aritxonly/bdfun/presentation/space/Test.kt',
                          'val state = AppGraph.spaceStateHolder(mid)')

    def test_space_presentation_cannot_read_global_session(self):
        self.check_source('app/src/main/kotlin/com/aritxonly/bdfun/presentation/space/Test.kt',
                          'val user = com.aritxonly.bdfun.AppGraph.session.value')

    def test_feature_application(self):
        self.check_source('app/src/main/kotlin/com/aritxonly/bdfun/feature/home/application/Test.kt', 'val x: org.json.JSONObject? = null')

    def test_own_feature_and_core_are_allowed(self):
        self.check_source('feature/home/src/main/kotlin/Test.kt', 'import com.aritxonly.bdfun.feature.home.State\nimport com.aritxonly.bdfun.core.model.Video', False)

    def test_literal_and_nested_comments_do_not_trigger(self):
        source = '''// import android.content.Context
/* org.json.JSONObject /* nested */ android.content.Context */
val message = "android.content.Context"
val raw = """org.json.JSONObject"""
'''
        self.check_source('core/domain/src/main/kotlin/Test.kt', source, False)
        code = layering.code_only(source + '\nval x: android.content.Context? = null')
        self.assertEqual(code.count('\n'), source.count('\n') + 1)
        self.assertIn('android.content.Context?', code)

    def test_string_template_expressions_are_real_dependencies(self):
        for source in ('val x = "${android.os.Build.MODEL}"',
                       'val x = """${org.json.JSONObject()}"""',
                       'val x = "${if (true) { android.os.Build.MODEL } else { "literal" }}"'):
            self.check_source('core/domain/src/main/kotlin/Test.kt', source)

    def test_nested_template_literals_and_escaped_dollars_remain_text(self):
        source = 'val x = "${if (true) { "android.os.Build" } else { "ok" }}"'
        self.check_source('core/domain/src/main/kotlin/Test.kt', source, False)
        self.check_source('core/domain/src/main/kotlin/Test.kt',
                          r'val x = "\${android.os.Build.MODEL}"', False)

    def test_legacy_player_cannot_import_media3(self):
        self.check_source('app/src/main/kotlin/com/aritxonly/bdfun/ui/player/Test.kt', 'import androidx.media3.ui.PlayerView')

    def test_legacy_player_cannot_reference_backend(self):
        self.check_source('app/src/main/kotlin/com/aritxonly/bdfun/ui/player/Test.kt', 'val ua = com.aritxonly.bdfun.data.BiliApi.UA')

    def test_activity_cannot_reach_api_through_global_graph(self):
        self.check_source('app/src/main/kotlin/com/aritxonly/bdfun/MainActivity.kt', 'val api = AppGraph.api')

    def test_activity_cannot_reference_backend_directly(self):
        self.check_source('app/src/main/kotlin/com/aritxonly/bdfun/VideoActivity.kt', 'import com.aritxonly.bdfun.infra.bili.BiliBackend')

    def test_player_settings_must_be_injected(self):
        self.check_source('app/src/main/kotlin/com/aritxonly/bdfun/ui/player/Test.kt', 'val enabled = AppGraph.settings.backgroundPlay')

    def test_detail_cannot_reintroduce_legacy_danmaku_proxy(self):
        self.check_source('app/src/main/kotlin/com/aritxonly/bdfun/ui/video/Test.kt', 'DanmakuSettings.enabled = true')

    def test_video_root_cannot_reach_global_settings(self):
        self.check_source('app/src/main/kotlin/com/aritxonly/bdfun/ui/VideoPlayer.kt', 'val speed = AppGraph.settings.defaultPlaybackSpeed')

    def test_comment_page_cannot_lookup_process_dependencies(self):
        self.check_source('app/src/main/kotlin/com/aritxonly/bdfun/presentation/comment/page/Test.kt',
                          'val likes = AppGraph.commentLikes')

    def test_video_cannot_reintroduce_legacy_dependency_bridges(self):
        for source in ('PlaybackProgressStore.save(bvid, 1, 2)', 'BackgroundPlayback.adopt(player, context)',
                       'com.aritxonly.bdfun.data.VideoPlayQueue.next()'):
            self.check_source('app/src/main/kotlin/com/aritxonly/bdfun/ui/video/Test.kt', source)

    def test_diagnostic_exemption_does_not_allow_other_data(self):
        self.check_source('app/src/main/kotlin/com/aritxonly/bdfun/presentation/Test.kt', 'com.aritxonly.bdfun.data.LogCollector.record("x", "y")', False)
        self.check_source('app/src/main/kotlin/com/aritxonly/bdfun/presentation/Test.kt', 'com.aritxonly.bdfun.data.SettingsStore()', True)


if __name__ == '__main__': unittest.main()
