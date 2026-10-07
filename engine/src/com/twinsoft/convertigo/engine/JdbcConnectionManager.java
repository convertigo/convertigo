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

package com.twinsoft.convertigo.engine;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.naming.NamingException;

import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;

/**
 * The JDBC connections of the SQL connectors. A driver provided by the libraries of a project belongs to the generation
 * of its class path (see {@link ProjectLibraries}): each project connects with its own version of a driver, and the
 * pools and drivers of a replaced generation are released once it is retired. Drivers of the engine are shared and
 * reached through DriverManager.
 */
public class JdbcConnectionManager implements AbstractManager, ProjectLibraries.Holder {

	private static Class<?> dataSourceCls = null;
	private static Method dataSourceClose = null;
	private static Method dataSourceSetDriverClassLoader = null;
	private static Method dataSourceSetDriverClassName = null;
	private static Method dataSourceSetUrl = null;
	private static Method dataSourceSetUsername = null;
	private static Method dataSourceSetPassword = null;
	private static Method dataSourceSetMaxActive = null;
	private static Method dataSourceSetValidationQuery = null;
	private static Method dataSourceSetTestOnBorrow = null;
	private static Method dataSourceSetTestOnReturn = null;
	private static Method dataSourceSetTestWhileIdle = null;
	private static Method dataSourceSetTimeBetweenEvictionRunsMillis = null;
	private static Method dataSourceSetNumTestsPerEvictionRun = null;
	private static Method dataSourceGetConnection = null;
	private static Method dataSourceGetNumActive = null;
	private static Method dataSourceGetMaxActive = null;
	private static Method dataSourceGetNumIdle = null;
	private static Method dataSourceGetMaxIdle = null;
	/** Pools by connector and generation of the class path of its project. */
	private Map<String, Object> databasePools;
	/** Drivers of the engine, registered once. */
	private Set<String> driversLoaded;
	/** Drivers of the libraries of the projects, by generation and class name. */
	private final Map<String, Map<String, Driver>> projectDrivers = new ConcurrentHashMap<>();
	/** Registered in DriverManager for the code of the projects that uses it, by generation. */
	private final Map<String, List<Driver>> projectShims = new ConcurrentHashMap<>();
	
	public JdbcConnectionManager() {
	}

	public void init() throws EngineException {
		databasePools = new HashMap<>(2048);
		driversLoaded = new HashSet<>();
		ProjectLibraries.addHolder(this);
		try {
			try {
				dataSourceCls = Class.forName("org.apache.tomcat.dbcp.dbcp2.BasicDataSource");
				dataSourceSetMaxActive = dataSourceCls.getMethod("setMaxOpenPreparedStatements", int.class);
				dataSourceGetMaxActive = dataSourceCls.getMethod("getMaxOpenPreparedStatements");
			} catch (ClassNotFoundException e) {
				dataSourceCls = Class.forName("org.apache.tomcat.dbcp.dbcp.BasicDataSource");
				dataSourceSetMaxActive = dataSourceCls.getMethod("setMaxActive", int.class);
				dataSourceGetMaxActive = dataSourceCls.getMethod("getMaxActive");
			}
			dataSourceClose = dataSourceCls.getMethod("close");
			dataSourceSetDriverClassLoader = dataSourceCls.getMethod("setDriverClassLoader", ClassLoader.class);
			dataSourceSetDriverClassName = dataSourceCls.getMethod("setDriverClassName", String.class);
			dataSourceSetUrl = dataSourceCls.getMethod("setUrl", String.class);
			dataSourceSetUsername = dataSourceCls.getMethod("setUsername", String.class);
			dataSourceSetPassword = dataSourceCls.getMethod("setPassword", String.class);
			dataSourceSetValidationQuery = dataSourceCls.getMethod("setValidationQuery", String.class);
			dataSourceSetTestOnBorrow = dataSourceCls.getMethod("setTestOnBorrow", boolean.class);
			dataSourceSetTestOnReturn = dataSourceCls.getMethod("setTestOnReturn", boolean.class);
			dataSourceSetTestWhileIdle = dataSourceCls.getMethod("setTestWhileIdle", boolean.class);
			dataSourceSetTimeBetweenEvictionRunsMillis = dataSourceCls.getMethod("setTimeBetweenEvictionRunsMillis", long.class);
			dataSourceSetNumTestsPerEvictionRun = dataSourceCls.getMethod("setNumTestsPerEvictionRun", int.class);
			
			dataSourceGetConnection = dataSourceCls.getMethod("getConnection");
			dataSourceGetNumActive = dataSourceCls.getMethod("getNumActive");
			dataSourceGetNumIdle = dataSourceCls.getMethod("getNumIdle");
			dataSourceGetMaxIdle = dataSourceCls.getMethod("getMaxIdle");
			Engine.logEngine.info("(JdbcConnectionManager) Init done"); 
		} catch (Exception e) {
			Engine.logEngine.error("(JdbcConnectionManager) Init failed", e); 
		}
	}
	
	public void destroy() throws EngineException {
		ProjectLibraries.removeHolder(this);
		synchronized (this) {
			for (var poolKey : new ArrayList<>(databasePools.keySet())) {
				closePool(poolKey);
			}
			databasePools = null;
		}
		for (var generation : new ArrayList<>(projectShims.keySet())) {
			deregisterDrivers(generation);
		}
	}

	private void closePool(String poolKey) {
		Engine.logEngine.debug("[SqlConnectionManager] Closing datasource '" + poolKey + "'...");
		try {
			dataSourceClose.invoke(databasePools.get(poolKey));
			Engine.logEngine.debug("[SqlConnectionManager] Datasource '" + poolKey + "' closed.");
		} catch (Exception e) {
			Engine.logEngine.debug("[SqlConnectionManager] Datasource '" + poolKey + "' close failure ! ");
		}
		databasePools.remove(poolKey);
	}

	/** Closes the pools of a connector, whatever the generation of the class path of its project. */
	public synchronized void removeDatabasePool(SqlConnector connector) {
		if (databasePools == null) {
			return;
		}
		var prefix = connector.getQName() + "\n";
		for (var poolKey : new ArrayList<>(databasePools.keySet())) {
			if (poolKey.startsWith(prefix)) {
				closePool(poolKey);
			}
		}
	}

	@Override
	public synchronized boolean inUse(ProjectClassLoader generation) {
		if (databasePools == null) {
			return false;
		}
		var suffix = "\n" + generation.getId();
		for (var poolEntry : databasePools.entrySet()) {
			try {
				if (poolEntry.getKey().endsWith(suffix) && ((Number) dataSourceGetNumActive.invoke(poolEntry.getValue())).intValue() > 0) {
					return true;
				}
			} catch (Exception e) {
				// released anyway at the time limit
			}
		}
		return false;
	}

	/** A retired generation: its pools are closed and its drivers deregistered. */
	@Override
	public void release(ProjectClassLoader generation) {
		synchronized (this) {
			if (databasePools != null) {
				var suffix = "\n" + generation.getId();
				for (var poolKey : new ArrayList<>(databasePools.keySet())) {
					if (poolKey.endsWith(suffix)) {
						closePool(poolKey);
					}
				}
			}
		}
		deregisterDrivers(generation.getId());
		try {
			var count = generation.defineEngineClass(ProjectDriversCleanup.class).getMethod("deregister").invoke(null);
			Engine.logEngine.debug("(JdbcConnectionManager) JDBC drivers registered by the classes of " + generation + ": " + count + " deregistered");
		} catch (Exception e) {
			Engine.logEngine.debug("(JdbcConnectionManager) Unable to deregister the JDBC drivers registered by the classes of " + generation, e);
		}
	}

	private void deregisterDrivers(String generation) {
		projectDrivers.remove(generation);
		var shims = projectShims.remove(generation);
		if (shims != null) {
			for (var shim : shims) {
				try {
					DriverManager.deregisterDriver(shim);
				} catch (SQLException e) {
					Engine.logEngine.debug("(JdbcConnectionManager) Unable to deregister a JDBC driver of " + generation, e);
				}
			}
		}
	}

	/**
	 * Closes the pools of the SQL connectors of an unloaded version of a project: the next
	 * version reuses the same keys and would otherwise keep the previous connection settings.
	 */
	public void removeDatabasePools(Project project) {
		if (databasePools == null) {
			return;
		}
		for (Connector connector : project.getConnectorsList()) {
			if (connector instanceof SqlConnector sqlConnector) {
				removeDatabasePool(sqlConnector);
			}
		}
	}

	private Object addDatabasePool(SqlConnector connector) throws Exception {
		Engine.logEngine.debug("(JdbcConnectionManager) Creating a new pool");
		
		Object pool = dataSourceCls.getConstructor().newInstance();
		
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		dataSourceSetDriverClassLoader.invoke(pool, cl);
		dataSourceSetDriverClassName.invoke(pool, connector.getJdbcDriverClassName());
		
		String jdbcURL = connector.getRealJdbcURL();
		Engine.logEngine.debug("(JdbcConnectionManager) JDBC URL: " + jdbcURL);
		dataSourceSetUrl.invoke(pool, jdbcURL);

		String user = connector.getRealJdbcUserName();
		Engine.logEngine.debug("(JdbcConnectionManager) User: " + user);
		dataSourceSetUsername.invoke(pool, user);

		String password = connector.getRealJdbcUserPassword();
		Engine.logEngine.trace("(JdbcConnectionManager) Password: " + password);
		dataSourceSetPassword.invoke(pool, password);

		int maxConnections = connector.getJdbcMaxConnection();
		Engine.logEngine.debug("(JdbcConnectionManager) maxConnections: " + maxConnections);
		dataSourceSetMaxActive.invoke(pool, maxConnections);

		/* Database query to list tables
			*JDBC Drivers
			SQLSERVER	:	SELECT * FROM INFORMATION_SCHEMA.TABLES
			MYSQL		:	SELECT * FROM INFORMATION_SCHEMA.TABLES | SHOW TABLES
			DB2			:	SELECT * FROM SYSCAT.TABLES
			ORACLE		: 	SELECT * FROM ALL_TABLES
			POSTGRES	:	SELECT * FROM pg_tables
			HSQLDB		:	SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES
			
			*JDBC-ODBC Bridge
			DHARMA SDK	: 	SELECT * FROM DHARMA.SYSTABLES | SELECT * FROM SYSTABLES
		 */
	
		String query = connector.getSystemTablesQuery();
		if (query.equals("")) {
			String jdbcDriverClassName = connector.getJdbcDriverClassName();
			/* SQLSERVER (limit to 1 row)*/
			if ("net.sourceforge.jtds.jdbc.Driver".equals(jdbcDriverClassName))
				query = "SELECT TOP 1 * FROM INFORMATION_SCHEMA.TABLES";
			/* MYSQL or MariaDB (limit to 1 row)*/
			else if ("com.mysql.jdbc.Driver".equals(jdbcDriverClassName) ||
					"com.mysql.cj.jdbc.Driver".equals(jdbcDriverClassName) ||
					"org.mariadb.jdbc.Driver".equals(jdbcDriverClassName))
				query = "SELECT * FROM INFORMATION_SCHEMA.TABLES LIMIT 1";
			/* HSQLDB (limit to 1 row)*/
			else if ("org.hsqldb.jdbcDriver".equals(jdbcDriverClassName))
				query = "SELECT TOP 1 * FROM INFORMATION_SCHEMA.SYSTEM_TABLES";
			/* DB2 (limit to 1 row)*/
			else if ("com.ibm.db2.jcc.DB2Driver".equals(jdbcDriverClassName))
				query = "SELECT * FROM SYSCAT.TABLES FETCH FIRST 1 ROWS";
			/* AS400 (limit to 1 row)*/
			else if ("com.ibm.as400.access.AS400JDBCDriver".equals(jdbcDriverClassName))
				query = "SELECT * FROM SYSIBM.SQLSCHEMAS FETCH FIRST 1 ROWS ONLY";
			/* ORACLE (limit 1 row) */
			else if ("oracle.jdbc.driver.OracleDriver".equals(jdbcDriverClassName))
				query = "SELECT 1 FROM DUAL";//"SELECT * FROM ALL_TABLES WHERE ROWNUM <= 1";
			/* Initialize the query by default with no limitation on returned resultset */
			else {
				query = "SELECT 1 AS dbcp_connection_test";
//				query = "SELECT * FROM INFORMATION_SCHEMA.TABLES";
			}
		}
		Engine.logEngine.debug("(JdbcConnectionManager) SQL validation query: " + query);
		dataSourceSetValidationQuery.invoke(pool, query);

		boolean testOnBorrow = connector.getTestOnBorrow();
		Engine.logEngine.debug("(JdbcConnectionManager) testOnBorrow=" + testOnBorrow);
		dataSourceSetTestOnBorrow.invoke(pool, testOnBorrow);
		
		dataSourceSetTestOnReturn.invoke(pool, true);
		dataSourceSetTestWhileIdle.invoke(pool, true);
		
		long timeBetweenEvictionRunsMillis = connector.getIdleConnectionTestTime() * 1000;
		Engine.logEngine.debug("(JdbcConnectionManager) Time between eviction runs millis: " + timeBetweenEvictionRunsMillis);
		dataSourceSetTimeBetweenEvictionRunsMillis.invoke(pool, timeBetweenEvictionRunsMillis);
		
		dataSourceSetNumTestsPerEvictionRun.invoke(pool, 3);
		
		databasePools.put(getKey(connector), pool);
		Engine.logEngine.debug("(JdbcConnectionManager) Pool added");

		return pool;
	}

	private synchronized Object getDatabasePool(SqlConnector connector) throws Exception {
		if (databasePools.containsKey(getKey(connector))) {
			Engine.logEngine.debug("(JdbcConnectionManager) getDatabasePool() returning existing pool");
			return databasePools.get(getKey(connector));
		}
		else {
			Engine.logEngine.debug("(JdbcConnectionManager) getDatabasePool() returning new pool");
			return addDatabasePool(connector);
		}
	}
	
	/** @return the key of the pool of a connector, for the generation of the class path running the request */
	private String getKey(SqlConnector connector) {
		var classLoader = Thread.currentThread().getContextClassLoader();
		return connector.getQName() + "\n" + (classLoader instanceof ProjectClassLoader generation ? generation.getId() : "");
	}
	
	public Connection getConnection(SqlConnector connector) throws Exception {
		Connection connection;
		Engine.logEngine.debug("(JdbcConnectionManager) Trying to get a SQL connection...");
		
		if ((connection = connector.getJNDIConnection()) != null) {
			Engine.logEngine.debug("(JdbcConnectionManager) getJNDIConnection for "
					+ connector.getProject().getName() + "." + connector.getName());
		} else {
			// Attempt to load the database driver
			String jdbcDriverClassName = connector.getJdbcDriverClassName();
			Engine.logEngine.debug("(JdbcConnectionManager) JDBC driver: " + jdbcDriverClassName);
			Driver projectDriver;
			try {
				projectDriver = loadDriver(jdbcDriverClassName);
			} catch (ClassNotFoundException e) {
				throw e;
			} catch (SQLException e) {
				throw e;
			} catch (NamingException e) {
				throw e;
			}catch (Exception e) {
				throw new ClassNotFoundException("Failed to load the JDBC driver: " + jdbcDriverClassName, e);
			}

			Engine.logEngine.debug("(JdbcConnectionManager) JDBC driver loaded");
			if (connector.getConnectionPool()) {
				Engine.logEngine.debug("(JdbcConnectionManager) getConnection for "
						+ connector.getProject().getName() + "." + connector.getName());

				Object pool = getDatabasePool(connector);
				Engine.logEngine.debug("(JdbcConnectionManager) pool = " + pool);
				Engine.logEngine.debug("(JdbcConnectionManager)    active connection(s): " + dataSourceGetNumActive.invoke(pool) + "/" + dataSourceGetMaxActive.invoke(pool));
				Engine.logEngine.debug("(JdbcConnectionManager)    idle connection(s):   " + dataSourceGetNumIdle.invoke(pool) + "/" + dataSourceGetMaxIdle.invoke(pool));

				connection = (Connection) dataSourceGetConnection.invoke(pool);
				Engine.logEngine.debug("(JdbcConnectionManager) pooled connection = " + connection);
				Engine.logEngine.debug("(JdbcConnectionManager)    active connection(s): " + dataSourceGetNumActive.invoke(pool) + "/" + dataSourceGetMaxActive.invoke(pool));
				Engine.logEngine.debug("(JdbcConnectionManager)    idle connection(s):   " + dataSourceGetNumIdle.invoke(pool) + "/" + dataSourceGetMaxIdle.invoke(pool));
			} else {

				String jdbcURL = connector.getRealJdbcURL();
				Engine.logEngine.debug("(JdbcConnectionManager) JDBC URL: " + jdbcURL);
				String user = connector.getRealJdbcUserName();
				Engine.logEngine.debug("(JdbcConnectionManager) User: " + user);
				String password = connector.getRealJdbcUserPassword();
				Engine.logEngine.trace("(JdbcConnectionManager) Password: " + password);

				connection = connectWithoutPool(projectDriver, jdbcURL, user, password);

				Engine.logEngine.debug("(JdbcConnectionManager) non pooled connection = " + connection);
			}
		}
		return connection;
	}
	
	/**
	 * A driver of the libraries of the project running the request is connected with directly: DriverManager would
	 * hand the URL to the first registered driver accepting it, whatever its project.
	 */
	Connection connectWithoutPool(Driver projectDriver, String jdbcURL, String user, String password) throws SQLException {
		var anonymous = "".equals(user);
		Engine.logEngine.debug("(JdbcConnectionManager) " + (anonymous ? "Anonymous" : "Non anonymous") + " connection requested");
		if (projectDriver != null) {
			var info = new Properties();
			if (!anonymous && user != null) {
				info.put("user", user);
			}
			if (!anonymous && password != null) {
				info.put("password", password);
			}
			var connection = projectDriver.connect(jdbcURL, info);
			if (connection != null) {
				return connection;
			}
		}
		return anonymous ? DriverManager.getConnection(jdbcURL) : DriverManager.getConnection(jdbcURL, user, password);
	}

	/**
	 * Loads a JDBC driver with the class loader of the request.
	 *
	 * @return the driver when the libraries of the project provide it, for the generation of their class path; null
	 *         for a driver of the engine, registered once in DriverManager
	 */
	Driver loadDriver(String jdbcDriverClassName) throws Exception {
		var classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader instanceof ProjectClassLoader generation) {
			var drivers = projectDrivers.computeIfAbsent(generation.getId(), id -> new ConcurrentHashMap<>());
			synchronized (drivers) {
				var driver = drivers.get(jdbcDriverClassName);
				if (driver == null) {
					var driverClass = generation.loadClass(jdbcDriverClassName);
					if (driverClass.getClassLoader() != generation) {
						driver = ENGINE_DRIVER;
					} else {
						driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
						// for the code of the project that connects through DriverManager
						var shim = new DriverShim(driver);
						DriverManager.registerDriver(shim);
						projectShims.computeIfAbsent(generation.getId(), id -> new ArrayList<>()).add(shim);
					}
					drivers.put(jdbcDriverClassName, driver);
				}
				if (driver != ENGINE_DRIVER) {
					return driver;
				}
			}
		}
		synchronized (driversLoaded) {
			if (!driversLoaded.contains(jdbcDriverClassName)) {
				Driver d = (Driver) classLoader.loadClass(jdbcDriverClassName).getDeclaredConstructor().newInstance();
				DriverManager.registerDriver(new DriverShim(d));
				driversLoaded.add(jdbcDriverClassName);
			}
		}
		return null;
	}

	/** Marks a driver of the engine in the drivers of a generation. */
	private static final Driver ENGINE_DRIVER = new Driver() {
		public boolean acceptsURL(String u) {
			return false;
		}
		public Connection connect(String u, Properties p) {
			return null;
		}
		public int getMajorVersion() {
			return 0;
		}
		public int getMinorVersion() {
			return 0;
		}
		public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) {
			return new DriverPropertyInfo[0];
		}
		public boolean jdbcCompliant() {
			return false;
		}
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			throw new SQLFeatureNotSupportedException();
		}
	};
	
	private static class DriverShim implements Driver {
		private Driver driver;
		DriverShim(Driver d) {
			this.driver = d;
		}
		public boolean acceptsURL(String u) throws SQLException {
			return this.driver.acceptsURL(u);
		}
		public Connection connect(String u, Properties p) throws SQLException {
			return this.driver.connect(u, p);
		}
		public int getMajorVersion() {
			return this.driver.getMajorVersion();
		}
		public int getMinorVersion() {
			return this.driver.getMinorVersion();
		}
		public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
			return this.driver.getPropertyInfo(u, p);
		}
		public boolean jdbcCompliant() {
			return this.driver.jdbcCompliant();
		}
		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			return this.driver.getParentLogger();
		}
	}
}
