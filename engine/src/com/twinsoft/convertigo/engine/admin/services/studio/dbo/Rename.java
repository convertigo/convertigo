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

package com.twinsoft.convertigo.engine.admin.services.studio.dbo;



import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder.BuilderUtils;

@ServiceDefinition(name = "Rename", roles = { Role.WEB_ADMIN,
		Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class Rename extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {

		// id the id of the bean in tree
		var id = request.getParameter("id");
		if (id == null) {
			throw new ServiceException("missing id parameter");
		}

		String newName = request.getParameter("name");
		if (newName == null) {
			throw new ServiceException("missing name parameter");
		}
		
		String update = request.getParameter("update");
		if (update == null) {
			throw new ServiceException("missing update parameter");
		}
		
		boolean done = false;
		JSONArray ids = new JSONArray();
		DatabaseObject dbo = resolveTarget(id);
		if (dbo instanceof com.twinsoft.convertigo.beans.flow.FlowVirtualObject virtual) {
			if (!com.twinsoft.convertigo.engine.flow.FlowStudioSupport.canRenameVirtualObject(virtual)) {
				throw new com.twinsoft.convertigo.engine.EngineException("This projected object does not support renaming.");
			}
			var result = renameVirtual(virtual, newName);
			DboUtils.copyResult(result, response);
			response.put("ids", ids.put(result.optString("id")));
			return;
		}
		if (dbo instanceof com.twinsoft.convertigo.beans.couchdb.DesignDocumentView view) {
			// a view is a part of the JSON of its design document, the views refer to it by name
			var designDocument = view.getDesignDocument();
			designDocument.renameView(view.getViewName(), newName);
			var renamed = designDocument.getView(newName);
			response.put("done", true);
			response.put("ids", ids.put(renamed == null ? designDocument.getFullQName() : renamed.getFullQName()));
			return;
		}
		if (dbo instanceof com.twinsoft.convertigo.beans.couchdb.DesignDocumentFunction function) {
			var designDocument = function.getDesignDocument();
			designDocument.renameFunction(function.getKind(), function.getFunctionName(), newName);
			var renamed = designDocument.getFunction(function.getKind(), newName);
			response.put("done", true);
			response.put("ids", ids.put(renamed == null ? designDocument.getFullQName() : renamed.getFullQName()));
			return;
		}
		// ASK: the Studio asks where to update the references of an object other objects use by its name
		if ("ASK".equals(update)) {
			var referenceType = dbo == null || dbo instanceof Project ? null : DboUtils.referenceType(dbo);
			if (referenceType != null) {
				response.put("done", false);
				response.put("ask", true);
				response.put("objectType", referenceType);
				return;
			}
			update = "UPDATE_NONE";
		}
		if (dbo != null) {
			if (dbo instanceof Project) {
				// TODO
			} else {
				String oldName = dbo.getName();
				
				// changes bean name and does the refactoring
				done = DboUtils.changeBeanName(ids, dbo, oldName, newName, update);
				
				// notify for app generation
				if (done) {
					BuilderUtils.dboChanged(dbo, "name", oldName, newName);
				}
			}
		}
		
		response.put("done", done);
		response.put("ids", ids);
	}

	protected DatabaseObject resolveTarget(String id) throws Exception {
		return DboUtils.findDbo(id);
	}

	protected JSONObject renameVirtual(com.twinsoft.convertigo.beans.flow.FlowVirtualObject object, String name) throws Exception {
		return com.twinsoft.convertigo.engine.flow.FlowStudioSupport.renameVirtualObject(object, name);
	}
}
