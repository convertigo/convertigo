package com.twinsoft.convertigo.engine.studio.popup.actions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.studio.editors.sequence.SequenceEditorWrap;
import com.twinsoft.convertigo.engine.studio.responses.sequences.SequenceExecuteSelectedResponse;
import com.twinsoft.convertigo.engine.studio.wrappers.ConnectorView;
import com.twinsoft.convertigo.engine.studio.wrappers.ProjectView;
import com.twinsoft.convertigo.engine.studio.wrappers.SequenceView;
import com.twinsoft.convertigo.engine.studio.wrappers.Studio;
import com.twinsoft.convertigo.engine.studio.wrappers.WrapDatabaseObject;
import com.twinsoft.convertigo.engine.studio.wrappers.WrapStudio;

public class SequenceExecuteSelectedAction extends AbstractRunnableAction {

    public SequenceExecuteSelectedAction(WrapStudio studio) {
        super(studio);
    }

    @Override
    protected void run2() {
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

    @Override
    public Element toXml(Document document, String qname) throws ConvertigoException, Exception {
        Element response = super.toXml(document, qname);
        if (response != null) {
            return response;
        }

        return new SequenceExecuteSelectedResponse().toXml(document, qname);
    }
}
