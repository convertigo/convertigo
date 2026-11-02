/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.DriverManager;
import java.util.Comparator;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Project;

/**
 * A pooled HSQLDB file database stays open between the transactions of its connector, and is shut down when its pool
 * is removed (project unloaded or deployed again, engine stopped).
 */
public class HsqldbPoolShutdownTest {
	private Engine previousEngine;
	private String previousProjectsPath;
	private Path root;
	private Path database;
	private Project project;
	private SqlConnector connector;

	@Before
	public void setUp() throws Exception {
		EnginePropertiesManager.initProperties();
		Engine.logEngine = Engine.logBeans = Engine.logDatabaseObjectManager = Logger.getLogger("hsqldb-pool-test");
		previousEngine = Engine.theApp;
		previousProjectsPath = Engine.PROJECTS_PATH;
		root = Files.createTempDirectory("convertigo-hsqldb-test-");
		Engine.PROJECTS_PATH = Files.createDirectory(root.resolve("projects")).toString();
		database = Files.createDirectories(root.resolve("projects/HsqlDemo/database"));
		Engine.theApp = new Engine();
		Engine.theApp.sqlConnectionManager = new JdbcConnectionManager();
		Engine.theApp.sqlConnectionManager.init();
		Engine.theApp.filePropertyManager = new FilePropertyManager();
		project = new Project();
		project.setName("HsqlDemo");
		connector = new SqlConnector();
		connector.setName("db");
		connector.setJdbcDriverClassName("org.hsqldb.jdbcDriver");
		connector.setJdbcURL("jdbc:hsqldb:file:.//database/demo;shutdown=true");
		connector.setJdbcUserName("sa");
		project.add(connector);
	}

	@After
	public void tearDown() throws Exception {
		if (Engine.theApp.sqlConnectionManager != null) {
			Engine.theApp.sqlConnectionManager.removeDatabasePools(project);
		}
		Engine.theApp = previousEngine;
		Engine.PROJECTS_PATH = previousProjectsPath;
		try (var paths = Files.walk(root)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
		}
	}

	/** The copy of the connector a transaction runs with, as a context gets it. */
	private SqlConnector transaction() throws Exception {
		var copy = connector.clone();
		copy.setParent(project);
		return copy;
	}

	@Test
	public void theEndOfATransactionLeavesTheDatabaseOpenForTheOthers() throws Exception {
		var first = transaction();
		var second = transaction();
		first.open();
		first.connection.createStatement().execute("CREATE TABLE T (V VARCHAR(10))");
		first.connection.createStatement().execute("INSERT INTO T VALUES ('one')");
		second.open();
		first.close();
		try (var rs = second.connection.createStatement().executeQuery("SELECT COUNT(*) FROM T")) {
			assertTrue(rs.next());
			assertEquals(1, rs.getInt(1));
		}
		second.close();
		assertTrue("the database stays open after the transactions", Files.exists(database.resolve("demo.lck")));
	}

	@Test
	public void removingThePoolReleasesTheFilesForANewVersion() throws Exception {
		var transaction = transaction();
		transaction.open();
		transaction.connection.createStatement().execute("CREATE TABLE T (V VARCHAR(10))");
		transaction.connection.createStatement().execute("INSERT INTO T VALUES ('one')");
		transaction.close();
		Engine.theApp.sqlConnectionManager.removeDatabasePools(project);
		assertFalse("the database is shut down with its pool", Files.exists(database.resolve("demo.lck")));

		var other = Files.createDirectories(root.resolve("other"));
		try (var connection = DriverManager.getConnection("jdbc:hsqldb:file:" + other.resolve("demo") + ";shutdown=true", "sa", "")) {
			connection.createStatement().execute("CREATE TABLE T (V VARCHAR(10))");
			connection.createStatement().execute("INSERT INTO T VALUES ('two')");
		}
		Files.deleteIfExists(database.resolve("demo.log"));
		for (var name : new String[] {"demo.script", "demo.properties"}) {
			Files.copy(other.resolve(name), database.resolve(name), StandardCopyOption.REPLACE_EXISTING);
		}

		var next = transaction();
		next.open();
		try (var rs = next.connection.createStatement().executeQuery("SELECT V FROM T")) {
			assertTrue(rs.next());
			assertEquals("the new files are read, not the previous database", "two", rs.getString(1));
			assertFalse(rs.next());
		}
		next.close();
	}

	@Test
	public void stoppingTheEngineShutsTheDatabaseDown() throws Exception {
		var transaction = transaction();
		transaction.open();
		transaction.connection.createStatement().execute("CREATE TABLE T (V VARCHAR(10))");
		transaction.close();
		assertTrue(Files.exists(database.resolve("demo.lck")));
		Engine.theApp.sqlConnectionManager.destroy();
		Engine.theApp.sqlConnectionManager = null;
		assertFalse(Files.exists(database.resolve("demo.lck")));
	}

	@Test
	public void aConnectionOutsideAPoolStillShutsTheDatabaseDown() throws Exception {
		connector.setConnectionPool(false);
		var transaction = transaction();
		transaction.open();
		transaction.connection.createStatement().execute("CREATE TABLE T (V VARCHAR(10))");
		transaction.close();
		assertFalse(Files.exists(database.resolve("demo.lck")));
	}
}
