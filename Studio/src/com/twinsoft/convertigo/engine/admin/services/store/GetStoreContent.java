package com.twinsoft.convertigo.engine.admin.services.store;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.mobileplatforms.Android;
import com.twinsoft.convertigo.beans.mobileplatforms.IOs;
import com.twinsoft.convertigo.beans.mobileplatforms.WindowsPhone8;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceParameterDefinition;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;
import com.twinsoft.convertigo.engine.enums.Accessibility;

@ServiceDefinition(
		name = "GetStoreContent",
		roles = { Role.TEST_PLATFORM, Role.STORE_CONFIG, Role.STORE_VIEW },
		parameters = {
		@ServiceParameterDefinition(
				name = "platform",
				description = "the authentication type required: all | Android | IOs | WindowsPhone8"
			),
		},
		returnValue = "the projects list and their platforms"
	)
public class GetStoreContent extends XmlService {
	private final static String ALL = "all";
	
	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element root = document.getDocumentElement();
		Element projectsListElement = document.createElement("projects");
		root.appendChild(projectsListElement);
		
		String requestedPlatform = ServiceUtils.getRequiredParameter(request, "platform");

		HttpSession httpSession = request.getSession();
		boolean bAdminRole = hasRole(httpSession, Role.WEB_ADMIN);
		boolean bAuthRole = hasRole(httpSession, Role.AUTHENTICATED);
		
    	for (String projectName : Engine.theApp.databaseObjectsManager.getAllProjectNamesList()) {
    		try {
    			Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
    			
    			Element projectElement = null;
    			MobileApplication mobileApplication = project.getMobileApplication();
    			if (mobileApplication != null) {
    				Accessibility accessibylity = mobileApplication.getAccessibility();
    				
    				if (accessibylity == Accessibility.Public || (accessibylity == Accessibility.Private && bAuthRole) || bAdminRole) {
						projectElement = createProjectElement(document, mobileApplication, projectName);
	    				for (MobilePlatform platform : mobileApplication.getMobilePlatformList()) {
	    					if (isRequestedPlatformValid(requestedPlatform, platform)) {
	        					projectElement.getLastChild().appendChild(createPlatform(document, platform));
	    					}
	    				}
	    				
	        			projectsListElement.appendChild(projectElement);
    				}
    			}
    		}
    		catch (EngineException e) {
    			String message = "Unable to get project information ('" + projectName + "')";
    			Engine.logAdmin.error(message, e);
    		}
    	}
	}
	
	private boolean isPlatformValid(MobilePlatform platform) {
		return platform instanceof Android || platform instanceof IOs || platform instanceof WindowsPhone8;
	}
	
	private boolean isRequestedPlatformValid(String requestedPlatform, MobilePlatform currentPlatform) {
		if (isPlatformValid(currentPlatform)) {
			if (requestedPlatform.equals(ALL)) {
				return true;
			}
			
			if (requestedPlatform.equals(currentPlatform.getClass().getSimpleName())) {
				return true;
			}
		}
		
		return false;
	}
	
	private Element createProjectElement(Document document, MobileApplication mobileApplication, String projectName) {
		Element projectElement = document.createElement("project");
		
		Element eProjectName = document.createElement("name");
		eProjectName.setTextContent(projectName);
		projectElement.appendChild(eProjectName);
		
		Element eApp = document.createElement("application");
		
		Element eAppName = document.createElement("name");
		String appName = mobileApplication.getApplicationName();
		if (appName.equals("")) {
			appName = projectName;
		}
		eAppName.setTextContent(appName);
		eApp.appendChild(eAppName);
		
		Element eAppDescr = document.createElement("descr");
		String descr = StringUtils.abbreviate(mobileApplication.getApplicationDescription(), 100);
		eAppDescr.setTextContent(descr);
		eApp.appendChild(eAppDescr);
		eApp.appendChild(document.createElement("platforms"));
		
		projectElement.appendChild(eApp);
		
		return projectElement;
	}
	
	private Element createPlatform(Document document, MobilePlatform platform) {
		Element appPlatform = document.createElement("platform");
		
		Element eClassName = document.createElement("classname");
		eClassName.setTextContent(platform.getClass().getSimpleName());
		appPlatform.appendChild(eClassName);
		
		Element eName = document.createElement("name");
		eName.setTextContent(platform.getName());
		appPlatform.appendChild(eName);
		
		return appPlatform;
	}
	
	private boolean hasRole(HttpSession httpSession, Role role) {
		return Engine.authenticatedSessionManager.hasRole(httpSession, role);
	}
}
