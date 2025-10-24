/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.twinsoft.convertigo.engine.Engine;

public class SqlRequester {

	private static final String PROPERTIES_JDBC_DRIVER_CLASSNAME = "jdbc.driver.class_name";
	public static final String PROPERTIES_JDBC_URL = "jdbc.url";
	private static final String PROPERTIES_JDBC_USER_NAME = "jdbc.user.name";
	private static final String PROPERTIES_JDBC_USER_PASSWORD = "jdbc.user.password";

	/**
	 * The properties object.
	 */
	private Properties properties;

	/**
	 * The database connection.
	 */
	public Connection connection;
	private transient java.lang.ref.Cleaner.Cleanable connectionCleanable;

	/**
	 * Constructs a SqlRequester object.
	 *
	 * @param propertiesFileName the properties file name.
	 * 
	 * @exception ClassNotFoundException if unable to load the JDBC driver.
	 * @exception SQLException if unable to create the connection to the database.
	 */
	public SqlRequester(String propertiesFileName) throws IOException {
		properties = PropertiesUtils.load(Engine.CONFIGURATION_PATH + propertiesFileName);
	}

	public String getProperty(String key) {
		return properties.getProperty(key);
	}

	public String getProperty(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
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
		String jdbcUserPassword = Crypto2.decodeFromHexString(getProperty(SqlRequester.PROPERTIES_JDBC_USER_PASSWORD));

		// MariaDB Java 3.x migration
		if (jdbcURL.startsWith("jdbc:mysql:") && "org.mariadb.jdbc.Driver".equals(jdbcClassName)) {
			jdbcURL = "jdbc:mariadb:" + jdbcURL.substring(11);
		}

		Engine.logEngine.debug("[SqlRequester] JDBC URL: " + jdbcURL);
		Engine.logEngine.debug("[SqlRequester] User name: " + jdbcUserName);
		Engine.logEngine.debug("[SqlRequester] User password: " + jdbcUserPassword);

		connection = DriverManager.getConnection(jdbcURL, jdbcUserName, jdbcUserPassword);
		registerConnectionCleanup(connection);

		Engine.logEngine.debug("[SqlRequester] " + text);
	}

	public synchronized void checkConnection() throws ClassNotFoundException, SQLException {
		try {
			if (connection == null || connection.isClosed() || !connection.isValid(30)) {
				open();
			}
		} catch (AbstractMethodError ame) { // jtds unimplemented isValid method : see #374
			if (connection == null || connection.isClosed()) {
				open();
			}
		}
	}

	public synchronized void close() {
		try {
			if (connection != null) {
				connection.close();
				Engine.logEngine.debug("[SqlRequester] Database closed");
			}
		}
		catch (Exception e) {
			Engine.logEngine.error("[SqlRequester] Unable to close the database!", e);
		}
		connection = null;
		if (connectionCleanable != null) {
			connectionCleanable.clean();
			connectionCleanable = null;
		}
	}

	private void registerConnectionCleanup(Connection connection) {
		if (connection == null) {
			return;
		}
		if (connectionCleanable != null) {
			connectionCleanable.clean();
		}
		connectionCleanable = Engine.RESOURCE_CLEANER.register(this, new SqlRequesterCleanup(connection));
	}

	private static final class SqlRequesterCleanup implements Runnable {
		private final Connection connection;

		SqlRequesterCleanup(Connection connection) {
			this.connection = connection;
		}

		@Override
		public void run() {
			if (connection != null) {
				try {
					if (!connection.isClosed()) {
						connection.close();
					}
				} catch (SQLException e) {
					Engine.logEngine.warn("[SqlRequester] Unable to close the database from cleaner", e);
				}
			}
		}
	}

}
