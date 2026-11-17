package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.beans.core.Project;

public class ProjectMigrationSaveTest {
    private static final String PROJECT = "MigrationSaveTest";
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private Engine previousApp;
    private String previousProjects, previousConfiguration;
    private boolean previousCli, previousJobsAdded;
    private Logger previousEngineLog, previousBeansLog, previousManagerLog;
    private Field jobsAdded;
    private FailingSaveManager manager;
    private Path projectFile;
    private Throwable lastLoadFailure;

    @Before public void setup() throws Exception {
        previousApp = Engine.theApp;
        previousProjects = Engine.PROJECTS_PATH;
        previousConfiguration = Engine.CONFIGURATION_PATH;
        previousCli = Engine.bCliMode;
        previousEngineLog = Engine.logEngine;
        previousBeansLog = Engine.logBeans;
        previousManagerLog = Engine.logDatabaseObjectManager;
        Engine.bCliMode = true;
        Engine.logEngine = Engine.logBeans = Engine.logDatabaseObjectManager = Logger.getLogger("migration-save-test");
        Engine.logDatabaseObjectManager.setLevel(org.apache.log4j.Level.WARN);
        Engine.logDatabaseObjectManager.addAppender(new org.apache.log4j.AppenderSkeleton() {
            @Override protected void append(org.apache.log4j.spi.LoggingEvent event) {
                if (event.getThrowableInformation() != null) lastLoadFailure = event.getThrowableInformation().getThrowable();
            }
            @Override public void close() { }
            @Override public boolean requiresLayout() { return false; }
        });
        EnginePropertiesManager.initProperties();
        Engine.theApp = new Engine();
        Engine.theApp.databaseObjectsManager = manager = new FailingSaveManager();
        Engine.theApp.referencedProjectManager = new ReferencedProjectManager();
        Engine.theApp.contextManager = new ContextManager();
        Engine.PROJECTS_PATH = folder.newFolder("projects").getAbsolutePath();
        Engine.CONFIGURATION_PATH = folder.newFolder("configuration").getAbsolutePath();
        manager.init();
        jobsAdded = MigrationManager.class.getDeclaredField("jobsAdded");
        jobsAdded.setAccessible(true);
        previousJobsAdded = jobsAdded.getBoolean(null);
        MigrationManager.performProjectsMigration();
        Path directory = Files.createDirectory(Path.of(Engine.PROJECTS_PATH, PROJECT));
        projectFile = directory.resolve("c8oProject.yaml");
        Files.writeString(projectFile, "↑convertigo: 7.8.0.m005\n↓" + PROJECT + " [core.Project]: \n  ↓fs [connectors.FullSyncConnector]: \n");
    }

    @After public void restore() throws Exception {
        Engine.logDatabaseObjectManager.removeAllAppenders();
        Engine.theApp = previousApp;
        Engine.PROJECTS_PATH = previousProjects;
        Engine.CONFIGURATION_PATH = previousConfiguration;
        Engine.bCliMode = previousCli;
        Engine.logEngine = previousEngineLog;
        Engine.logBeans = previousBeansLog;
        Engine.logDatabaseObjectManager = previousManagerLog;
        if (jobsAdded != null) jobsAdded.setBoolean(null, previousJobsAdded);
    }

    @Test public void saveFailureDoesNotPreventTheLoadedProjectFromCompletingImport() throws Exception {
        Engine.bCliMode = false;
        Project project = manager.importProject(projectFile.toFile(), true);
        assertNotNull(lastLoadFailure == null ? "project must load" : org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage(lastLoadFailure), project);
        assertSame(project, manager.getOriginalProjectByName(PROJECT));
        assertEquals(1, manager.saves);
        assertFalse("migration must not request shared-workspace reloads", manager.lastAnnounce);
        assertEquals(com.twinsoft.convertigo.engine.util.CarUtils.ExportFormat.KEPT, manager.lastFormat);
        assertEquals(1, manager.syncs);
        assertSame(project, manager.synchronizedProject);
        assertEquals("verbose", project.getJsonOutput().name());
        assertEquals("AccessDeniedException: read-only-project", project.migrationSaveError);
        assertEquals(1, project.getConnectorsList().size());
    }

    @Test public void cliDoesNotSynchronizeDocuments() throws Exception {
        manager.failSave = false;
        Project project = manager.importProject(projectFile.toFile(), true);
        assertNotNull(lastLoadFailure == null ? "project must load" : org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage(lastLoadFailure), project);
        assertEquals(0, manager.syncs);
        assertNull(project.migrationSaveError);
    }

    @Test public void cachedImportRetainsWarningAndCanonicalSaveClearsIt() throws Exception {
        Engine.bCliMode = false;
        Project project = manager.importProject(projectFile.toFile(), true);
        assertSame(project, manager.importProject(projectFile.toFile(), false));
        assertNotNull(project.migrationSaveError);
        assertEquals(1, manager.syncs);
        var copy = folder.newFolder("copy").toPath().resolve("c8oProject.yaml");
        com.twinsoft.convertigo.engine.util.CarUtils.exportProject(project, copy.toString());
        assertNotNull(project.migrationSaveError);
        assertFalse(Files.readString(copy).contains("migrationSaveError"));
        assertFalse(java.util.Arrays.stream(java.beans.Introspector.getBeanInfo(Project.class).getPropertyDescriptors())
                .anyMatch(property -> property.getName().equals("migrationSaveError")));
        manager.failSave = false;
        manager.exportProject(project);
        assertNull(project.migrationSaveError);
        assertFalse(Files.readString(projectFile).contains("migrationSaveError"));
        Project reloaded = manager.importProject(projectFile.toFile(), true);
        assertNotSame(project, reloaded);
        assertNull(reloaded.migrationSaveError);
        assertEquals(2, manager.syncs);
    }

    @Test public void yamlFallbackRetainsTheSaveWarning() throws Exception {
        Engine.bCliMode = false;
        Engine.theApp.eventManager = new EventManager();
        Project project = manager.importProject(projectFile.toFile(), true);
        manager.failSave = false;
        Files.delete(projectFile);
        Files.createDirectory(projectFile);
        // A recovery XML does not count as a successful canonical save on this branch.
        assertThrows(EngineException.class, () -> manager.exportProject(project));
        assertNotNull(project.migrationSaveError);
        assertTrue(Files.isRegularFile(projectFile.getParent().resolve(PROJECT + ".xml")));
        Files.delete(projectFile);
        Files.writeString(projectFile, "");
        manager.exportProject(project);
        assertNull(project.migrationSaveError);
    }

    @Test public void failuresOfBothFilesystemTargetsStillAllowInitialization() throws Exception {
        Engine.bCliMode = false;
        Engine.theApp.eventManager = new EventManager();
        manager.failSave = false;
        manager.blockTargets = true;
        Project project = manager.importProject(projectFile.toFile(), true);
        assertNotNull(project);
        assertSame(project, manager.synchronizedProject);
        assertEquals(1, manager.syncs);
        assertTrue(project.migrationSaveError.contains(PROJECT));
        assertTrue(Files.isDirectory(projectFile));
        assertTrue(Files.isDirectory(projectFile.getParent().resolve(PROJECT + ".xml")));
    }

    @Test public void projectsListExposesTheDiagnosticWithoutPersistingIt() throws Exception {
        Project project = manager.importProject(projectFile.toFile(), true);
        var document = com.twinsoft.convertigo.engine.util.XMLUtils.createDom();
        document.appendChild(document.createElement("admin"));
        var request = (jakarta.servlet.http.HttpServletRequest) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { jakarta.servlet.http.HttpServletRequest.class },
                (proxy, method, arguments) -> method.getName().equals("getLocale") ? java.util.Locale.ENGLISH : null);
        new com.twinsoft.convertigo.engine.admin.services.projects.List() {
            void render() throws Exception { getServiceResult(request, document); }
        }.render();
        var item = (org.w3c.dom.Element) document.getElementsByTagName("project").item(0);
        assertEquals(project.migrationSaveError, item.getAttribute("migrationSaveError"));
    }

    @Test public void invalidProjectDoesNotRequestSynchronization() throws Exception {
        Engine.bCliMode = false;
        Files.writeString(projectFile, "not a project");
        try {
            assertNull(manager.importProject(projectFile.toFile(), true));
        } catch (EngineException expected) {
            // Keep the existing failed-import contract.
        }
        assertEquals(0, manager.saves);
        assertEquals(0, manager.syncs);
    }

    @Test public void diagnosticsAreBoundedAndDoNotIncludeSerializedValues() {
        String secretLikeValue = "synthetic-sensitive-value";
        assertEquals("IllegalArgumentException", com.twinsoft.convertigo.engine.util.CarUtils.exportErrorMessage(
                new IllegalArgumentException(secretLikeValue)));
        assertTrue(com.twinsoft.convertigo.engine.util.CarUtils.exportErrorMessage(
                new AccessDeniedException("x".repeat(2000))).length() <= 1024);
    }

    private static class FailingSaveManager extends DatabaseObjectsManager {
        private int saves, syncs;
        private boolean lastAnnounce;
        private com.twinsoft.convertigo.engine.util.CarUtils.ExportFormat lastFormat;
        private Project synchronizedProject;
        private boolean failSave = true, blockTargets;
        @Override protected void synchronizeFullSyncDocuments(Project project) {
            syncs++;
            synchronizedProject = project;
        }
        @Override protected void exportProject(Project project, boolean announce, com.twinsoft.convertigo.engine.util.CarUtils.ExportFormat format) throws EngineException {
            saves++;
            lastAnnounce = announce;
            lastFormat = format;
            if (failSave) {
                throw new EngineException("Unable to save migrated project", new AccessDeniedException("read-only-project"));
            }
            if (blockTargets) {
                try {
                    Path yaml = Engine.projectFile(project.getName()).toPath();
                    Files.delete(yaml);
                    Files.createDirectory(yaml);
                    Files.createDirectory(yaml.getParent().resolve(project.getName() + ".xml"));
                } catch (java.io.IOException e) {
                    throw new EngineException("Cannot prepare the test save targets", e);
                }
            }
            super.exportProject(project, announce, format);
        }
    }
}
