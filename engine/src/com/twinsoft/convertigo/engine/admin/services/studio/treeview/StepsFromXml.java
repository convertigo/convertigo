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

package com.twinsoft.convertigo.engine.admin.services.studio.treeview;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.StepUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * Creates the steps building an XML structure in a sequence or a step, as the "Create steps structure from
 * XML" action of the Eclipse Studio: an element step for each element, an attribute step for each attribute.
 * <ul>
 * <li>id: the sequence or the step receiving the steps</li>
 * <li>xml: the XML structure</li>
 * </ul>
 */
@ServiceDefinition(name = "StepsFromXml", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class StepsFromXml extends JSonService {

	static boolean handles(DatabaseObject dbo) {
		return dbo instanceof Sequence || dbo instanceof StepWithExpressions;
	}

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		var xml = request.getParameter("xml");
		var dbo = id == null ? null : Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(id);
		if (!handles(dbo)) {
			throw new ServiceException("The object " + id + " cannot hold steps.");
		}
		if (xml == null || xml.isBlank()) {
			throw new ServiceException("missing xml parameter");
		}
		var root = XMLUtils.parseDOMFromString(xml).getDocumentElement();
		var sequence = dbo instanceof Sequence main ? main : ((StepWithExpressions) dbo).getSequence();
		var step = root == null ? null : StepUtils.createStepFromXmlDomModel(sequence, root);
		if (step == null) {
			throw new ServiceException("The XML structure makes no step.");
		}
		if (dbo instanceof Sequence main) {
			main.addStep(step);
		} else {
			((StepWithExpressions) dbo).addStep(step);
		}
		sequence.hasChanged = true;
		response.put("done", true);
		response.put("id", step.getFullQName());
	}
}
