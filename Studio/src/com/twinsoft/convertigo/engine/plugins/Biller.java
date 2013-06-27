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
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.engine.CertificateManager;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.SqlRequester;
import com.twinsoft.util.StringEx;

public abstract class Biller extends AbstractBiller {
	
	public static final String PROPERTIES_SQL_REQUEST_INSERT_BILLING = "sql.request.insert_billing";
	public static final String PROPERTIES_SQL_DATE_FORMAT = "sql.date_format";

	protected SqlRequester sqlRequester;
	
	/**
	 * Constructs a Biller object.
	 * 
	 * @exception ClassNotFoundException if unable to load the JDBC driver.
	 * @exception SQLException if unable to create the connection to the database.
	 */
	public Biller() throws IOException {
		sqlRequester = new SqlRequester("/biller.properties");
	}

	protected abstract double getCost(Context context, Object data);
	protected abstract String getService(Context context, Object data);
	protected abstract String getModule(Context context, Object data);
	protected abstract String getDataKey(Context context, Object data);
	
	public void insertBilling(Context context) throws EngineException {
		insertBilling(context, null);
	}
	
	public void insertBilling(Context context, Object data) throws EngineException {
		if (context.isRequestFromVic) {
			insertVicBilling(context, data);
		}
		else {
			insertCariocaBilling(context, data);
		}
	}
	
	public void insertVicBilling(Context context) {
		insertVicBilling(context, null);
	}

	public void insertVicBilling(Context context, Object data) {
		String sSqlRequest = null;
		try {
			Engine.logBillers.debug("[Biller] Trying to insert the billing into a VIC database ");
			sqlRequester.open();

			CertificateManager certificateManager = ((HttpConnector) context.getConnector()).certificateManager;
			if (!certificateManager.storeInformationCollected) {
				certificateManager.collectStoreInformation(context);
			}

			int cache = 0;
			double cost = getCost(context, data);
			if (cost == -1) {
				Engine.logBillers.debug("[Biller] Billing aborted because the returned cost is -1, i.e. do not need to bill.");
				return;
			} 
			else if (cost == -2) {
				Engine.logBillers.debug("[Biller] Billing zero cost because the response was in cache.");
				cost = 0;
				cache = 1;
			} 

			TransactionWithVariables transaction = null;
			try {
				transaction = (TransactionWithVariables) context.requestedObject;
			}
			catch (ClassCastException e) {
				throw new Exception("Requested object is not a transaction");
			}
			
			StringEx sqlRequest = new StringEx(sqlRequester.getProperty(Biller.PROPERTIES_SQL_REQUEST_INSERT_BILLING));
			sqlRequest.replace("{UserName}", context.tasUserName);
			sqlRequest.replace("{UserGroup}", context.tasUserGroup);
			sqlRequest.replace("{Service}", context.tasServiceCode);
			sqlRequest.replace("{CVX25}", "0");
			String cdbanque = transaction.getVariableValue("cdbanque").toString();
			if (cdbanque == null) cdbanque = context.get("cdbanque").toString();
			String cdguichet = transaction.getVariableValue("cdguichet").toString();
			if (cdguichet == null) cdguichet = context.get("cdguichet").toString();
			sqlRequest.replace("{CodeBanque}", cdbanque);
			sqlRequest.replace("{CodeGuichet}", cdguichet);

			String certificate = new File(certificateManager.keyStore).getName();
			int idx = certificate.indexOf('.');
			if (idx != -1 ) {
				certificate = certificate.substring(0, idx);
			}
			sqlRequest.replace("{Certificat}", certificate);
			sqlRequest.replace("{Cache}", Integer.toString(cache));

			Engine.logBillers.debug("[Biller] Replacement done");
		
			Calendar rightNow = Calendar.getInstance();
			SimpleDateFormat df = new SimpleDateFormat(sqlRequester.getProperty(Biller.PROPERTIES_SQL_DATE_FORMAT));
			String date = df.format(rightNow.getTime());
			sqlRequest.replace("{StartHour}", date);
			sqlRequest.replace("{EndHour}", date);
			Engine.logBillers.debug("[Biller] Start and End hour done");
		
			sqlRequest.replace("{NomSv}", context.tasVirtualServerName);
			Engine.logBillers.debug("[Biller] NomSv done");
			sqlRequest.replace("{Cost}", Double.toString(cost));
			Engine.logBillers.debug("[Biller] Cost Done");
			sSqlRequest = sqlRequest.toString();
			Engine.logBillers.debug("[Biller] SQL: " + sSqlRequest);

			Statement statement = null;
			try {
				statement = sqlRequester.connection.createStatement();
				int nResult = statement.executeUpdate(sSqlRequest);
				Engine.logBillers.debug("[Biller] " + nResult + " row(s) inserted.");
			}
			finally {
				if (statement != null) {
					statement.close();
				}
				sqlRequester.close();
			}
		}
		catch(SQLException e) {
			Engine.logBillers.warn("[Biller] Unable to insert the billing.\n" + e.getMessage() + " (error code: " + e.getErrorCode() + ")\nSQL: " + sSqlRequest);
		}
		catch(Exception e) {
			Engine.logBillers.error("[Biller] Unable to insert the billing", e);
		}
	}

	public void insertCariocaBilling(Context context) throws EngineException {
		insertCariocaBilling(context, null);
	}

	public void insertCariocaBilling(Context context, Object data) throws EngineException {
		String sSqlRequest = null;
		try {
			int cache = 0;
			double cost = getCost(context, data);
			if (cost == -1) {
				Engine.logBillers.debug("[Biller] Billing aborted because the returned cost is -1, i.e. do not need to bill.");
				return;
			} 
			else if (cost == -2) {
				Engine.logBillers.debug("[Biller] Billing zero cost because the response was in cache.");
				cost = 0;
				cache = 1;
			}

			Connector connector = context.getConnector();
			CertificateManager certificateManager = null;
			if (connector instanceof HttpConnector) {
				certificateManager = ((HttpConnector) connector).certificateManager;
			}
			else if (connector instanceof SiteClipperConnector) {
				certificateManager = ((SiteClipperConnector) connector).certificateManager;
			} 
			
			if (!certificateManager.storeInformationCollected) {
				certificateManager.collectStoreInformation(context);
			}

			String certificate = new File(certificateManager.keyStore).getName();
			int idx = certificate.indexOf('.');
			if (idx != -1 ) {
				certificate = certificate.substring(0, idx);
			}

			Statement statement = null;
			try {
				Engine.logBillers.debug("[Biller] Replacements from the context done");
				
				Engine.logBillers.debug("[Biller] Trying to insert the billing into a Carioca database ");
				sqlRequester.open();
				
				StringEx sqlRequest = new StringEx(sqlRequester.getProperty(Biller.PROPERTIES_SQL_REQUEST_INSERT_BILLING));
				
				try {
					Engine.logBillers.debug("[Biller] Replacing TAS IDs");
					sqlRequest.replace("{IDSVR}", context.get("IDSVR").toString());
					sqlRequest.replace("{IDSERV}", context.get("IDSERV").toString());
					sqlRequest.replace("{IDUSER}", context.get("IDUSER").toString());
					sqlRequest.replace("{IDPROF}", context.get("IDPROF").toString());
					sqlRequest.replace("{IDEMUL}", context.get("IDEMUL").toString());

					Engine.logBillers.debug("[Biller] Replacing TAS variables");
					sqlRequest.replaceSQL("{NomSv}", context.tasVirtualServerName, '\'');
					sqlRequest.replaceSQL("{UserName}", context.tasUserName, '\'');
					sqlRequest.replaceSQL("{UserGroup}", context.tasUserGroup, '\'');
					sqlRequest.replaceSQL("{Service}", getService(context, data), '\'');
					
					Engine.logBillers.debug("[Biller] Replacing POBI variables");
					sqlRequest.replace("{cdbanque}", context.get("cdbanque").toString());
					sqlRequest.replace("{cdguichet}", context.get("cdguichet").toString());
					sqlRequest.replaceSQL("{certificat}", certificate, '\'');
					sqlRequest.replace("{cache}", Integer.toString(cache));
					sqlRequest.replaceSQL("{module}", getModule(context, data), '\'');
					sqlRequest.replaceSQL("{userdata}", context.get("userdata").toString(), '\'');
					sqlRequest.replaceSQL("{BDFKey}", getDataKey(context, data), '\'');
					sqlRequest.replaceSQL("{UserGroupAuto}", context.get("UserGroupAuto").toString(), '\'');
				}
				catch(NullPointerException e) {
					throw new EngineException("One parameter for SQL replacement is missing.", e);
				}

				Calendar rightNow = Calendar.getInstance();
				SimpleDateFormat df = new SimpleDateFormat(sqlRequester.getProperty(Biller.PROPERTIES_SQL_DATE_FORMAT));
				String date = df.format(rightNow.getTime());
				sqlRequest.replace("{StartHour}", date);
				sqlRequest.replace("{EndHour}", date);
				Engine.logBillers.debug("[Biller] Start and End hour computed");
			
				sqlRequest.replace("{Cost}", Double.toString(cost));
				Engine.logBillers.debug("[Biller] Cost computed");
				
				sSqlRequest = sqlRequest.toString();
				Engine.logBillers.debug("[Biller] SQL: " + sSqlRequest);

				statement = sqlRequester.connection.createStatement();
				int nResult = statement.executeUpdate(sSqlRequest);
				Engine.logBillers.debug("[Biller] " + nResult + " row(s) inserted.");
			}
			finally {
				if (statement != null) {
					statement.close();
				}
				sqlRequester.close();
			}
		}
		catch(SQLException e) {
			Engine.logBillers.warn("[Biller] Unable to insert the billing.\n" + e.getMessage() + " (error code: " + e.getErrorCode() + ")\nSQL: " + sSqlRequest);
		}
		catch(Exception e) {
			Engine.logBillers.error("[Biller] Unable to insert the billing", e);
		}
	}
}
