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

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;

/**
 * Sets the first line of the comment of an object, as the comment column of the tree of the Eclipse Studio
 * edits it: the next lines of the comment stay.
 * <ul>
 * <li>id: the object</li>
 * <li>comment: its new first line</li>
 * </ul>
 */
@ServiceDefinition(name = "Comment", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class Comment extends JSonService {

	/**
	 * @return the first line of the comment, the one the tree shows
	 */
	static String firstLine(String comment) {
		if (comment == null) {
			return "";
		}
		var i = comment.indexOf('\n');
		return i == -1 ? comment : comment.substring(0, i);
	}

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var dbo = Utils.getDbo(request.getParameter("id"));
		if (dbo == null) {
			throw new ServiceException("The object " + request.getParameter("id") + " does not exist.");
		}
		var line = String.valueOf(request.getParameter("comment")).replaceAll("[\\r\\n]+", " ");
		var comment = dbo.getComment() == null ? "" : dbo.getComment();
		var i = comment.indexOf('\n');
		var next = i == -1 ? line : line + comment.substring(i);
		if (!next.equals(comment)) {
			dbo.setComment(next);
			dbo.hasChanged = true;
			response.put("changed", true);
		}
		response.put("comment", firstLine(next));
	}
}
