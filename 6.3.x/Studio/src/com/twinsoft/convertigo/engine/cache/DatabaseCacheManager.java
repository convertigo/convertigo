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

package com.twinsoft.convertigo.engine.cache;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.util.SqlRequester;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.util.StringEx;

public class DatabaseCacheManager extends CacheManager {

	public static final String PROPERTIES_SQL_REQUEST_STORE_RESPONSE		 		= "sql.request.store_response";
	public static final String PROPERTIES_SQL_REQUEST_GET_ID 						= "sql.request.get_id";
	public static final String PROPERTIES_SQL_REQUEST_GET_STORED_RESPONSE			= "sql.request.get_stored_response";
	public static final String PROPERTIES_SQL_REQUEST_REMOVE_RESPONSE		 		= "sql.request.remove_response";
	public static final String PROPERTIES_SQL_REQUEST_UPDATE_CACHE_ENTRY			= "sql.request.update_cache_entry";
	public static final String PROPERTIES_SQL_REQUEST_REMOVE_EXPIRED_CACHE_ENTRY	= "sql.request.remove_expired_cache_entry";
	public static final String PROPERTIES_SQL_REQUEST_GET_CACHE_ENTRY				= "sql.request.get_cache_entry";
	
	public static final String DB_PROP_FILE_NAME			 						= "/database_cache_manager.properties";

	private SqlRequester sqlRequester = null;
	
	public DatabaseCacheManager() {
		Engine.logCacheManager.debug("Using a database cache manager...");
	}
	
	public void init() throws EngineException {
		super.init();

		try {
			sqlRequester = new SqlRequester(DB_PROP_FILE_NAME);
			sqlRequester.open();
		}
		catch ( IOException e ) {
			throw new EngineException("[DatabaseCacheManager] Unable to load the SQLRequester properties file: " + DB_PROP_FILE_NAME, e);
		}
		catch ( SQLException e ) {
			throw new EngineException("[DatabaseCacheManager] Unable to establish connection with the dataBase.", e);
		}
		catch ( ClassNotFoundException e ) {
			throw new EngineException("[DatabaseCacheManager] Unable to load JDBC Driver.", e);
		}
	}
	
	public void destroy() throws EngineException {
		super.destroy();
		
		if (sqlRequester != null)
			sqlRequester.close();
	}

	public void updateCacheEntry(CacheEntry cacheEntry) throws EngineException {
		DatabaseCacheEntry dbCacheEntry = (DatabaseCacheEntry) cacheEntry;
		
		try {
			Engine.logCacheManager.debug("Trying to update cache entry: " + cacheEntry);
			
			StringEx sqlRequest = new StringEx(sqlRequester.getProperty(DatabaseCacheManager.PROPERTIES_SQL_REQUEST_UPDATE_CACHE_ENTRY));

			sqlRequest.replace("{ExpiryDate}", Long.toString(dbCacheEntry.expiryDate));
			sqlRequest.replace("{SheetUrl}", dbCacheEntry.sheetUrl);
			sqlRequest.replace("{AbsoluteSheetUrl}", dbCacheEntry.absoluteSheetUrl);
			sqlRequest.replace("{ContentType}", dbCacheEntry.contentType);

			sqlRequest.replace("{Id}", Long.toString(dbCacheEntry.id));

			String sSqlRequest = sqlRequest.toString();
			Engine.logCacheManager.debug("SQL: " + sSqlRequest);

			Statement statement = null;
			try {
				statement = sqlRequester.connection.createStatement();
				
				// Retry request if needed
				int i = 1;
				do {
					try {
						int nResult = statement.executeUpdate(sSqlRequest);
						Engine.logCacheManager.debug(nResult + " row(s) updated.");
						break;
					} catch (SQLException e) {
						Engine.logCacheManager.warn("updateCacheEntry(): exception while processing SQL request: " + e.getMessage() + "; attempt " + i + " of 3");
						Thread.sleep(500);
						i++;
					}
				} while (i < 4);
			}
			finally {
				if (statement != null) {
					statement.close();
				}
			}
			
			Engine.logCacheManager.debug("The cache entry has been updated.");
		}
		catch(Exception e) {
			throw new EngineException("Unable to update the cache entry!", e);
		}
	}
	
	protected void removeExpiredCacheEntries(long time) throws EngineException {
		try {
			Engine.logCacheManager.debug("Trying to remove expired cache entries from the cache Database");
			
			StringEx sqlRequest = new StringEx(sqlRequester.getProperty(DatabaseCacheManager.PROPERTIES_SQL_REQUEST_REMOVE_EXPIRED_CACHE_ENTRY));
			
			sqlRequest.replace("{CurrentTime}", Long.toString(time));
			String sSqlRequest = sqlRequest.toString();
			Engine.logCacheManager.debug("SQL: " + sSqlRequest);

			Statement statement = null;
			try {
				statement = sqlRequester.connection.createStatement();
				int nResult = statement.executeUpdate(sSqlRequest);
				Engine.logCacheManager.debug(nResult + " row(s) deleted.");
			}
			finally {
				if (statement != null) {
					statement.close();
				}
			}
			
			Engine.logCacheManager.debug("The expired cache entries has been removed.");
		}
		catch(Exception e) {
			throw new EngineException("Unable to remove the expired cache entries!", e);
		}
	}
	
	protected void checkRepository() throws EngineException {
		// Nothing to do
	}
	
	public CacheEntry getCacheEntry(String requestString) throws EngineException {
		try {
			Engine.logCacheManager.debug("Trying to get the cache entry from this request string: "+requestString);
			
			StringEx sqlRequest = new StringEx(sqlRequester.getProperty(DatabaseCacheManager.PROPERTIES_SQL_REQUEST_GET_CACHE_ENTRY));

			sqlRequest.replace("{RequestString}", escapeString(requestString));
			String sSqlRequest = sqlRequest.toString();
			Engine.logCacheManager.debug("SQL: " + sSqlRequest);

			Statement statement = null;
			ResultSet rs = null;
			DatabaseCacheEntry cacheEntry = null;
			try {
				statement = sqlRequester.connection.createStatement();
				rs = statement.executeQuery(sSqlRequest);
				
				if(rs.next()) {
					cacheEntry = new DatabaseCacheEntry();
					cacheEntry.requestString = requestString.trim();
					cacheEntry.id = rs.getLong("Id"); 
					cacheEntry.expiryDate = rs.getLong("ExpiryDate");
					cacheEntry.sheetUrl = rs.getString("SheetUrl");
					if (cacheEntry.sheetUrl != null) cacheEntry.sheetUrl = cacheEntry.sheetUrl.trim();
					cacheEntry.absoluteSheetUrl = rs.getString("AbsoluteSheetUrl");
					if (cacheEntry.absoluteSheetUrl != null) cacheEntry.absoluteSheetUrl = cacheEntry.absoluteSheetUrl.trim();
					cacheEntry.contentType = rs.getString("ContentType");
					if (cacheEntry.contentType != null) cacheEntry.contentType = cacheEntry.contentType.trim();
				}
			}
			finally {
				if (rs != null)
					rs.close();
				if (statement != null)
					statement.close();
			}
			
			Engine.logCacheManager.debug("The cache entry has been retrieved: [" + cacheEntry + "]");

			return cacheEntry;
		}
		catch(Exception e) {
			throw new EngineException("Unable to get the cache entry! (requestString: " + requestString + ")", e);
		}
	}
	
	private String escapeString(String s) {
		StringEx sx = new StringEx(s);
		sx.replaceAll("'", "''");
		return sx.toString();
	}

	protected CacheEntry storeResponse(Document response, String requestString, long expiryDate) throws EngineException {
		try {
			Engine.logCacheManager.debug("Trying to store the response in the cache Database");
			
			StringEx sqlRequest = new StringEx(sqlRequester.getProperty(DatabaseCacheManager.PROPERTIES_SQL_REQUEST_STORE_RESPONSE));

			String xml = XMLUtils.prettyPrintDOM(response);
			sqlRequest.replace("{Xml}", escapeString(xml));
			sqlRequest.replace("{RequestString}", escapeString(requestString));
			sqlRequest.replace("{ExpiryDate}", Long.toString(expiryDate));
						
			Element documentElement = response.getDocumentElement();
			
			String project = documentElement.getAttribute("project");
			sqlRequest.replace("{Project}", project);

			String transaction = documentElement.getAttribute("transaction");
			sqlRequest.replace("{Transaction}", transaction);

			String sSqlRequest = sqlRequest.toString();
			Engine.logCacheManager.debug("SQL: " + sSqlRequest);

			Statement statement = null;
			
			
			try {						
				statement = sqlRequester.connection.createStatement();
				try{
					//sqlServer	
					int nResult = statement.executeUpdate(sSqlRequest);
					Engine.logCacheManager.debug(nResult + " row(s) inserted.");
				}catch(SQLException sqle){
					//mysql						
					Engine.logCacheManager.warn("Request failed: trying to translate it into mySql");
					sqlRequest.replace("[Transaction]", "Transaction");
					sSqlRequest = sqlRequest.toString();
					Engine.logCacheManager.debug("SQL translated into mySQL: " + sSqlRequest);
					int nResult = statement.executeUpdate(sSqlRequest);
					Engine.logCacheManager.debug(nResult + " row(s) inserted.");
				}			
			}finally {
				if (statement != null) {
					statement.close();
				}
			}
			
			DatabaseCacheEntry cacheEntry = new DatabaseCacheEntry();
			cacheEntry.requestString = requestString;
			cacheEntry.id = getId(requestString);
			cacheEntry.expiryDate = expiryDate;

			Engine.logCacheManager.debug("The response has been stored: [" + cacheEntry + "]");

			return cacheEntry;
		}
		catch(Exception e) {
			throw new EngineException("Unable to store the response! (requestString: " + requestString + ")", e);
		}
	}

	protected Document getStoredResponse(Requester requester, CacheEntry cacheEntry) throws EngineException {
		DatabaseCacheEntry dbCacheEntry = (DatabaseCacheEntry) cacheEntry;
		Engine.logCacheManager.debug("cacheEntry=[" + cacheEntry.toString() + "]");

		try {
			Engine.logCacheManager.debug("Trying to get from the cache the stored response corresponding to this cache entry.");
				
			StringEx sqlRequest = new StringEx(sqlRequester.getProperty(DatabaseCacheManager.PROPERTIES_SQL_REQUEST_GET_STORED_RESPONSE));
	
			long id = dbCacheEntry.id;
			sqlRequest.replace("{Id}", Long.toString(id));
	
			Engine.logCacheManager.debug("Replacement done");
	
			String sSqlRequest = sqlRequest.toString();
			Engine.logCacheManager.debug("SQL: " + sSqlRequest);
	
			Statement statement = null;
			ResultSet rs = null;
			Document document = null; 
			try {
				statement = sqlRequester.connection.createStatement();
				rs = statement.executeQuery(sSqlRequest);
				rs.next();

								
				String xml =rs.getString("Xml");

				document = requester.parseDOM(xml);
			}
			finally {
				if (rs != null)
					rs.close();
				if (statement != null) {
					statement.close();
				}
			}
			
			Engine.logCacheManager.debug("Response built from the cache");

			return document;
		}
		catch(Exception e) {
			throw new EngineException("Unable to get the response [" + cacheEntry.toString() + "] from the cache !", e);
		}
	}

	protected synchronized void removeStoredResponse(CacheEntry cacheEntry) throws EngineException {
		DatabaseCacheEntry dbCacheEntry = (DatabaseCacheEntry) cacheEntry;
		Engine.logCacheManager.debug("cacheEntry=[" + cacheEntry.toString() + "]");

		long id = 0;
		
		try {
			Engine.logCacheManager.debug("Trying to remove stored response from the cache Database");
			
			StringEx sqlRequest = new StringEx(sqlRequester.getProperty(DatabaseCacheManager.PROPERTIES_SQL_REQUEST_REMOVE_RESPONSE));

			id = dbCacheEntry.id;
			sqlRequest.replace("{Id}", Long.toString(id));
			Engine.logCacheManager.debug("Replacement done");

			String sSqlRequest = sqlRequest.toString();
			Engine.logCacheManager.debug("SQL: " + sSqlRequest);

			Statement statement = null;
			try {
				statement = sqlRequester.connection.createStatement();
				int nResult = statement.executeUpdate(sSqlRequest);
				Engine.logCacheManager.debug("" + nResult + " row(s) deleted.");
			}
			finally {
				if (statement != null) {
					statement.close();
				}
			}
			Engine.logCacheManager.debug("The stored response has sucessfully been removed.");
		}
		catch(Exception e) {
			throw new EngineException("Unable to remove the response! (id: " + id + ")", e);
		}
	}

	private long getId (String requestString) throws EngineException {
		try {
			Engine.logCacheManager.debug("Trying to get Id.");
				
			StringEx sqlRequest = new StringEx(sqlRequester.getProperty(DatabaseCacheManager.PROPERTIES_SQL_REQUEST_GET_ID));
	
			sqlRequest.replace("{RequestString}", escapeString(requestString));

			String sSqlRequest = sqlRequest.toString();
			Engine.logCacheManager.debug("SQL: " + sSqlRequest);
	
			Statement statement = null;
			ResultSet rs = null;
			long lId = 0;
			try {
				statement = sqlRequester.connection.createStatement();
				rs = statement.executeQuery(sSqlRequest);
				rs.next();

				String sId = new String(rs.getString("Id"));
				lId = Long.parseLong(sId);
			}
			finally {
				if (rs != null)
					rs.close();
				if (statement != null) {
					statement.close();
				}
			}
			
			Engine.logCacheManager.debug("ID retrieved: " + lId);
			return lId;
		} catch(Exception e) {
			throw new EngineException("Unable to get the Id.", e);
		}
	}
}
