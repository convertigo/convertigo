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

package com.twinsoft.convertigo.engine.plugins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.engine.CertificateManager;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.util.StringEx;

public abstract class FrontalBiller extends Biller {
	private static final String PROPERTIES_FRONTAL_BILLER_COSTS_POLICY = "frontal.biller.costs.policy";
	private static final String DATABASE_POLICY = "database";
	private static final String FILE_POLICY = "file";
	
	public static final String PROPERTIES_SQL_REQUEST_INSERT_REQUEST = "sql.request.insert_request";
	public static final String PROPERTIES_SQL_REQUEST_GET_REQUEST = "sql.request.get_request";
	public static final String PROPERTIES_SQL_REQUEST_DELETE_REQUESTS = "sql.request.delete_requests";

	public static final String PROPERTIES_SQL_REQUEST_GET_COSTS = "sql.request.get_costs";
	
	public FrontalBiller() throws IOException {
		super();
	}
	
	Properties costProperties;
	String certificate;
	
	protected String getService(Context context, Object data) {
		return context.tasServiceCode;
	}
	
	protected Properties loadFromDatabase(Context context) {
		Properties properties = new Properties();
		
		String projectName = context.projectName;
		String sSqlRequest = null;
		try {
			Engine.logBillers.debug("[FrontalBiller] Trying to retrieve costs from database for project \""+projectName+"\"");
			
			StringEx sqlRequest = new StringEx(sqlRequester.getProperty(PROPERTIES_SQL_REQUEST_GET_COSTS));
			sqlRequest.replace("{project}", projectName);
			sSqlRequest = sqlRequest.toString();
			
			Statement statement = null;
			ResultSet resultSet = null;
			try {
				statement = sqlRequester.connection.createStatement();
				resultSet = statement.executeQuery(sSqlRequest);
				
				String key, value;
				while (resultSet.next()) {
					key = resultSet.getString("key");
					value = resultSet.getString("value");
					properties.put(key, value);
				}
			}
			finally {
				if (resultSet != null) {
					resultSet.close();
				}
				if (statement != null) {
					statement.close();
				}
			}
		}
		catch(SQLException e) {
			Engine.logBillers.warn("[FrontalBiller] Unable to retrieve costs for project \"" + projectName + "\".\n" + e.getMessage() + " (error code: " + e.getErrorCode() + ")\nSQL: " + sSqlRequest);
		}
		catch(Exception e) {
			Engine.logBillers.error("[FrontalBiller] Unable to retrieve costs for project \"" + projectName + "\".", e);
		}
		
		return properties;
	}
	
	protected Properties loadCosts(Context context) throws FileNotFoundException, IOException {
		String policy = Engine.theApp.pluginsManager.getProperty(PROPERTIES_FRONTAL_BILLER_COSTS_POLICY, FILE_POLICY);
		if (DATABASE_POLICY.equals(policy))
			return loadFromDatabase(context);
		// FILE_POLICY : default
		return loadFromFile(context);
	}
	
	public double getCost(Context context, Object data) {
		try {
			costProperties = loadCosts(context);
			
			Connector connector = context.getConnector();
			CertificateManager certificateManager = null;
			if (connector instanceof HttpConnector) {
				certificateManager = ((HttpConnector) connector).certificateManager;
			}
			else if (connector instanceof SiteClipperConnector) {
				certificateManager = ((SiteClipperConnector) connector).certificateManager;
			} 
			
			certificate = new File(certificateManager.keyStore).getName();
			int idx = certificate.indexOf('.');
			if (idx != -1 ) {
				certificate = certificate.substring(0, idx);
			}
		
			return getCostImpl(context, data);
		}
		catch(Throwable e) {
			Engine.logBillers.error("[FrontalBiller] Unable to calculate the transaction cost; aborting billing.", e);
		}
		
		return -1;
	}
	
	public abstract double getCostImpl(Context context, Object data) throws Exception;
	
}
