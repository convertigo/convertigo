package com.twinsoft.convertigo.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

import com.twinsoft.convertigo.engine.util.XSLTUtils;

public class XSLTProjectLifecycleTest {
    private static final String PROJECT = "XsltLifecycleTest";
    private static final String YAML = "↑convertigo: 8.4.0.m006\n↓" + PROJECT + " [core.Project]: \n";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Engine previousApp;
    private String previousProjects;
    private boolean previousCli;
    private Logger previousEngineLog;
    private Logger previousBeansLog;
    private Logger previousManagerLog;
    private Field migrationJobsAdded;
    private boolean previousJobsAdded;
    private DatabaseObjectsManager manager;
    private Path projectFile;
    private Path root;

    @Before
    public void setup() throws Exception {
        previousApp = Engine.theApp;
        previousProjects = Engine.PROJECTS_PATH;
        previousCli = Engine.bCliMode;
        previousEngineLog = Engine.logEngine;
        previousBeansLog = Engine.logBeans;
        previousManagerLog = Engine.logDatabaseObjectManager;
        Engine.bCliMode = true;
        Engine.logEngine = Engine.logBeans = Engine.logDatabaseObjectManager = Logger.getLogger("xslt-lifecycle-test");
        EnginePropertiesManager.initProperties();
        Engine.theApp = new Engine();
        Engine.theApp.databaseObjectsManager = manager = new DatabaseObjectsManager();
        Engine.theApp.referencedProjectManager = new ReferencedProjectManager();
        Engine.PROJECTS_PATH = folder.newFolder("projects").getAbsolutePath();
        manager.init();
        migrationJobsAdded = MigrationManager.class.getDeclaredField("jobsAdded");
        migrationJobsAdded.setAccessible(true);
        previousJobsAdded = migrationJobsAdded.getBoolean(null);
        MigrationManager.performProjectsMigration();
        Path directory = Files.createDirectory(Path.of(Engine.PROJECTS_PATH, PROJECT));
        projectFile = Files.writeString(directory.resolve("c8oProject.yaml"), YAML);
        root = Files.writeString(directory.resolve("root.xsl"), stylesheet("before"));
    }

    @After
    public void restore() throws Exception {
        XSLTUtils.clearProjectCache(PROJECT);
        Engine.theApp = previousApp;
        Engine.PROJECTS_PATH = previousProjects;
        Engine.bCliMode = previousCli;
        Engine.logEngine = previousEngineLog;
        Engine.logBeans = previousBeansLog;
        Engine.logDatabaseObjectManager = previousManagerLog;
        if (migrationJobsAdded != null) {
            migrationJobsAdded.setBoolean(null, previousJobsAdded);
        }
    }

    @Test
    public void clearsTemplatesEvenWithoutALoadedProject() throws Exception {
        Templates before = templates();
        manager.clearCache(PROJECT);
        assertNotSame(before, templates());
    }

    @Test
    public void reloadAndImportWithReplacementInvalidateTemplates() throws Exception {
        manager.importProject(projectFile.toFile(), true);
        Templates before = templates();

        manager.importProject(projectFile.toFile(), false);
        assertSame(before, templates());

        Files.writeString(root, stylesheet("reloaded"));
        manager.clearCache(PROJECT);
        manager.getProjectByName(PROJECT);
        Templates reloaded = templates();
        assertNotSame(before, reloaded);
        assertEquals("reloaded", transform(reloaded));

        Files.writeString(root, stylesheet("replaced"));
        manager.importProject(projectFile.toFile(), true);
        assertNotSame(reloaded, templates());
        assertEquals("replaced", transform(templates()));
    }

    @Test
    public void redeploymentAndDeletionInvalidateTemplates() throws Exception {
        manager.importProject(projectFile.toFile(), true);
        Templates before = templates();
        Path archive = folder.newFile("updated.car").toPath();
        try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (var entry : Map.of("c8oProject.yaml", YAML, "root.xsl", stylesheet("deployed")).entrySet()) {
                zip.putNextEntry(new ZipEntry(PROJECT + "/" + entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        manager.deployProject(archive.toString(), PROJECT, true);
        assertNotSame(before, templates());
        assertEquals("deployed", transform(templates()));

        manager.deleteProject(PROJECT);
        assertThrows(TransformerException.class, this::templates);
    }

    private Templates templates() throws Exception {
        return XSLTUtils.getTemplates(PROJECT, root.toString());
    }

    private static String stylesheet(String text) {
        return "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
                + "<xsl:output method=\"text\"/><xsl:template match=\"/\">"
                + text + "</xsl:template></xsl:stylesheet>";
    }

    private static String transform(Templates templates) throws Exception {
        var result = new StringWriter();
        templates.newTransformer().transform(new StreamSource(new StringReader("<input/>")),
                new StreamResult(result));
        return result.toString();
    }
}
