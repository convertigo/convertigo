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

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.engine.admin.services.projects.ConvertYaml;
import com.twinsoft.convertigo.engine.admin.services.studio.project.Create;
import com.twinsoft.convertigo.engine.util.CarUtils;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;
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
