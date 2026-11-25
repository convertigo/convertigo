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

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.engine.CLI;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.FilePropertyManager;
import com.twinsoft.convertigo.engine.JdbcConnectionManager;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;

/**
 * Manual end-to-end run of the SQL CRUD user interface generator, without the Studio: creates an NGX project from
 * the template of the new project wizard, adds SQL connectors, runs what the Design tab runs for their tables, then
 * builds the NGX application like the Mobile Builder (npm and ng) in a projects folder that an engine can load.
 *
 * System properties: crud.e2e.projects (projects folder), crud.e2e.connectors, a list of
 * name|driver|generationUrl|runtimeUrl|user|password|TABLE,TABLE separated by "#", crud.e2e.build (false to skip npm).
 */
public final class SqlCrudEndToEnd {
	private static final String TEMPLATE = "https://github.com/convertigo/c8oprj-template-ngx-builder/archive/8.4.0.zip";
	private static final List<String> CRUDS = List.of("LIST", "INSERT", "SELECT", "UPDATE", "DELETE");

	public static void main(String[] args) throws Exception {
		var projects = new File(System.getProperty("crud.e2e.projects")).getCanonicalFile();
		var name = System.getProperty("crud.e2e.project", "crudtest");
		projects.mkdirs();
		Engine.PROJECTS_PATH = projects.getPath();
		var checkInit = CLI.class.getDeclaredMethod("checkInit");
		checkInit.setAccessible(true);
		checkInit.invoke(CLI.instance);
		Engine.theApp.sqlConnectionManager = new JdbcConnectionManager();
		Engine.theApp.sqlConnectionManager.init();
		Engine.theApp.filePropertyManager = new FilePropertyManager();

		// what the new project wizard does for an NGX project
		var project = Engine.theApp.databaseObjectsManager.existsProject(name)
				? Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name, false)
				: Engine.theApp.referencedProjectManager.importProject(new ProjectUrlParser(name + "=" + TEMPLATE), true, true);
		log("project " + project.getName() + " in " + project.getDirPath());

		var report = new StringBuilder();
		var runtimeUrls = new ArrayList<String[]>();
		for (var definition : System.getProperty("crud.e2e.connectors").split("#")) {
			var parts = definition.split("\\|", -1);
			SqlConnector connector = null;
			try {
				connector = (SqlConnector) project.getConnectorByName(parts[0]);
			} catch (Exception e) {
				// a new connector
			}
			if (connector == null) {
				connector = new SqlConnector();
				connector.setName(parts[0]);
				project.add(connector);
			}
			connector.setJdbcDriverClassName(parts[1]);
			connector.setJdbcURL(parts[2]);
			connector.setJdbcUserName(parts[4]);
			connector.setJdbcUserPassword(parts[5]);
			if (parts[1].contains("hsqldb")) {
				// the validation query of the pool needs a FROM on HSQLDB
				connector.setSystemTablesQuery("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS");
			}
			runtimeUrls.add(new String[] { parts[0], parts[3] });
			var tables = List.of(parts[6].split(","));

			// the Design tab, "Generate NGX shared component" checked, "Generate Sequences" unchecked, Hidden, not authenticated
			for (var table : tables) {
				for (var transaction : SqlConnector.createSqlTransaction(connector, table, CRUDS)) {
					var added = SqlCrudGeneration.addTransaction(connector, transaction, false);
					if (added != null && !SqlCrudGeneration.hasSequence(added)) {
						SqlCrudGeneration.createSequence(added, Accessibility.Hidden, false);
					}
				}
			}
			NgxCrudLibrary.importIfMissing(project);
			var problem = NgxCrudLibrary.check();
			if (problem != null) {
				throw new IllegalStateException(problem);
			}
			report.append("== ").append(connector.getName()).append('\n')
				.append(SqlCrudGeneration.generateUserInterface(project, connector, tables, true)).append('\n');
			// a second generation never overwrites
			report.append("== ").append(connector.getName()).append(", again\n")
				.append(SqlCrudGeneration.generateUserInterface(project, connector, tables, true)).append('\n');
			connector.close();
		}
		log(report.toString());
		Files.writeString(new File(projects.getParentFile(), "generation-report.txt").toPath(), report);

		// a developer can switch a grid to pages: the products page does it with the pagination variable
		var pageNames = System.getProperty("crud.e2e.paginated", "");
		for (var pageName : pageNames.split(",")) {
			if (pageName.isBlank()) {
				continue;
			}
			var app = NgxCrudGenerator.ngxApplication(project);
			for (var page : app.getPageComponentList()) {
				if (page.getName().equals(pageName)) {
					for (var dbo : page.getAllChildren()) {
						for (var use : dbo.getAllChildren()) {
							if (use instanceof com.twinsoft.convertigo.beans.ngx.components.UIUseShared shared) {
								var variable = new com.twinsoft.convertigo.beans.ngx.components.UIUseVariable();
								variable.setName("pagination");
								variable.bNew = true;
								variable.setVarSmartType(NgxCrudGenerator.script("true"));
								shared.add(variable);
								log("pagination=true on the page " + pageName);
							}
						}
					}
				}
			}
		}

		// the engine that serves the application reaches the databases at other addresses
		for (var url : runtimeUrls) {
			((SqlConnector) project.getConnectorByName(url[0])).setJdbcURL(url[1]);
		}
		CLI.instance.export(project);
		log("project saved");

		if (!"false".equals(System.getProperty("crud.e2e.build"))) {
			CLI.instance.generateMobileBuilder(project, "production");
			log("NGX sources generated");
			// a node_modules folder of a previous run spares a full npm install
			var cache = System.getProperty("crud.e2e.node_modules");
			var nodeModules = new File(project.getDirPath(), "_private/ionic/node_modules");
			if (cache != null && new File(cache).isDirectory() && !nodeModules.exists()) {
				Files.move(new File(cache).toPath(), nodeModules.toPath());
			}
			CLI.instance.compileMobileBuilder(project, "production");
			log("NGX application built in " + new File(project.getDirPath(), "DisplayObjects/mobile"));
		}
		System.exit(0);
	}

	private static void log(String message) {
		System.out.println("[crud-e2e] " + message);
	}
}
