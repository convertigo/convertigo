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

package com.twinsoft.convertigo.beans.connectors;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.api.Session;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ConnectorEvent;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction.SqlQueryInfos;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.KeyExpiredException;
import com.twinsoft.convertigo.engine.MaxCvsExceededException;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.servlets.EngineServlet;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.PropertiesUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.tas.KeyManager;
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
	
	private long idleConnectionTestTime = 60;
	
	private boolean testOnBorrow = false;
	
	private boolean connectionPool = true;
	
	public SqlConnector() {
		super();
	}

	@Override
	public SqlConnector clone() throws CloneNotSupportedException {
		SqlConnector clonedObject = (SqlConnector) super.clone();
		clonedObject.jdbc = new HashMap<String, String>();
		return clonedObject;
	}
	
	public Connection getJNDIConnection() throws NamingException, SQLException {
		Connection conn = null;
		if (jdbcDriverClassName.equals("JNDI")) {
			String path = jdbcURL;
			if (!path.startsWith("java:")) {
				path = "java:comp/env/" + path;
			}
			DataSource ds = EngineServlet.getDataSource(path);
			if (ds == null) {
				throw new NamingException(path + " not found in context");
			}
				
			conn = ds.getConnection();
		}
		return conn;
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
				Engine.logBeans.warn("[SqlConnector] Successfully updated connection string for \""+ getName() +"\" (v 4.6.0)");
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
		
		realJdbcURL = jdbcURL;
		
		if(jdbcURL.startsWith("jdbc:hsqldb:file:")){ // make relativ path for hsqldb driver
			String fileURL = jdbcURL.substring("jdbc:hsqldb:file:".length());
			fileURL = Engine.theApp.filePropertyManager.getFilepathFromProperty(fileURL, getProject().getName());
			realJdbcURL = "jdbc:hsqldb:file:"+fileURL;
		}
		
		// Now attempt to create a database connection
		Engine.logBeans.debug("(SqlConnector)  JDBC URL: " + realJdbcURL);//jdbc:hsqldb:file:C:\projets\Convertigo4\tomcat\webapps\convertigo\minime\crawlingDatabase
		Engine.logBeans.debug("(SqlConnector)  User name: " + jdbcUserName);
		Engine.logBeans.debug("(SqlConnector)  User password: ******");

		try {
			Engine.logBeans.debug("[SqlConnector] Connecting...");
			connection = Engine.theApp.sqlConnectionManager.getConnection(this);// manager handles retry
		} catch (SQLException e) {
			throw new EngineException("Unable to retrieve a valid connection from pool", e);
		} catch (NamingException e) {
			throw new EngineException("Unable to find the JNDI resource", e);
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
					removeDatabasePool();
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
	
	public PreparedStatement prepareStatement(String sqlQuery, SqlQueryInfos sqlQueryInfos) throws SQLException, ClassNotFoundException, EngineException {
		PreparedStatement preparedStatement = null;
		if (isClosed() || needReset)
			open();
		
		// In the case when we want to escape the bracket "\{id\}"
		sqlQuery = sqlQuery.replace("\\{", "{").replace("\\}", "}");
		
		boolean isCallableStmt = sqlQueryInfos.isCallable();
		
		try {
			Engine.logBeans.debug("[SqlConnector] Preparing statement...");
			preparedStatement = isCallableStmt ? connection.prepareCall(sqlQuery) : connection.prepareStatement(sqlQuery);
		}
		catch (SQLException e) {
			//Retry once (the connector's connection may have been closed)
			if (context.requestedObject.runningThread.bContinue) {
				Engine.logBeans.trace("[SqlConnector] An exception occured :" + e.getMessage());
				Engine.logBeans.debug("[SqlConnector] Failure! Try again to prepare statement...");
				try {
					open();
					preparedStatement = isCallableStmt ? connection.prepareCall(sqlQuery) : connection.prepareStatement(sqlQuery);
				}
				catch (Exception e1) {
					throw new EngineException("Unable to retrieve a prepared statement for the query on connection",e1);
				}
			}
		}
		
		// Call the method prepareParameters with the preparedStatement and the ArrayList 
		prepareParameters(preparedStatement, sqlQueryInfos); 
		
		return preparedStatement;
	}

	private void prepareParameters(PreparedStatement preparedStatement, SqlQueryInfos sqlQueryInfos) throws SQLException{ 
		Map<String, String> values = sqlQueryInfos.getParametersMap();
		List<Map<String, Object>> parameterList = getProcedureParameters(connection, sqlQueryInfos);
		int i = 1;
		for (Map<String, Object> parameterMap : parameterList) {
			boolean isIn = false, isOut = false;
			String param_name = (String) parameterMap.get("COLUMN_NAME");
			int param_mode = (Integer) parameterMap.get("COLUMN_TYPE");
			int param_type = (Integer) parameterMap.get("DATA_TYPE");
            switch (param_mode) {
	  	    	case DatabaseMetaData.procedureColumnIn :
	  	    		isIn = true;
	 	        	break; 
	  	    	case DatabaseMetaData.procedureColumnInOut :
	  	    		isIn = true; isOut = true;
	 	        	break; 
	  	    	case DatabaseMetaData.procedureColumnOut :
	  	    	case DatabaseMetaData.procedureColumnReturn :
	  	    	case DatabaseMetaData.procedureColumnResult :
	  	    		isOut = true;
	 	        	break; 
	  	    	case DatabaseMetaData.procedureColumnUnknown : 
	  	    	default :
	  	    		break;  
            }
            
            if (isOut) {
            	Engine.logBeans.trace("[SqlConnector] Registering OUT parameter at index "+i);
            	((CallableStatement)preparedStatement).registerOutParameter(i, param_type);
            }
            
            if (isIn) {
            	Engine.logBeans.trace("[SqlConnector] Setting IN parameter at index "+i);
            	Object java_value = null;
				try {
	            	String param_in = StringUtils.normalize(param_name);
					java_value = getParameterJavaValue(param_type, values.get(param_in));
					if (java_value == null) {// param name differs from variable name
						List<String> params = sqlQueryInfos.getOrderedParametersList();
						int j = parameterList.size() - values.size();
						java_value = getParameterJavaValue(param_type, values.get(params.get(i-j-1)));
					}
					preparedStatement.setObject(i , java_value);
				} catch (Exception e) {
					Engine.logBeans.warn("[SqlConnector] Error generating value for parameter at index "+i, e);
					preparedStatement.setNull(i, Types.OTHER);
				}
            }
            i++;
		}
		
		Engine.logBeans.trace("[SqlConnector] Preparing statement done");
	}
	
	public void prepareForTransaction(Context context) throws EngineException {
		SqlTransaction sqlTransaction = null;
		try {
			sqlTransaction = (SqlTransaction) context.requestedObject;
		} catch (ClassCastException e) {
			throw new EngineException("Requested object is not a SQL transaction", e);
		}
		
		if (Engine.isEngineMode() && KeyManager.getCV(Session.EmulIDSQL) < 1) {
			String msg;
			if (KeyManager.has(Session.EmulIDSQL) && KeyManager.hasExpired(Session.EmulIDSQL)) {
				Engine.logEngine.error(msg = "Key expired for the SQL connector.");
				throw new KeyExpiredException(msg);
			}
			Engine.logEngine.error(msg = "No key for the SQL connector.");
			throw new MaxCvsExceededException(msg);
		}

		// Overwrites JDBC url if needed
		String variableValue = sqlTransaction.getParameterStringValue(Parameter.ConnectorConnectionString.getName());
		if (variableValue != null && !variableValue.isEmpty()) {
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
			try (InputStream stream = getClass().getResourceAsStream("/jdbc_drivers.properties")) {
				Properties properties = PropertiesUtils.load(stream);
				
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
		removeDatabasePool();
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
		removeDatabasePool();
			
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
		removeDatabasePool();
	}

	private void removeDatabasePool() {
		if (Engine.theApp != null && Engine.theApp.sqlConnectionManager != null) {
			Engine.theApp.sqlConnectionManager.removeDatabasePool(this);
		}
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
		if (connection != null) needReset = true;
		removeDatabasePool();
	}

	public String getRealJdbcURL() {
		return realJdbcURL;
	}
	
	public void setSystemTablesQuery(String systemTablesQuery) {
		this.systemTablesQuery = systemTablesQuery;
		if (connection != null) needReset = true;
		removeDatabasePool();
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
				Properties properties = PropertiesUtils.load(getClass().getResourceAsStream("/jdbc_drivers.properties"));
				
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

	@Override
	public boolean isMaskedProperty(Visibility target, String propertyName) {
		if ("jdbcUserPassword".equals(propertyName)) {
			return true;
		}
		return super.isMaskedProperty(target, propertyName);
	}

	@Override
	public boolean isCipheredProperty(String propertyName) {
		if ("jdbcUserPassword".equals(propertyName)) {
			return true;
		}
		return super.isCipheredProperty(propertyName);
	}
	
	@Override
	public SqlTransaction getDefaultTransaction() throws EngineException {
		return (SqlTransaction) super.getDefaultTransaction();
	}

	public long getIdleConnectionTestTime() {
		return idleConnectionTestTime;
	}

	public void setIdleConnectionTestTime(long idleConnectionTestTime) {
		this.idleConnectionTestTime = idleConnectionTestTime;
		if (connection != null) needReset = true;
		removeDatabasePool();
	}

	public boolean getConnectionPool() {
		return connectionPool;
	}

	public void setConnectionPool(boolean connectionPool) {
		this.connectionPool = connectionPool;
	}

	public boolean getTestOnBorrow() {
		return testOnBorrow;
	}

	public void setTestOnBorrow(boolean testOnBorrow) {
		this.testOnBorrow = testOnBorrow;
		if (connection != null) needReset = true;
		removeDatabasePool();
	}
	
	static public Object getParameterJavaValue(int param_type, String value) throws UnsupportedEncodingException, MalformedURLException, SQLException {
		if (value != null) {
			switch (param_type) {
		    	case Types.ARRAY: 
		    		return value.split(",");
		        case Types.BIGINT: 
		        	return Long.valueOf(value);
		        case Types.BINARY: 
		        	return String.valueOf(value).getBytes("UTF-8");
		        case Types.BIT: 
		        	return Boolean.valueOf(value);
		        case Types.BLOB:
		        	return value;//connection.createBlob();//
		        case Types.BOOLEAN:
		        	return Boolean.valueOf(value);
		        case Types.CHAR: 
		        	return String.valueOf(value);
		        case Types.CLOB: 
		        	return value;//connection.createClob();//
		        case Types.DATALINK: 
		        	return new java.net.URL(value);
		        case Types.DATE: 
		        	return java.sql.Date.valueOf(value); 
		        case Types.DECIMAL: 
		        	return java.math.BigDecimal.valueOf(Double.valueOf(value)); 
		        case Types.DISTINCT: 
		        	return value;//Object.class; 
		        case Types.DOUBLE: 
		        	return Double.valueOf(value); 
		        case Types.FLOAT: 
		        	return Double.valueOf(value); 
		        case Types.INTEGER: 
		        	return Integer.valueOf(value, 10); 
		        case Types.JAVA_OBJECT: 
		        	return value;//Object.class; 
		        case Types.LONGVARBINARY: 
		        	return String.valueOf(value).getBytes("UTF-8"); 
		        case Types.LONGVARCHAR: 
		        	return String.valueOf(value);
		        case Types.NCLOB: 
		        	return value;//connection.createNClob();//
		        case Types.NULL: 
		        	return null;//Object.class; 
		        case Types.NUMERIC: 
		        	return java.math.BigDecimal.valueOf(Double.parseDouble(value));  
		        case Types.NCHAR: 
		        case Types.NVARCHAR: 
		        case Types.LONGNVARCHAR: 
		        	return String.valueOf(value);
		        case Types.OTHER: 
		        	return value;//Object.class; 
		        case Types.REAL: 
		        	return Float.parseFloat(value); 
		        case Types.REF: 
		        	return value;//java.sql.Ref.class;
		        case Types.ROWID: 
		        	return value;//java.sql.RowId.class; 
		        case Types.SMALLINT: 
		        	return Short.valueOf(value); 
		        case Types.STRUCT: 
		        	return value;//connection.createStruct(typeName, attributes);//
		        case Types.SQLXML: 
		        	return value;//connection.createSQLXML();//
		        case Types.TIME: 
		        	return java.sql.Time.valueOf(value); 
		        case Types.TIMESTAMP: 
		        	return java.sql.Timestamp.valueOf(value); 
		        case Types.TINYINT: 
		        	return Byte.valueOf(value); 
		        case Types.VARBINARY: 
		        	return String.valueOf(value).getBytes("UTF-8");
		        case Types.VARCHAR: 
		        	return String.valueOf(value);
		        default: 
		        	return value;
	        }
		}
		return null;
	}
	
	static public Class<?> getParameterJavaClass(int param_type) {
		switch (param_type) {
	    	case Types.ARRAY: 
	    		return Object[].class; 
	        case Types.BIGINT: 
	        	return Long.class; 
	        case Types.BINARY: 
	        	return byte[].class; 
	        case Types.BIT: 
	        	return Boolean.class; 
	        case Types.BLOB: 
	        	return java.sql.Blob.class; 
	        case Types.BOOLEAN:
	        	return Boolean.class; 
	        case Types.CHAR: 
	        	return String.class; 
	        case Types.CLOB: 
	        	return java.sql.Clob.class; 
	        case Types.DATALINK: 
	        	return java.net.URL.class; 
	        case Types.DATE: 
	        	return java.sql.Date.class; 
	        case Types.DECIMAL: 
	        	return java.math.BigDecimal.class; 
	        case Types.DISTINCT: 
	        	return Object.class; 
	        case Types.DOUBLE: 
	        	return Double.class; 
	        case Types.FLOAT: 
	        	return Double.class; 
	        case Types.INTEGER: 
	        	return Integer.class; 
	        case Types.JAVA_OBJECT: 
	        	return Object.class; 
	        case Types.LONGVARBINARY: 
	        	return byte[].class; 
	        case Types.LONGVARCHAR: 
	        	return String.class; 
	        case Types.NCLOB: 
	        	return java.sql.NClob.class; 
	        case Types.NULL: 
	        	return Object.class; 
	        case Types.NUMERIC: 
	        	return java.math.BigDecimal.class; 
	        case Types.NCHAR: 
	        case Types.NVARCHAR: 
	        case Types.LONGNVARCHAR: 
	        	return String.class; 
	        case Types.OTHER: 
	        	return Object.class; 
	        case Types.REAL: 
	        	return Float.class; 
	        case Types.REF: 
	        	return java.sql.Ref.class; 
	        case Types.ROWID: 
	        	return java.sql.RowId.class; 
	        case Types.SMALLINT: 
	        	return Short.class; 
	        case Types.STRUCT: 
	        	return java.sql.Struct.class; 
	        case Types.SQLXML: 
	        	return java.sql.SQLXML.class; 
	        case Types.TIME: 
	        	return java.sql.Time.class; 
	        case Types.TIMESTAMP: 
	        	return java.sql.Timestamp.class; 
	        case Types.TINYINT: 
	        	return Byte.class; 
	        case Types.VARBINARY: 
	        	return byte[].class; 
	        case Types.VARCHAR: 
	        	return String.class; 
	        default: 
	        	return Object.class;
        }
	}
	
	static public Document executeSearch(SqlConnector sqlConnector, String pattern) throws EngineException, ClassNotFoundException, SQLException {
		Document doc = SqlTransaction.createDOM("UTF-8");
		if (sqlConnector != null) {
			sqlConnector.open();
			
			ResultSet rs = null;
			try {
				
				Connection connection = sqlConnector.connection;
				DatabaseMetaData dmd = connection.getMetaData();
				String catalog = connection.getCatalog();
				
				Element item, child;
				Element root = (Element) doc.appendChild(doc.createElement("items"));
				
				// Retrieve all stored procedures
				rs = dmd.getProcedures(catalog, null, pattern);
				if (rs != null) {
					while (rs.next()) {
						item = (Element) root.appendChild(doc.createElement("item"));
						
						String callableName = rs.getString("PROCEDURE_NAME");
						
						int index = callableName.indexOf(";");
						if (index != -1) {// seen for ms sql server
							callableName = callableName.substring(0, index);
						}
						
						child = (Element) item.appendChild(doc.createElement("NAME"));
						child.appendChild(doc.createTextNode(callableName));
						try {
							child.setAttribute("specific_name", rs.getString("SPECIFIC_NAME"));
						} catch(Exception e) {}
						
						String callableDesc = rs.getString("REMARKS");
						child = (Element) item.appendChild(doc.createElement("REMARKS"));
						child.appendChild(doc.createTextNode(callableDesc));
						
						child = (Element) item.appendChild(doc.createElement("TYPE"));
						child.appendChild(doc.createTextNode(index == -1 ? "PROCEDURE":"FUNCTION"));
					}
					rs.close();
					rs = null;
				}
				
				// Retrieve all stored functions
				rs = dmd.getFunctions(catalog, null, pattern);
				if (rs != null) {
					while (rs.next()) {
						item = (Element) root.appendChild(doc.createElement("item"));
						
						String callableName = rs.getString(3);//"FUNCTION_NAME");
						child = (Element) item.appendChild(doc.createElement("NAME"));
						child.appendChild(doc.createTextNode(callableName));
						try {
							child.setAttribute("specific_name", rs.getString("SPECIFIC_NAME"));
						}
						catch (Exception e) {}
						
						String callableDesc = rs.getString("REMARKS");
						child = (Element) item.appendChild(doc.createElement("REMARKS"));
						child.appendChild(doc.createTextNode(callableDesc));
						
						child = (Element) item.appendChild(doc.createElement("TYPE"));
						child.appendChild(doc.createTextNode("FUNCTION"));
					}
					rs.close();
					rs = null;
				}
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
			finally {
				if (rs != null) {
					rs.close();
				}
				rs = null;
			}
			
			sqlConnector.close();
			
			//System.out.println(XMLUtils.prettyPrintDOM(doc));
		}
		return doc;
	}

	public static List<Map<String, Object>> getProcedureParameters(Connection connection, SqlQueryInfos sqlQueryInfos) {
		if (sqlQueryInfos.isCallable()) {
			return getProcedureParameters(connection, sqlQueryInfos.getProcedure());
		}
		else {
			List<Map<String, Object>> parameterList = new ArrayList<Map<String, Object>>();
			for (String param : sqlQueryInfos.getOrderedParametersList()) {
				Map<String, Object> parameterMap = new HashMap<String, Object>();
				parameterMap.put("COLUMN_NAME", param);
				parameterMap.put("COLUMN_TYPE", DatabaseMetaData.procedureColumnIn);
				parameterMap.put("DATA_TYPE", Types.VARCHAR);
				parameterList.add(parameterMap);
			}
			return parameterList;
		}
	}
	
	public static List<Map<String, Object>> getProcedureParameters(Connection connection, String procedure) {
		List<Map<String, Object>> parameterList = new ArrayList<Map<String, Object>>();
		if (connection != null && procedure != null && !procedure.isEmpty()) {
			try {
				int i=1;
				DatabaseMetaData dmd = connection.getMetaData();
				ResultSet rs = dmd.getProcedureColumns(connection.getCatalog(), null, procedure,"%");
				while (rs.next()) {
					Map<String, Object> parameterMap = new HashMap<String, Object>();
					try {
						parameterMap.put("SPECIFIC_NAME", rs.getString("SPECIFIC_NAME"));
					} catch (SQLException e) {
						Engine.logBeans.trace("Unable to retrieve specific name for procedure '"+procedure+"'", e);
						parameterMap.put("SPECIFIC_NAME", procedure);
					}
					try {
						parameterMap.put("PROCEDURE_NAME", rs.getString("PROCEDURE_NAME"));
					} catch (SQLException e) {
						Engine.logBeans.warn("Unable to retrieve specific name for procedure '"+procedure+"'", e);
						parameterMap.put("PROCEDURE_NAME", procedure);
					}
					try {
						parameterMap.put("COLUMN_NAME", rs.getString("COLUMN_NAME"));
					} catch (SQLException e) {
						Engine.logBeans.warn("Unable to retrieve parameter name for procedure '"+procedure+"'", e);
						parameterMap.put("COLUMN_NAME", "column"+i);
					}
					try {
						parameterMap.put("COLUMN_TYPE", rs.getInt("COLUMN_TYPE"));
					} catch (SQLException e) {
						Engine.logBeans.warn("Unable to retrieve parameter type for procedure '"+procedure+"'", e);
						parameterMap.put("COLUMN_TYPE", 0);
					}
					try {
						parameterMap.put("DATA_TYPE", rs.getInt("DATA_TYPE"));
					} catch (SQLException e) {
						Engine.logBeans.warn("Unable to retrieve parameter data type for procedure '"+procedure+"'", e);
						parameterMap.put("DATA_TYPE", 0);
					}
					parameterList.add(parameterMap);
					i++;
				}
			}
			catch (SQLException e) {
				Engine.logBeans.error("Unable to retrieve parameters for procedure '"+procedure+"'", e);
			}
		}
		return parameterList;
	}
	
	public static SqlTransaction createSqlTransaction(SqlConnector sqlConnector, String callableName, String specific_name) throws EngineException, ClassNotFoundException, SQLException {
		SqlTransaction sqlTransaction = null;
		if (sqlConnector != null && !callableName.isEmpty()) {
			// get a connection
			sqlConnector.open();
			
			// Create transaction
			sqlTransaction = sqlConnector.newTransaction();
			sqlTransaction.setName("Call_"+StringUtils.normalize(callableName));
			sqlTransaction.hasChanged = true;
			sqlTransaction.bNew = true;
			
			// Build sqlQuery and retrieve parameters
			boolean isFunction = false;
			String sqlQuery = "CALL "+ callableName +"(";
			Connection connection = sqlConnector.connection;
			List<Map<String, Object>> parameterList = getProcedureParameters(connection, callableName);
			for (Map<String, Object> parameterMap : parameterList) {
				String proc_specific_name = (String) parameterMap.get("SPECIFIC_NAME");
				if (specific_name.equals(proc_specific_name)) {
					String param_name = (String) parameterMap.get("COLUMN_NAME");
					int param_mode = (Integer) parameterMap.get("COLUMN_TYPE");
					//int param_type = (Integer) parameterMap.get("DATA_TYPE");
		            switch(param_mode) {
			  	    	case DatabaseMetaData.procedureColumnReturn :
			  	    	case DatabaseMetaData.procedureColumnResult :
			  	    		isFunction = true;
			  	    		break;
		  	    		case DatabaseMetaData.procedureColumnIn :
			  	    	case DatabaseMetaData.procedureColumnInOut :
			  	    		// update query and create input variable
			  	    		if (param_name != null && !param_name.isEmpty()) {
			  	    			RequestableVariable variable = new RequestableVariable();
			  	    			variable.setName(StringUtils.normalize(param_name));
			  	    			variable.hasChanged = true;
			  	    			variable.bNew = true;
			  	    			
				  	    		sqlQuery += sqlQuery.endsWith("(") ? "":",";
				  	    		sqlQuery += "{"+ variable.getName()+"}";
			  	    			
			  	    			sqlTransaction.addVariable(variable);
			  	    		}
			  	    		else {
				  	    		sqlQuery += sqlQuery.endsWith("(") ? "?":",?";
			  	    		}
			  	    		break;
			  	    	case DatabaseMetaData.procedureColumnOut :
			  	    		sqlQuery += sqlQuery.endsWith("(") ? "?":",?";
			  	    		break;
			  	    	case DatabaseMetaData.procedureColumnUnknown : 
			  	    	default :
			  	    		break;  
		            }
				}
			}
			sqlQuery += ")";
			if (isFunction) {
				sqlQuery = "? = "+ sqlQuery;
			}
			sqlQuery = "{"+sqlQuery+"}";
			sqlTransaction.setSqlQuery(sqlQuery);
			
			// close connection
			sqlConnector.close();
		}
		return sqlTransaction;
	}
}
