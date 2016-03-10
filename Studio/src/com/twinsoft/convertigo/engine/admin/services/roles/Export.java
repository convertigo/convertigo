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

package com.twinsoft.convertigo.engine.admin.services.roles;

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
		returnValue = "return the users.properties file"
	)

public class Export extends DownloadService {

	@Override
	protected void writeResponseResult(HttpServletRequest request, HttpServletResponse response) throws Exception {
		//We recover selected users 
		String users = "{ users : [" + request.getParameter("users") + "] }";
		
		
		if ( users != null && !users.equals("") ) {
			//Parse string requested parameter to JSON
//			JSONObject jsonObj = new JSONObject(users);
//			JSONArray usersNames = jsonObj.getJSONArray("users");
			
			//Write header information
			String writedString = "#users\n";
			writedString += "#" + new Date() + "\n";

			//Write users saved with name and value for each requested/selected users
//			for (int i = 0; i < usersNames.length(); i++) {
//				JSONObject jo = usersNames.getJSONObject(i);
//				String userValue = Engine.theApp.databaseObjectsManager
//						.usersGetValue(jo.getString("name"));
//				writedString += jo.getString("name") + "=" + userValue + "\n";
//			}

			response.setHeader("Content-Disposition",
					"attachment; filename=\"users.properties\"");
			response.setContentType("text/plain");
			
			//We directly write the concatenated string into the output stream of response
			PrintStream printStream = new PrintStream(response.getOutputStream());
			printStream.print(writedString);
			printStream.close();

			String message = "The users file has been exported.";
			Engine.logAdmin.info(message);
		} else {
			String message = "Error when parsing the requested parameter!";
			Engine.logAdmin.error(message);
			throw new Exception ("Error when parsing the requested parameter!");
		}
	}
}
