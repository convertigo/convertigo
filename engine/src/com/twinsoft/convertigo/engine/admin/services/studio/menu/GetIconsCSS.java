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
