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

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder.BuilderUtils;
import com.twinsoft.convertigo.engine.flow.FlowStudioSupport;

@ServiceDefinition(name = "Remove", roles = { Role.WEB_ADMIN,
		Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class Remove extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {

		// id the id of the bean in tree
		var id = request.getParameter("id");
		if (id == null) {
			throw new ServiceException("missing id parameter");
		}

		boolean done = false;
		DatabaseObject dbo = DboUtils.findDbo(id);
		if (dbo != null) {
			refuse(dbo);
			if ("true".equals(request.getParameter("check"))) {
				// what the deletion can remove too, which the Studio asks, as the Eclipse Studio does
				response.put("folders", new org.codehaus.jettison.json.JSONArray(linkedFolders(dbo)));
				if (dbo instanceof com.twinsoft.convertigo.beans.connectors.CouchDbConnector couch
						&& !couch.getDatabaseName().isEmpty()) {
					response.put("database", couch.getDatabaseName());
				}
				response.put("done", false);
				return;
			}
			if (dbo instanceof FlowVirtualObject) {
				DboUtils.copyResult(FlowStudioSupport.removeNode(dbo), response);
				return;
			} else if (dbo instanceof Project) {
				// TODO
			} else {
				DatabaseObject targetDbo = dbo.getParent();
				var folders = linkedFolders(dbo);
				targetDbo.remove(dbo);
				done = true;
				// the folders and the database the user chose to remove too
				var chosen = new org.codehaus.jettison.json.JSONArray(
						request.getParameter("folders") == null ? "[]" : request.getParameter("folders"));
				for (int i = 0; i < chosen.length(); i++) {
					if (folders.contains(chosen.getString(i))) {
						org.apache.commons.io.FileUtils.deleteQuietly(new java.io.File(chosen.getString(i)));
					}
				}
				if ("true".equals(request.getParameter("dropDatabase"))
						&& dbo instanceof com.twinsoft.convertigo.beans.connectors.CouchDbConnector couch
						&& !couch.getDatabaseName().isEmpty()) {
					couch.getCouchClient().deleteDatabase(couch.getDatabaseName());
				}
				
				// notify for app generation
				BuilderUtils.dboRemoved(targetDbo, dbo);
			}
		}
		
		response.put("done", done);
	}

	/**
	 * Refuses the deletions the Eclipse Studio refuses.
	 */
	private static void refuse(DatabaseObject dbo) throws ServiceException {
		if (dbo instanceof com.twinsoft.convertigo.beans.core.Connector connector && connector.isDefault) {
			throw new ServiceException("Cannot delete the default connector!");
		}
		if (dbo instanceof com.twinsoft.convertigo.beans.core.Transaction transaction && transaction.isDefault) {
			throw new ServiceException("Cannot delete the default transaction!");
		}
		if (dbo instanceof com.twinsoft.convertigo.beans.steps.ThenStep || dbo instanceof com.twinsoft.convertigo.beans.steps.ElseStep) {
			throw new ServiceException("Cannot delete this step!");
		}
		if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.PageComponent page && page.isRoot
				|| dbo instanceof com.twinsoft.convertigo.beans.mobile.components.PageComponent mobilePage && mobilePage.isRoot) {
			throw new ServiceException("Cannot delete the root page!");
		}
	}

	/**
	 * @return the folders linked to the object, which the deletion can remove too: the SOAP templates and
	 *         the traces of a connector, the resources of a mobile platform
	 */
	private static java.util.List<String> linkedFolders(DatabaseObject dbo) {
		var folders = new java.util.ArrayList<String>();
		if (dbo instanceof com.twinsoft.convertigo.beans.core.Connector) {
			var projectDir = com.twinsoft.convertigo.engine.Engine.projectDir(dbo.getProject().getName());
			for (var kind : new String[] { "soap-templates", "Traces" }) {
				var folder = new java.io.File(projectDir + "/" + kind + "/" + dbo.getName());
				if (folder.exists()) {
					folders.add(folder.getAbsolutePath());
				}
			}
		} else if (dbo instanceof com.twinsoft.convertigo.beans.core.MobilePlatform platform
				&& platform.getResourceFolder().exists()) {
			folders.add(platform.getResourceFolder().getAbsolutePath());
		}
		return folders;
	}
}
