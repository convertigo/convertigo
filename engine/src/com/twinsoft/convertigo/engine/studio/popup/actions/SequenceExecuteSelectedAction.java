/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.engine.studio.popup.actions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.studio.AbstractRunnableAction;
import com.twinsoft.convertigo.engine.studio.Studio;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.editors.sequences.SequenceEditorWrap;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.ConnectorView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.ProjectView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.SequenceView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapDatabaseObject;

public class SequenceExecuteSelectedAction extends AbstractRunnableAction {

    public SequenceExecuteSelectedAction(WrapStudio studio) {
        super(studio);
    }

    @Override
    protected void run2() throws Exception {
        try {
            WrapDatabaseObject treeObject = (WrapDatabaseObject) studio.getFirstSelectedTreeObject();
            if (treeObject != null && treeObject.instanceOf(Sequence.class)) {
                SequenceView sequenceTreeObject = (SequenceView) treeObject;
                openEditors(/*explorerView, */sequenceTreeObject);

                Sequence sequence = sequenceTreeObject.getObject();
                ProjectView projectTreeObject = sequenceTreeObject.getProjectView();
                SequenceEditorWrap sequenceEditor = projectTreeObject.getSequenceEditor(sequence);
                if (sequenceEditor != null) {
                    //getActivePage().activate(sequenceEditor);
                    sequenceEditor.getDocument(sequence.getName(), null, isStubRequested());
                }
            }
        }
        catch (Exception e) {
            throw e;
        }
    }

    protected boolean isStubRequested() {
        return false;
    }

    protected void openEditors(/*ProjectExplorerView explorerView, */WrapDatabaseObject treeObject) {
        openEditors(/*explorerView, */treeObject, new HashSet<SequenceStep>());
    }

    private void openEditors(/*ProjectExplorerView explorerView, */WrapDatabaseObject treeObject, Set<SequenceStep> alreadyOpened) {
        if (treeObject.instanceOf(Sequence.class)) {
            SequenceView sequenceTreeObject = (SequenceView) treeObject;
            openEditors(/*explorerView, */sequenceTreeObject.getObject().getSteps(), alreadyOpened);
            sequenceTreeObject.openSequenceEditor();
        }
    }

    private void openEditors(/*ProjectExplorerView explorerView, */List<Step> steps, Set<SequenceStep> alreadyOpened) {
        for (Step step: steps) {
            if (step.isEnabled()) {
                if (step instanceof SequenceStep) {
                    SequenceStep sequenceStep = (SequenceStep)step;
                    String projectName = sequenceStep.getProjectName();
                    // load project if necessary
                    if (!step.getSequence().getProject().getName().equals(projectName)) {
                        //loadProject(explorerView, projectName);
                    }

                    if (alreadyOpened.contains(sequenceStep)) {
                        return; // avoid sequence recursion
                    }
                    alreadyOpened.add(sequenceStep);

                    try {
                       // ProjectTreeObject projectTreeObject = (ProjectTreeObject)explorerView.getProjectRootObject(projectName);
                        Project p = step.getProject();
                        Sequence subSequence = p.getSequenceByName(sequenceStep.getSequenceName());
                        SequenceView subSequenceTreeObject = (SequenceView) Studio.getViewFromDbo(subSequence, studio);
                        openEditors(/*explorerView, */subSequenceTreeObject, alreadyOpened); // recurse on sequence
                    }
                    catch (EngineException e) {
                    }
                }
                else if (step instanceof TransactionStep) {
                    TransactionStep transactionStep = (TransactionStep) step;
                    String projectName = transactionStep.getProjectName();
                    if (!step.getSequence().getProject().getName().equals(projectName)) {
                        //loadProject(explorerView, projectName); // load project if necessary
                    }

                    try {
                        //ProjectTreeObject projectTreeObject = (ProjectTreeObject)explorerView.getProjectRootObject(projectName);
                        Project project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
                        ProjectView projectTreeObject = new ProjectView(project, studio);
                        Connector connector = projectTreeObject.getObject().getConnectorByName(transactionStep.getConnectorName());
                        ConnectorView connectorTreeObject = new ConnectorView(connector, studio);
                        connectorTreeObject.openConnectorEditor(); // open connector editor
                    }
                    catch (EngineException e) {
                    }
                }
                else if (step instanceof StepWithExpressions) {
                    openEditors(/*explorerView, */((StepWithExpressions)step).getSteps(), alreadyOpened);
                }
            }
        }
    }
}
