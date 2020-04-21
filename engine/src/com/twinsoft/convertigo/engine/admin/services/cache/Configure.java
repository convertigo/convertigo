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

package com.twinsoft.convertigo.engine.admin.services.cache;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceParameterDefinition;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;
import com.twinsoft.convertigo.engine.cache.CacheManager;
import com.twinsoft.convertigo.engine.cache.DatabaseCacheManager;
import com.twinsoft.convertigo.engine.util.Crypto2;
import com.twinsoft.convertigo.engine.util.PropertiesUtils;
import com.twinsoft.convertigo.engine.util.SqlRequester;

@ServiceDefinition(
		name = "Configure",
		roles = { Role.WEB_ADMIN, Role.CACHE_CONFIG },
		parameters = {
				@ServiceParameterDefinition(
						name = "cacheType",
						description = "the cache type : database | file(default)"
					),
				@ServiceParameterDefinition(
						name = "create",
						description = "this option must be added when you want to create a new table in the cache database"
					),
				@ServiceParameterDefinition(
						name = "databaseDriver",
						description = "the driver of the database to use: mysql | oracle | sqlserver(default)"
					),
				@ServiceParameterDefinition(
						name = "databaseServerName",
						description = "the server name to connect"
					),
				@ServiceParameterDefinition(
						name = "databaseServerPort",
						description = "the port of the server to connect"
					),
				@ServiceParameterDefinition(
						name = "databaseName",
						description = "the name of the database to connect"
					),
				@ServiceParameterDefinition(
						name = "user",
						description = "the user in the database"
					),
				@ServiceParameterDefinition(
						name = "password",
						description = "the password in the database"
					),
				@ServiceParameterDefinition(
						name = "cacheTableName",
						description = "the cache table name: <schema>.<table> | CacheTable(default)"
					)
		},
		returnValue = ""
	)
public class Configure extends XmlService {
	
	Document document;	
	Element root;

	Properties dbCacheProp = new Properties();
	String dbCachePropFileName = null;
	
	String sqlServerDriver = "net.sourceforge.jtds.jdbc.Driver";
	String mySQLDriver = "com.mysql.jdbc.Driver";
	String oracleDriver = "oracle.jdbc.driver.OracleDriver";
	
	String cacheManagerDatabaseType = "com.twinsoft.convertigo.engine.cache.DatabaseCacheManager";
	String cacheManagerFileType = "com.twinsoft.convertigo.engine.cache.FileCacheManager";
	
	String cacheType;

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
			
		this.document = document;
		root = document.getDocumentElement();		
		
		cacheType = request.getParameter("cacheType");
		if(cacheType!=null  && cacheType.equals("database")){
			cacheType=cacheManagerDatabaseType;
		}
		else{			
			cacheType=cacheManagerFileType;
		}

		dbCachePropFileName = Engine.CONFIGURATION_PATH + DatabaseCacheManager.DB_PROP_FILE_NAME;
		PropertiesUtils.load(dbCacheProp, dbCachePropFileName);

		try {
			saveProps(request);
		} catch(Exception e) {			
			throw new ServiceException("Unable to save the cache manager properties.",e.getCause());
		}
		
		String create = request.getParameter("create");	
		if (create != null && cacheType.equals(cacheManagerDatabaseType)) {
			boolean dbCreationSupport = true;
			String databaseDriver = dbCacheProp.getProperty("jdbc.driver.class_name");
			String sqlCreateTableFileName = "/create_cache_table_";
			String sqlTest = "select * from CacheTable limit 1";
			String sqlRequest = "";
			
			if (sqlServerDriver.equals(databaseDriver)) {
				sqlCreateTableFileName += "sqlserver.sql";
				sqlTest = "select top 1 * FROM CacheTable";
			} else if (mySQLDriver.equals(databaseDriver)) {
				sqlCreateTableFileName += "mysql.sql";
				sqlTest = "select * from CacheTable limit 1";
			} else if (oracleDriver.equals(databaseDriver)) {
				sqlCreateTableFileName += "oracle.sql";
				sqlTest = "select * from CacheTable where rownum <= 1";
				dbCreationSupport = false;
			}
			
			if (dbCreationSupport) {
				// Create Cache table into Database
				String fileName = Engine.WEBAPP_PATH + "/WEB-INF/sql" + sqlCreateTableFileName;
				BufferedReader br = new BufferedReader(new FileReader(fileName.toString()));
				
				try {
					SqlRequester sqlRequester = null;
					java.sql.Statement statement = null;
					while ((sqlRequest = br.readLine()) != null) {
						try {
							sqlRequester = new SqlRequester(DatabaseCacheManager.DB_PROP_FILE_NAME);
							sqlRequester.open();

							String cacheTableName = sqlRequester.getProperty(DatabaseCacheManager.PROPERTIES_SQL_CACHE_TABLE_NAME,"CacheTable");
							sqlRequest = sqlRequest.substring(0, sqlRequest.length() - 1);
							sqlRequest = sqlRequest.replaceAll("CacheTable", cacheTableName);						
							
							statement = sqlRequester.connection.createStatement();
							statement.execute(sqlRequest);
							ServiceUtils.addMessage(document, root, "Request: \"" + sqlRequest + "\" executed.", "message");
						} finally {
							if (statement != null) {
								statement.close();
							}
							sqlRequester.close();
						}
					}
					ServiceUtils.addMessage(document, root, "Cache table created.", "message");
				} catch (Exception e) {
					throw new ServiceException("Unable to create the cache table.",e);
				} finally {
					br.close();
				}
			}
			
			// Test if Cache table exist
			SqlRequester sqlRequester = null;
			java.sql.Statement statement = null;
			try {
				sqlRequester = new SqlRequester(DatabaseCacheManager.DB_PROP_FILE_NAME);
				sqlRequester.open();

				String cacheTableName = sqlRequester.getProperty(DatabaseCacheManager.PROPERTIES_SQL_CACHE_TABLE_NAME,"CacheTable");
				sqlRequest = sqlTest.replaceAll("CacheTable", cacheTableName);						
				
				statement = sqlRequester.connection.createStatement();
				statement.execute(sqlRequest);
				ServiceUtils.addMessage(document, root, "Request: \"" + sqlRequest + "\" executed.", "message");
				
				ServiceUtils.addMessage(document, root, "Cache table tested.", "message");
			} catch (Exception e) {
				throw new ServiceException("Unable to test the cache table.",e);
			} finally {
				if (statement != null) {
					statement.close();
				}
				sqlRequester.close();
			}
		}	
		restartCacheManager();
	}
				
	
	private void restartCacheManager() throws ServiceException {
	
		try {
			Engine.theApp.cacheManager.destroy();	
			String cacheManagerClassName = EnginePropertiesManager.getProperty(PropertyName.CACHE_MANAGER_CLASS);
			Engine.logAdmin.debug("Cache manager class: " + cacheManagerClassName);
			Engine.theApp.cacheManager = (CacheManager) Class.forName(cacheManagerClassName).getConstructor().newInstance();
			Engine.theApp.cacheManager.init();
		}
		catch(Exception e) {
			String message = "Unable to restart the cache manager.";
			Engine.logAdmin.error(message, e);
			throw new ServiceException(message,e);
		}
		
	}


	private void saveProps(HttpServletRequest request) throws Exception {
	
		if (EnginePropertiesManager.getProperty(PropertyName.CACHE_MANAGER_CLASS) != cacheType ) {
			EnginePropertiesManager.setProperty(PropertyName.CACHE_MANAGER_CLASS, cacheType);						
			EnginePropertiesManager.saveProperties();
		}
		
		if ( cacheManagerDatabaseType.equals(cacheType)) {
			String cacheTableName = request.getParameter("cacheTableName");
			if (cacheTableName == null || cacheTableName.isEmpty()) {
				cacheTableName = "CacheTable";
			}
			
			dbCacheProp.setProperty("sql.table.name", cacheTableName);
			
			String databaseDriver = request.getParameter("databaseDriver");
			if (databaseDriver.equals("mysql")) {
				databaseDriver=mySQLDriver;
			} else if(databaseDriver.equals("sqlserver")) {
				databaseDriver=sqlServerDriver;
			} else if(databaseDriver.equals("oracle")) {
				databaseDriver=oracleDriver;
			}
			
			dbCacheProp.setProperty("jdbc.driver.class_name", databaseDriver);

			String databaseUrl = "jdbc:";
			if (sqlServerDriver.equals(databaseDriver))
				databaseUrl += "jtds:sqlserver://";
			else if (mySQLDriver.equals(databaseDriver))
				databaseUrl += "mysql://";
			else if (oracleDriver.equals(databaseDriver))
				databaseUrl += "oracle:thin:@//";

			String databaseServerName = request.getParameter("databaseServerName");
			if (!databaseServerName.equals(""))
				databaseUrl += databaseServerName;
			String databaseServerPort = request.getParameter("databaseServerPort");
			if (!databaseServerPort.equals(""))
				databaseUrl += ":" + databaseServerPort;
			String databaseName = request.getParameter("databaseName");
			if (!databaseName.equals(""))
				databaseUrl += "/"+ databaseName;

			dbCacheProp.setProperty("jdbc.url", databaseUrl);			
			dbCacheProp.setProperty("jdbc.user.name", request.getParameter("user"));
			dbCacheProp.setProperty("jdbc.user.password", Crypto2.encodeToHexString(request.getParameter("password")));
			
			PropertiesUtils.store(dbCacheProp, dbCachePropFileName);
		} 	
		ServiceUtils.addMessage(document,root,"Cache manager properties succesfully saved.","message");
	}

	
}	