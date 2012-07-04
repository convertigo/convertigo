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

package com.twinsoft.convertigo.beans.connectors;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ConnectorEvent;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.util.StringEx;

public class SqlConnector extends Connector {

	private static final long serialVersionUID = -8541954462382010491L;

	/** The database connection. */
	transient public Connection connection = null;

	transient private boolean needReset = false;
	
	transient private String realJdbcURL = null;
	
	/** Holds keys/values of driver/url. */
	transient private Map<String, String> jdbc = new HashMap<String, String>();
	 
	/** Holds value of property jdbcDriverClassName. */
	private String jdbcDriverClassName = "";
	
	/** Holds value of property jdbcURL. */
	private String jdbcURL = "";
	
	/** Holds value of property jdbcUserName. */
	private String jdbcUserName = "";
	
	/** Holds value of property jdbcUserPassword. */
	private String jdbcUserPassword = "";

	private String systemTablesQuery = "";
	
	/** Holds value of property maxConnection. */
	private int jdbcMaxConnection = 10;
	
	private boolean keepConnectionAliveAfterTransaction = true;
	
	public SqlConnector() {
		super();
		vPropertiesForAdmin.add("jdbcDriverClassName");
		vPropertiesForAdmin.add("jdbcURL");
		vPropertiesForAdmin.add("jdbcUserName");
		vPropertiesForAdmin.add("jdbcUserPassword");
		vPropertiesForAdmin.add("jdbcMaxConnection");
	}

	@Override
	public SqlConnector clone() throws CloneNotSupportedException {
		SqlConnector clonedObject = (SqlConnector) super.clone();
		clonedObject.jdbc = new HashMap<String, String>();
		return clonedObject;
	}
	
	
	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
        String version = element.getAttribute("version");
        
        if (version == null) {
            String s = XMLUtils.prettyPrintDOM(element);
            EngineException ee = new EngineException(
                "Unable to find version number for the database object \"" + getName() + "\".\n" +
                "XML data: " + s
            );
            throw ee;
        }
        
        if (VersionUtils.compare(version, "4.6.0") < 0) {
        	if ((jdbcURL.startsWith("jdbc:hsqldb:file:")) && (jdbcURL.indexOf("/WEB-INF/minime/") != -1)) {
        		StringEx sx = new StringEx(jdbcURL);
        		sx.replace("/WEB-INF/minime/", "/WEB-INF/databases/");
        		jdbcURL = sx.toString();
				hasChanged = true;
				Engine.logBeans.warn("[SqlConnector] Successfully updated connection string for \""+ name +"\" (v 4.6.0)");
        	}
        }
		
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.Connector#addTransaction(com.twinsoft.convertigo.beans.core.Transaction)
	 */
	@Override
	protected void addTransaction(Transaction transaction) throws EngineException {
		if (!(transaction instanceof SqlTransaction))
			throw new EngineException("You cannot add to an SQL connector a database object of type " + transaction.getClass().getName());
		super.addTransaction(transaction);
	}

	public String[] getDriverNames() {
		int i=0;
		List<Driver> drivers = getDrivers();
		String[] driverNames = new String[drivers.size()];
		for (Driver driver : drivers)
			driverNames[i++] = driver.getClass().getName();
		return driverNames;
	}

	public List<Driver> getDrivers() {
		List<Driver> drivers = Collections.list(DriverManager.getDrivers());
		Collections.sort(drivers, new Comparator<Driver>(){
			public int compare(Driver o1, Driver o2) {
				return o1.getClass().toString().compareTo(o2.getClass().toString());
			}
		});
		return drivers;
	}

	/**
	 * Opens or reopens the connection to database.
	 * 
	 * @exception ClassNotFoundException if unable to load the JDBC driver.
	 * @exception SQLException if unable to create the connection to the database.
	 * @throws EngineException if unable to retrieve a valid connection.
	 */
	public synchronized void open() throws ClassNotFoundException, SQLException, EngineException {
		String text = "";
		
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {}
			text = "Reconnected to the database";
		} else text = "Connected to the database";
		
		// Attempt to load the database driver
		Class.forName(jdbcDriverClassName);
		Engine.logBeans.debug("(SqlConnector) JDBC driver loaded (" + jdbcDriverClassName + ")");
		
		realJdbcURL = jdbcURL;
		
		if(jdbcURL.startsWith("jdbc:hsqldb:file:")){ // make relativ path for hsqldb driver
			String fileURL = jdbcURL.substring("jdbc:hsqldb:file:".length());
			fileURL = Engine.theApp.filePropertyManager.getFilepathFromProperty(fileURL, getProject().getName());
			realJdbcURL = "jdbc:hsqldb:file:"+fileURL;
		}
		
		// Now attempt to create a database connection
		Engine.logBeans.debug("(SqlConnector)  JDBC URL: " + realJdbcURL);//jdbc:hsqldb:file:C:\projets\Convertigo4\tomcat\webapps\convertigo\minime\crawlingDatabase
		Engine.logBeans.debug("(SqlConnector)  User name: " + jdbcUserName);
		Engine.logBeans.debug("(SqlConnector)  User password: " + jdbcUserPassword);

		try {
			Engine.logBeans.debug("[SqlConnector] Connecting...");
			connection = Engine.theApp.sqlConnectionManager.getConnection(this);// manager handles retry
		} catch (SQLException e) {
			throw new EngineException("Unable to retrieve a valid connection from pool",e);
		}

		Engine.logBeans.debug("[SqlConnector] Open connection ("+connection.hashCode()+") on database " + realJdbcURL);
		
		needReset = false;
		
		Engine.logBeans.debug("(SqlConnector) " + text);
	}
	
	/**
	 * Determines if the connection is opened.
	 * 
	 * @return true if connection is opened, false otherwise.
	 * 
	 */
	public synchronized boolean isClosed() {
		boolean isClosed = true;
		if (connection != null) {
			try {
				isClosed = connection.isClosed();
			} catch (SQLException e) {}
		}
		return isClosed;	
	}
	
	public synchronized void close() {
		try {
			if (connection != null) {
				if(jdbcURL.startsWith("jdbc:hsqldb:file:")){
					Statement statement = connection.createStatement();
					statement.executeQuery("SHUTDOWN");
					statement.close();
				}
				Engine.logBeans.debug("[SqlConnector] Close connection ("+connection.hashCode()+") on database " + realJdbcURL);
				if(!connection.isClosed())
					connection.close();
				
				if (!keepConnectionAliveAfterTransaction) {
					Engine.theApp.sqlConnectionManager.removeDatabasePool(this);
				}
				
				Engine.logBeans.debug("(SqlConnector) Database connection closed");
			}
		}
		catch (Exception e) {
			Engine.logBeans.warn("(SqlConnector) Unable to close the connection on database, it may have been already closed.", e);
		}
		finally {
			connection = null;
			needReset = false;
		}
	}

	public PreparedStatement prepareStatement(String sqlQuery) throws SQLException, ClassNotFoundException, EngineException {
		PreparedStatement preparedStatement = null;
		if (isClosed() || needReset)
			open();
		
		try {
			Engine.logBeans.debug("[SqlConnector] Preparing statement...");
			preparedStatement = connection.prepareStatement(sqlQuery);
		}
		catch (SQLException e) {
			//Retry once (the connector's connection may have been closed)
			if (context.requestedObject.runningThread.bContinue) {
				Engine.logBeans.trace("[SqlConnector] An exception occured :" + e.getMessage());
				Engine.logBeans.debug("[SqlConnector] Failure! Try again to prepare statement...");
				try {
					open();
					preparedStatement = connection.prepareStatement(sqlQuery);
				}
				catch (Exception e1) {
					throw new EngineException("Unable to retrieve a prepared statement for the query on connection",e1);
				}
			}
		}
		
		return preparedStatement;
	}

	public void prepareForTransaction(Context context) throws EngineException {
		SqlTransaction sqlTransaction = null;
		try {
			sqlTransaction = (SqlTransaction) context.requestedObject;
		} catch (ClassCastException e) {
			throw new EngineException("Requested object is not a SQL transaction", e);
		}

		// Overwrites JDBC url if needed
		String variableValue = (String) sqlTransaction.variables.get(Parameter.ConnectorConnectionString.getName());
		if (variableValue != null) {
			if (!getJdbcURL().equals(variableValue)) {// Fix #2926 
				setJdbcURL(variableValue);
				Engine.logBeans.debug("(SqlConnector) Connection string overriden!");
			}
		}

		Engine.logBeans.debug("(SqlConnector) JDBC URL: " + jdbcURL);
	}

	public void setData(List<List<String>> data, List<String> columnHeaders) {
		fireDataChanged(new ConnectorEvent(this, new SqlData(data, columnHeaders)));
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		connection = null;
		super.finalize();
	}

	/**
	 * Getter for property jdbcDriverClassName
	 * @return
	 */
	public String getJdbcDriverClassName() {
		return jdbcDriverClassName;
	}

	/**
	 * Setter for property jdbcDriverClassName
	 * @param string
	 */
	public void setJdbcDriverClassName(String string) {
		if (connection != null) needReset = true;
		
		String url = "";
		if (jdbc.containsKey(jdbcDriverClassName = string))
			url = jdbc.get(jdbcDriverClassName);
		if (url.equals("")) {
			try {
				Properties properties = new Properties();
				InputStream propsInputstream = getClass().getResourceAsStream("/jdbc_drivers.properties");
				properties.load(propsInputstream);

				// Enumeration of the properties
				for(Map.Entry<String, String> prop : GenericUtils.<Set<Map.Entry<String,String>>>cast(properties.entrySet())){
					String propertyValue = prop.getValue()!=null?prop.getValue():"";
					if (propertyValue.indexOf(string) != -1) {
						String[]propertyValues = propertyValue.split(",");
						if (propertyValues.length>1)
							url = propertyValues[1];
						break;
					}
				}
			} catch (IOException e) {;}
		}
		setJdbcURL(url);
	}

	/**
	 * Getter for property jdbcURL
	 * @return
	 */
	public String getJdbcURL() {
		return jdbcURL;
	}

	/**
	 * Setter for property jdbcURL
	 * @param string
	 */
	public void setJdbcURL(String string) {
		jdbc.put(jdbcDriverClassName, jdbcURL=string);
		if (connection != null) needReset = true;
		Engine.theApp.sqlConnectionManager.removeDatabasePool(this);
	}

	/**
	 * Getter for property jdbcUserName
	 * @return
	 */
	public String getJdbcUserName() {
		return jdbcUserName;
	}

	/**
	 * Setter for property jdbcUserName
	 * @param string
	 */
	public void setJdbcUserName(String string) {
		jdbcUserName = string;
		if (connection != null) needReset = true;
		Engine.theApp.sqlConnectionManager.removeDatabasePool(this);
	}

	/**
	 * Getter for property jdbcUserPassword
	 * @return
	 */
	public String getJdbcUserPassword() {
		return jdbcUserPassword;
	}

	/**
	 * Setter for property jdbcUserPassword
	 * @param string
	 */
	public void setJdbcUserPassword(String string) {
		jdbcUserPassword = string;
		if (connection != null) needReset = true;
		Engine.theApp.sqlConnectionManager.removeDatabasePool(this);
	}

	@Override
	public void release(){
		close();
	}

	public int getJdbcMaxConnection() {
		return jdbcMaxConnection;
	}

	public void setJdbcMaxConnection(int maxConnection) {
		this.jdbcMaxConnection = maxConnection;
	}

	public String getRealJdbcURL() {
		return realJdbcURL;
	}
	
	public void setSystemTablesQuery(String systemTablesQuery) {
		this.systemTablesQuery = systemTablesQuery;
	}
	
	public String getSystemTablesQuery() {
		return systemTablesQuery;
	}

	public boolean isKeepConnectionAliveAfterTransaction() {
		return keepConnectionAliveAfterTransaction;
	}

	public void setKeepConnectionAliveAfterTransaction(boolean keepConnectionAliveAfterTransaction) {
		this.keepConnectionAliveAfterTransaction = keepConnectionAliveAfterTransaction;
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if(propertyName.equals("jdbcDriverClassName")){
			String[] sDriverNames = new String[0];
			
			try {
				Properties properties = new Properties();
				properties.load(getClass().getResourceAsStream("/jdbc_drivers.properties"));
				
				sDriverNames = properties.values().toArray(new String[properties.size()]);
				for(int i=0;i<sDriverNames.length;i++)
					sDriverNames[i] = sDriverNames[i]!=null?sDriverNames[i].split(",")[0]:"";
			}
			catch (IOException e) { }
			return sDriverNames;
		}
		return super.getTagsForProperty(propertyName);
	}
	
	@Override
	public SqlTransaction newTransaction() {
		return new SqlTransaction();
	}
}
