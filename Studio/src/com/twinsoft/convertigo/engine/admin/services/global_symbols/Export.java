/*
 * Copyright (c) 2001-2014 Convertigo SA.
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
 */

package com.twinsoft.convertigo.engine.admin.services.global_symbols;

import java.io.PrintStream;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "Export", 
		roles = { Role.WEB_ADMIN }, 
		parameters = {}, 
		returnValue = "return the global_symbols.properties file"
	)

public class Export extends DownloadService {

	@Override
	protected void writeResponseResult(HttpServletRequest request, HttpServletResponse response) throws Exception {
		//We recover selected symbols 
		String symbols = "{ symbols : [" + request.getParameter("symbols") + "] }";
		
		
		if ( symbols != null && !symbols.equals("") ) {
			//Parse string requested parameter to JSON
			JSONObject jsonObj = new JSONObject(symbols);
			JSONArray symbolsNames = jsonObj.getJSONArray("symbols");
			
			//Write header information
			String writedString = "#global symbols\n";
			writedString += "#" + new Date() + "\n";

			//Write symbols saved with name and value for each requested/selected symbols
			for (int i = 0; i < symbolsNames.length(); i++) {
				JSONObject jo = symbolsNames.getJSONObject(i);
				String symbolValue = Engine.theApp.databaseObjectsManager
						.symbolsGetValue(jo.getString("name"));
				writedString += jo.getString("name") + "=" + symbolValue + "\n";
			}

			response.setHeader("Content-Disposition",
					"attachment; filename=\"global_symbols.properties\"");
			response.setContentType("text/plain");
			
			//We directly write the concatenated string into the output stream of response
			PrintStream printStream = new PrintStream(response.getOutputStream());
			printStream.print(writedString);
			printStream.close();

			String message = "The global symbols file has been exported.";
			Engine.logAdmin.info(message);
		} else {
			String message = "Error when parsing the requested parameter!";
			Engine.logAdmin.error(message);
			throw new Exception ("Error when parsing the requested parameter!");
		}
	}
}
