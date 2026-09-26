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

import java.util.TreeMap;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

/**
 * The fonts an NGX application can use, as the font editor of the Eclipse Studio lists them.
 * <ul>
 * <li>font: a font to describe with its files, for its preview; missing to list the fonts</li>
 * </ul>
 */
@ServiceDefinition(name = "Fonts", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class Fonts extends JSonService {
	private static final String[] SUMMARY = { "id", "family", "category", "type", "weights", "styles", "subsets", "defSubset" };

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var fontId = request.getParameter("font");
		if (fontId != null) {
			var font = ComponentManager.getFont(fontId);
			if (font == null) {
				throw new ServiceException("Unknown font " + fontId);
			}
			response.put("font", font);
			return;
		}
		var fonts = new JSONArray();
		for (var font : new TreeMap<>(ComponentManager.getFonts()).values()) {
			var summary = new JSONObject();
			for (var key : SUMMARY) {
				if (font.has(key)) {
					summary.put(key, font.get(key));
				}
			}
			fonts.put(summary);
		}
		response.put("fonts", fonts);
	}
}
