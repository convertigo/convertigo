/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.admin.services.studio.dbo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

/**
 * A default connector or transaction moved or copied to another parent by the console is not the default
 * one there, and its previous parent has no default anymore (#1225).
 */
public class DefaultPasteTest {
	private Engine previousEngine;

	@BeforeClass
	public static void initializeLogging() {
		Engine.logBeans = Logger.getLogger("default-paste-test");
		Engine.logEngine = Engine.logStudio = Engine.logAdmin = Engine.logDatabaseObjectManager = Engine.logBeans;
	}

	@Before
	public void installEngine() throws Exception {
		EnginePropertiesManager.initProperties();
		previousEngine = Engine.theApp;
		Engine.theApp = new Engine();
		// the copies read their properties without global symbols
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			@Override
			public Object getCompiledValue(Object propertyObjectValue) {
				return propertyObjectValue;
			}
		};
	}

	@After
	public void restoreEngine() {
		Engine.theApp = previousEngine;
	}

	@Test
	public void theDefaultConnectorCanBeCutAndIsNotTheDefaultOfItsNewProject() throws Exception {
		var source = project("Source");
		var moved = connector(source, "Moved", "one", "two");
		connector(source, "Other", "three");
		source.setDefaultConnector(moved);
		moved.setDefaultTransaction(moved.getTransactionByName("two"));
		var target = project("Target");
		var kept = connector(target, "Kept", "four");
		target.setDefaultConnector(kept);

		assertTrue(DboUtils.canCut(moved));
		move(moved, target);

		assertFalse(moved.isDefault);
		assertSame(target, moved.getParent());
		assertSame("the default of the target is kept", kept, target.getDefaultConnector());
		assertNull("the source has no default anymore", source.getDefaultConnector());
		assertSame("the transactions of the moved connector keep their default", moved.getTransactionByName("two"),
				moved.getDefaultTransaction());
	}

	@Test
	public void theDefaultTransactionCanBeCutAndIsNotTheDefaultOfItsNewConnector() throws Exception {
		var project = project("Project");
		var source = connector(project, "Source", "moved", "other");
		var target = connector(project, "Target", "kept");
		var moved = source.getTransactionByName("moved");
		source.setDefaultTransaction(moved);
		target.setDefaultTransaction(target.getTransactionByName("kept"));

		assertTrue(DboUtils.canCut(moved));
		move(moved, target);

		assertFalse(moved.isDefault);
		assertSame(target.getTransactionByName("kept"), target.getDefaultTransaction());
		assertNull(source.getDefaultTransaction());
		assertSame("the only transaction left runs implicitly", source.getTransactionByName("other"),
				source.getImplicitTransaction());
	}

	@Test
	public void aFailedMoveGivesBackTheDefaultState() throws Exception {
		var project = project("Project");
		var connector = connector(project, "Connector", "one");
		project.setDefaultConnector(connector);

		var wasDefault = DboUtils.dropDefault(connector);
		connector.delete();
		DboUtils.restoreDefault(connector, wasDefault);
		project.add(connector);
		assertTrue(connector.isDefault);
		assertSame(connector, project.getDefaultConnector());
	}

	@Test
	public void theCopyOfADefaultConnectorIsNotTheDefaultOfItsProject() throws Exception {
		var project = project("Project");
		var original = connector(project, "Original", "one", "two");
		project.setDefaultConnector(original);
		original.setDefaultTransaction(original.getTransactionByName("two"));

		var copy = (Connector) DboFactory.copyOf(original);
		assertFalse(copy.isDefault);
		copy.setName("Copy");
		project.add(copy);
		assertSame("the original stays the default connector", original, project.getDefaultConnector());
		assertTrue("the copy keeps the default transaction it holds", copy.getTransactionByName("two").isDefault);
		assertSame(copy.getTransactionByName("two"), copy.getDefaultTransaction());
	}

	@Test
	public void theCopyOfADefaultTransactionIsNotTheDefaultOfItsConnector() throws Exception {
		var project = project("Project");
		var connector = connector(project, "Connector", "original");
		var original = connector.getTransactionByName("original");
		connector.setDefaultTransaction(original);

		var copy = (Transaction) DboFactory.copyOf(original);
		assertFalse(copy.isDefault);
		copy.setName("copy");
		connector.add(copy);
		assertSame(original, connector.getDefaultTransaction());
	}

	/**
	 * Moves the object to another parent, as the Move and Paste services of the console do.
	 */
	private static void move(DatabaseObject dbo, DatabaseObject target) throws EngineException {
		DboUtils.dropDefault(dbo);
		dbo.delete();
		target.add(dbo);
	}

	private static Project project(String name) throws EngineException {
		var project = new Project();
		project.setName(name);
		return project;
	}

	private static Connector connector(Project project, String name, String... transactions) throws EngineException {
		var connector = new SqlConnector();
		connector.setName(name);
		project.add(connector);
		for (var transactionName : transactions) {
			var transaction = new SqlTransaction();
			transaction.setName(transactionName);
			connector.add(transaction);
		}
		return connector;
	}
}
