#!/usr/bin/env python3
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('module_graph', Path(__file__).with_name('check-module-graph.py'))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


def graph(*edges):
    nodes = {':app', ':feature:home', ':feature:video', ':core:model', ':core:domain',
             ':core:application', ':core:navigation', ':core:common', ':TeakUI',
             ':infra:bili', ':infra:playback', ':danmaku-engine', ':tools:kotlin-audit'}
    return '\n'.join(['project\t' + n for n in sorted(nodes)] +
                     ['edge\t' + '\t'.join(e) for e in edges])


class ModuleGraphTest(unittest.TestCase):
    def test_current_directions(self):
        self.assertEqual([], module.violations(graph(
            (':app', 'implementation', ':feature:home'),
            (':feature:home', 'api', ':core:domain'),
            (':feature:home', 'debugImplementation', ':TeakUI'),
            (':core:domain', 'implementation', ':core:model'),
            (':infra:bili', 'implementation', ':danmaku-engine'))))

    def test_feature_cannot_see_app_infra_or_other_feature(self):
        for target in (':app', ':infra:bili', ':feature:video'):
            with self.subTest(target=target):
                self.assertTrue(module.violations(graph((':feature:home', 'api', target))))

    def test_variant_dependency_is_checked(self):
        self.assertTrue(module.violations(graph((':feature:home', 'releaseImplementation', ':app'))))

    def test_app_cannot_package_development_tools(self):
        errors = module.violations(graph((':app', 'implementation', ':tools:kotlin-audit')))
        self.assertTrue(any('禁止依赖' in error for error in errors))

    def test_core_order_and_shared_ui_are_enforced(self):
        for source, target in ((':core:model', ':core:domain'), (':core:domain', ':core:application'),
                               (':TeakUI', ':core:model'), (':infra:bili', ':feature:home')):
            with self.subTest(source=source, target=target):
                self.assertTrue(module.violations(graph((source, 'implementation', target))))

    def test_cycle_between_infra_modules(self):
        errors = module.violations(graph((':infra:bili', 'api', ':infra:playback'),
                                         (':infra:playback', 'implementation', ':infra:bili')))
        self.assertTrue(any('循环' in e for e in errors))

    def test_unknown_module_and_malformed_graph(self):
        self.assertTrue(module.violations(graph((':app', 'api', ':missing'))))
        self.assertTrue(module.violations('nonsense'))


if __name__ == '__main__': unittest.main()
