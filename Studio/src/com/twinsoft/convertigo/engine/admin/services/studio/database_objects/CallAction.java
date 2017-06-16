package com.twinsoft.convertigo.engine.admin.services.studio.database_objects;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.studio.popup.actions.AbstractRunnableAction;
import com.twinsoft.convertigo.engine.studio.responses.XmlResponseFactory;
import com.twinsoft.convertigo.engine.studio.wrappers.CheStudio;
import com.twinsoft.convertigo.engine.studio.wrappers.WrapStudio;

@ServiceDefinition(
		name = "CallAction",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG, Role.PROJECT_DBO_VIEW },
		parameters = {},
		returnValue = ""
	)
public class CallAction extends XmlService {

	private final static String PARAM_CHE_STUDIO = "cheStudio";

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		HttpSession session = request.getSession();

		CheStudio cheStudio = (CheStudio) session.getAttribute(PARAM_CHE_STUDIO);
		if (cheStudio == null || cheStudio.isActionDone()) {
			String[] qnames = request.getParameterValues("qnames[]");
			String action = request.getParameter("action");
		
			if (qnames != null && action != null) {
				cheStudio = new CheStudio(document);
				session.setAttribute(PARAM_CHE_STUDIO, cheStudio);

				// Remove duplicates
				Set<String> uniqueQnames = new HashSet<>(Arrays.asList(qnames));

				// Get all dbos from the qnames
				Iterator<String> uniqueQnamesIt = uniqueQnames.iterator();
				while (uniqueQnamesIt.hasNext()) {
					String uniqueQname = (String) uniqueQnamesIt.next();
					// Add the dbo
					cheStudio.addSelectedObject(Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(uniqueQname));
				}

				String actionClassName = action.replace(
						"com.twinsoft.convertigo.eclipse.popup.actions",
						"com.twinsoft.convertigo.engine.studio.popup.actions"
				);
				try {
					Constructor<?> c = Class.forName(actionClassName).getConstructor(WrapStudio.class);
					synchronized (cheStudio) {
						// Create a new instance of the action then run it
						AbstractRunnableAction runnableAction = (AbstractRunnableAction) c.newInstance(cheStudio);
						final CheStudio localCheStudio = cheStudio;
						Engine.execute(() -> {
							try {
								localCheStudio.runAction(runnableAction);
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						});

						cheStudio.wait();
					}
				}
				catch (ClassNotFoundException e) {
					// Action not defined
					String actionName = actionClassName.substring(actionClassName.lastIndexOf(".") + 1, actionClassName.length());
					Element response = XmlResponseFactory.createMessageBoxResponse(document, null, "The action " + actionName + " is not defined.");
					response.setAttribute("state", "error");

					document.getDocumentElement().appendChild(response);
					
					// We don't forget to delete it from the session to start a new action the next time
					session.removeAttribute(PARAM_CHE_STUDIO);
				}
			}
		}
		// Continue the action (case of action with dialogs)
		else {
			synchronized (cheStudio) {
				// Setting the new Document is important, else it will keep the old reference of the document
				cheStudio.setDocument(document);
				String response = request.getParameter("response");
				if (response != null) {
					int intResponse = Integer.parseInt(response);
					cheStudio.setResponse(intResponse);
				}

				cheStudio.wait();
			}
		}
	}

}
