package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.engine.Engine;

public class XSLTUtilsTest {
    private static final String PROJECT_A = "XsltCacheTestA";
    private static final String PROJECT_B = "XsltCacheTestB";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Logger previousEngineLog;

    @Before
    public void initializeLogger() {
        previousEngineLog = Engine.logEngine;
        Engine.logEngine = Logger.getLogger("xslt-cache-test");
    }

    @After
    public void clearCaches() {
        try {
            XSLTUtils.clearProjectCache(PROJECT_A);
            XSLTUtils.clearProjectCache(PROJECT_B);
        } finally {
            Engine.logEngine = previousEngineLog;
        }
    }

    @Test
    public void reusesCanonicalPathsUntilExplicitInvalidation() throws Exception {
        Path root = stylesheet(folder.newFile("root.xsl").toPath(), output("before"));
        Templates before = XSLTUtils.getTemplates(PROJECT_A, root.toString());
        assertSame(before, XSLTUtils.getTemplates(PROJECT_A,
                root.getParent().resolve("./root.xsl").toString()));

        stylesheet(root, output("after"));
        assertSame(before, XSLTUtils.getTemplates(PROJECT_A, root.toString()));
        assertEquals("before", transform(before));

        XSLTUtils.clearProjectCache(PROJECT_A);
        Templates after = XSLTUtils.getTemplates(PROJECT_A, root.toString());
        assertNotSame(before, after);
        assertEquals("after", transform(after));
        assertEquals("before", transform(before));
    }

    @Test
    public void recompilesRelativeImportsAndNestedIncludesAfterInvalidation() throws Exception {
        Path root = folder.newFile("root.xsl").toPath();
        Path nested = folder.newFolder("nested").toPath();
        Path leaf = stylesheet(nested.resolve("leaf.xsl"), output("before"));
        stylesheet(nested.resolve("imported.xsl"), "<xsl:include href=\"leaf.xsl\"/>");
        stylesheet(root, "<xsl:import href=\"nested/imported.xsl\"/>");
        var rootTimestamp = Files.getLastModifiedTime(root);
        Templates before = XSLTUtils.getTemplates(PROJECT_A, root.toString());
        assertEquals("before", transform(before));

        stylesheet(leaf, output("after"));
        assertSame(before, XSLTUtils.getTemplates(PROJECT_A, root.toString()));
        XSLTUtils.clearProjectCache(PROJECT_A);

        Templates after = XSLTUtils.getTemplates(PROJECT_A, root.toString());
        assertNotSame(before, after);
        assertEquals("after", transform(after));
        assertEquals(rootTimestamp, Files.getLastModifiedTime(root));
    }

    @Test
    public void invalidationOnlyAffectsTheSelectedProject() throws Exception {
        Path first = stylesheet(folder.newFolder("a").toPath().resolve("root.xsl"), output("A"));
        Path second = stylesheet(folder.newFolder("b").toPath().resolve("root.xsl"), output("B"));
        Templates a = XSLTUtils.getTemplates(PROJECT_A, first.toString());
        Templates b = XSLTUtils.getTemplates(PROJECT_B, second.toString());

        XSLTUtils.clearProjectCache(PROJECT_A);
        XSLTUtils.clearProjectCache(PROJECT_A);
        XSLTUtils.clearProjectCache(null);

        assertNotSame(a, XSLTUtils.getTemplates(PROJECT_A, first.toString()));
        assertSame(b, XSLTUtils.getTemplates(PROJECT_B, second.toString()));
        assertEquals("B", transform(b));
    }

    @Test
    public void retriesAfterACompilationFailure() throws Exception {
        Path root = stylesheet(folder.newFile("root.xsl").toPath(),
                "<xsl:include href=\"missing.xsl\"/>");
        assertThrows(TransformerException.class, () -> XSLTUtils.getTemplates(PROJECT_A, root.toString()));

        stylesheet(root.resolveSibling("missing.xsl"), output("repaired"));
        assertEquals("repaired", transform(XSLTUtils.getTemplates(PROJECT_A, root.toString())));
    }

    @Test
    public void concurrentRequestsReuseTheSameTemplates() throws Exception {
        Path root = stylesheet(folder.newFile("root.xsl").toPath(), output("concurrent"));
        var executor = Executors.newFixedThreadPool(8);
        var ready = new CountDownLatch(8);
        var start = new CountDownLatch(1);
        List<Future<Templates>> requests = new ArrayList<>();
        try {
            for (int i = 0; i < 8; i++) {
                requests.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    Templates templates = XSLTUtils.getTemplates(PROJECT_A, root.toString());
                    assertEquals("concurrent", transform(templates));
                    return templates;
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            Templates first = requests.get(0).get(10, TimeUnit.SECONDS);
            for (var request : requests) {
                assertSame(first, request.get(10, TimeUnit.SECONDS));
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private static Path stylesheet(Path path, String body) throws Exception {
        return Files.writeString(path, "<xsl:stylesheet version=\"1.0\" "
                + "xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
                + body + "</xsl:stylesheet>");
    }

    private static String output(String text) {
        return "<xsl:output method=\"text\"/><xsl:template match=\"/\">"
                + text + "</xsl:template>";
    }

    private static String transform(Templates templates) throws Exception {
        var result = new StringWriter();
        templates.newTransformer().transform(new StreamSource(new StringReader("<input/>")),
                new StreamResult(result));
        return result.toString();
    }
}
