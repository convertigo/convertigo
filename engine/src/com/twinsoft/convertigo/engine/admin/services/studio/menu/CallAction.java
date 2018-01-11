package com.twinsoft.convertigo.engine.admin.services.studio.menu;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.studio.AbstractRunnableAction;
import com.twinsoft.convertigo.engine.studio.CheStudio;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.popup.actions.DatabaseObjectDeleteAction;

@ServiceDefinition(
		name = "CallAction",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG, Role.PROJECT_DBO_VIEW },
		parameters = {},
		returnValue = ""
	)
public class CallAction extends XmlService {

    public final static String PARAM_CHE_STUDIO = "cheStudio";
    public final static String PARAM_LAST_ACTION = "lastAction";

    private Exception currentException;

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		HttpSession session = request.getSession();

		CheStudio cheStudio = getStudio(session);
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
                session.setAttribute(PARAM_LAST_ACTION, actionClassName);
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
                                synchronized (localCheStudio) {
                                    currentException = e;
                                    localCheStudio.notify();
                                }
                            }
						});

						cheStudio.wait();
					}
				}
				catch (ClassNotFoundException e) {
					// We don't forget to delete it from the session to start a new action the next time
					session.removeAttribute(PARAM_CHE_STUDIO);
					session.removeAttribute(PARAM_LAST_ACTION);

					// Action not defined
                    String actionName = actionClassName.substring(actionClassName.lastIndexOf(".") + 1, actionClassName.length());
					throw new Exception("The action " + actionName + " is not defined yet.");
				}
			}
		}
		else {
		    if (isCurrentAction(DatabaseObjectDeleteAction.class, session)) {
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
		    else {
                synchronized (cheStudio) {
                    cheStudio.setDocument(document);
                    cheStudio.notify();
                    cheStudio.wait();
                }
		    }
		}

        if (currentException != null) {
            throw currentException;
        }
	}

	public static boolean isCurrentAction(Class<? extends AbstractRunnableAction> action, HttpSession session) {
	    return action.getName().equals(session.getAttribute(PARAM_LAST_ACTION));
	}

	public static CheStudio getStudio(HttpSession session) {
	    return (CheStudio) session.getAttribute(PARAM_CHE_STUDIO);
	}
}
