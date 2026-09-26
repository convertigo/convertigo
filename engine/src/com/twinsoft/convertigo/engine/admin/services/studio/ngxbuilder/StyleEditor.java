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

import java.util.LinkedHashSet;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIElement;
import com.twinsoft.convertigo.beans.ngx.components.UIStyle;
import com.twinsoft.convertigo.beans.ngx.components.UIText;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

/**
 * Applies to an NGX application the changes of the style editor of its preview, as the application editor
 * of the Eclipse Studio: the styles of the components, in a UIStyle of each, their plain texts and their
 * moves.
 * <ul>
 * <li>project</li>
 * <li>changes: {scss: {priority: scss}, text: {priority: text}, move: [{target, parent, index}]}</li>
 * </ul>
 */
@ServiceDefinition(name = "StyleEditor", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class StyleEditor extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(request.getParameter("project"));
		var application = project == null || project.getMobileApplication() == null ? null
				: project.getMobileApplication().getApplicationComponent();
		if (!(application instanceof DatabaseObject root)) {
			throw new ServiceException("The project has no application.");
		}
		var changes = new JSONObject(request.getParameter("changes"));
		var changed = new LinkedHashSet<DatabaseObject>();

		var scss = changes.optJSONObject("scss");
		for (var keys = scss == null ? null : scss.keys(); keys != null && keys.hasNext();) {
			var priority = (String) keys.next();
			if (!(Authoring.find(root, Long.parseLong(priority)) instanceof UIElement element)) {
				continue;
			}
			// one style for the style editor, the others removed
			UIStyle style = null;
			var removed = false;
			for (var child : element.getDatabaseObjectChildren()) {
				if (child instanceof UIStyle s) {
					if (style == null) {
						style = s;
					} else {
						element.remove(s);
						removed = true;
					}
				}
			}
			var content = scss.getString(priority);
			if (style == null) {
				style = new UIStyle();
				style.setName("styleEditor");
				element.add(style);
				BuilderUtils.dboAdded(element);
			} else if (!removed && style.getStyleContent().getString().equals(content)) {
				continue;
			}
			style.setStyleContent(new FormatedContent(content));
			style.hasChanged = true;
			changed.add(element);
		}

		var text = changes.optJSONObject("text");
		for (var keys = text == null ? null : text.keys(); keys != null && keys.hasNext();) {
			var priority = (String) keys.next();
			var dbo = Authoring.find(root, Long.parseLong(priority));
			if (dbo == null) {
				continue;
			}
			// the text of a component holding a single plain text
			UIText uitext = null;
			for (var child : dbo.getAllChildren()) {
				if (child instanceof UIText t) {
					if (uitext != null) {
						uitext = null;
						break;
					}
					uitext = t;
				}
			}
			if (uitext != null && uitext.getTextSmartType().getMode() == MobileSmartSourceType.Mode.PLAIN
					&& !uitext.getTextSmartType().getSmartValue().equals(text.getString(priority))) {
				uitext.getTextSmartType().setSmartValue(text.getString(priority));
				uitext.hasChanged = true;
				changed.add(uitext);
			}
		}

		var moves = changes.optJSONArray("move");
		for (var i = 0; moves != null && i < moves.length(); i++) {
			var move = moves.getJSONObject(i);
			if (!(Authoring.find(root, move.optLong("target")) instanceof UIComponent target)
					|| !(Authoring.find(root, move.optLong("parent")) instanceof UIComponent parent)) {
				continue;
			}
			var index = move.optLong("index");
			var previousParent = target.getParent();
			if (parent != previousParent) {
				previousParent.remove(target);
				changed.add(previousParent);
				parent.add(target, null);
			}
			var order = (long) parent.getOrder(target);
			while (order < index) {
				parent.decreasePriority(target);
				order = (long) parent.getOrder(target);
			}
			while (order > index) {
				parent.increasePriority(target);
				order = (long) parent.getOrder(target);
			}
			parent.hasChanged = true;
			changed.add(parent);
		}

		var ids = new JSONArray();
		for (var dbo : changed) {
			BuilderUtils.dboUpdated(dbo);
			ids.put(dbo.getFullQName());
		}
		response.put("changed", ids);
	}
}
