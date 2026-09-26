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

package com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder;

import java.util.HashSet;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.MobileComponent;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.DatabaseObjectFoundException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;

/**
 * Links the components of an NGX application to the elements of its preview, as the application editor of
 * the Eclipse Studio: each element of a component carries the class "class" + its priority.
 * <ul>
 * <li>action=reference, id: the classes to look for in the preview, the one of the component then the ones of
 * its parents, and the segment of its page</li>
 * <li>action=find, project, priority: the id of the component of the application with this priority</li>
 * </ul>
 */
@ServiceDefinition(name = "Authoring", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Authoring extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var action = request.getParameter("action");
		if ("find".equals(action)) {
			var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(request.getParameter("project"));
			var application = project == null || project.getMobileApplication() == null ? null
					: project.getMobileApplication().getApplicationComponent();
			if (!(application instanceof DatabaseObject root)) {
				throw new ServiceException("The project has no application.");
			}
			var found = find(root, Long.parseLong(request.getParameter("priority")));
			response.put("id", found == null ? "" : found.getFullQName());
			return;
		}
		var dbo = Utils.getDbo(request.getParameter("id"));
		var classes = new JSONArray();
		var segment = "";
		if (dbo instanceof MobileComponent component) {
			if (component instanceof UIComponent uic && uic.getPage() != null) {
				segment = uic.getPage().getSegment();
			} else if (component instanceof PageComponent page) {
				segment = page.getSegment();
			}
			// a shared component shows as its first displayable component
			var shared = component instanceof UISharedComponent uisc ? uisc
					: component instanceof UIUseShared use ? use.getTargetSharedComponent() : null;
			if (shared != null && !shared.getDisplayableComponentList().isEmpty()) {
				classes.put("class" + shared.getDisplayableComponentList().get(0).priority);
			}
			for (DatabaseObject current = component; current instanceof MobileComponent; current = current.getParent()) {
				classes.put("class" + current.priority);
			}
		}
		response.put("classes", classes);
		response.put("segment", segment == null ? "" : segment);
	}

	/**
	 * @return the component of the application with the priority, the shared components walked where they
	 * are used, as the Eclipse Studio finds it
	 */
	private static DatabaseObject find(DatabaseObject application, long priority) throws Exception {
		var walked = new HashSet<DatabaseObject>();
		try {
			new WalkHelper() {
				@Override
				protected void walk(DatabaseObject databaseObject) throws Exception {
					if (databaseObject instanceof UIUseShared use && use.getTargetSharedComponent() != null) {
						databaseObject = use.getTargetSharedComponent();
					} else if (databaseObject instanceof UIDynamicInvoke invoke && invoke.getTargetSharedAction() != null
							&& !invoke.isRecursive()) {
						databaseObject = invoke.getTargetSharedAction();
					}
					if (databaseObject.priority == priority) {
						throw new DatabaseObjectFoundException(databaseObject);
					}
					if (walked.add(databaseObject)) {
						super.walk(databaseObject);
					}
				}
			}.init(application);
		} catch (DatabaseObjectFoundException e) {
			return e.getDatabaseObject();
		}
		return null;
	}
}
