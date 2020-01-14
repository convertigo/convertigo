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

package com.twinsoft.convertigo.engine.admin.services.engine;

import java.sql.ResultSet;
import java.sql.Statement;

import javax.servlet.http.HttpServletRequest;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.admin.services.TextService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.cache.DatabaseCacheManager;
import com.twinsoft.convertigo.engine.util.SqlRequester;
import com.twinsoft.util.StringEx;

@ServiceDefinition(
		name = "Supervision",
		roles = { Role.ANONYMOUS },
		parameters = {},
		returnValue = "the supervision data"
	)
public class Supervision extends TextService {

	@Override
	protected String getServiceResult(HttpServletRequest request) throws Exception {
		boolean isConvertigoStarted = Engine.isStarted;
		boolean bCacheDatabase = false;
		
		String cacheType = EnginePropertiesManager.getProperty(PropertyName.CACHE_MANAGER_CLASS);
		if ("com.twinsoft.convertigo.engine.cache.DatabaseCacheManager".equals(cacheType)) {
			try {
				SqlRequester sqlRequester = new SqlRequester(DatabaseCacheManager.DB_PROP_FILE_NAME);
				sqlRequester.open();
	
				Statement statement = null;
				ResultSet resultSet = null;
				
				StringEx sqlRequest = new StringEx(sqlRequester.getProperty(DatabaseCacheManager.PROPERTIES_SQL_REQUEST_GET_CACHE_ENTRY));
	
				sqlRequest.replace("{RequestString}", "<supervision>");
				String sSqlRequest = sqlRequest.toString();
				try {
					statement = sqlRequester.connection.createStatement();
					resultSet = statement.executeQuery(sSqlRequest);
				}
				finally {
					if (resultSet != null) {
						resultSet.close();
					}
					if (statement != null) {
						statement.close();
					}
					sqlRequester.close();
				}
				bCacheDatabase = true;
			}
			catch(Exception e) {
				System.err.println("supervision: error while the database access for the Convertigo Cache");
				e.printStackTrace();
			}
		}
		
		String result = "";
		result += "convertigo.started="+ (isConvertigoStarted?"OK":"KO") + "\n";
		result += "convertigo.database.cache="+ (bCacheDatabase?"OK":"KO") + "\n";
		
		return result;
	}

}
