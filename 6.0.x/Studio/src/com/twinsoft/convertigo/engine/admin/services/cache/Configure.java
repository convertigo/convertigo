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

package com.twinsoft.convertigo.engine.admin.services.cache;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceParameterDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition.Role;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;
import com.twinsoft.convertigo.engine.cache.CacheManager;
import com.twinsoft.convertigo.engine.cache.DatabaseCacheManager;
import com.twinsoft.convertigo.engine.util.SqlRequester;
import com.twinsoft.util.DESKey;

@ServiceDefinition(
		name = "Configure",
		roles = { Role.WEB_ADMIN },
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
						description = "the driver of the database to use: mysql | sqlserver(default)"
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

		String create = request.getParameter("create");	
		dbCachePropFileName = Engine.CONFIGURATION_PATH + DatabaseCacheManager.DB_PROP_FILE_NAME;
		dbCacheProp.load(new FileInputStream(dbCachePropFileName.toString()));

		try {
			saveProps(request);

		} catch(Exception e) {			
			throw new ServiceException("Unable to save the cache manager properties.",e.getCause());
		}
		
		if(create!=null && cacheType.equals(cacheManagerDatabaseType)) {		
		
			String sqlRequest = "";
			try {
				String databaseDriver = dbCacheProp.getProperty("jdbc.driver.class_name");
				String sqlCreateTableFileName = "/create_cache_table_";
				if (sqlServerDriver.equals(databaseDriver))
					sqlCreateTableFileName += "sqlserver.sql";
				else if (mySQLDriver.equals(databaseDriver))
					sqlCreateTableFileName += "mysql.sql";
			
				String fileName = Engine.WEBAPP_PATH + "/WEB-INF/sql" + sqlCreateTableFileName;
				BufferedReader br = new BufferedReader(new FileReader(fileName.toString()));
				
				SqlRequester sqlRequester =null;
				java.sql.Statement statement = null;
				while ( (sqlRequest = br.readLine()) != null ) {
					try {
						sqlRequester = new SqlRequester(DatabaseCacheManager.DB_PROP_FILE_NAME);
						sqlRequester.open();

						sqlRequest = sqlRequest.substring(0, sqlRequest.length() - 1);						
						statement =  sqlRequester.connection.createStatement();
						statement.execute(sqlRequest);
						ServiceUtils.addMessage(document,root,"Request: \""+sqlRequest+"\" executed.","message");
						
					}finally {
						if (statement != null) {
							statement.close();
						}
						sqlRequester.close();
					}
				}
	
			} catch(Exception e) {			
				throw new ServiceException("Unable to create the cache table.",e);
			}
	
		}	
		restartCacheManager();
	}
				
	
	private void restartCacheManager() throws ServiceException {
	
		try {
			Engine.theApp.cacheManager.destroy();	
			String cacheManagerClassName = EnginePropertiesManager.getProperty(PropertyName.CACHE_MANAGER_CLASS);
			Engine.logEngine.debug("Cache manager class: " + cacheManagerClassName);
			Engine.theApp.cacheManager = (CacheManager) Class.forName(cacheManagerClassName).newInstance();
			Engine.theApp.cacheManager.init();

			Thread vulture = new Thread(Engine.theApp.cacheManager);
			Engine.theApp.cacheManager.executionThread = vulture;
			vulture.setName("CacheManager");
			vulture.start();
		}
		catch(Exception e) {
			String message="Unable to restart the cache manager.";
			Engine.logEngine.error(message, e);
			throw new ServiceException(message,e);
		}
		
	}


	private void saveProps(HttpServletRequest request) throws Exception {
	
		if (EnginePropertiesManager.getProperty(PropertyName.CACHE_MANAGER_CLASS) != cacheType ) {
			EnginePropertiesManager.setProperty(PropertyName.CACHE_MANAGER_CLASS, cacheType);						
			EnginePropertiesManager.saveProperties();
		}
		
		if ( cacheManagerDatabaseType.equals(cacheType)) {
			String databaseDriver = request.getParameter("databaseDriver");
			if(databaseDriver.equals("mysql")){
				databaseDriver=mySQLDriver;
			}
			else{
				databaseDriver=sqlServerDriver;
			}
			
			dbCacheProp.setProperty("jdbc.driver.class_name", databaseDriver);

			String databaseUrl = "jdbc:";
			if (sqlServerDriver.equals(databaseDriver))
				databaseUrl += "jtds:sqlserver://";
			else if (mySQLDriver.equals(databaseDriver))
				databaseUrl += "mysql://";

			String databaseServerName = request.getParameter("databaseServerName");
			if (!databaseServerName.equals(""))
				databaseUrl += databaseServerName;
			String databaseServerPort = request.getParameter("databaseServerPort");
			if (!databaseServerPort.equals(""))
				databaseUrl += ":" + databaseServerPort;
			databaseUrl += "/";
			String databaseName = request.getParameter("databaseName");
			if (!databaseName.equals(""))
				databaseUrl += databaseName;

			dbCacheProp.setProperty("jdbc.url", databaseUrl);			
			dbCacheProp.setProperty("jdbc.user.name", request.getParameter("user"));
			dbCacheProp.setProperty("jdbc.user.password", DESKey.encodeToHexString(request.getParameter("password")));

			FileOutputStream fos = new FileOutputStream(dbCachePropFileName.toString());
			dbCacheProp.store(fos, "");
			fos.flush();
			fos.close();
		} 	
		ServiceUtils.addMessage(document,root,"Cache manager properties succesfully saved.","message");
	}

	
}	