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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.SqlRequester;
import com.twinsoft.util.StringEx;

public class VicApi extends SqlRequester {

	public static final String PROPERTIES_SQL_GET_AUTHORIZATION = "sql.request.get_authorization";

	public VicApi() throws IOException {
		super("/vicapi.properties");
	}
	
	public boolean isServiceAuthorized(String userName, String virtualServer, String service) {
		String sSqlRequest = null;
		try {
			Engine.logBillers.debug("[VicApi] >>> isServiceAutorized()");
			Engine.logBillers.debug("[VicApi] User: " + userName);
			Engine.logBillers.debug("[VicApi] Virtual server: " + virtualServer);
			Engine.logBillers.debug("[VicApi] Service: " + service);
			open();
			
			StringEx sqlRequest = new StringEx(getProperty(VicApi.PROPERTIES_SQL_GET_AUTHORIZATION));
			sqlRequest.replace("{UserName}", userName);
			sqlRequest.replace("{VirtualServer}", virtualServer);
			sqlRequest.replace("{Service}", service);

			sSqlRequest = sqlRequest.toString();
			Engine.logBillers.debug("[VicApi] SQL: " + sSqlRequest);

			Statement statement = null;
			try {
				statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sSqlRequest);

				boolean isServiceAuthorized = resultSet.next();
				Engine.logBillers.debug("[VicApi] isServiceAuthorized: " + isServiceAuthorized);
				
				return isServiceAuthorized;
			}
			finally {
				if (statement != null) {
					statement.close();
				}
				close();
			}
		}
		catch(SQLException e) {
			Engine.logBillers.warn("[VicApi] Unable to execute the SQL request.\n" + e.getMessage() + " (error code: " + e.getErrorCode() + ")\nSQL: " + sSqlRequest);
		}
		catch(Exception e) {
			Engine.logBillers.error("[VicApi] Unable to determine whether the service is authorized or not.", e);
		}
		return false;
	}
}
