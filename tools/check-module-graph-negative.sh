#!/usr/bin/env bash
# 在独立Gradle配置中注入一条违规生产依赖；必须命中指定错误，不能将环境失败当成功。
set -euo pipefail
cd "$(dirname "$0")/.."
case_dir=$(mktemp -d "${TMPDIR:-/tmp}/bdfun-module-negative.XXXXXX")
trap 'rm -rf "$case_dir"' EXIT
cat > "$case_dir/forbidden.gradle" <<'GRADLE'
gradle.projectsEvaluated {
    def feature = rootProject.findProject(':feature:home')
    if (feature == null) return
    feature.dependencies.add('debugImplementation', rootProject.project(':infra:bili'))
}
GRADLE
if bash ./gradlew checkModuleGraph --init-script "$case_dir/forbidden.gradle" "$@" > "$case_dir/result.log" 2>&1; then
    cat "$case_dir/result.log"
    echo 'FAIL: forbidden feature -> infra dependency passed'
    exit 1
fi
if ! grep -F ':feature:home [debugImplementation] 禁止依赖 :infra:bili' "$case_dir/result.log"; then
    cat "$case_dir/result.log"
    echo 'FAIL: Gradle failed for a reason other than the injected dependency'
    exit 1
fi
echo 'PASS: actual Gradle production dependency was rejected'
