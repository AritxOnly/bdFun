package com.aritxonly.bdfun.tools;

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.FrontendConfigurationKeysKt;
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar;
import org.jetbrains.kotlin.com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.psi.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 只解析语法树，不编译或执行被审计源码；编译器依赖不进入Android产物。 */
public final class KotlinSizeAudit implements AutoCloseable {
    public record Measurement(String path, int line, String kind, String name, int bodyLines, int parameters) {}
    public record Violation(String path, int line, String kind, String name, int actual, int limit) {}
    public record Result(List<Measurement> measurements, List<Violation> violations) {}
    private final Disposable disposable = Disposer.newDisposable("bdFun-kotlin-size-audit");
    private final KtPsiFactory parser;

    public KotlinSizeAudit() {
        var configuration = new CompilerConfiguration();
        FrontendConfigurationKeysKt.setExtensionsStorage(configuration, new CompilerPluginRegistrar.ExtensionStorage());
        var environment = KotlinCoreEnvironment.createForProduction(disposable,
                configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
        parser = new KtPsiFactory(environment.getProject(), false);
    }

    public Result scan(String path, String source) {
        // IntelliJ PSI使用LF文档；直接传CRLF会把回车误报为Kotlin语法错误。
        source = source.replace("\r\n", "\n").replace('\r', '\n');
        var measurements = new ArrayList<Measurement>();
        var violations = new ArrayList<Violation>();
        int lines = (int) source.lines().count();
        if (lines > 400) violations.add(new Violation(path, 1, "file-lines", "<file>", lines, 400));
        KtFile file = parser.createFile(Path.of(path).getFileName().toString(), source);
        walk(file, path, source, measurements, violations);
        return new Result(List.copyOf(measurements), List.copyOf(violations));
    }

    private void walk(PsiElement element, String path, String source,
                      List<Measurement> measurements, List<Violation> violations) {
        if (element instanceof PsiErrorElement error) {
            violations.add(new Violation(path, lineAt(source, element.getTextOffset()),
                    "syntax", error.getErrorDescription(), 1, 0));
        }
        if (element instanceof KtDeclarationWithBody declaration) {
            String kind = element instanceof KtPrimaryConstructor ? "primary-constructor"
                    : element instanceof KtSecondaryConstructor ? "secondary-constructor"
                    : element instanceof KtFunctionLiteral ? "lambda"
                    : element instanceof KtPropertyAccessor ? "accessor" : "function";
            String name = declaration.getName();
            if (element instanceof KtPropertyAccessor accessor) {
                name = accessor.getProperty().getName() + (accessor.isGetter() ? ".get" : ".set");
            }
            measure(element, element instanceof KtFunctionLiteral ? element : declaration.getBodyExpression(), parameterCount(declaration),
                    kind, name == null ? "<anonymous>" : name, path, source, measurements, violations);
        } else if (element instanceof KtFunctionType type) {
            var contexts = type.getContextParameterList();
            int count = type.getParameters().size() + (contexts == null ? 0
                    : contexts.getContextParameters().size() + contexts.contextReceivers().size());
            measure(element, null, count, "function-type", "<function-type>",
                    path, source, measurements, violations);
        } else if (element instanceof KtClassInitializer initializer) {
            measure(element, initializer.getBody(), 0, "init-block", "<init>",
                    path, source, measurements, violations);
        }
        for (PsiElement child : element.getChildren()) walk(child, path, source, measurements, violations);
    }

    private static int parameterCount(KtDeclarationWithBody declaration) {
        KtCallableDeclaration owner = declaration instanceof KtCallableDeclaration callable ? callable
                : declaration instanceof KtPropertyAccessor accessor ? accessor.getProperty() : null;
        return declaration.getValueParameters().size() + (owner == null ? 0
                : owner.getContextParameters().size() + owner.getContextReceivers().size());
    }

    private void measure(PsiElement declaration, PsiElement body, int parameters, String kind, String name,
                         String path, String source, List<Measurement> measurements, List<Violation> violations) {
        int line = lineAt(source, declaration.getTextOffset());
        int bodyLines = body == null ? 0 : lineAt(source, Math.max(body.getTextRange().getStartOffset(),
                body.getTextRange().getEndOffset() - 1)) - lineAt(source, body.getTextRange().getStartOffset()) + 1;
        measurements.add(new Measurement(path, line, kind, name, bodyLines, parameters));
        if (bodyLines > 80) violations.add(new Violation(path, line, "body-lines", kind + ":" + name, bodyLines, 80));
        if (parameters > 12) violations.add(new Violation(path, line, "parameters", kind + ":" + name, parameters, 12));
    }

    private static int lineAt(String source, int offset) {
        int line = 1;
        for (int i = 0; i < Math.min(offset, source.length()); i++) if (source.charAt(i) == '\n') line++;
        return line;
    }

    private static List<Path> ownedSources(Path root) throws IOException {
        var result = new ArrayList<Path>();
        for (String folder : List.of("app/src/main/kotlin/com/aritxonly/bdfun", "app/src/debug/kotlin/com/aritxonly/bdfun")) {
            Path directory = root.resolve(folder);
            if (Files.isDirectory(directory)) try (var files = Files.walk(directory)) {
                result.addAll(files.filter(p -> p.toString().endsWith(".kt")).toList());
            }
        }
        for (String folder : List.of("core", "infra", "feature")) {
            Path directory = root.resolve(folder);
            if (Files.isDirectory(directory)) try (var files = Files.walk(directory)) {
                result.addAll(files.filter(p -> {
                    String text = p.toString().replace('\\', '/');
                    return text.endsWith(".kt") && text.contains("/src/main/") && !text.contains("/build/");
                }).toList());
            }
        }
        return result.stream().distinct().sorted().toList();
    }

    private static String quote(String value) {
        var out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            if (c == '\\' || c == '"') out.append('\\').append(c);
            else if (c < 32) out.append(String.format("\\u%04x", (int) c));
            else out.append(c);
        }
        return out.append('"').toString();
    }

    private static String json(int files, List<Measurement> measurements, List<Violation> violations) {
        var out = new StringBuilder("{\"files\":").append(files).append(",\"callables\":[");
        var separator = "";
        for (var m : measurements) {
            out.append(separator).append("{\"path\":").append(quote(m.path())).append(",\"line\":").append(m.line())
                    .append(",\"kind\":").append(quote(m.kind())).append(",\"name\":").append(quote(m.name()))
                    .append(",\"bodyLines\":").append(m.bodyLines()).append(",\"parameters\":").append(m.parameters()).append('}');
            separator = ",";
        }
        out.append("],\"violations\":["); separator = "";
        for (var v : violations) {
            out.append(separator).append("{\"path\":").append(quote(v.path())).append(",\"line\":").append(v.line())
                    .append(",\"kind\":").append(quote(v.kind())).append(",\"name\":").append(quote(v.name()))
                    .append(",\"actual\":").append(v.actual()).append(",\"limit\":").append(v.limit()).append('}');
            separator = ",";
        }
        return out.append("]}\n").toString();
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) throw new IllegalArgumentException("需要仓库目录和报告路径");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Path report = Path.of(args[1]).toAbsolutePath().normalize();
        // 本轮无法扫描时不能留下上一轮的报告，避免旧结果被误当成当前通过。
        Files.deleteIfExists(report);
        var measurements = new ArrayList<Measurement>();
        var violations = new ArrayList<Violation>();
        var sources = ownedSources(root);
        if (sources.isEmpty()) throw new IOException("未找到自有生产Kotlin源码，拒绝空扫描");
        try (var audit = new KotlinSizeAudit()) {
            for (Path source : sources) {
                var result = audit.scan(root.relativize(source).toString(), Files.readString(source));
                measurements.addAll(result.measurements()); violations.addAll(result.violations());
            }
        }
        violations.sort(Comparator.comparing(Violation::path).thenComparingInt(Violation::line));
        Files.createDirectories(report.getParent());
        Files.writeString(report, json(sources.size(), measurements, violations));
        System.out.printf("Kotlin PSI audit: files=%d callables=%d violations=%d%n",
                sources.size(), measurements.size(), violations.size());
        for (var v : violations.stream().limit(60).toList()) {
            System.out.printf("%s:%d %s %s %d > %d%n", v.path(), v.line(), v.kind(), v.name(), v.actual(), v.limit());
        }
        if (!violations.isEmpty() && !(args.length > 2 && args[2].equals("--report-only"))) System.exit(1);
    }

    @Override public void close() { Disposer.dispose(disposable); }
}
