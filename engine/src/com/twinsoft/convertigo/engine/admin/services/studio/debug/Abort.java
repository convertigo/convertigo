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

package com.twinsoft.convertigo.engine.admin.services.studio.debug;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

/**
 * Stops a sequence or a transaction the web Studio runs, as the Stop button of the sequence and connector
 * editors of the Eclipse Studio.
 * <ul>
 * <li>context: the name of the context of the execution, in the session of the Studio</li>
 * </ul>
 */
@ServiceDefinition(name = "Abort", roles = { Role.WEB_ADMIN }, parameters = {}, returnValue = "", available_without_web_studio = true)
public class Abort extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var contextName = request.getParameter("context");
		if (contextName == null || contextName.isBlank()) {
			throw new ServiceException("missing context parameter");
		}
		var contextId = request.getSession().getId() + "_" + contextName;
		response.put("aborted", Engine.theApp.contextManager.requestAbort(contextId));
	}
}
