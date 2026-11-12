#!/usr/bin/env python3
"""校验 Gradle 实际配置导出的生产 project 依赖，不解析构建脚本文本。"""
import sys
from pathlib import Path

CORE = {
    ':core:model': set(),
    ':core:common': set(),
    ':core:domain': {':core:model', ':core:common'},
    ':core:navigation': {':core:model', ':core:common'},
    ':core:application': {':core:model', ':core:common', ':core:domain', ':core:navigation'},
}


def violations(text):
    nodes, edges, errors = set(), [], []
    for line in text.splitlines():
        row = line.split('\t')
        if len(row) == 2 and row[0] == 'project': nodes.add(row[1])
        elif len(row) == 4 and row[0] == 'edge': edges.append(tuple(row[1:]))
        else: errors.append('无法识别模块图行: ' + line)
    if ':app' not in nodes: errors.append('模块图缺少 :app')
    graph = {n: set() for n in nodes}
    for source, config, target in edges:
        if source not in nodes or target not in nodes:
            errors.append(f'未知模块: {source} -> {target}')
            continue
        graph[source].add(target)
        if source in CORE:
            allowed = target in CORE[source]
        elif source.startswith(':feature:'):
            allowed = target.startswith(':core:') or target == ':TeakUI'
        elif source.startswith(':infra:'):
            allowed = target.startswith((':core:', ':infra:')) or target == ':danmaku-engine'
        elif source in (':TeakUI', ':danmaku-engine'):
            allowed = False
        elif source == ':app':
            allowed = not target.startswith(':tools:')
        else:
            allowed = False
        if not allowed: errors.append(f'{source} [{config}] 禁止依赖 {target}')
    active, done = set(), set()
    def visit(node, path):
        if node in active:
            errors.append('模块循环: ' + ' -> '.join(path + [node]))
            return
        if node in done: return
        active.add(node)
        for child in sorted(graph[node]): visit(child, path + [node])
        active.remove(node)
        done.add(node)
    for node in sorted(nodes): visit(node, [])
    return errors


if __name__ == '__main__':
    errors = violations(Path(sys.argv[1]).read_text())
    for error in errors: print(error)
    if not errors: print('PASS Gradle production project dependency graph')
    raise SystemExit(bool(errors))
