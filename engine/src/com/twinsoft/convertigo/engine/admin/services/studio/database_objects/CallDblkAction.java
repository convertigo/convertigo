/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.studio.database_objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.menu.CallAction;
import com.twinsoft.convertigo.engine.studio.AbstractRunnableAction;
import com.twinsoft.convertigo.engine.studio.CheStudio;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.actions.LauncEditableEditorAction;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.actions.LaunchConnectorEditorAction;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.actions.LaunchSequenceEditorAction;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.IEditableTreeViewWrap;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapDatabaseObject;

@ServiceDefinition(
        name = "CallDblkAction",
        roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG, Role.PROJECT_DBO_VIEW },
        parameters = {},
        returnValue = ""
    )
public class CallDblkAction extends XmlService {

    private Exception currentException;

    @Override
    protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
        HttpSession session = request.getSession();
        CheStudio cheStudio = CallAction.getStudio(session);

        if (cheStudio == null || cheStudio.isActionDone()) {
            String qname = request.getParameter("qname");
            if (qname != null) {
                // Create sutdio
                cheStudio = new CheStudio(document);
                session.setAttribute(CallAction.PARAM_CHE_STUDIO, cheStudio);
                cheStudio.addSelectedObject(Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname));

                // Equivalent of com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView#makeActions

                // Get the right action
                WrapDatabaseObject treeObject = (WrapDatabaseObject) cheStudio.getFirstSelectedTreeObject();
                AbstractRunnableAction runnableAction = null;
                if (treeObject.instanceOf(Connector.class)) {
                    runnableAction = new LaunchConnectorEditorAction(cheStudio);
                    session.setAttribute(CallAction.PARAM_LAST_ACTION, LaunchConnectorEditorAction.class.getName());
                }
                else if (treeObject.instanceOf(Sequence.class)) {
                    runnableAction = new LaunchSequenceEditorAction(cheStudio);
                    session.setAttribute(CallAction.PARAM_LAST_ACTION, LaunchSequenceEditorAction.class.getName());
                }
                else if (treeObject.instanceOf(Step.class)) {
                    //showStepInPickerAction.run();
                    if (treeObject instanceof IEditableTreeViewWrap) {
                        runnableAction = new LauncEditableEditorAction(cheStudio);
                        session.setAttribute(CallAction.PARAM_LAST_ACTION, LauncEditableEditorAction.class.getName());
                    }
                }
                else if (treeObject instanceof IEditableTreeViewWrap) {
                    runnableAction = new LauncEditableEditorAction(cheStudio);
                    session.setAttribute(CallAction.PARAM_LAST_ACTION, LauncEditableEditorAction.class.getName());
                }

                // Execute action if found
                if (runnableAction != null) {

                    synchronized (cheStudio) {
                        final AbstractRunnableAction localAction = runnableAction;
                        final CheStudio localCheStudio = cheStudio;

                        Engine.execute(() -> {
                            try {
                                localCheStudio.runAction(localAction);
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
            }
        }
        else {
            if (CallAction.isCurrentAction(LaunchConnectorEditorAction.class, session)) {
                synchronized (cheStudio) {
                    cheStudio.setDocument(document);
                    cheStudio.notify();
                    cheStudio.wait();
                }
            }
            else if (CallAction.isCurrentAction(LaunchSequenceEditorAction.class, session)) {
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
}
