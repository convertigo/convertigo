/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.BeansDoc;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBeans;
import com.twinsoft.convertigo.engine.dbo_explorer.DboCategory;
import com.twinsoft.convertigo.engine.dbo_explorer.DboExplorerManager;
import com.twinsoft.convertigo.engine.dbo_explorer.DboGroup;

/**
 * The beans of an optional library project (requires="project" in database_objects.xml) are offered by the palette of
 * the Web Studio and documented only while that project is loaded, following its loading without a restart; the
 * experimental switch neither reveals them nor changes its own rule; an existing object still loads without the
 * project.
 */
public class RequiredProjectsTest {
	private static final String LIBRARY = "lib_flow_engine";
	private static final Set<String> LIBRARY_BEANS = Set.of(FlowEngine.class.getName(), Flow.class.getName());
	private static final String NATIVE_STEP = "com.twinsoft.convertigo.beans.steps.JsonGroupStep";

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private final Map<String, Project> loaded = new HashMap<>();
	private final DatabaseObject[] target = { null };
	private Engine previous;

	@BeforeClass
	public static void loggers() throws Exception {
		Engine.logBeans = Engine.logContext = Engine.logEngine = Engine.logDatabaseObjectManager = Logger.getLogger(RequiredProjectsTest.class);
		EnginePropertiesManager.initProperties();
	}

	@Before
	public void engine() {
		System.clearProperty(ExperimentalFeatures.SYSTEM_PROPERTY);
		previous = Engine.theApp;
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			{
				symbolsProperties = new Properties();
			}

			@Override
			public Project getLoadedProjectByName(String name) {
				return loaded.get(name);
			}

			@Override
			public Project getOriginalProjectByName(String name, boolean checkOpenable) {
				return loaded.get(name);
			}

			@Override
			public DatabaseObject getDatabaseObjectByQName(String qname) {
				return target[0];
			}
		};
	}

	@After
	public void restore() {
		Engine.theApp = previous;
		System.clearProperty(ExperimentalFeatures.SYSTEM_PROPERTY);
	}

	private void library(boolean load) throws Exception {
		if (load) {
			var project = new Project();
			project.setName(LIBRARY);
			loaded.put(LIBRARY, project);
		} else {
			loaded.remove(LIBRARY);
		}
	}

	private static DboBean bean(String className) throws Exception {
		for (DboGroup group : new DboExplorerManager().getGroups()) {
			for (DboCategory category : group.getCategories()) {
				for (DboBeans beans : category.getBeans()) {
					for (DboBean bean : beans.getBeans()) {
						if (bean.getClassName().equals(className)) {
							return bean;
						}
					}
				}
			}
		}
		throw new AssertionError(className);
	}

	@Test
	public void theLibraryBeansAreTheOnlyOnesRequiringAProject() throws Exception {
		var required = new TreeMap<String, String>();
		for (DboGroup group : new DboExplorerManager().getGroups()) {
			for (DboCategory category : group.getCategories()) {
				for (DboBeans beans : category.getBeans()) {
					for (DboBean bean : beans.getBeans()) {
						if (bean.getRequiredProject() != null) {
							required.put(bean.getClassName(), bean.getRequiredProject());
						}
					}
				}
			}
		}
		assertEquals(Map.of(FlowEngine.class.getName(), LIBRARY, Flow.class.getName(), LIBRARY), required);
		assertEquals(Set.of(LIBRARY), RequiredProjects.getRequiredProjects());
		assertTrue(RequiredProjects.isRequired(LIBRARY));
		assertFalse(RequiredProjects.isRequired("lib_other"));
	}

	@Test
	public void aLibraryBeanIsHiddenUntilItsProjectIsLoaded() throws Exception {
		var classic = bean(GenericSequence.class.getName());
		for (boolean load : new boolean[] { false, true, false }) {
			library(load);
			assertEquals(load, RequiredProjects.isLoaded(LIBRARY));
			assertEquals(load ? LIBRARY : "", RequiredProjects.getLoadedKey());
			for (String className : LIBRARY_BEANS) {
				assertEquals(className, !load, RequiredProjects.isHiddenBean(bean(className)));
			}
			assertFalse(RequiredProjects.isHiddenBean(classic));
		}
		// without an engine, as the generator of the reference documentation: nothing is loaded
		Engine.theApp = null;
		assertFalse(RequiredProjects.isLoaded(LIBRARY));
		assertTrue(RequiredProjects.isHiddenBean(bean(Flow.class.getName())));
		assertFalse(RequiredProjects.isHiddenBean(classic));
	}

	@Test
	public void theExperimentalSwitchIsIndependentOfTheLibrary() throws Exception {
		var flow = bean(Flow.class.getName());
		var step = bean(NATIVE_STEP);
		// the switch does not reveal a library bean
		ExperimentalFeatures.setShown(true);
		assertFalse(ExperimentalFeatures.isHiddenBean(flow));
		assertTrue(RequiredProjects.isHiddenBean(flow));
		// the library does not reveal an experimental bean, nor needs the switch for its own beans
		ExperimentalFeatures.setShown(false);
		library(true);
		assertTrue(ExperimentalFeatures.isHiddenBean(step));
		assertFalse(RequiredProjects.isHiddenBean(step));
		assertFalse(ExperimentalFeatures.isHiddenBean(flow));
		assertFalse(RequiredProjects.isHiddenBean(flow));
	}

	private static String palette(String id) throws Exception {
		var palette = new com.twinsoft.convertigo.engine.admin.services.studio.palette.Get();
		Method get = palette.getClass().getDeclaredMethod("getPalette", String.class);
		get.setAccessible(true);
		return get.invoke(palette, id).toString();
	}

	@Test
	public void theWebPaletteFollowsTheLibraryWithoutRestarting() throws Exception {
		var project = new Project();
		project.setName("Fixture");
		target[0] = project;
		for (boolean load : new boolean[] { false, true, false }) {
			library(load);
			String entries = palette(project.getName());
			for (String className : LIBRARY_BEANS) {
				assertEquals(className, load, entries.contains("\"classname\":\"" + className + "\""));
			}
			assertTrue(entries, entries.contains("\"classname\":\"" + GenericSequence.class.getName() + "\""));
		}
		ExperimentalFeatures.setShown(true);
		String shown = palette(project.getName());
		for (String className : LIBRARY_BEANS) {
			assertFalse(className, shown.contains("\"classname\":\"" + className + "\""));
		}
	}

	@Test
	public void anExistingProjectStillLoadsItsObjectsWithoutTheLibrary() throws Exception {
		var document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		var project = new Project();
		project.setName("ExistingFixture");
		var engine = new FlowEngine();
		engine.setName("Engine");
		var flow = new Flow();
		flow.setName("Existing");
		var export = document.createElement("convertigo");
		export.setAttribute("beans", com.twinsoft.convertigo.beans.Version.version);
		document.appendChild(export);
		var root = project.toXml(document);
		export.appendChild(root);
		root.appendChild(engine.toXml(document));
		root.appendChild(flow.toXml(document));

		assertFalse(RequiredProjects.isLoaded(LIBRARY));
		var load = DatabaseObjectsManager.class.getDeclaredMethod("importDatabaseObject", Node.class, DatabaseObject.class);
		load.setAccessible(true);
		var existing = (Project) load.invoke(Engine.theApp.databaseObjectsManager, root, null);
		assertEquals("Engine", existing.getFlowEngine().getName());
		assertTrue(existing.getSequenceByName("Existing") instanceof Flow);
	}

	private File beansDoc(String name) throws Exception {
		File dir = folder.newFolder(name);
		File output = new File(dir, "reference-manual/convertigo-objects");
		Constructor<BeansDoc> constructor = BeansDoc.class.getDeclaredConstructor(File.class, File.class, int.class);
		constructor.setAccessible(true);
		BeansDoc doc = constructor.newInstance(output, new File(dir, "images/beans"), -1);
		Method run = BeansDoc.class.getDeclaredMethod("run");
		run.setAccessible(true);
		java.io.PrintStream out = System.out;
		// BeansDoc prints a line per object
		System.setOut(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
		try {
			run.invoke(doc);
		} finally {
			System.setOut(out);
		}
		return output;
	}

	private static String objectClasses(File dir) throws Exception {
		StringBuilder sb = new StringBuilder();
		try (Stream<java.nio.file.Path> files = Files.walk(dir.toPath())) {
			for (java.nio.file.Path file : (Iterable<java.nio.file.Path>) files.filter(Files::isRegularFile)::iterator) {
				for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
					if (line.startsWith("ObjClass: ")) {
						sb.append(line).append('\n');
					}
				}
			}
		}
		return sb.toString();
	}

	@Test
	public void theReferenceDocumentationHasTheLibraryBeansOnlyWhileTheLibraryIsLoaded() throws Exception {
		String absent = objectClasses(beansDoc("absent"));
		for (String className : LIBRARY_BEANS) {
			assertFalse(className, absent.contains("ObjClass: " + className + "\n"));
		}
		assertTrue(absent.contains("ObjClass: " + GenericSequence.class.getName() + "\n"));

		library(true);
		String present = objectClasses(beansDoc("present"));
		for (String className : LIBRARY_BEANS) {
			assertTrue(className, present.contains("ObjClass: " + className + "\n"));
		}
	}
}
