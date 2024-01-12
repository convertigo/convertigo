/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.studio.sourcepicker;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.variables.StepVariable;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.menu.CallAction;
import com.twinsoft.convertigo.engine.admin.util.DOMUtils;
import com.twinsoft.convertigo.engine.studio.CheStudio;
import com.twinsoft.convertigo.engine.studio.dnd.StepSourceWrap;
import com.twinsoft.convertigo.engine.studio.views.sourcepicker.SourcePickerViewWrap;

@ServiceDefinition(
        name = "CanCreateSource",
        roles = { Role.WEB_ADMIN },
        parameters = {},
        returnValue = ""
    )
public class CanCreateSource extends XmlService {

    @Override
    protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
        HttpSession session = request.getSession();

        CheStudio cheStudio = CallAction.getStudio(session);
        if (cheStudio != null) {
            SourcePickerViewWrap spv = cheStudio.getSourcePickerView();
            if (spv != null) {
                String qname = request.getParameter("qname");
                DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
                StepSourceWrap ssw = (StepSourceWrap) spv.getDragData();
                boolean canCreateSource = canCreateSource(dbo, ssw);
                document.getDocumentElement().appendChild(DOMUtils.createElementWithText(document, "response", Boolean.toString(canCreateSource)));
            }
        }
    }

    public static boolean canCreateSource(Object obj, StepSourceWrap ssw) {
        //if (target instanceof TreeObject) {
            //TreeObject targetTreeObject = (TreeObject) target;
            // Check for drop to a step which contains a stepSource definition
            //if (targetTreeObject.getObject() instanceof IStepSourceContainer) {
            DatabaseObject targetDbo = (DatabaseObject) obj;
            if (targetDbo instanceof Step && ((Step) targetDbo).canWorkOnSource() || targetDbo instanceof IStepSourceContainer) {
                StepSourceWrap stepSource = ssw;//StepSourceTransfer.getInstance().getStepSource();
                if (stepSource != null) {
                    Step targetStep = (Step) ((targetDbo instanceof StepVariable) ? ((StepVariable) targetDbo).getParent() : targetDbo);

                    // Check for drop to a step in the same sequence
                    Long key = Long.valueOf(stepSource.getPriority());
                    Step sourceStep = targetStep.getSequence().loadedSteps.get(key);
                    if ((sourceStep != null) && (!targetStep.equals(sourceStep))) {
                        // Check for drop on a 'following' step
                        try {
                            List<DatabaseObject> siblings = new ArrayList<>();
                            getNextSiblings(siblings, targetDbo.getProject(), sourceStep);
                            //System.out.println("siblings: "+siblings.toString());
                            return siblings.contains(targetDbo);
                        }
                        catch (Exception e) {
                            e.printStackTrace(System.out);
                        };
                    }
                }
            }
        //}
        return false;
    }

    private static void getNextSiblings(List<DatabaseObject> siblings, DatabaseObject parent, Object object) throws Exception {
        if (parent != null && object != null) {
            List<DatabaseObject> children = parent.getDatabaseObjectChildren();
            for (DatabaseObject dbo: children) {
                if (dbo.equals(object)) {
                    siblings.add(dbo);
                    //if (treeObject instanceof TreeParent) {
                        siblings.addAll(dbo.getDatabaseObjectChildren(true));
                    //}
                    continue;
                }
                if (!siblings.isEmpty()) {
                    siblings.add(dbo);
                }
                //if (treeObject instanceof TreeParent) {
                    getNextSiblings(siblings, dbo, object);
                //}
            }
        }
    }
}
