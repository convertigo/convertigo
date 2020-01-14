/*
 * Copyright (c) 2001-2020 Convertigo SA.
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.naming.NamingException;

import com.twinsoft.convertigo.beans.connectors.SqlConnector;

public class JdbcConnectionManager implements AbstractManager {

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
	private Map<String, Object> databasePools;
	private Set<String> driversLoaded;
	
	public JdbcConnectionManager() {
	}

	public void init() throws EngineException {
		databasePools = new HashMap<>(2048);
		driversLoaded = new HashSet<>();
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
		Enumeration<String> keysEnum = Collections.enumeration(databasePools.keySet());
		while(keysEnum.hasMoreElements()) {
			String poolKey = (String)keysEnum.nextElement();
			Engine.logEngine.debug("[SqlConnectionManager] Closing datasource '" + poolKey + "'...");
			try {
				dataSourceClose.invoke(databasePools.get(poolKey));
				Engine.logEngine.debug("[SqlConnectionManager] Datasource '" + poolKey + "' closed.");
			} catch (Exception e) {
				Engine.logEngine.debug("[SqlConnectionManager] Datasource '" + poolKey + "' close failure ! ");
			}
			databasePools.remove(poolKey);
		}
		databasePools = null;
	}
	
	public void removeDatabasePool(SqlConnector connector) {
		String poolKey = getKey(connector);
		if (databasePools.containsKey(poolKey)) {
			Engine.logEngine.debug("[SqlConnectionManager] Closing datasource '" + poolKey + "'...");
			try {
				dataSourceClose.invoke(databasePools.get(poolKey));
				Engine.logEngine.debug("[SqlConnectionManager] Datasource '" + poolKey + "' closed.");
			} catch (Exception e) {
				Engine.logEngine.debug("[SqlConnectionManager] Datasource '" + poolKey + "' close failure ! ");
			}
			databasePools.remove(poolKey);
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

		String user = connector.getJdbcUserName();
		Engine.logEngine.debug("(JdbcConnectionManager) User: " + user);
		dataSourceSetUsername.invoke(pool, user);

		String password = connector.getJdbcUserPassword();
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
				query = "SELECT * FROM ALL_TABLES WHERE ROWNUM <= 1";
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

	private synchronized Object getDatabasePool (SqlConnector connector) throws Exception {
		if (databasePools.containsKey(getKey(connector))) {
			Engine.logEngine.debug("(JdbcConnectionManager) getDatabasePool() returning existing pool");
			return databasePools.get(getKey(connector));
		}
		else {
			Engine.logEngine.debug("(JdbcConnectionManager) getDatabasePool() returning new pool");
			return addDatabasePool(connector);
		}
	}
	
	private String getKey (SqlConnector connector) {
		return connector.getQName();
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
			try {
				checkDriverLoaded(jdbcDriverClassName);
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
				String user = connector.getJdbcUserName();
				Engine.logEngine.debug("(JdbcConnectionManager) User: " + user);
				String password = connector.getJdbcUserPassword();
				Engine.logEngine.trace("(JdbcConnectionManager) Password: " + password);

				if ("".equals(user)) {
					Engine.logEngine.debug("(JdbcConnectionManager) Anonymous connection requested");
					connection = DriverManager.getConnection(jdbcURL);
				}
				else {
					Engine.logEngine.debug("(JdbcConnectionManager) Non anonymous connection requested");
					connection = DriverManager.getConnection(
							jdbcURL,
							user,
							password);
				}

				Engine.logEngine.debug("(JdbcConnectionManager) non pooled connection = " + connection);
			}
		}
		return connection;
	}
	
	private void checkDriverLoaded(String jdbcDriverClassName) throws Exception {
		synchronized (driversLoaded) {
			if (!driversLoaded.contains(jdbcDriverClassName)) {
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				Driver d = (Driver) classLoader.loadClass(jdbcDriverClassName).getDeclaredConstructor().newInstance();
				DriverManager.registerDriver(new DriverShim(d));
				driversLoaded.add(jdbcDriverClassName);
			}
		}
	}
	
	private class DriverShim implements Driver {
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
