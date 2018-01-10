package com.twinsoft.convertigo.engine.studio.popup.actions;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.studio.AbstractRunnableAction;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.editors.connectors.ConnectorEditorWrap;
import com.twinsoft.convertigo.engine.studio.editors.sequences.SequenceEditorWrap;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.ProjectView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.SequenceView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.TransactionView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapDatabaseObject;

public class TestCaseExecuteSelectedAction extends AbstractRunnableAction {

    public TestCaseExecuteSelectedAction(WrapStudio studio) {
        super(studio);
    }

    @Override
    protected void run2() throws Exception {
        try {
            WrapDatabaseObject treeObject = (WrapDatabaseObject) studio.getFirstSelectedTreeObject();
            if ((treeObject != null) && (treeObject.instanceOf(TestCase.class))) {
                TestCase testCase = (TestCase) treeObject.getObject();
                ProjectView projectTreeObject = treeObject.getProjectViewObject();

                RequestableObject requestable = (RequestableObject) testCase.getParent();
                if (requestable instanceof Transaction) {
                    TransactionView transactionTreeObject = (TransactionView) treeObject/*.getParent()*/.getParent();
                    transactionTreeObject.getConnectorTreeObject().openConnectorEditor();

                    Transaction transaction = (Transaction) testCase.getParent();
                    Connector connector = (Connector) transaction.getParent();
                    ConnectorEditorWrap connectorEditor = projectTreeObject.getConnectorEditor(connector);
                    if (connectorEditor != null) {
                        //getActivePage().activate(connectorEditor);
                        connectorEditor.getDocument(transaction.getName(), testCase.getName(), false);
                    }
                }
                else if (requestable instanceof Sequence) {
                    SequenceView sequenceTreeObject = (SequenceView) treeObject/*.getParent()*/.getParent();
                    new SequenceExecuteSelectedAction(studio).openEditors(sequenceTreeObject);

                    Sequence sequence = (Sequence) testCase.getParent();
                    SequenceEditorWrap sequenceEditor = projectTreeObject.getSequenceEditor(sequence);
                    if (sequenceEditor != null) {
                        //getActivePage().activate(sequenceEditor);
                        sequenceEditor.getDocument(sequence.getName(), testCase.getName(), false);
                    }
                }
            } 
        }
        catch (Exception e) {
            throw e;
        }
    }
}
