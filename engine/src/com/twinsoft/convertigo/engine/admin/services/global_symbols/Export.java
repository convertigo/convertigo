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

package com.twinsoft.convertigo.engine.admin.services.global_symbols;

import java.io.Writer;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.util.PropertiesUtils;

@ServiceDefinition(
		name = "Export", 
		roles = { Role.WEB_ADMIN, Role.SYMBOLS_CONFIG, Role.SYMBOLS_VIEW }, 
		parameters = {}, 
		returnValue = "return the global_symbols.properties file"
	)

public class Export extends DownloadService {

	@Override
	protected void writeResponseResult(HttpServletRequest request, HttpServletResponse response) throws Exception {
		//We recover selected symbols 
		String symbols = request.getParameter("symbols");
		
		
		if (StringUtils.isNotEmpty(symbols)) {
			//Parse string requested parameter to JSON
			JSONArray symbolsNames = new JSONArray(symbols);
			
			Properties properties = new Properties();

			//Write symbols saved with name and value for each requested/selected symbols
			for (int i = 0; i < symbolsNames.length(); i++) {
				JSONObject jo = symbolsNames.getJSONObject(i);
				String name = jo.getString("name");
				String symbolValue = Engine.theApp.databaseObjectsManager.symbolsGetValueStore(name);
				properties.setProperty(name, symbolValue);
			}

			HeaderName.ContentDisposition.setHeader(response,
					"attachment; filename=\"global_symbols.properties\"");
			response.setContentType(MimeType.Plain.value());
			response.setCharacterEncoding("UTF-8");
			try (Writer writer = response.getWriter()) {
				PropertiesUtils.store(properties, writer, "global symbols");
			}
			String message = "The global symbols file has been exported.";
			Engine.logAdmin.info(message);
		} else {
			String message = "Error when parsing the requested parameter!";
			Engine.logAdmin.error(message);
			throw new Exception ("Error when parsing the requested parameter!");
		}
	}
}
