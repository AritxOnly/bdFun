package com.aritxonly.bdfun.tools;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class KotlinSizeAuditTest {
    private static KotlinSizeAudit audit;
    @BeforeClass public static void start() { audit = new KotlinSizeAudit(); }
    @AfterClass public static void stop() { if (audit != null) audit.close(); }
    private KotlinSizeAudit.Result scan(String source) { return audit.scan("Example.kt", source); }
    private String parameters(int count) {
        return java.util.stream.IntStream.range(0, count).mapToObj(i -> "p" + i + ": Int")
                .collect(java.util.stream.Collectors.joining(", "));
    }
    private void exceeds(String source, String kind) {
        var result = scan(source);
        assertTrue(result.violations().toString(), result.violations().stream().anyMatch(v -> v.kind().equals(kind)));
        assertFalse(result.violations().toString(), result.violations().stream().anyMatch(v -> v.kind().equals("syntax")));
    }
    @Test public void blockFunctionBoundaryIsExactly80() {
        var result = scan("fun block() {\n" + "Unit\n".repeat(78) + "}\n");
        assertTrue(result.violations().toString(), result.violations().isEmpty());
        assertEquals(80, result.measurements().getFirst().bodyLines());
        exceeds("fun block() {\n" + "Unit\n".repeat(79) + "}\n", "body-lines");
    }
    @Test public void expressionFunctionsAreMeasured() {
        exceeds("fun expression() = listOf(\n" + "1,\n".repeat(79) + ")\n", "body-lines");
    }
    @Test public void primaryConstructorParametersAreMeasured() {
        exceeds("class C(" + parameters(13) + ")", "parameters");
        assertTrue(scan("class C(" + parameters(12) + ")").violations().isEmpty());
    }
    @Test public void dataClassConstructorIsNotSkipped() {
        exceeds("data class C(" + parameters(13).replace("p", "val p") + ")", "parameters");
    }
    @Test public void secondaryConstructorBodyAndParametersAreMeasured() {
        String source = "class C() { constructor(" + parameters(13) + ") : this() {\n" + "Unit\n".repeat(79) + "} }";
        exceeds(source, "parameters"); exceeds(source, "body-lines");
    }
    @Test public void lazyAndInitializerLambdasAreMeasuredIncludingTheirBraces() {
        var result = scan("val value by lazy {\n" + "Unit\n".repeat(78) + "}\n");
        assertTrue(result.violations().toString(), result.violations().isEmpty());
        assertEquals(80, result.measurements().getFirst().bodyLines());
        exceeds("val action = {\n" + "Unit\n".repeat(79) + "}\n", "body-lines");
    }
    @Test public void lambdaAndAnonymousFunctionParametersAreMeasured() {
        exceeds("val lambda = { " + parameters(13) + " -> Unit }", "parameters");
        exceeds("val function = fun(" + parameters(13) + ") { Unit }", "parameters");
    }
    @Test public void gettersAndSettersAreMeasured() {
        exceeds("val value: Int\n get() {\n" + "Unit\n".repeat(79) + "return 1\n}", "body-lines");
        exceeds("var value: Int = 0\n set(next) {\n" + "Unit\n".repeat(79) + "field = next\n}", "body-lines");
    }
    @Test public void initBlocksAreMeasured() {
        exceeds("class C { init {\n" + "Unit\n".repeat(79) + "} }", "body-lines");
    }
    @Test public void stringInterpolationCodeIsNotMaskedAsLiteral() {
        exceeds("val text = \"${run {\n" + "Unit\n".repeat(79) + "}}\"", "body-lines");
    }
    @Test public void commentsAndLiteralTextDoNotInventDeclarations() {
        var result = scan("// fun fake() {}\n/* class Fake(a: Int) {} */\nval text = \"\"\"fun fake() {\n }\"\"\"");
        assertTrue(result.violations().toString(), result.violations().isEmpty());
        assertTrue(result.measurements().isEmpty());
    }
    @Test public void genericAndDefaultLambdaCommasDoNotInflateParameterCount() {
        var result = scan("fun f(a: Map<String, Pair<Int, Int>> = emptyMap(), b: String = \",\", action: (Int, Int) -> Unit = { _, _ -> }) {} ");
        assertTrue(result.violations().toString(), result.violations().isEmpty());
        assertTrue(result.measurements().stream().anyMatch(m -> m.name().equals("f") && m.parameters() == 3));
    }
    @Test public void fileBoundaryIncludesCommentsAndBlankLines() {
        assertTrue(scan("\n".repeat(400)).violations().isEmpty());
        exceeds("\n".repeat(401), "file-lines");
    }
    @Test public void malformedKotlinCannotPass() {
        assertTrue(scan("fun broken(").violations().stream().anyMatch(v -> v.kind().equals("syntax")));
    }
    @Test public void crlfFilesHaveSameSyntaxMeasurementsAndLimitsAsLfFiles() {
        String source = "fun f(a: Int,\n b: Int,\n) {\n when (a) {\n 1,\n 2,\n -> Unit\n else -> Unit\n }\n}\n";
        var lf = scan(source); var crlf = scan(source.replace("\n", "\r\n"));
        assertTrue(lf.violations().toString(), lf.violations().isEmpty());
        assertEquals(lf, crlf);
    }
    @Test public void functionTypeContractsCannotHideLongParameterLists() {
        exceeds("typealias Callback = (" + parameters(13) + ") -> Unit", "parameters");
        assertTrue(scan("typealias Callback = (" + parameters(12) + ") -> Unit").violations().isEmpty());
    }
    @Test public void explicitContextParametersCountTowardsFunctionInputs() {
        exceeds("context(ctx: String) fun f(" + parameters(12) + ") { Unit }", "parameters");
        var boundary = scan("context(ctx: String) fun f(" + parameters(11) + ") { Unit }");
        assertTrue(boundary.violations().toString(), boundary.violations().isEmpty());
        assertTrue(boundary.measurements().stream().anyMatch(m -> m.name().equals("f") && m.parameters() == 12));
    }
}
