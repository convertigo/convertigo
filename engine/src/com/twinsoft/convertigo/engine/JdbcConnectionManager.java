/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

import org.apache.tomcat.dbcp.dbcp.BasicDataSource;

import com.twinsoft.convertigo.beans.connectors.SqlConnector;

public class JdbcConnectionManager implements AbstractManager {

	private Map<String,BasicDataSource> databasePools;
	private Set<String> driversLoaded;
	
	public JdbcConnectionManager() {
	}

	public void init() throws EngineException {
		databasePools = new HashMap<>(2048);
		driversLoaded = new HashSet<>();
	}
	
	public void destroy() throws EngineException {
		Enumeration<String> keysEnum = Collections.enumeration(databasePools.keySet());
		while(keysEnum.hasMoreElements()) {
			String poolKey = (String)keysEnum.nextElement();
			Engine.logEngine.debug("[SqlConnectionManager] Closing datasource '" + poolKey + "'...");
			try {
				((BasicDataSource)databasePools.get(poolKey)).close();
				Engine.logEngine.debug("[SqlConnectionManager] Datasource '" + poolKey + "' closed.");
			} catch (SQLException e) {
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
				((BasicDataSource)databasePools.get(poolKey)).close();
				Engine.logEngine.debug("[SqlConnectionManager] Datasource '" + poolKey + "' closed.");
			} catch (SQLException e) {
				Engine.logEngine.debug("[SqlConnectionManager] Datasource '" + poolKey + "' close failure ! ");
			}
			databasePools.remove(poolKey);
		}
	}

	private BasicDataSource addDatabasePool(SqlConnector connector) {
		Engine.logEngine.debug("(JdbcConnectionManager) Creating a new pool");
		
		BasicDataSource pool = new BasicDataSource();
		
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		pool.setDriverClassLoader(cl);
		pool.setDriverClassName(connector.getJdbcDriverClassName());
		
		String jdbcURL = connector.getRealJdbcURL();
		Engine.logEngine.debug("(JdbcConnectionManager) JDBC URL: " + jdbcURL);
		pool.setUrl(jdbcURL);

		String user = connector.getJdbcUserName();
		Engine.logEngine.debug("(JdbcConnectionManager) User: " + user);
		pool.setUsername(user);

		String password = connector.getJdbcUserPassword();
		Engine.logEngine.trace("(JdbcConnectionManager) Password: " + password);
		pool.setPassword(password);

		int maxConnections = connector.getJdbcMaxConnection();
		Engine.logEngine.debug("(JdbcConnectionManager) maxConnections: " + maxConnections);
		pool.setMaxActive(maxConnections);

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
		pool.setValidationQuery(query);

		boolean testOnBorrow = connector.getTestOnBorrow();
		Engine.logEngine.debug("(JdbcConnectionManager) testOnBorrow=" + testOnBorrow);
		pool.setTestOnBorrow(testOnBorrow);
		
		pool.setTestOnReturn(true);
		pool.setTestWhileIdle(true);
		
		long timeBetweenEvictionRunsMillis = connector.getIdleConnectionTestTime() * 1000;
		Engine.logEngine.debug("(JdbcConnectionManager) Time between eviction runs millis: " + timeBetweenEvictionRunsMillis);
		pool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
		
		pool.setNumTestsPerEvictionRun(3);
		
		databasePools.put(getKey(connector), pool);
		Engine.logEngine.debug("(JdbcConnectionManager) Pool added");

		return pool;
	}

	private synchronized BasicDataSource getDatabasePool (SqlConnector connector) {
		if (databasePools.containsKey(getKey(connector))) {
			Engine.logEngine.debug("(JdbcConnectionManager) getDatabasePool() returning existing pool");
			return (BasicDataSource) databasePools.get(getKey(connector));
		}
		else {
			Engine.logEngine.debug("(JdbcConnectionManager) getDatabasePool() returning new pool");
			return addDatabasePool(connector);
		}
	}
	
	private String getKey (SqlConnector connector) {
		return connector.getQName();
	}
	
	public Connection getConnection(SqlConnector connector) throws SQLException, ClassNotFoundException, NamingException {
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

				BasicDataSource pool = getDatabasePool(connector);
				Engine.logEngine.debug("(JdbcConnectionManager) pool = " + pool);
				Engine.logEngine.debug("(JdbcConnectionManager)    active connection(s): " + pool.getNumActive() + "/" + pool.getMaxActive());
				Engine.logEngine.debug("(JdbcConnectionManager)    idle connection(s):   " + pool.getNumIdle() + "/" + pool.getMaxIdle());

				connection = pool.getConnection();
				Engine.logEngine.debug("(JdbcConnectionManager) pooled connection = " + connection);
				Engine.logEngine.debug("(JdbcConnectionManager)    active connection(s): " + pool.getNumActive() + "/" + pool.getMaxActive());
				Engine.logEngine.debug("(JdbcConnectionManager)    idle connection(s):   " + pool.getNumIdle() + "/" + pool.getMaxIdle());
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
				Driver d = (Driver) classLoader.loadClass(jdbcDriverClassName).newInstance();
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
