package com.twinsoft.convertigo.engine.studio.editors.connectors;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.engine.studio.editors.EditorPartWrap;

public class ConnectorEditorWrap extends EditorPartWrap {
    private ConnectorEditorPartWrap connectorEditorPart;
    private boolean dirty;
    
    public ConnectorEditorWrap(Connector connector) {
        connectorEditorPart = new ConnectorEditorPartWrap(this, connector);
    }

    public void getDocument(String transactionName, boolean isStubRequested) {
        getDocument(transactionName, null, isStubRequested);
    }

    public void getDocument(String transactionName, String testcaseName, boolean isStubRequested) {
        connectorEditorPart.getDocument(transactionName, testcaseName, isStubRequested);
    }

    public Document getLastGeneratedDocument() {
        return connectorEditorPart.lastGeneratedDocument;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }
}
