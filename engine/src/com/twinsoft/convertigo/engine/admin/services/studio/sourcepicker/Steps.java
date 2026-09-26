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

package com.twinsoft.convertigo.engine.admin.services.studio.sourcepicker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;

/**
 * The steps of the sequence of a step, as the step source editor of the Eclipse Studio shows them: the steps
 * before it can be its source, when they are pickable and are not the XML steps holding an XML step.
 * <ul>
 * <li>id: the step</li>
 * </ul>
 */
@ServiceDefinition(name = "Steps", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class Steps extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		if (!(Utils.getDbo(request.getParameter("id")) instanceof Step owner) || owner.getParentSequence() == null) {
			throw new ServiceException("The object is not a step of a sequence.");
		}
		Set<Long> xmlParents = new HashSet<>();
		if (owner.isXml()) {
			for (var parent = owner.getParent(); parent instanceof Step step; parent = step.getParent()) {
				if (step.isXml()) {
					xmlParents.add(step.priority);
				}
			}
		}
		var steps = new JSONArray();
		add(steps, owner.getParentSequence().getSteps(), 0, owner, xmlParents, new boolean[1]);
		response.put("steps", steps);
	}

	private static void add(JSONArray steps, List<Step> children, int depth, Step owner, Set<Long> xmlParents,
			boolean[] found) throws Exception {
		for (var step : children) {
			var isOwner = step.priority == owner.priority;
			found[0] |= isOwner;
			steps.put(new JSONObject()
					.put("priority", Long.toString(step.priority))
					.put("label", step.toString())
					.put("depth", depth)
					.put("owner", isOwner)
					.put("pickable", !found[0] && step.isPickable() && !xmlParents.contains(step.priority)));
			if (step instanceof StepWithExpressions container) {
				add(steps, container.getSteps(), depth + 1, owner, xmlParents, found);
			}
		}
	}
}
