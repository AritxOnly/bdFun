#!/usr/bin/env python3
"""检查核心层与 feature 的依赖边界；可用 --root 在临时目录验证规则。"""
import argparse
from pathlib import Path
import re


def violations(root):
    errors = []
    for layer in ('model', 'domain', 'common', 'application', 'navigation'):
        for path in (root / 'core' / layer).rglob('*.kt'):
            for line, text in enumerate(path.read_text().splitlines(), 1):
                if re.match(r'\s*import\s+(android\.|androidx\.|okhttp3\.|org\.json\.|coil\.)', text):
                    errors.append(f'{path}:{line}: 核心层依赖平台实现：{text.strip()}')
    for path in (root / 'feature').rglob('*.kt'):
        feature = path.relative_to(root / 'feature').parts[0]
        for line, text in enumerate(path.read_text().splitlines(), 1):
            match = re.match(r'\s*import\s+com\.aritxonly\.bdfun\.feature\.(\w+)', text)
            if match and match[1] != feature:
                errors.append(f'{path}:{line}: feature {feature} 引用了 {match[1]}')
    return errors


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    errors = violations(args.root)
    for error in errors:
        print(error)
    raise SystemExit(bool(errors))
