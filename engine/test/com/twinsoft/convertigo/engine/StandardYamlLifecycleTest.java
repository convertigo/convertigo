package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.BeansDefaultValues;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.engine.admin.services.projects.ConvertYaml;
import com.twinsoft.convertigo.engine.admin.services.studio.project.Create;
import com.twinsoft.convertigo.engine.util.CarUtils;
import com.twinsoft.convertigo.engine.util.CarUtils.ExportFormat;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.YamlConverter;
import com.twinsoft.convertigo.engine.util.YamlConverter.Format;

/** Local filesystem integration of actual exports; no server, remote template or deployment. */
public class StandardYamlLifecycleTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private Engine previous;
	private String projectsPath, workspacePath;
	private final Map<String, Project> projects = new HashMap<>();

	@Before public void setup() throws Exception {
		previous = Engine.theApp; projectsPath = Engine.PROJECTS_PATH; workspacePath = Engine.USER_WORKSPACE_PATH;
		Engine.logBeans = Engine.logEngine = Engine.logDatabaseObjectManager = Engine.logAdmin = Logger.getLogger("yaml-lifecycle");
		EnginePropertiesManager.initProperties();
		Engine.PROJECTS_PATH = Engine.USER_WORKSPACE_PATH = folder.getRoot().getAbsolutePath();
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			@Override public Project getLoadedProjectByName(String name) { return projects.get(name); }
			@Override public Project getOriginalProjectByName(String name) { return projects.get(name); }
			@Override public List<String> getAllProjectNamesList(boolean openable) { return List.copyOf(projects.keySet()); }
			@Override public boolean existsProject(String name) { return projects.containsKey(name); }
			@Override public void exportProject(Project project) throws EngineException {
				CarUtils.exportProject(project, yaml(project).toString());
			}
		};
		Engine.theApp.databaseObjectsManager.symbolsProperties = new Properties();
	}
	@After public void restore() {
		Engine.theApp = previous; Engine.PROJECTS_PATH = projectsPath; Engine.USER_WORKSPACE_PATH = workspacePath;
	}
	private Path yaml(Project project) { return folder.getRoot().toPath().resolve(project.getName()).resolve("c8oProject.yaml"); }
	private Project project(String name, Format format) throws Exception {
		Project project = new Project(); project.setName(name); project.isSubLoaded = true; project.setYamlFormat(format);
		var sequence = new GenericSequence(); sequence.setName("Hello"); sequence.isSubLoaded = true; sequence.setComment("line\nwith quotes ' and \\");
		project.add(sequence); projects.put(name, project); Files.createDirectories(yaml(project).getParent());
		return project;
	}
	private static HttpServletRequest request(String method, Map<String, String> parameters) {
		return (HttpServletRequest) Proxy.newProxyInstance(StandardYamlLifecycleTest.class.getClassLoader(),
				new Class<?>[] { HttpServletRequest.class }, (proxy, call, args) -> switch (call.getName()) {
					case "getMethod" -> method;
					case "getParameter" -> parameters.get(args[0]);
					default -> null;
				});
	}

	/** the project of its files, read as DatabaseObjectsManager.importProject reads them, without writing them */
	private Project load(Path yaml) throws Exception {
		var document = BeansDefaultValues.unshrinkProject(YamlConverter.readYaml(yaml.toFile()));
		var root = document.getDocumentElement();
		var element = (Element) XMLUtils.findChildNode(root, Node.ELEMENT_NODE);
		element.setAttribute("version", root.getAttribute("beans"));
		var importer = DatabaseObjectsManager.class.getDeclaredMethod("importDatabaseObject", Node.class, DatabaseObject.class);
		importer.setAccessible(true);
		var project = (Project) importer.invoke(Engine.theApp.databaseObjectsManager, element, null);
		projects.put(project.getName(), project);
		return project;
	}
	/** the format studio.treeview.Get reports for a project, loaded or not */
	private static String treeFormat(String projectName, boolean loaded) throws Exception {
		var service = new com.twinsoft.convertigo.engine.admin.services.studio.treeview.Get();
		var method = service.getClass().getDeclaredMethod("getProjectNode", String.class, boolean.class);
		method.setAccessible(true);
		return ((JSONObject) method.invoke(service, projectName, loaded)).getString("yamlFormat");
	}

	/**
	 * A bean introduced in 8.5 makes the project require engine 8.5.0, which no older engine opens: read from legacy
	 * files, the project stays legacy while it loads, as its migration at load writes it, and its next save writes
	 * standard YAML, which the Studio and the services report from then on.
	 */
	@Test public void legacyProjectWhoseBeansRequire85IsSavedInStandardYaml() throws Exception {
		Project earlier = project("Recent", Format.LEGACY);
		var flow = new Flow(); flow.setName("Answer"); flow.isSubLoaded = true; earlier.add(flow);
		// the legacy files an engine without the rule wrote
		CarUtils.exportProject(earlier, yaml(earlier).toString(), ExportFormat.KEPT);
		assertEquals(Format.LEGACY, YamlConverter.getFormat(yaml(earlier).toFile()));
		assertTrue(Files.readString(yaml(earlier)).startsWith("↑convertigo: 8.5.0.m006\n"));
		byte[] legacy = Files.readAllBytes(yaml(earlier));
		var beansBefore = BeansDefaultValues.unshrinkProject(YamlConverter.readYaml(yaml(earlier).toFile()));

		Project project = load(yaml(earlier));
		assertNotSame(earlier, project);
		assertEquals("read from legacy files", Format.LEGACY, project.getYamlFormat());
		assertEquals("8.5.0.m006", project.getMinVersion());
		assertEquals("legacy", treeFormat("Recent", true));
		// its migration as it loads is not a save
		CarUtils.exportProject(project, yaml(project).toString(), ExportFormat.KEPT);
		assertEquals(Format.LEGACY, project.getYamlFormat());
		assertArrayEquals(legacy, Files.readAllBytes(yaml(project)));

		// an export elsewhere (an archive without its test cases) is written in standard YAML; the project keeps its files
		Path copy = folder.newFolder("export").toPath().resolve("c8oProject.yaml");
		CarUtils.exportProject(project, copy.toString());
		assertEquals(Format.STANDARD, YamlConverter.getFormat(copy.toFile()));
		assertEquals(Format.LEGACY, project.getYamlFormat());
		assertArrayEquals(legacy, Files.readAllBytes(yaml(project)));

		// the save
		Engine.theApp.databaseObjectsManager.exportProject(project);
		assertEquals(Format.STANDARD, project.getYamlFormat());
		assertEquals("the minimum of its beans", "8.5.0.m006", project.getMinVersion());
		assertEquals(Format.STANDARD, YamlConverter.getFormat(yaml(project).toFile()));
		assertTrue(Files.readString(yaml(project)).startsWith("convertigo: 8.5.0.m006\n"));
		assertEquals("Recent", DatabaseObjectsManager.getProjectName(yaml(project).toFile()));
		assertEquals("8.5.0.m006", DatabaseObjectsManager.getProjectVersion(yaml(project).toFile()));
		var beansAfter = BeansDefaultValues.unshrinkProject(YamlConverter.readYaml(yaml(project).toFile()));
		assertTrue("the same beans and values", beansBefore.getDocumentElement().getFirstChild()
				.isEqualNode(beansAfter.getDocumentElement().getFirstChild()));
		assertEquals("standard", treeFormat("Recent", true));
		projects.remove("Recent");
		assertEquals("standard", treeFormat("Recent", false));

		// read again, it stays standard and its next save writes the same files
		byte[] standard = Files.readAllBytes(yaml(project));
		Project reloaded = load(yaml(project));
		assertEquals(Format.STANDARD, reloaded.getYamlFormat());
		Engine.theApp.databaseObjectsManager.exportProject(reloaded);
		assertArrayEquals(standard, Files.readAllBytes(yaml(reloaded)));
	}

	@Test public void legacyProjectBelow85KeepsItsFormatAndStandardKeepsTheFloor() throws Exception {
		Project legacy = project("Older", Format.LEGACY);
		Engine.theApp.databaseObjectsManager.exportProject(legacy);
		assertEquals(Format.LEGACY, legacy.getYamlFormat());
		assertEquals(Format.LEGACY, YamlConverter.getFormat(yaml(legacy).toFile()));
		assertTrue("the minimum of its beans", VersionUtils.compare((String) legacy.getMinVersion(), YamlConverter.STANDARD_MIN_VERSION) < 0);
		Project standard = project("Newer", Format.STANDARD);
		Engine.theApp.databaseObjectsManager.exportProject(standard);
		assertEquals(Format.STANDARD, standard.getYamlFormat());
		assertEquals("max(beans, 8.5.0)", "8.5.0.m006", standard.getMinVersion());
		assertEquals(Format.STANDARD, YamlConverter.getFormat(yaml(standard).toFile()));
	}

	@Test public void loadedLegacyStaysLegacyAndExplicitServiceConversionIsDurable() throws Exception {
		Project project = project("Demo", Format.LEGACY);
		CarUtils.exportProject(project, yaml(project).toString());
		byte[] before = Files.readAllBytes(yaml(project));
		var beforeBeans = com.twinsoft.convertigo.beans.BeansDefaultValues.unshrinkProject(YamlConverter.readYaml(yaml(project).toFile()));
		CarUtils.exportProject(project, yaml(project).toString());
		assertArrayEquals(before, Files.readAllBytes(yaml(project)));
		assertEquals(Format.LEGACY, YamlConverter.getFormat(yaml(project).toFile()));
		var service = new ConvertYaml() {
			void call(HttpServletRequest request) throws Exception {
				var response = XMLUtils.createDom(); response.appendChild(response.createElement("admin"));
				getServiceResult(request, response);
			}
		};
		assertThrows(Exception.class, () -> service.call(request("GET", Map.of("projectName", "Demo"))));
		assertArrayEquals(before, Files.readAllBytes(yaml(project)));
		service.call(request("POST", Map.of("projectName", "Demo")));
		assertEquals(Format.STANDARD, project.getYamlFormat());
		assertEquals("8.5.0.m006", project.getMinVersion());
		assertEquals(Format.STANDARD, YamlConverter.getFormat(yaml(project).toFile()));
		assertEquals("Demo", DatabaseObjectsManager.getProjectName(yaml(project).toFile()));
		assertEquals(2, YamlConverter.readYaml(yaml(project).toFile()).getElementsByTagName("bean").getLength());
		var afterBeans = com.twinsoft.convertigo.beans.BeansDefaultValues.unshrinkProject(YamlConverter.readYaml(yaml(project).toFile()));
		assertTrue("Conversion must preserve expanded bean defaults and values",
				beforeBeans.getDocumentElement().getFirstChild().isEqualNode(afterBeans.getDocumentElement().getFirstChild()));
		CarUtils.exportProject(project, yaml(project).toString());
		assertEquals(Format.STANDARD, YamlConverter.getFormat(yaml(project).toFile()));
	}

	@Test public void creatingFromALegacyTemplateWritesAStandardProject() throws Exception {
		Engine.theApp.referencedProjectManager = new ReferencedProjectManager() {
			@Override public Project importProject(ProjectUrlParser parser, boolean force, boolean newPriorities) throws Exception {
				return project(parser.getProjectName(), Format.LEGACY);
			}
		};
		var service = new Create() {
			void call(HttpServletRequest request) throws Exception { getServiceResult(request, new JSONObject()); }
		};
		service.call(request("POST", Map.of("name", "Created", "template", "template_test=https://example.invalid/template.zip")));
		Project created = projects.get("Created");
		assertEquals(Format.STANDARD, created.getYamlFormat());
		assertEquals("8.5.0.m006", created.getMinVersion());
		assertEquals(Format.STANDARD, YamlConverter.getFormat(yaml(created).toFile()));
		assertEquals("Created", YamlConverter.projectName(YamlConverter.readYaml(yaml(created).toFile())));
	}

	@Test public void projectTreeReportsTheFormatForLoadedAndUnloadedProjects() throws Exception {
		Project project = project("TreeFormat", Format.LEGACY);
		CarUtils.exportProject(project, yaml(project).toString());
		var service = new com.twinsoft.convertigo.engine.admin.services.studio.treeview.Get();
		var method = service.getClass().getDeclaredMethod("getProjectNode", String.class, boolean.class);
		method.setAccessible(true);
		assertEquals("legacy", ((JSONObject) method.invoke(service, project.getName(), false)).getString("yamlFormat"));
		CarUtils.convertToStandardYaml(project);
		assertEquals("standard", ((JSONObject) method.invoke(service, project.getName(), true)).getString("yamlFormat"));
		projects.remove(project.getName());
		assertEquals("standard", ((JSONObject) method.invoke(service, project.getName(), false)).getString("yamlFormat"));
	}

	@Test public void conversionTellsTheOtherInstancesOfASharedWorkspaceAsASave() throws Exception {
		Project project = project("Shared", Format.LEGACY);
		CarUtils.exportProject(project, yaml(project).toString());
		String configuration = Engine.CONFIGURATION_PATH;
		String shared = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.SESSION_SHARED_WORKSPACE_SYNC_ENABLED);
		Engine.CONFIGURATION_PATH = folder.newFolder("configuration").getAbsolutePath();
		Path marker = Path.of(Engine.CONFIGURATION_PATH, "cluster-sync", "projects", "Shared.marker");
		EnginePropertiesManager.setProperty(EnginePropertiesManager.PropertyName.SESSION_SHARED_WORKSPACE_SYNC_ENABLED, "true");
		try {
			Engine.theApp.databaseObjectsManager.convertProjectToStandardYaml(project);
		} finally {
			EnginePropertiesManager.setProperty(EnginePropertiesManager.PropertyName.SESSION_SHARED_WORKSPACE_SYNC_ENABLED, shared);
			Engine.CONFIGURATION_PATH = configuration;
		}
		assertEquals(Format.STANDARD, project.getYamlFormat());
		assertEquals(Format.STANDARD, YamlConverter.getFormat(yaml(project).toFile()));
		assertTrue("the other instances reload the project in its new format", Files.isRegularFile(marker));
		var properties = new Properties();
		try (var in = Files.newInputStream(marker)) { properties.load(in); }
		assertEquals("project", properties.getProperty("kind"));
		assertEquals("Shared", properties.getProperty("target"));
		assertEquals("reload", properties.getProperty("operation"));
	}

	@Test public void failedConversionKeepsPreviousSourcesAndFormat() throws Exception {
		Project project = project("Demo", Format.LEGACY);
		CarUtils.exportProject(project, yaml(project).toString());
		byte[] before = Files.readAllBytes(yaml(project));
		Object minimum = project.getMinVersion();
		// An unwritable source target fails the writer's preflight before any replacement.
		Path sequence = yaml(project).getParent().resolve("_c8oProject/sequences/Hello.yaml");
		Files.delete(sequence); Files.createDirectory(sequence);
		assertThrows(EngineException.class, () -> CarUtils.convertToStandardYaml(project));
		assertEquals(Format.LEGACY, project.getYamlFormat());
		assertEquals(minimum, project.getMinVersion());
		assertArrayEquals(before, Files.readAllBytes(yaml(project)));
		assertFalse(Files.exists(yaml(project).getParent().resolve("Demo.xml")));
	}
}
