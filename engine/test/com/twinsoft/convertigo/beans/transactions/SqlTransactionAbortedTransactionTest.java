/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.beans.transactions;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.JdbcConnectionManager;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * A query that fails inside a database transaction reports its own error, not the refusal of a retry.
 * <p>
 * PostgreSQL refuses every statement of a transaction after an error ("current transaction is aborted, commands ignored
 * until end of transaction block") until it ends. The test runs on an in-memory HSQLDB database behind a driver that
 * applies this rule, or on a real PostgreSQL server with -Dconvertigo.test.postgresql.url=jdbc:postgresql://host/db
 * (and -Dconvertigo.test.postgresql.user, postgres by default).
 */
public class SqlTransactionAbortedTransactionTest {
	private static final String ABORTED = "current transaction is aborted";
	private static final String POSTGRESQL_URL = System.getProperty("convertigo.test.postgresql.url");

	private Engine previousEngine;
	private Project project;
	private SqlConnector connector;
	private Context context;
	private int transactions = 0;

	@Before
	public void setUp() throws Exception {
		EnginePropertiesManager.initProperties();
		Engine.logEngine = Engine.logBeans = Engine.logContext = Engine.logDatabaseObjectManager = Engine.logStatistics =
				org.apache.log4j.Logger.getLogger("sql-aborted-transaction-test");
		previousEngine = Engine.theApp;
		Engine.theApp = new Engine();
		Engine.theApp.sqlConnectionManager = new JdbcConnectionManager();
		Engine.theApp.sqlConnectionManager.init();
		project = new Project();
		project.setName("SqlAborted");
		connector = new SqlConnector();
		connector.setName("db");
		if (POSTGRESQL_URL != null) {
			connector.setJdbcDriverClassName("org.postgresql.Driver");
			connector.setJdbcURL(POSTGRESQL_URL);
			connector.setJdbcUserName(System.getProperty("convertigo.test.postgresql.user", "postgres"));
		} else {
			connector.setJdbcDriverClassName(AbortingDriver.class.getName());
			connector.setJdbcURL(AbortingDriver.PREFIX + "hsqldb:mem:sqlaborted" + System.nanoTime());
			connector.setJdbcUserName("SA");
			connector.setSystemTablesQuery("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS");
		}
		project.add(connector);
		context = new Context("sql-aborted-transaction-test");
		context.project = project;
		context.setConnector(connector);
		assertNull(error(run("DROP TABLE IF EXISTS items;"
				+ "CREATE TABLE items (id integer PRIMARY KEY, label varchar(20) NOT NULL);"
				+ "INSERT INTO items VALUES (1, 'one')", SqlTransaction.AUTOCOMMIT_EACH)));
	}

	@After
	public void tearDown() throws Exception {
		connector.close();
		Engine.theApp.sqlConnectionManager.destroy();
		Engine.theApp = previousEngine;
	}

	@Test
	public void aFailingQueryInsideATransactionReportsItsOwnError() throws Exception {
		for (int autoCommit : new int[] {SqlTransaction.AUTOCOMMIT_END, SqlTransaction.AUTOCOMMIT_OFF}) {
			String error = error(run("INSERT INTO items VALUES (2, 'two');"
					+ "INSERT INTO items VALUES (1, 'duplicate');"
					+ "UPDATE items SET label = 'changed' WHERE id = 1", autoCommit));
			assertNotNull("the duplicate key fails (auto-commit " + autoCommit + ")", error);
			assertFalse("the error of the query is reported, not the refusal of a retry: " + error, error.contains(ABORTED));
			assertEquals("the transaction is rolled back and the next one works (auto-commit " + autoCommit + ")",
					List.of("1:one"), list(autoCommit));
		}
	}

	@Test
	public void aSuccessfulTransactionIsCommitted() throws Exception {
		assertNull(error(run("INSERT INTO items VALUES (2, 'two');UPDATE items SET label = 'deux' WHERE id = 2",
				SqlTransaction.AUTOCOMMIT_END)));
		connector.close();
		assertEquals(List.of("1:one", "2:deux"), list(SqlTransaction.AUTOCOMMIT_EACH));
	}

	private List<String> list(int autoCommit) throws Exception {
		Document output = run("SELECT id, label FROM items ORDER BY id", autoCommit);
		assertNull("the list works", error(output));
		List<String> rows = new ArrayList<>();
		NodeList rowElements = output.getElementsByTagName("row");
		for (int i = 0; i < rowElements.getLength(); i++) {
			// id and label, named in upper case by HSQLDB
			List<String> values = new ArrayList<>();
			for (var child = rowElements.item(i).getFirstChild(); child != null; child = child.getNextSibling()) {
				if (child instanceof Element) {
					values.add(child.getTextContent());
				}
			}
			rows.add(String.join(":", values));
		}
		return rows;
	}

	/** Runs a transaction of the context as the engine does, on the connector of the context. */
	private Document run(String sqlQuery, int autoCommit) throws Exception {
		SqlTransaction transaction = new SqlTransaction();
		transaction.setName("transaction" + (++transactions));
		transaction.setSqlQuery(sqlQuery);
		transaction.setAutoCommit(autoCommit);
		connector.add(transaction);
		Document output = XMLUtils.getDefaultDocumentBuilder().newDocument();
		output.appendChild(output.createElement("document"));
		context.outputDocument = output;
		context.requestedObject = context.transaction = transaction;
		transaction.context = context;
		transaction.createRequestableThread();
		transaction.runCore();
		return output;
	}

	private static String error(Document output) {
		NodeList errors = output.getElementsByTagName("error");
		if (errors.getLength() == 0) {
			return null;
		}
		NodeList message = ((Element) errors.item(0)).getElementsByTagName("message");
		return message.getLength() > 0 ? message.item(0).getTextContent() : "";
	}

	/**
	 * Opens HSQLDB connections that follow the PostgreSQL rule: after an error inside a transaction (auto-commit off, or
	 * a BEGIN statement with auto-commit on), every statement is refused until the transaction ends.
	 */
	public static class AbortingDriver implements Driver {
		static final String PREFIX = "jdbc:aborting:";

		@Override
		public Connection connect(String url, Properties info) throws SQLException {
			if (!acceptsURL(url)) {
				return null;
			}
			Connection database = new org.hsqldb.jdbc.JDBCDriver().connect("jdbc:" + url.substring(PREFIX.length()), info);
			database.setAutoCommit(false);
			return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Connection.class},
					new AbortingConnection(database));
		}

		@Override
		public boolean acceptsURL(String url) {
			return url != null && url.startsWith(PREFIX);
		}

		@Override
		public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
			return new DriverPropertyInfo[0];
		}

		@Override
		public int getMajorVersion() {
			return 1;
		}

		@Override
		public int getMinorVersion() {
			return 0;
		}

		@Override
		public boolean jdbcCompliant() {
			return false;
		}

		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			throw new SQLFeatureNotSupportedException();
		}
	}

	private static class AbortingConnection implements InvocationHandler {
		private final Connection database;
		private boolean autoCommit = true;
		private boolean block = false;
		private boolean aborted = false;

		AbortingConnection(Connection database) {
			this.database = database;
		}

		private boolean inTransaction() {
			return !autoCommit || block;
		}

		private void end(boolean commit) throws SQLException {
			if (commit && !aborted) {
				database.commit();
			} else {
				database.rollback();
			}
			aborted = block = false;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch (method.getName()) {
			case "getAutoCommit":
				return autoCommit;
			case "setAutoCommit":
				if ((Boolean) args[0] && !autoCommit) {
					end(true);
				}
				autoCommit = (Boolean) args[0];
				return null;
			case "commit":
			case "rollback":
				if (autoCommit) {
					throw new SQLException("Cannot " + method.getName() + " when autoCommit is enabled.");
				}
				end(method.getName().equals("commit"));
				return null;
			case "close":
				if (!database.isClosed()) {
					database.rollback();
					database.close();
				}
				return null;
			case "isValid":
				return !database.isClosed();
			case "createStatement":
			case "prepareStatement":
			case "prepareCall":
				String sql = args != null && args.length > 0 && args[0] instanceof String s ? s : null;
				boolean begin = sql != null && sql.trim().replaceFirst(";$", "").trim().equalsIgnoreCase("BEGIN");
				if (begin) {
					args = args.clone();
					args[0] = "SET SCHEMA PUBLIC";
				}
				Statement statement = (Statement) call(database, method, args);
				return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {method.getReturnType()},
						(statementProxy, statementMethod, statementArgs) -> {
							if (!statementMethod.getName().startsWith("execute")) {
								return call(statement, statementMethod, statementArgs);
							}
							if (aborted) {
								throw new SQLException("ERROR: " + ABORTED + ", commands ignored until end of transaction block", "25P02");
							}
							try {
								Object result = call(statement, statementMethod, statementArgs);
								if (begin) {
									block = true;
								} else if (!inTransaction()) {
									database.commit();
								}
								return result;
							} catch (SQLException e) {
								if (inTransaction()) {
									aborted = true;
								} else {
									database.rollback();
								}
								throw e;
							}
						});
			default:
				return call(database, method, args);
			}
		}

		private static Object call(Object target, Method method, Object[] args) throws Throwable {
			try {
				return method.invoke(target, args);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}
	}
}
