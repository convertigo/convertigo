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

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.util.StringEx;

public abstract class PobiBiller extends FrontalBiller {
	protected static Object semaphore = new Object();
	
	public PobiBiller() throws IOException {
		super();
	}
	
	public abstract double getCostImpl(Context context, Object data) throws Exception;
	
	protected double getFibenCost(String module) {
		double point = Double.parseDouble(frontalProperties.getProperty("cost.fiben.point"));
		int modulePoints = Integer.parseInt(frontalProperties.getProperty("cost.fiben.module." + module));
		double cost = point * modulePoints;
		//System.out.println("Module: " + module);
		//System.out.println("Point: " + point);
		//System.out.println("Module points: " + modulePoints);
		//System.out.println("Cost: " + cost);
	
		return cost;
	}
	
	protected double getDunCost(String siren, String code) throws SQLException {
		synchronized(PobiBiller.semaphore) {
			Statement statement = null;

			boolean bNewRequest = true;
				
			String sSqlRequest = sqlRequester.getProperty(PobiBiller.PROPERTIES_SQL_REQUEST_DELETE_REQUESTS);
			Engine.logBillers.debug("[PobiBiller] SQL: " + sSqlRequest);

			try {
				statement = sqlRequester.connection.createStatement();
				statement.execute(sSqlRequest);

				Engine.logBillers.debug("[PobiBiller] Requests of previous days deleted");
			}
			catch(SQLException e) {
				Engine.logBillers.warn("[PobiBiller] Unable to delete requests history from the database; ignoring.\n" + e.getMessage() + " (error code: " + e.getErrorCode() + ")\nSQL: " + sSqlRequest);
			}
			catch(Exception e) {
				Engine.logBillers.error("[PobiBiller] Unable to delete requests history from the database; ignoring.", e);
			}
			finally {
				if (statement != null) {
					statement.close();
				}
			}
				
			StringEx sqlRequest = new StringEx(sqlRequester.getProperty(PobiBiller.PROPERTIES_SQL_REQUEST_GET_REQUEST));

			sqlRequest.replace("{RefClient}", certificate);
			sqlRequest.replace("{CleBdf}", siren);
			sqlRequest.replace("{SousCleCompOp}", "=");
			sqlRequest.replace("{SousCle}", code);
			sqlRequest.replace("{Application}", "dun");

			sSqlRequest = sqlRequest.toString();
			Engine.logBillers.debug("[PobiBiller] SQL: " + sSqlRequest);

			try {
				statement = sqlRequester.connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sSqlRequest);

				resultSet.next();
				bNewRequest = (resultSet.getInt("nbclebdf") == 0);

				Engine.logBillers.debug("[PobiBiller] New request for the current day: " + bNewRequest);
			}
			catch(SQLException e) {
				Engine.logBillers.warn("[PobiBiller] Unable to get requests history from the database; ignoring and assuming a new request.\n" + e.getMessage() + " (error code: " + e.getErrorCode() + ")\nSQL: " + sSqlRequest);
			}
			catch(Exception e) {
				Engine.logBillers.error("[PobiBiller] Unable to get requests history from the database; ignoring and assuming a new request.", e);
				bNewRequest = true;
			}
			finally {
				if (statement != null) {
					statement.close();
				}
			}

			if (bNewRequest) {
				double moduleCost = Double.parseDouble(frontalProperties.getProperty("cost.dun.module." + code));
				double pointCost = Double.parseDouble(frontalProperties.getProperty("cost.point"));
				double cost = (moduleCost / pointCost);
				BigDecimal bd = new BigDecimal(cost);
				bd = bd.setScale(2,BigDecimal.ROUND_HALF_UP);

				double responseCost = bd.doubleValue();

				sqlRequest = new StringEx(sqlRequester.getProperty(PobiBiller.PROPERTIES_SQL_REQUEST_INSERT_REQUEST));
				sqlRequest.replace("{RefClient}", certificate);
				sqlRequest.replace("{CleBdf}", siren);
				sqlRequest.replace("{SousCle}", code);
				sqlRequest.replace("{Application}", "dun");
		
				Calendar rightNow = Calendar.getInstance();
				SimpleDateFormat df = new SimpleDateFormat(sqlRequester.getProperty(Biller.PROPERTIES_SQL_DATE_FORMAT));
				String date = df.format(rightNow.getTime());
				sqlRequest.replace("{Date}", date);
		
				sSqlRequest = sqlRequest.toString();
				Engine.logBillers.debug("[PobiBiller] SQL: " + sSqlRequest);
		
				try {
					statement = sqlRequester.connection.createStatement();
					int nResult = statement.executeUpdate(sSqlRequest);

					Engine.logBillers.debug("[PobiBiller] " + nResult + " row(s) inserted.");
				}
				catch(SQLException e) {
					Engine.logBillers.warn("[PobiBiller] Unable to update requests history from the database; ignoring.\n" + e.getMessage() + " (error code: " + e.getErrorCode() + ")\nSQL: " + sSqlRequest);
				}
				catch(Exception e) {
					Engine.logBillers.error("Unable to update requests history from the database; ignoring.", e);
				}
				finally {
					if (statement != null) {
						statement.close();
					}
				}

				return responseCost;
			}
			else {
				return 0;
			}
		}
	}
	
	protected double getApplicationCost(String application, String cleBDF, String sousCle, int nbp) throws SQLException {
		synchronized(PobiBiller.semaphore) {
			Statement statement = null;

			boolean bNewRequest = true;
				
			String sSqlRequest = sqlRequester.getProperty(PobiBiller.PROPERTIES_SQL_REQUEST_DELETE_REQUESTS);
			Engine.logBillers.debug("[PobiBiller] SQL: " + sSqlRequest);

			try {
				statement = sqlRequester.connection.createStatement();
				statement.execute(sSqlRequest);

				Engine.logBillers.debug("[PobiBiller] Requests of previous days deleted");
			}
			catch(SQLException e) {
				Engine.logBillers.warn("[PobiBiller] Unable to delete requests history from the database; ignoring.\n" + e.getMessage() + " (error code: " + e.getErrorCode() + ")\nSQL: " + sSqlRequest);
			}
			catch(Exception e) {
				Engine.logBillers.error("[PobiBiller] Unable to delete requests history from the database; ignoring.", e);
			}
			finally {
				if (statement != null) {
					statement.close();
				}
			}
				
			StringEx sqlRequest = new StringEx(sqlRequester.getProperty(PobiBiller.PROPERTIES_SQL_REQUEST_GET_REQUEST));

			sqlRequest.replace("{RefClient}", certificate);
			sqlRequest.replace("{CleBdf}", cleBDF);
			sqlRequest.replace("{SousCleCompOp}", "=");
			sqlRequest.replace("{SousCle}", sousCle);
			sqlRequest.replace("{Application}", application);

			sSqlRequest = sqlRequest.toString();
			Engine.logBillers.debug("[PobiBiller] SQL: " + sSqlRequest);

			try {
				statement = sqlRequester.connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sSqlRequest);

				resultSet.next();
				bNewRequest = (resultSet.getInt("nbclebdf") == 0);

				Engine.logBillers.debug("[PobiBiller] New request for the current day: " + bNewRequest);
			}
			catch(SQLException e) {
				Engine.logBillers.warn("[PobiBiller] Unable to get requests history from the database; ignoring and assuming a new request.\n" + e.getMessage() + " (error code: " + e.getErrorCode() + ")\nSQL: " + sSqlRequest);
			}
			catch(Exception e) {
				Engine.logBillers.error("[PobiBiller] Unable to get requests history from the database; ignoring and assuming a new request.", e);
				bNewRequest = true;
			}
			finally {
				if (statement != null) {
					statement.close();
				}
			}

			if (bNewRequest) {
				String sCost = frontalProperties.getProperty("cost." + application + ".reponse");
				double responseCost = Double.parseDouble(sCost) * nbp;

				sqlRequest = new StringEx(sqlRequester.getProperty(PobiBiller.PROPERTIES_SQL_REQUEST_INSERT_REQUEST));
				sqlRequest.replace("{RefClient}", certificate);
				sqlRequest.replace("{CleBdf}", cleBDF);
				sqlRequest.replace("{SousCle}", sousCle);
				sqlRequest.replace("{Application}", application);
		
				Calendar rightNow = Calendar.getInstance();
				SimpleDateFormat df = new SimpleDateFormat(sqlRequester.getProperty(Biller.PROPERTIES_SQL_DATE_FORMAT));
				String date = df.format(rightNow.getTime());
				sqlRequest.replace("{Date}", date);
		
				sSqlRequest = sqlRequest.toString();
				Engine.logBillers.debug("[PobiBiller] SQL: " + sSqlRequest);
		
				try {
					statement = sqlRequester.connection.createStatement();
					int nResult = statement.executeUpdate(sSqlRequest);

					Engine.logBillers.debug("[PobiBiller] " + nResult + " row(s) inserted.");
				}
				catch(SQLException e) {
					Engine.logBillers.warn("[PobiBiller] Unable to update requests history from the database; ignoring.\n" + e.getMessage() + " (error code: " + e.getErrorCode() + ")\nSQL: " + sSqlRequest);
				}
				catch(Exception e) {
					Engine.logBillers.error("Unable to update requests history from the database; ignoring.", e);
				}
				finally {
					if (statement != null) {
						statement.close();
					}
				}

				return responseCost;
			}
			else {
				return 0;
			}
		}
	}	
}
