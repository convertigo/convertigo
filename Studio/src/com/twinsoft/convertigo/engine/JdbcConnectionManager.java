/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.tomcat.dbcp.dbcp.BasicDataSource;
import com.twinsoft.convertigo.beans.connectors.SqlConnector;

public class JdbcConnectionManager implements AbstractManager {

	private Hashtable<String,BasicDataSource> databasePools;
	
	public JdbcConnectionManager() {
	}

	public void init() throws EngineException {
		databasePools = new Hashtable<String,BasicDataSource>(2048);
	}
	
	public void destroy() throws EngineException {
		Enumeration<String> keysEnum = databasePools.keys();
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
		BasicDataSource pool = new BasicDataSource();
		pool.setDriverClassName(connector.getRealJdbcDriverClassName());
		pool.setUrl(connector.getRealJdbcURL());
		pool.setUsername(connector.getJdbcUserName());
		pool.setPassword(connector.getJdbcUserPassword());
		pool.setMaxActive(connector.getJdbcMaxConnection());
		databasePools.put(getKey(connector), pool);
		return pool;
	}

	private synchronized BasicDataSource getDatabasePool (SqlConnector connector) {
		if (databasePools.containsKey(getKey(connector)))
			return (BasicDataSource) databasePools.get(getKey(connector));
		else
			return addDatabasePool(connector);
	}
	
	private String getKey (SqlConnector connector) {
		return Long.toString(connector.getIdentity());
	}
	
	public Connection getConnection(SqlConnector connector) throws SQLException {
		return getConnection(connector, 10);
	}
	
	public Connection getConnection(SqlConnector connector, int retry_cpt) throws SQLException {
		Engine.logEngine.trace("(jdbcConnectionManager) getConnection for "+connector.getProject().getName()+"."+connector.getName());
		BasicDataSource pool = getDatabasePool(connector);
		Connection connection = pool.getConnection();
		
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
			/* MYSQL (limit to 1 row)*/
			else if ("com.mysql.jdbc.Driver".equals(jdbcDriverClassName))
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
			else
				query = "SELECT * FROM INFORMATION_SCHEMA.TABLES";
		}
		
		try{
			connection.prepareStatement(query).executeQuery();
		}catch (SQLException e) {
			String exceptionClassName = e.getClass().getName();
			if(retry_cpt>0){
				if(exceptionClassName.equals("com.mysql.jdbc.CommunicationsException")){
					Engine.logEngine.trace("(JdbcConnectionManager) Connection not valid : getting another connection ("+retry_cpt+" retry before abort)");
					retry_cpt--;
				}else{
					Engine.logEngine.error("(JdbcConnectionManager) Unknow SQLException ["+exceptionClassName+"] : retry getConnection", e);
					retry_cpt = 0;
				}
				try{
					connection.close();
				}catch (Exception ex) {} // may be already closed
				connection = getConnection(connector, retry_cpt);
			}else{
				Engine.logEngine.error("(JdbcConnectionManager) Connection not valid : unable to get a valid connection (no more retry)");
				throw e;
			}
		}
		return connection;
	}
}
