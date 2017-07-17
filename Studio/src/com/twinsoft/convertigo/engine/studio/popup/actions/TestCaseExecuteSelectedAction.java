package com.twinsoft.convertigo.engine.studio.popup.actions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.studio.editors.connector.ConnectorEditorWrap;
import com.twinsoft.convertigo.engine.studio.editors.sequence.SequenceEditorWrap;
import com.twinsoft.convertigo.engine.studio.responses.sequences.SequenceExecuteSelectedResponse;
import com.twinsoft.convertigo.engine.studio.wrappers.ProjectView;
import com.twinsoft.convertigo.engine.studio.wrappers.SequenceView;
import com.twinsoft.convertigo.engine.studio.wrappers.TransactionView;
import com.twinsoft.convertigo.engine.studio.wrappers.WrapDatabaseObject;
import com.twinsoft.convertigo.engine.studio.wrappers.WrapStudio;

public class TestCaseExecuteSelectedAction extends AbstractRunnableAction {

    public TestCaseExecuteSelectedAction(WrapStudio studio) {
        super(studio);
    }

    @Override
    protected void run2() {
        WrapDatabaseObject treeObject = (WrapDatabaseObject) studio.getFirstSelectedTreeObject();
        if ((treeObject != null) && (treeObject.instanceOf(TestCase.class))) {
            TestCase testCase = (TestCase) treeObject.getObject();
            ProjectView projectTreeObject = treeObject.getProjectTreeObject();

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

    @Override
    public Element toXml(Document document, String qname) throws ConvertigoException, Exception {
        Element response = super.toXml(document, qname);
        if (response != null) {
            return response;
        }

        return new SequenceExecuteSelectedResponse().toXml(document, qname);
    }
}
