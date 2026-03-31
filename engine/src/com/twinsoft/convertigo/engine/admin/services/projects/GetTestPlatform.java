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

package com.twinsoft.convertigo.engine.admin.services.projects;

import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IApplicationComponent;
import com.twinsoft.convertigo.beans.core.ITestCaseContainer;
import com.twinsoft.convertigo.beans.core.IVariableContainer;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceParameterDefinition;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.GenericUtils;

@ServiceDefinition(
		name = "GetTestPlatform",
		roles = { Role.TEST_PLATFORM, Role.PROJECTS_CONFIG, Role.PROJECTS_VIEW },
		parameters = {
				@ServiceParameterDefinition(
						name = "projectName",
						description = "the name of the project to retrieve"
					),
				@ServiceParameterDefinition(
						name = "lang",
						description = "optional preferred language for multilingual comments"
					)
			},
		returnValue = "all project's objects and properties"
	)
public class GetTestPlatform extends XmlService {
	private static final Pattern LANG_KEY = Pattern.compile("^[a-zA-Z]{2,3}(?:[-_][a-zA-Z0-9]{2,8})*$");

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element root = document.getDocumentElement();

		String projectName = request.getParameter("projectName");
		String lang = request.getParameter("lang");
		if (lang == null || lang.isBlank()) {
			lang = request.getLocale() == null ? null : request.getLocale().toLanguageTag();
		}

		Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
		
		Element e_project = createDatabaseObjectElement(document, project, lang);
		
		boolean bTpHiddenRole = Engine.authenticatedSessionManager.hasRole(request.getSession(), Role.TEST_PLATFORM_HIDDEN);
		boolean bTpPrivateRole = Engine.authenticatedSessionManager.hasRole(request.getSession(), Role.TEST_PLATFORM_PRIVATE);
		
		for (Connector connector : project.getConnectorsList()) {			
			Element e_connector = createDatabaseObjectElement(document, connector, lang);				
			for (Transaction transaction : connector.getTransactionsList()) {
				// WEB_ADMIN role is allowed to execute all requestables
				if (transaction.isPublicAccessibility()
						|| (transaction.isHiddenAccessibility() && bTpHiddenRole)
						|| bTpPrivateRole) {
					e_connector.appendChild(createRequestableElement(document, transaction, lang));				
				}
			}
			e_project.appendChild(e_connector);
		}

		for (Sequence sequence : project.getSequencesList()) {
			// WEB_ADMIN role is allowed to execute all requestables
			if (sequence.isPublicAccessibility()
					|| (sequence.isHiddenAccessibility() && bTpHiddenRole)
					|| bTpPrivateRole) {
				e_project.appendChild(createRequestableElement(document, sequence, lang));
			}
		}
		
		MobileApplication mobileApplication = project.getMobileApplication();
		if (mobileApplication != null && (
				mobileApplication.getAccessibility() == Accessibility.Public
				|| (mobileApplication.getAccessibility() == Accessibility.Hidden && bTpHiddenRole)
				|| bTpPrivateRole
				)) {
			Element e_mobileApplication = createDatabaseObjectElement(document, mobileApplication, lang);
			
			String applicationID = mobileApplication.getComputedApplicationId();
			e_mobileApplication.setAttribute("applicationID", applicationID);

			String endpoint = mobileApplication.getComputedEndpoint(request);
			e_mobileApplication.setAttribute("endpoint", endpoint);
			
			String version = mobileApplication.getComputedApplicationVersion();
			e_mobileApplication.setAttribute("applicationVersion", version);
			
			e_project.appendChild(e_mobileApplication);

			for (MobilePlatform platform : mobileApplication.getMobilePlatformList()) {
				Element e_device = createDatabaseObjectElement(document, platform, lang);
				e_device.setAttribute("classname", platform.getClass().getSimpleName());
				e_device.setAttribute("displayName", CachedIntrospector.getBeanInfo(platform.getClass()).getBeanDescriptor().getDisplayName());
				e_device.setAttribute("packageType", platform.getPackageType());
				e_device.setAttribute("revision", "computing...");
				
				e_mobileApplication.appendChild(e_device);
			}
		
			try {
				String mobileProjectName = mobileApplication.getComputedApplicationName();
				e_mobileApplication.setAttribute("mobileProjectName", mobileProjectName);
			} catch (Exception e) {
				Engine.logAdmin.error("Failed to retrieve the application mobile name", e);
			}
			
			IApplicationComponent app = mobileApplication.getApplicationComponent();
			String msg = app != null ? app.getUnbuiltMessage() : null;
			if (msg != null) {
				e_mobileApplication.setAttribute("unbuiltMessage", msg);
			}
		}
		
		root.appendChild(e_project);
	}
	
	private Element createDatabaseObjectElement(Document document, DatabaseObject dbo, String lang) {
		Element elt = document.createElement(dbo.getDatabaseType().toLowerCase());
		elt.setAttribute("name", dbo.getName());
		elt.setAttribute("comment", formatText(dbo.getComment(), lang));
		elt.setAttribute("version", dbo.getVersion());
		
		if (dbo instanceof RequestableObject) {
			elt.setAttribute("accessibility", ((RequestableObject)dbo).getAccessibility() + "" );
			if (dbo instanceof Sequence) {
				elt.setAttribute("autostart", String.valueOf(((Sequence)dbo).isAutoStart()));
			}
		}
		
		return elt;
	}
	
	private Element createRequestableElement(Document document, RequestableObject requestable, String lang) {
		Element e_requestable = createDatabaseObjectElement(document, requestable, lang);
		if (requestable instanceof IVariableContainer) {
			handleIVariableContainer(document, e_requestable, (IVariableContainer) requestable, lang);
		}
		if (requestable instanceof ITestCaseContainer) {
			for (TestCase testcase : ((ITestCaseContainer) requestable).getTestCasesList()) {
				Element e_testcase = createDatabaseObjectElement(document, testcase, lang);
				handleIVariableContainer(document, e_testcase, testcase, lang);
				e_requestable.appendChild(e_testcase);
			}
		}
		return e_requestable;
	}
	
	private void handleIVariableContainer(Document document, Element e_vars, IVariableContainer vars, String lang) {
		for (Variable variable : vars.getVariables()) {
			Element e_variable = createDatabaseObjectElement(document, variable, lang);
			Object val = variable.getValueOrNull();
			String strval;
			try {
				strval = val == null ?
						null :
						variable.isMultiValued() ?
								new JSONArray(GenericUtils.<XMLVector<String>>cast(val)).toString() :
									val.toString();
			} catch (JSONException e) {
				strval = null;
			}
			e_variable.setAttribute("value", strval);
			e_variable.setAttribute("isMasked", Visibility.Platform.isMasked(variable.getVisibility()) ? "true":"false");
			e_variable.setAttribute("isMultivalued", "" + variable.isMultiValued());
			e_variable.setAttribute("isFileUpload", "" + variable.getIsFileUpload());
			e_variable.setAttribute("description", formatText(variable.getDescription(), lang));
			e_variable.setAttribute("required", "" + variable.isRequired());
			e_vars.appendChild(e_variable);
		}
	}

	private String formatText(String text, String lang) {
		if (text == null || text.isEmpty()) {
			return text == null ? "" : text;
		}
		try {
			var json = new JSONObject(text);
			String[] candidates = { lang, lang == null ? null : lang.toLowerCase(), null, null, "en" };
			if (lang != null) {
				int index = lang.indexOf('-');
				if (index == -1) {
					index = lang.indexOf('_');
				}
				if (index != -1) {
					candidates[2] = lang.substring(0, index);
					candidates[3] = candidates[2].toLowerCase();
				}
			}
			for (String candidate : candidates) {
				if (candidate != null && !candidate.isBlank() && json.has(candidate)) {
					String localized = getTextValue(json.get(candidate));
					if (localized != null) {
						return localized;
					}
				}
			}
			var keys = json.keys();
			if (keys.hasNext()) {
				String key = keys.next().toString();
				if (LANG_KEY.matcher(key).matches()) {
					String localized = getTextValue(json.get(key));
					if (localized != null) {
						return localized;
					}
				}
			}
		} catch (Exception e) {
		}
		return text;
	}

	private String getTextValue(Object value) throws Exception {
		if (value instanceof JSONObject json) {
			if (json.has("comment")) {
				return json.get("comment").toString();
			}
			if (json.has("description")) {
				return json.get("description").toString();
			}
			var keys = json.keys();
			if (keys.hasNext()) {
				return json.get(keys.next().toString()).toString();
			}
			return "";
		}
		return value == null ? "" : value.toString();
	}
}
