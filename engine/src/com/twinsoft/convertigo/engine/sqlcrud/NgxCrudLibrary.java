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
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.sqlcrud;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.UICompEvent;
import com.twinsoft.convertigo.beans.ngx.components.UICompVariable;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;

/** The project reference to the library that provides the grid of the generated user interface. */
public final class NgxCrudLibrary {

	private NgxCrudLibrary() {
	}

	/** The reference of the project to the library, null when it has none. */
	public static ProjectSchemaReference findReference(Project project) {
		for (var reference : project.getReferenceList()) {
			if (reference instanceof ProjectSchemaReference schemaReference
					&& CrudUiDefaults.LIB_PROJECT_NAME.equals(schemaReference.getParser().getProjectName())) {
				return schemaReference;
			}
		}
		return null;
	}

	/**
	 * Adds the reference to the library when the project has none ({@link CrudUiDefaults#LIB_REFERENCE}). An existing
	 * reference is kept as it is, whatever its version. Returns true when a reference was added.
	 */
	public static boolean ensureReference(Project project) throws EngineException {
		if (findReference(project) != null && CrudUiDefaults.KEEP_EXISTING_LIB_REFERENCE) {
			return false;
		}
		var reference = new ProjectSchemaReference();
		reference.setName(CrudUiDefaults.LIB_PROJECT_NAME);
		reference.setProjectName(CrudUiDefaults.LIB_REFERENCE);
		reference.bNew = true;
		reference.hasChanged = true;
		project.add(reference);
		return true;
	}

	/** Imports the library in the workspace when it is not there yet, from the reference of the project. */
	public static void importIfMissing(Project project) throws Exception {
		if (Engine.theApp.databaseObjectsManager.existsProject(CrudUiDefaults.LIB_PROJECT_NAME)) {
			return;
		}
		var reference = findReference(project);
		var url = reference != null ? reference.getProjectName() : CrudUiDefaults.LIB_REFERENCE;
		var imported = Engine.theApp.referencedProjectManager.importProject(new ProjectUrlParser(url), true);
		if (imported == null) {
			throw new EngineException("Unable to import the project " + CrudUiDefaults.LIB_PROJECT_NAME + " from " + url
					+ ". Behind a proxy, import its .car archive by hand, then generate again.");
		}
	}

	/** Checks the library of the workspace. Returns null when it has what the generated objects use, or the problem. */
	public static String check() {
		try {
			var lib = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(CrudUiDefaults.LIB_PROJECT_NAME, false);
			return check(lib);
		} catch (Exception e) {
			return "the project " + CrudUiDefaults.LIB_PROJECT_NAME + " cannot be loaded: " + e.getMessage();
		}
	}

	/** Checks a library project: the agGrid shared component with GetRows and rowModelType, and agGridUpdateRows. */
	public static String check(Project lib) {
		var app = lib == null ? null : NgxCrudGenerator.ngxApplication(lib);
		if (app == null) {
			return "the project " + CrudUiDefaults.LIB_PROJECT_NAME + " has no NGX application";
		}
		var grid = app.getSharedComponentList().stream().filter(c -> c.getName().equals(lastPart(CrudUiDefaults.LIB_GRID))).findFirst().orElse(null);
		if (grid == null) {
			return "the shared component " + CrudUiDefaults.LIB_GRID + " is missing";
		}
		var hasGetRows = grid.getAllChildren().stream().anyMatch(c -> c instanceof UICompEvent && c.getName().equals("GetRows"));
		var hasRowModel = grid.getAllChildren().stream().anyMatch(c -> c instanceof UICompVariable && c.getName().equals("rowModelType"));
		if (!hasGetRows || !hasRowModel) {
			return "the version of " + CrudUiDefaults.LIB_PROJECT_NAME + " in the workspace is too old (its agGrid has no "
					+ (hasGetRows ? "rowModelType" : "GetRows") + "): update it to " + CrudUiDefaults.LIB_VERSION + " or later";
		}
		var hasUpdateRows = app.getSharedActionList().stream().anyMatch(a -> a.getName().equals(lastPart(CrudUiDefaults.LIB_GRID_UPDATE_ROWS)));
		if (!hasUpdateRows) {
			return "the shared action " + CrudUiDefaults.LIB_GRID_UPDATE_ROWS + " is missing";
		}
		return null;
	}

	private static String lastPart(String qname) {
		return qname.substring(qname.lastIndexOf('.') + 1);
	}
}
