package com.twinsoft.convertigo.engine.admin.services.studio.database_objects;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject.ExportOption;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "Get",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG, Role.PROJECT_DBO_VIEW },
		parameters = {},
		returnValue = ""
	)
public class Get extends XmlService {
	public static Element getProperties(DatabaseObject dbo, Document document, String qname) throws EngineException {
		Element elt = dbo.toXml(document, ExportOption.bIncludeBlackListedElements, ExportOption.bIncludeCompiledValue, ExportOption.bIncludeDisplayName, ExportOption.bIncludeEditorClass, ExportOption.bIncludeShortDescription, ExportOption.bHidePassword);
		elt.setAttribute("qname", qname);
		return elt;
	}
	
	private Map<String, String> getInformationProperties(DatabaseObject dbo) {
		Map<String, String> informationProperties = new HashMap<String, String>();
		informationProperties.put("depth", dbo instanceof ScreenClass ? Integer.toString(((ScreenClass) dbo).getDepth()) : "n/a");
		informationProperties.put("exported", (dbo instanceof Project) ? ((Project) dbo).getInfoForProperty("exported") : "n/a");
		informationProperties.put("java_class", dbo.getClass().getName());

		return informationProperties;
	}
	
	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		String qname = request.getParameter("qname");
		DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
		Element elt = getProperties(dbo, document, qname);
		Map<String, String> map = getInformationProperties(dbo);
		for (Map.Entry<String, String> m : map.entrySet()) {
			elt.setAttribute(m.getKey(), m.getValue());
		}
        boolean isExtractionRule = dbo instanceof com.twinsoft.convertigo.beans.core.ExtractionRule;
        elt.setAttribute("isExtractionRule", Boolean.toString(isExtractionRule));

		document.getDocumentElement().appendChild(elt);
	}
}
