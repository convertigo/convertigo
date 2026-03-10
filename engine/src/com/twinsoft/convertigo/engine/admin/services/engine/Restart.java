/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

import jakarta.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceParameterDefinition;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;

@ServiceDefinition(
		name = "Restart",
		roles = { Role.WEB_ADMIN, Role.MANAGER, Role.HOME_CONFIG },
		parameters = {
			@ServiceParameterDefinition(
				name = "hard",
				description = "if true, stops the engine and exits the JVM"
			)
		},
		returnValue = "nothing"
	)
public class Restart extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		boolean hard = "true".equalsIgnoreCase(request.getParameter("hard"));

		try {
			Engine.stop();
			Engine.logAdmin.info("Engine stopped");
		} catch (Exception e) {
			Engine.logAdmin.error("Error while stopping engine", e);
		}

		Element root = document.getDocumentElement();
		Element response = document.createElement("response");
		response.setAttribute("state", "success");
		response.setAttribute(
			"message",
			hard ? "Service will hard restart soon" : "Service will soft restart soon"
		);
		root.appendChild(response);

		new Thread(() -> {
			try {
				if (hard) {
					Engine.logAdmin.info("Engine hard restart requested");
					System.exit(0);
				} else {
					Engine.start();
					Engine.logAdmin.info("Engine restarted");
				}
			} catch (Throwable t) {
				Engine.logAdmin.error("Error while restarting engine", t);
			}
		}, "engine-restart-thread").start();
	}

}
