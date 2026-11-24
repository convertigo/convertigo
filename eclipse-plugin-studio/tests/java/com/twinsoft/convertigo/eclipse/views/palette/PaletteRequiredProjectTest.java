/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.eclipse.views.palette;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean;
import com.twinsoft.convertigo.engine.dbo_explorer.DboExplorerManager;
import com.twinsoft.convertigo.engine.util.LogWrapper;

/**
 * No workbench required: the rule of the production palette for the beans of a library project. They are offered only
 * while the project is loaded, following its loading, and the experimental preference neither reveals them nor loses
 * its own beans. On macOS the JVM needs -XstartOnFirstThread for the plug-in's SWT resources.
 */
public final class PaletteRequiredProjectTest {
	private static final String LIBRARY = "lib_flow_engine";

	public static void main(String[] args) throws Exception {
		Engine.logBeans = Engine.logEngine = Engine.logStudio = Engine.logDatabaseObjectManager = Logger.getLogger(PaletteRequiredProjectTest.class);
		var preferences = new PreferenceStore();
		field("plugin").set(null, new ConvertigoPlugin() {
			@Override public IPreferenceStore getPreferenceStore() { return preferences; }
		});
		field("studioLog").set(null, new LogWrapper(Logger.getLogger(PaletteRequiredProjectTest.class)));
		Map<String, Project> loaded = new HashMap<>();
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			@Override public Project getLoadedProjectByName(String name) { return loaded.get(name); }
		};

		var flowEngine = bean("com.twinsoft.convertigo.beans.flow.FlowEngine");
		var flow = bean("com.twinsoft.convertigo.beans.flow.Flow");
		var sequence = bean("com.twinsoft.convertigo.beans.sequences.GenericSequence");
		var nativeStep = bean("com.twinsoft.convertigo.beans.steps.JsonGroupStep");
		for (boolean experimental : new boolean[] { false, true }) {
			preferences.setValue(ConvertigoPlugin.PREFERENCE_EXPERIMENTAL_FEATURES, experimental);
			for (boolean load : new boolean[] { false, true, false }) {
				if (load) {
					var library = new Project();
					library.setName(LIBRARY);
					loaded.put(LIBRARY, library);
				} else {
					loaded.remove(LIBRARY);
				}
				check(PaletteView.isHiddenBean(flowEngine) == !load, "the library engine bean follows its project, loaded=" + load);
				check(PaletteView.isHiddenBean(flow) == !load, "the library requestable bean follows its project, loaded=" + load);
				check(!PaletteView.isHiddenBean(sequence), "a classic bean is always offered");
				check(PaletteView.isHiddenBean(nativeStep) == !experimental, "a native step follows only the experimental preference");
			}
		}
		System.out.println("PaletteRequiredProjectTest OK: library beans follow their project, the experimental preference keeps its own rule");
	}

	private static Field field(String name) throws Exception {
		var field = ConvertigoPlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	private static DboBean bean(String className) throws Exception {
		for (var group : new DboExplorerManager().getGroups()) {
			for (var category : group.getCategories()) {
				for (var beans : category.getBeans()) {
					for (var bean : beans.getBeans()) {
						if (bean.getClassName().equals(className)) {
							return bean;
						}
					}
				}
			}
		}
		throw new AssertionError(className);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
