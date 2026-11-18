package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;

/** Real project imports and normal file replacement, without database/network synchronization. */
public class ProjectBundleLifecycleTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private Engine oldApp;
	private DatabaseObjectsManager.StudioProjects oldStudio;
	private String oldProjects, oldConfiguration;
	private boolean oldCli, oldJobs;
	private Field jobs;
	private Manager manager;
	private References references;
	private Path projects;
	private final List<String> loaded = new ArrayList<>();
	private final Map<String, Integer> syncs = new HashMap<>();
	private String nested, failed;
	private boolean absorbNestedFailure, failSave;
	private Runnable afterDelete;
	private final Map<String, File> declared = new HashMap<>();

	@Before public void setup() throws Exception {
		oldApp = Engine.theApp;
		oldStudio = DatabaseObjectsManager.studioProjects;
		oldProjects = Engine.PROJECTS_PATH;
		oldConfiguration = Engine.CONFIGURATION_PATH;
		oldCli = Engine.bCliMode;
		Engine.bCliMode = true;
		Engine.logEngine = Engine.logBeans = Engine.logDatabaseObjectManager = Engine.logAdmin = org.apache.log4j.Logger.getLogger("bundle-lifecycle-test");
		EnginePropertiesManager.initProperties();
		EnginePropertiesManager.setProperty(PropertyName.ZIP_BACKUP_OLD_PROJECT, "true");
		EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_RUNTIME_WARMUP, "off");
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = manager = new Manager();
		Engine.theApp.referencedProjectManager = references = new References();
		Engine.theApp.contextManager = new ContextManager();
		projects = folder.newFolder("projects").toPath();
		Engine.PROJECTS_PATH = projects.toString();
		Engine.CONFIGURATION_PATH = folder.newFolder("configuration").getAbsolutePath();
		DatabaseObjectsManager.studioProjects = new DatabaseObjectsManager.StudioProjects() {
			@Override public void declareProject(String name, File file) { declared.put(name, file); }
			@Override public File getProject(String name) {
				var known = declared.get(name);
				return known != null && known.exists() ? known : projects.resolve(name + "/c8oProject.yaml").toFile();
			}
			@Override public void reloadProject(String name) throws EngineException { manager.importProject(getProject(name), true); }
			@Override public void renameProject(String oldName, String newName) { declared.remove(oldName); }
			@Override public void projectLoaded(Project project) {
				String name = project.getName();
				loaded.add(name);
				if (name.equals(failed)) throw new IllegalStateException("synthetic initialization failure");
				if (name.equals("A") && nested != null) {
					try { manager.importProject(projects.resolve(nested + "/c8oProject.yaml").toFile(), true); }
					catch (EngineException failure) { if (!absorbNestedFailure) throw new IllegalStateException(failure); }
				}
			}
		};
		manager.init();
		jobs = MigrationManager.class.getDeclaredField("jobsAdded");
		jobs.setAccessible(true);
		oldJobs = jobs.getBoolean(null);
		MigrationManager.performProjectsMigration();
		Engine.bCliMode = false;
	}

	@After public void restore() throws Exception {
		var workerField = FileUtils.class.getDeclaredField("asyncDeleteThread");
		workerField.setAccessible(true);
		for (int i = 0; i < 10; i++) {
			var worker = (Thread) workerField.get(null);
			if (worker == null) break;
			worker.join(500);
		}
		assertNull("asynchronous fixture removal must finish before restoring the engine", workerField.get(null));
		manager.destroy();
		var loading = DatabaseObjectsManager.class.getDeclaredField("projectLoadingDataThreadLocal");
		loading.setAccessible(true);
		((ThreadLocal<?>) loading.get(null)).remove();
		Engine.theApp = oldApp;
		DatabaseObjectsManager.studioProjects = oldStudio;
		Engine.PROJECTS_PATH = oldProjects;
		Engine.CONFIGURATION_PATH = oldConfiguration;
		Engine.bCliMode = oldCli;
		jobs.setBoolean(null, oldJobs);
	}

	@Test public void installsAllGroupSourcesBeforeLoadingInOrderAndSynchronizesOnce() throws Exception {
		try (var scope = manager.openProjectImportGroup(Set.of("A", "B"))) {
			var a = install("A", "B", "8.4.0.m006");
			var b = install("B", "A", "8.4.0.m006");
			assertTrue(loaded.isEmpty());
			assertTrue(Files.isRegularFile(projects.resolve("A/c8oProject.yaml")));
			assertTrue(Files.isRegularFile(projects.resolve("B/c8oProject.yaml")));
			var pa = a.load();
			a.finish();
			var pb = b.load();
			b.finish();
			assertEquals(List.of("A", "B"), loaded);
			assertEquals(Map.of("A", 1, "B", 1), syncs);
			assertTrue(references.requested.isEmpty());
			assertSame(pa, manager.importProject(projects.resolve("A/c8oProject.yaml").toFile(), true));
			assertSame(pb, manager.getOriginalProjectByName("B", false));
			assertEquals(Map.of("A", 1, "B", 1), syncs);
			assertThrows(IllegalStateException.class, a::load);
			assertThrows(IllegalStateException.class, a::finish);
		}
		assertNull(ProjectImportGroup.current(manager));
		assertNoLoadingContext();
	}

	@Test public void optionalHttpHeaderStaysAbsentAfterImportAndEngineReload() throws Exception {
		assertHttpHeadersAfterReload(false);
	}

	@Test public void attributeOnlyHttpHeaderIsRepairedOnImportAndEngineReload() throws Exception {
		assertHttpHeadersAfterReload(true);
	}

	private void assertHttpHeadersAfterReload(boolean attributeOnly) throws Exception {
		var received = new java.util.concurrent.CopyOnWriteArrayList<Map<String, List<String>>>();
		var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			var headers = new java.util.TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
			headers.putAll(exchange.getRequestHeaders());
			received.add(headers);
			byte[] response = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, response.length);
			exchange.getResponseBody().write(response);
			exchange.close();
		});
		server.start();
		try {
			var descriptor = projects.resolve("HeaderFixture/c8oProject.yaml");
			Files.createDirectories(descriptor.getParent());
			Files.writeString(descriptor, "↑convertigo: 8.4.0.m006\n↓HeaderFixture [core.Project]: \n"
					+ "  ↓http [connectors.HttpConnector]: \n    server: 127.0.0.1\n    port: " + server.getAddress().getPort() + "\n"
					+ "    ↓Echo [transactions.JsonHttpTransaction]: \n"
					+ "      ↓__header_X_Optional [variables.RequestableHttpVariable-1]: \n"
					+ (attributeOnly ? "        value: \n          - ↑traceable: false\n" : "")
					+ "      ↓__header_X_Empty [variables.RequestableHttpVariable-2]: \n        value: ''\n"
					+ "      ↓__header_X_Populated [variables.RequestableHttpVariable-3]: \n        value: synthetic-header-value\n");
			var first = manager.importProject(descriptor.toFile(), true);
			callHeaderEcho(first);
			manager.exportProject(first);
			// Discard all loaded objects and HTTP state, then import only the persisted YAML.
			manager.destroy();
			Engine.theApp = new Engine();
			Engine.theApp.databaseObjectsManager = manager = new Manager();
			Engine.theApp.referencedProjectManager = references;
			Engine.theApp.contextManager = new ContextManager();
			manager.init();
			var reloaded = manager.importProject(descriptor.toFile(), true);
			assertNotSame(first, reloaded);
			callHeaderEcho(reloaded);
			assertEquals(2, received.size());
			for (var headers : received) {
				assertFalse("An absent value must not send a header", headers.containsKey("X_optional"));
				assertEquals(List.of(""), headers.get("X_empty"));
				assertEquals(List.of("synthetic-header-value"), headers.get("X_populated"));
			}
		} finally {
			server.stop(0);
		}
	}

	private void callHeaderEcho(Project project) throws Exception {
		var connector = (com.twinsoft.convertigo.beans.connectors.HttpConnector) project.getConnectorByName("http");
		var transaction = (com.twinsoft.convertigo.beans.transactions.JsonHttpTransaction) connector.getTransactionByName("Echo");
		var context = new Context("header-fixture");
		context.project = project;
		context.connector = connector;
		context.requestedObject = context.transaction = transaction;
		connector.context = transaction.context = context;
		transaction.setHttpPool(com.twinsoft.convertigo.engine.enums.HttpPool.no);
		transaction.createRequestableThread();
		Engine.theApp.pluginsManager = new PluginsManager();
		Engine.theApp.proxyManager = new ProxyManager() {
			@Override public void setProxy(org.apache.commons.httpclient.HostConfiguration host,
					org.apache.commons.httpclient.HttpState state, java.net.URL url) { }
		};
		context.nativeParameters = Map.of();
		transaction.parseInputDocument(context);
		connector.prepareForTransaction(context);
		assertEquals("{}", new String(connector.getData(context), java.nio.charset.StandardCharsets.UTF_8));
	}

	@Test public void reusesACompletedNestedImportWithoutAnotherSynchronization() throws Exception {
		nested = "B";
		try (var scope = manager.openProjectImportGroup(Set.of("A", "B"))) {
			var a = install("A", "B", "8.4.0.m006");
			var b = install("B", "A", "8.4.0.m006");
			a.load(); a.finish();
			b.load(); b.finish();
			assertEquals(List.of("A", "B"), loaded);
			assertEquals(Map.of("A", 1, "B", 1), syncs);
		}
		assertNoLoadingContext();
		var next = install("Next", null, "8.4.0.m006");
		assertEquals("Next", next.load().getName());
		next.finish();
		assertNoLoadingContext();
	}

	@Test public void remembersAnAbsorbedPostCacheFailureInsteadOfTreatingItAsLoaded() throws Exception {
		nested = failed = "B";
		absorbNestedFailure = true;
		try (var scope = manager.openProjectImportGroup(Set.of("A", "B"))) {
			var a = install("A", "B", "8.4.0.m006");
			var b = install("B", "A", "8.4.0.m006");
			a.load(); a.finish();
			assertThrows(EngineException.class, b::load);
			assertThrows(EngineException.class, () -> manager.getOriginalProjectByName("B", false));
			assertThrows(EngineException.class, () -> manager.importProject(projects.resolve("B/c8oProject.yaml").toFile(), true));
			assertEquals(List.of("A", "B"), loaded);
			assertEquals(Map.of("A", 1), syncs);
			assertNull(manager.getCachedProject("B"));
		}
		assertNull(ProjectImportGroup.current(manager));
		assertNoLoadingContext();
	}

	@Test public void keepsMigrationSaveWarningsAndExternalReferenceChecks() throws Exception {
		failSave = true;
		try (var scope = manager.openProjectImportGroup(Set.of("A"))) {
			var a = install("A", "External", "7.8.0.m005");
			var project = a.load();
			a.finish();
			assertNotNull(project.migrationSaveError);
			assertEquals(Map.of("A", 1), syncs);
			assertEquals(List.of("External"), references.requested);
			assertSame(project, manager.importProject(projects.resolve("A/c8oProject.yaml").toFile(), true));
		}
		assertNull(ProjectImportGroup.current(manager));
		assertNoLoadingContext();
	}

	@Test public void replacesSourcesWithTheNormalBackupWithoutLoadingThePreviousProject() throws Exception {
		var old = Files.createDirectory(projects.resolve("A"));
		Files.writeString(old.resolve("c8oProject.yaml"), yaml("A", null, "8.4.0.m006"));
		Files.writeString(old.resolve("old.txt"), "old data");
		Files.writeString(old.resolve(".project"), "workspace metadata");
		var installed = install("A", null, "8.4.0.m006");
		assertTrue(loaded.isEmpty());
		assertTrue("Preparation must wait for import, not just extraction", isDeploying("A"));
		assertFalse(Files.exists(old.resolve("old.txt")));
		assertEquals("workspace metadata", Files.readString(old.resolve(".project")));
		List<Path> backups;
		try (var files = Files.list(projects)) { backups = files.filter(path -> path.getFileName().toString().matches("A_.*\\.zip")).toList(); }
		assertEquals(1, backups.size());
		try (var zip = new ZipFile(backups.get(0).toFile())) {
			assertEquals("old data", new String(zip.getInputStream(zip.getEntry("A/old.txt")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
		}
		installed.load(); installed.finish();
		assertFalse(isDeploying("A"));
		assertEquals(List.of("A"), loaded);
	}

	@Test public void releasesInstalledMembersWhenAGroupStopsBeforeLoading() throws Exception {
		var old = Files.createDirectory(projects.resolve("A"));
		Files.writeString(old.resolve("c8oProject.yaml"), yaml("A", null, "8.4.0.m006"));
		try (var scope = manager.openProjectImportGroup(Set.of("A", "B"))) {
			install("A", null, "8.4.0.m006");
			assertTrue(isDeploying("A"));
			assertTrue(loaded.isEmpty());
		}
		assertFalse("Abandoned installed members must not block later preparation", isDeploying("A"));
	}

	@Test public void groupImportsKeepTheSourceAdmissionCheckAndReleaseFailedDeployments() throws Exception {
		var old = Files.createDirectory(projects.resolve("A"));
		Files.writeString(old.resolve("c8oProject.yaml"), yaml("A", null, "8.4.0.m006"));
		try (var scope = manager.openProjectImportGroup(Set.of("A"))) {
			var installed = install("A", null, "8.4.0.m006");
			var marker = projects.resolve("A").resolve(com.twinsoft.convertigo.engine.flow.FlowProjectLoadBoundary.PENDING_FILE);
			Files.createDirectories(marker.getParent());
			Files.writeString(marker, "pending");
			var failure = assertThrows(EngineException.class, installed::load);
			assertTrue(failure.getMessage().contains("FLOW_PROJECT_RECOVERY_REQUIRED"));
			assertTrue(loaded.isEmpty());
			assertTrue(syncs.isEmpty());
			assertFalse(isDeploying("A"));
		}
	}

	private static boolean isDeploying(String name) throws Exception {
		var type = com.twinsoft.convertigo.engine.flow.FlowRuntimeWarmup.class;
		var lock = type.getDeclaredField("lock");
		var deploying = type.getDeclaredField("deploying");
		lock.setAccessible(true);
		deploying.setAccessible(true);
		synchronized (lock.get(null)) { return ((Set<?>) deploying.get(null)).contains(name); }
	}

	@Test public void preservesSingleDeploymentRenamePrioritiesAndTraceNormalization() throws Exception {
		var archive = projects.resolve("Source-1.car");
		writeArchive(archive, Map.of("Source/c8oProject.yaml", yaml("Source", null, "8.4.0.m006")
				+ "  ↓sequence [sequences.GenericSequence-123]: \n", "Source/Traces/Legacy connector/data.txt", "trace"));
		var project = manager.deployProject(archive.toString(), "Renamed", true, false, true);
		assertEquals("Renamed", project.getName());
		assertEquals(1, project.getSequencesList().size());
		assertNotEquals(123L, project.getSequencesList().get(0).priority);
		assertTrue(Files.exists(projects.resolve("Renamed/Traces/Legacy_connector/data.txt")));
		assertFalse(Files.exists(archive));
		assertEquals(Map.of("Renamed", 1), syncs);
	}

	@Test public void preservesLegacyXmlRenamePriorityAndXslAssemblyContracts() throws Exception {
		// Isolate bean import so this test targets the existing deployment/XML/XSL helpers.
		var importer = new DatabaseObjectsManager() {
			@Override public Project importProject(File file, boolean override) throws EngineException {
				assertTrue(file.isFile());
				var project = new Project();
				try { project.setName(file.getParentFile().getName()); }
				catch (Exception failure) { throw new EngineException("fixture name", failure); }
				return project;
			}
		};
		String xml = "<convertigo beans=\"8.4.0.m006\"><project classname=\"com.twinsoft.convertigo.beans.core.Project\" priority=\"123\">"
				+ "<property name=\"name\"><java.lang.String value=\"Legacy\"/></property>"
				+ "<sheet><property name=\"url\"><java.lang.String value=\"main.xsl\"/></property></sheet></project></convertigo>";
		var renameArchive = projects.resolve("Legacy.car");
		writeArchive(renameArchive, Map.of("Legacy/Legacy.xml", xml));
		var renamed = importer.deployProject(renameArchive.toString(), "RenamedXml", true, false, true);
		assertEquals("RenamedXml", renamed.getName());
		String renamedXml = Files.readString(projects.resolve("RenamedXml/RenamedXml.xml"));
		assertTrue(renamedXml.contains("value=\"RenamedXml\""));
		assertFalse(renamedXml.contains("priority=\"123\""));
		assertFalse(Files.exists(renameArchive));
		var xslArchive = projects.resolve("Legacy-xsl.car");
		writeArchive(xslArchive, Map.of("Legacy/Legacy.xml", xml,
				"Legacy/main.xsl", "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\"><xsl:include href=\"part.xsl\"/></xsl:stylesheet>",
				"Legacy/part.xsl", "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\"><xsl:template match=\"/\"><result/></xsl:template></xsl:stylesheet>"));
		assertEquals("Legacy", importer.deployProject(xslArchive.toString(), true, true).getName());
		String assembled = Files.readString(projects.resolve("Legacy/main.xsl"));
		assertFalse(assembled.contains("xsl:include"));
		assertTrue(assembled.contains("xsl:template"));
		assertFalse(Files.exists(xslArchive));
	}

	@Test public void deploysAndExportsACyclicBundleThroughTheRealServices() throws Exception {
		Engine.theApp.schemaManager = new SchemaManager();
		var a = folder.newFile("A.car").toPath();
		var b = folder.newFile("B.car").toPath();
		writeArchive(a, Map.of("A/c8oProject.yaml", yaml("A", "B", "8.4.0.m006")));
		writeArchive(b, Map.of("B/c8oProject.yaml", yaml("B", "A", "8.4.0.m006")));
		var request = new com.twinsoft.convertigo.engine.servlets.GenericServletMultipartTest.Multipart()
				.file("file", "A.car", Files.readAllBytes(a)).file("file", "B.car", Files.readAllBytes(b)).request("/");
		var document = com.twinsoft.convertigo.engine.util.XMLUtils.createDom();
		document.appendChild(document.createElement("admin"));
		class Deployment extends com.twinsoft.convertigo.engine.admin.services.projects.DeployBundle {
			void run() throws Exception { getServiceResult(request, document); }
		}
		new Deployment().run();
		assertEquals("true", ((org.w3c.dom.Element) document.getElementsByTagName("deployment").item(0)).getAttribute("success"));
		assertEquals(List.of("A", "B"), loaded);
		assertEquals(Map.of("A", 1, "B", 1), syncs);
		assertNoLoadingContext();
		var response = new com.twinsoft.convertigo.engine.servlets.GenericServletMultipartTest.Response();
		var options = Map.of("projects", "[\"A\"]", "includeDependencies", "true");
		var exportRequest = (jakarta.servlet.http.HttpServletRequest) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] {jakarta.servlet.http.HttpServletRequest.class},
				(proxy, method, arguments) -> method.getName().equals("getParameter") ? options.get(arguments[0]) : null);
		class Exporter extends com.twinsoft.convertigo.engine.admin.services.projects.ExportBundle {
			void run() throws Exception { writeResponseResult(exportRequest, response.proxy()); }
		}
		new Exporter().run();
		assertEquals("application/zip", response.headers.get("Content-Type"));
		var output = Files.write(folder.newFile("bundle.zip").toPath(), response.body.toByteArray());
		try (var bundle = com.twinsoft.convertigo.engine.util.ProjectBundleImport.prepare(List.of(output), folder.newFolder().toPath(),
				com.twinsoft.convertigo.engine.util.ProjectBundleZip.Limits.defaults())) {
			assertEquals(List.of("A", "B"), bundle.plan().projects().stream().map(com.twinsoft.convertigo.engine.util.ProjectBundlePlan.ProjectInfo::name).toList());
			assertTrue(bundle.plan().groups().get(0).cyclic());
		}
	}

	@Test public void exportsAndReloadsStoredPrioritizedReferencesButRejectsUnsavedChanges() throws Exception {
		var shared = install("Shared", null, "8.4.0.m006");
		shared.load(); shared.finish();
		var appDirectory = Files.createDirectory(projects.resolve("App"));
		var descriptor = Files.writeString(appDirectory.resolve("c8oProject.yaml"),
				yaml("App", "Shared", "8.4.0.m006").replace(
						"[references.ProjectSchemaReference]", "[references.ProjectSchemaReference-1736958610130]"));
		var original = Files.readAllBytes(descriptor);
		var app = manager.importProject(descriptor.toFile(), true);
		assertArrayEquals(original, Files.readAllBytes(descriptor));
		assertEquals(1, app.getReferenceList().size());
		assertEquals(1736958610130L, app.getReferenceList().get(0).priority);
		var output = folder.getRoot().toPath().resolve("prioritized.zip");
		var plan = com.twinsoft.convertigo.engine.util.ProjectBundle.export(output, List.of("App"), true,
				com.twinsoft.convertigo.engine.enums.ArchiveExportOption.all);
		assertEquals(List.of("Shared", "App"), plan.projects().stream()
				.map(com.twinsoft.convertigo.engine.util.ProjectBundlePlan.ProjectInfo::name).toList());
		assertArrayEquals(original, Files.readAllBytes(descriptor));
		var savedBundle = Files.readAllBytes(output);
		app.setVersion("unsaved");
		assertThrows(java.io.IOException.class, () -> com.twinsoft.convertigo.engine.util.ProjectBundle.export(
				output, List.of("App"), true, com.twinsoft.convertigo.engine.enums.ArchiveExportOption.all));
		assertArrayEquals(savedBundle, Files.readAllBytes(output));
		app.setVersion("fixture");
		try (var bundle = com.twinsoft.convertigo.engine.util.ProjectBundleImport.prepare(List.of(output),
				folder.newFolder().toPath(), com.twinsoft.convertigo.engine.util.ProjectBundleZip.Limits.defaults())) {
			assertEquals(plan.projects(), bundle.plan().projects());
			assertTrue(bundle.warnings().isEmpty());
			for (var info : bundle.plan().projects()) {
				var deployment = manager.installPreparedProjectArchive(bundle.archive(info.name()).toString(), info.name());
				var reloaded = deployment.load();
				deployment.finish();
				if (info.name().equals("App")) {
					assertNotSame(app, reloaded);
					assertEquals(1736958610130L, reloaded.getReferenceList().get(0).priority);
					assertEquals(Set.of("Shared"), com.twinsoft.convertigo.engine.util.ProjectBundle.describe(reloaded).dependencies());
				}
			}
		}
	}

	@Test public void replacesAnExistingBundleWithTheSameVersions() throws Exception {
		assertBundleReplacement("fixture");
	}

	@Test public void replacesAnExistingBundleWithDifferentVersions() throws Exception {
		assertBundleReplacement("next");
	}

	private void assertBundleReplacement(String version) throws Exception {
		for (var name : List.of("A", "B")) {
			var old = install(name, null, "8.4.0.m006");
			old.load(); old.finish();
			Files.writeString(projects.resolve(name + "/payload.txt"), "old payload");
			Files.writeString(projects.resolve(name + "/obsolete.txt"), "old only");
			Files.writeString(projects.resolve(name + "/.project"), "workspace metadata");
			Files.createDirectory(projects.resolve(name + "/.git"));
			Files.writeString(projects.resolve(name + "/.git/config"), "local vcs metadata");
		}
		var archive = folder.newFile("replacement.zip").toPath();
		writeArchive(archive, Map.of("A/c8oProject.yaml", yaml("A", null, "8.4.0.m006").replace("version: fixture", "version: " + version),
				"A/payload.txt", "new A", "B/c8oProject.yaml", yaml("B", "A", "8.4.0.m006").replace("version: fixture", "version: " + version),
				"B/payload.txt", "new B"));
		var response = deployBundle(archive);
		assertEquals("true", ((org.w3c.dom.Element) response.getElementsByTagName("deployment").item(0)).getAttribute("success"));
		for (var name : List.of("A", "B")) {
			assertEquals(version, manager.getCachedProject(name).getVersion());
			assertEquals("new " + name, Files.readString(projects.resolve(name + "/payload.txt")));
			assertFalse(Files.exists(projects.resolve(name + "/obsolete.txt")));
			assertEquals("workspace metadata", Files.readString(projects.resolve(name + "/.project")));
			assertEquals("local vcs metadata", Files.readString(projects.resolve(name + "/.git/config")));
			List<Path> backups;
			try (var files = Files.list(projects)) { backups = files.filter(path -> path.getFileName().toString().matches(name + "_.*\\.zip")).toList(); }
			assertEquals(1, backups.size());
			try (var zip = new ZipFile(backups.get(0).toFile())) {
				assertEquals("old payload", new String(zip.getInputStream(zip.getEntry(name + "/payload.txt")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
			}
		}
		assertEquals(Map.of("A", 2, "B", 2), syncs);
	}

	@Test public void replacesABundleWhileKeepingAnExcludedDependency() throws Exception {
		var external = install("External", null, "8.4.0.m006");
		var dependency = external.load(); external.finish();
		var app = install("App", "External", "8.4.0.m006");
		app.load(); app.finish();
		var descriptor = Files.readAllBytes(projects.resolve("External/c8oProject.yaml"));
		var archive = folder.getRoot().toPath().resolve("excluded.zip");
		var plan = com.twinsoft.convertigo.engine.util.ProjectBundle.export(archive, List.of("App"), true, List.of(), List.of("External"),
				com.twinsoft.convertigo.engine.enums.ArchiveExportOption.all);
		assertEquals(List.of("App"), plan.projects().stream().map(com.twinsoft.convertigo.engine.util.ProjectBundlePlan.ProjectInfo::name).toList());
		var response = deployBundle(archive);
		assertEquals("true", ((org.w3c.dom.Element) response.getElementsByTagName("deployment").item(0)).getAttribute("success"));
		assertArrayEquals(descriptor, Files.readAllBytes(projects.resolve("External/c8oProject.yaml")));
		assertSame(dependency, manager.getCachedProject("External"));
		assertEquals(Map.of("External", 1, "App", 2), syncs);
	}

	@Test public void replacesMobileFilesRecreatedDuringTemplateReload() throws Exception {
		var oldTemplates = Engine.TEMPLATES_PATH;
		try {
			var templates = folder.newFolder("templates").toPath();
			Engine.TEMPLATES_PATH = templates.toString();
			Files.createDirectories(templates.resolve("base/Flashupdate"));
			Files.writeString(templates.resolve("base/Flashupdate/index.html"), "generated flash update");
			Files.writeString(templates.resolve("base/index_mb.html"), "generated mobile page longer than replacement");
			var old = install("App", null, "8.4.0.m006");
			var project = old.load(); old.finish();
			var mobile = new com.twinsoft.convertigo.beans.core.MobileApplication();
			project.add(mobile);
			var application = new com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent();
			mobile.add(application);
			var index = projects.resolve("App/DisplayObjects/mobile/index-fu.html");
			assertTrue(Files.isRegularFile(index));
			// Template notifications reload mobile beans independently of bundle installation.
			afterDelete = () -> {
				assertFalse(Files.exists(index));
				application.checkFolder();
				assertTrue(Files.isRegularFile(index));
			};
			var archive = folder.newFile("mobile.zip").toPath();
			writeArchive(archive, Map.of("App/c8oProject.yaml", yaml("App", null, "8.4.0.m006"),
					"App/DisplayObjects/mobile/index-fu.html", "archive page"));
			var response = deployBundle(archive);
			assertEquals("true", ((org.w3c.dom.Element) response.getElementsByTagName("deployment").item(0)).getAttribute("success"));
			assertEquals("archive page", Files.readString(index));
		} finally {
			Engine.TEMPLATES_PATH = oldTemplates;
			afterDelete = null;
		}
	}

	@Test public void retriesAPartialInstallationLikeASingleCarDeployment() throws Exception {
		var directory = Files.createDirectory(projects.resolve("App"));
		Files.createDirectory(directory.resolve("Flashupdate"));
		Files.writeString(directory.resolve("Flashupdate/index.html"), "previous generated contents");
		Files.writeString(directory.resolve(".project"), "workspace metadata");
		var archive = folder.newFile("partial.zip").toPath();
		writeArchive(archive, Map.of("App/c8oProject.yaml", yaml("App", null, "8.4.0.m006"),
				"App/Flashupdate/index.html", "new"));
		var response = deployBundle(archive);
		assertEquals("true", ((org.w3c.dom.Element) response.getElementsByTagName("deployment").item(0)).getAttribute("success"));
		assertEquals("new", Files.readString(directory.resolve("Flashupdate/index.html")));
		assertEquals("workspace metadata", Files.readString(directory.resolve(".project")));
	}

	@Test public void reportsTheProjectAndInstallationCauseWithoutLocalPaths() throws Exception {
		var directory = Files.createDirectory(projects.resolve("App"));
		Files.writeString(directory.resolve("assets"), "blocks the archive directory");
		var archive = folder.newFile("unwritable.zip").toPath();
		writeArchive(archive, Map.of("App/c8oProject.yaml", yaml("App", null, "8.4.0.m006"), "App/assets/file.txt", "new"));
		var response = deployBundle(archive);
		assertEquals("false", ((org.w3c.dom.Element) response.getElementsByTagName("deployment").item(0)).getAttribute("success"));
		var result = (org.w3c.dom.Element) response.getElementsByTagName("project").item(0);
		assertEquals("App", result.getAttribute("name"));
		assertEquals("failed", result.getAttribute("status"));
		var detail = result.getAttribute("detail");
		assertTrue(detail, detail.contains("App"));
		assertTrue(detail, detail.contains("FileAlreadyExistsException"));
		assertFalse(detail, detail.contains(folder.getRoot().getAbsolutePath()));
		assertFalse(detail, detail.contains("convertigo-project-upload-"));
	}

	private org.w3c.dom.Document deployBundle(Path archive) throws Exception {
		Engine.theApp.schemaManager = new SchemaManager();
		var request = new com.twinsoft.convertigo.engine.servlets.GenericServletMultipartTest.Multipart()
				.file("file", "bundle.zip", Files.readAllBytes(archive)).request("/");
		var document = com.twinsoft.convertigo.engine.util.XMLUtils.createDom();
		document.appendChild(document.createElement("admin"));
		class Deployment extends com.twinsoft.convertigo.engine.admin.services.projects.DeployBundle {
			void run() throws Exception { getServiceResult(request, document); }
		}
		new Deployment().run();
		return document;
	}

	private static void assertNoLoadingContext() throws Exception {
		// get() creates an empty value when none is present; remove that observation afterwards.
		var name = DatabaseObjectsManager.ProjectLoadingData.class.getDeclaredField("projectName");
		name.setAccessible(true);
		assertNull(name.get(DatabaseObjectsManager.getProjectLoadingData()));
		var field = DatabaseObjectsManager.class.getDeclaredField("projectLoadingDataThreadLocal");
		field.setAccessible(true);
		((ThreadLocal<?>) field.get(null)).remove();
	}

	private static void writeArchive(Path archive, Map<String, String> contents) throws Exception {
		try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
			for (var entry : contents.entrySet()) {
				zip.putNextEntry(new ZipEntry(entry.getKey()));
				zip.write(entry.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
				zip.closeEntry();
			}
		}
	}

	private DatabaseObjectsManager.ProjectDeployment install(String name, String dependency, String version) throws Exception {
		var archive = folder.newFile(name + "-" + System.nanoTime() + ".car").toPath();
		try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
			zip.putNextEntry(new ZipEntry(name + "/c8oProject.yaml"));
			zip.write(yaml(name, dependency, version).getBytes(java.nio.charset.StandardCharsets.UTF_8));
			zip.closeEntry();
		}
		return manager.installPreparedProjectArchive(archive.toString(), name);
	}

	private static String yaml(String name, String dependency, String version) {
		return "↑convertigo: " + version + "\n↓" + name + " [core.Project]: \n  version: fixture\n"
				+ (dependency == null ? "" : "  ↓Reference [references.ProjectSchemaReference]: \n    projectName: " + dependency + "\n")
				+ (version.startsWith("7.8") ? "  ↓fs [connectors.FullSyncConnector]: \n" : "");
	}

	private class Manager extends DatabaseObjectsManager {
		@Override public void deleteProject(String name, com.twinsoft.convertigo.engine.enums.DeleteProjectOption... options) throws EngineException {
			super.deleteProject(name, options);
			if (afterDelete != null) afterDelete.run();
		}
		@Override protected void synchronizeFullSyncDocuments(Project project) { syncs.merge(project.getName(), 1, Integer::sum); }
		@Override protected void exportProject(Project project, boolean announce, com.twinsoft.convertigo.engine.util.CarUtils.ExportFormat format) throws EngineException {
			if (failSave) throw new EngineException("synthetic read-only descriptor");
			super.exportProject(project, announce, format);
		}
	}

	private static class References extends ReferencedProjectManager {
		final List<String> requested = new ArrayList<>();
		@Override public Project importProject(ProjectUrlParser parser) {
			requested.add(parser.getProjectName());
			return null;
		}
	}
}
