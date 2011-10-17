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

package com.twinsoft.convertigo.engine.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.util.DESKey;

public class SqlRequester {
	
	public static final String PROPERTIES_JDBC_DRIVER_CLASSNAME = "jdbc.driver.class_name";
	public static final String PROPERTIES_JDBC_URL = "jdbc.url";
	public static final String PROPERTIES_JDBC_USER_NAME = "jdbc.user.name";
	public static final String PROPERTIES_JDBC_USER_PASSWORD = "jdbc.user.password";

	/**
	 * The properties file name for the database connection.
	 */
	protected String propertiesFileName;
    
	/**
	 * The properties object.
	 */
	private Properties properties;

	/**
	 * The database connection.
	 */
	public Connection connection;
	
	/**
	 * Constructs a SqlRequester object.
	 *
	 * @param propertiesFileName the properties file name.
	 * 
	 * @exception ClassNotFoundException if unable to load the JDBC driver.
	 * @exception SQLException if unable to create the connection to the database.
	 */
	public SqlRequester(String propertiesFileName) throws IOException {
		this.propertiesFileName = propertiesFileName;
		properties = new Properties();
		properties.load(new FileInputStream(Engine.CONFIGURATION_PATH + propertiesFileName));
	}

	public String getProperty(String key) {
		String result = properties.getProperty(key);

		/*if (result == null) {
			if (key.equals(CARIOCA_URL)) return "http://localhost/carioca";
			else return "";
		}*/

		return result;
	}
    
	/**
	 * Opens or reopens the connection to database.
	 * 
	 * @exception ClassNotFoundException if unable to load the JDBC driver.
	 * @exception SQLException if unable to create the connection to the database.
	 */
	public synchronized void open() throws ClassNotFoundException, SQLException {
		String text = "";
		
		if (connection != null) {
			connection.close();
			text = "Reconnected to the database";
		}
		else {
			text = "Connected to the database";
		}
		
		// Attempt to load the database driver
		String jdbcClassName = getProperty(SqlRequester.PROPERTIES_JDBC_DRIVER_CLASSNAME);
		Class.forName(jdbcClassName);
		Engine.logEngine.debug("[SqlRequester] JDBC driver loaded (" + jdbcClassName + ")");

		// Now attempt to create a database connection
		String jdbcURL = getProperty(SqlRequester.PROPERTIES_JDBC_URL);
		String jdbcUserName = getProperty(SqlRequester.PROPERTIES_JDBC_USER_NAME);
		String jdbcUserPassword = DESKey.decodeFromHexString(getProperty(SqlRequester.PROPERTIES_JDBC_USER_PASSWORD));

		Engine.logEngine.debug("[SqlRequester] JDBC URL: " + jdbcURL);
		Engine.logEngine.debug("[SqlRequester] User name: " + jdbcUserName);
		Engine.logEngine.debug("[SqlRequester] User password: " + jdbcUserPassword);

		connection = DriverManager.getConnection(jdbcURL, jdbcUserName, jdbcUserPassword);
			
		Engine.logEngine.debug("[SqlRequester] " + text);
	}
	
	/**
	 * Determines if the connection is opened.
	 * 
	 * @return true if connection is opened, false otherwise.
	 * 
	 * @exception SQLException if unable connection is invalid.
	 */
	public synchronized boolean isClosed() throws SQLException {
		if (connection != null) {
			return connection.isClosed();
		}
		return true;	
	}
	
	public synchronized void close() {
		try {
			if (connection != null) {
				connection.close();
				connection = null;
				Engine.logEngine.debug("[SqlRequester] Database closed");
			}
		}
		catch (Exception e) {
			Engine.logEngine.error("[SqlRequester] Unable to close the database!", e);
		}
	}
	
}
