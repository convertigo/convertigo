/**
 * 
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
				}
				bCacheDatabase = true;
			}
			catch(Exception e) {
				System.err.println("supervision: erreur lors de l'accès à la base de données Cache Convertigo");
				e.printStackTrace();
			}
		}
		
		String result = "";
		result += "convertigo.started="+ (isConvertigoStarted?"OK":"KO") + "\n";
		result += "convertigo.database.cache="+ (bCacheDatabase?"OK":"KO") + "\n";
		
		return result;
	}

}
