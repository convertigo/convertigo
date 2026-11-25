/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details: <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.sqlcrud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UICompEvent;
import com.twinsoft.convertigo.beans.ngx.components.UICompVariable;
import com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.Engine;

/** The reference to the library of the grid: added once, an existing one kept, and the library checked. */
public class NgxCrudLibraryTest {

	@BeforeClass
	public static void logs() {
		if (Engine.logEngine == null) {
			Engine.logEngine = org.apache.log4j.Logger.getLogger("sql-crud-ui-test");
			Engine.logBeans = Engine.logEngine;
		}
	}

	@Test
	public void referenceIsAddedOnce() throws Exception {
		var project = NgxCrudGeneratorTest.project("T", List.of()).getProject();
		assertNull(NgxCrudLibrary.findReference(project));
		assertTrue(NgxCrudLibrary.ensureReference(project));
		var reference = NgxCrudLibrary.findReference(project);
		assertEquals(CrudUiDefaults.LIB_REFERENCE, reference.getProjectName());
		assertTrue(reference.getProjectName().contains("/releases/download/v8.4.0.49/lib_ExtendedComponents_ui_ngx.car"));
		assertFalse(NgxCrudLibrary.ensureReference(project));
		assertEquals(1, project.getReferenceList().size());
	}

	@Test
	public void existingReferenceIsKept() throws Exception {
		var project = NgxCrudGeneratorTest.project("T", List.of()).getProject();
		var existing = new ProjectSchemaReference();
		existing.setName("grid");
		existing.setProjectName("lib_ExtendedComponents_ui_ngx=https://github.com/convertigo/c8oprj-lib-extended-components-ui-ngx.git:branch=8.4.0.0");
		project.add(existing);
		assertFalse(NgxCrudLibrary.ensureReference(project));
		assertEquals(List.of(existing), project.getReferenceList());
	}

	@Test
	public void libraryCheck() throws Exception {
		assertTrue(NgxCrudLibrary.check(null).contains("no NGX application"));
		var app = library(true, true, true);
		assertNull(NgxCrudLibrary.check(app.getProject()));
		assertTrue(NgxCrudLibrary.check(library(false, true, true).getProject()).contains("too old"));
		assertTrue(NgxCrudLibrary.check(library(true, false, true).getProject()).contains("too old"));
		assertTrue(NgxCrudLibrary.check(library(true, true, false).getProject()).contains("agGridUpdateRows"));
	}

	private static ApplicationComponent library(boolean getRows, boolean rowModelType, boolean updateRows) throws Exception {
		var project = new Project();
		project.setName(CrudUiDefaults.LIB_PROJECT_NAME);
		var mobile = new MobileApplication();
		project.add(mobile);
		var app = new ApplicationComponent();
		mobile.add(app);
		var grid = new UISharedRegularComponent();
		grid.setName("agGrid");
		app.add(grid);
		if (getRows) {
			var event = new UICompEvent();
			event.setName("GetRows");
			grid.add(event);
		}
		if (rowModelType) {
			var variable = new UICompVariable();
			variable.setName("rowModelType");
			grid.add(variable);
		}
		if (updateRows) {
			var action = new UIActionStack();
			action.setName("agGridUpdateRows");
			app.add(action);
		}
		return app;
	}
}
