/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.studio.menu;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;

@ServiceDefinition(
		name = "GetIconsCSS",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG, Role.PROJECT_DBO_VIEW },
		parameters = {},
		returnValue = ""
	)
public class GetIconsCSS extends DownloadService  {

	@SuppressWarnings("unlikely-arg-type")
	@Override
	protected void writeResponseResult(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Document pluginDocument = Get.getPluginDocument();			
		
		TwsCachedXPathAPI xpathApi = new TwsCachedXPathAPI();
		List<Node> nActions = xpathApi.selectList(pluginDocument, "/plugin/extension[@point='org.eclipse.ui.popupMenus']//action[@icon]");

		Set<String> actionIconAttrs = new HashSet<>();
		StringBuilder sb = new StringBuilder();

		for (Node nAction: nActions) {
			Element eAction = (Element) nAction;
			String attrIcon = eAction.getAttribute("icon");

			if (!actionIconAttrs.contains(actionIconAttrs)) {
				sb.append(".")
				  .append(eAction.getAttribute("id").replaceAll("\\.", "-"))
				  .append(" {background-image:url(")
  
				  .append("../../admin/services/studio.menu.GetIcon?iconPath=")
				  .append(attrIcon)
				  .append(") !important;")
				  .append("}\r\n");

				actionIconAttrs.add(attrIcon);
			}
		}

		response.setContentType(MimeType.Css.value());
		IOUtils.write(sb.toString(), response.getOutputStream(), "UTF-8");
	}
}
