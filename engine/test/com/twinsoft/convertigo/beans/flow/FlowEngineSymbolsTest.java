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

package com.twinsoft.convertigo.beans.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

/**
 * The global symbols of the Engine source (engine.yaml), which a running Flow reads resolved, are declared as those
 * of the properties: an undefined one without default marks the FlowEngine, one with a default is listed among the
 * default symbols of the project.
 */
public class FlowEngineSymbolsTest {
	private static Engine previous;
	private static final Properties symbols = new Properties();

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@BeforeClass
	public static void initialize() throws Exception {
		previous = Engine.theApp;
		Engine.logBeans = Engine.logEngine = Engine.logDatabaseObjectManager = Logger.getLogger("flow-engine-symbols-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			{
				symbolsProperties = symbols;
			}
		};
	}

	@AfterClass
	public static void restore() {
		Engine.theApp = previous;
	}

	private FlowEngine engine(File directory) {
		var project = new Project() {
			@Override
			public File getDirFile() {
				return directory;
			}
		};
		return new FlowEngine() {
			@Override
			public Project getProject() {
				return project;
			}
		};
	}

	private static final String SOURCE = "version: 1\nconfigs:\n  lab:\n    sql:\n      connections:\n        pg:\n"
			+ "          user: \"${test.pg.user=reader}\"\n          password: \"${test.pg.password.secret}\"\n";

	@Test
	public void anUndefinedSymbolWithoutDefaultMarksTheEngineUntilItIsDefined() throws Exception {
		symbols.clear();
		var engine = engine(folder.newFolder());
		engine.setEngineSource(SOURCE);
		assertTrue("the undefined symbol marks the FlowEngine", engine.isSymbolError());
		assertEquals(Set.of("test.pg.password.secret"), engine.getSymbolsErrors().get("engineSource"));

		symbols.setProperty("test.pg.password.secret", "s3cr3t");
		engine.updateSymbols();
		assertFalse("once declared, the mark goes", engine.isSymbolError());
	}

	@Test
	public void aSymbolWithDefaultIsListedAmongTheDefaultSymbols() throws Exception {
		symbols.clear();
		symbols.setProperty("test.pg.password.secret", "s3cr3t");
		var engine = engine(folder.newFolder());
		engine.setEngineSource(SOURCE);
		var data = DatabaseObjectsManager.getProjectLoadingData();
		data.defaultSymbols = new TreeSet<>();
		try {
			engine.updateSymbols();
			assertTrue("listed with its default, as the symbols of the properties: " + data.defaultSymbols,
					data.defaultSymbols.contains(Pair.of("test.pg.user", "reader")));
			assertFalse(engine.isSymbolError());
		} finally {
			data.defaultSymbols = null;
		}
	}

	@Test
	public void aSourceWithoutSymbolIsNotMarked() throws Exception {
		symbols.clear();
		var engine = engine(folder.newFolder());
		engine.setEngineSource("version: 1\nconfig:\n  greeting: \"Hello\"\n");
		assertFalse(engine.isSymbolError());
	}
}
