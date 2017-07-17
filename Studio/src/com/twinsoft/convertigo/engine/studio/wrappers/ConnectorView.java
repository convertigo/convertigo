package com.twinsoft.convertigo.engine.studio.wrappers;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.engine.studio.editors.IEditorPartWrap;
import com.twinsoft.convertigo.engine.studio.responses.connectors.SequenceExecuteSelectedOpenConnectorEditor;

public class ConnectorView extends DatabaseObjectView {

    public ConnectorView(Connector connector, WrapStudio studio) {
        super(connector, studio);
    }

    public void openConnectorEditor() {
        // To add listeners
        getConnectorEditor(getObject());

        synchronized (studio) {
            try {
                studio.createResponse(
                    new SequenceExecuteSelectedOpenConnectorEditor(getObject())
                        .toXml(studio.getDocument(), getObject().getQName())
                );
            }
            catch (Exception e1) {
            }

            studio.notify();

            try {
                studio.wait();
            }
            catch (InterruptedException e) {
            }
        }
    }

    private IEditorPartWrap getConnectorEditor(Connector connector) {
        return getProjectView().getConnectorEditor(connector);
    }

    @Override
    public Connector getObject() {
        return (Connector) super.getObject();
    }

    public ProjectView getProjectView() {
        return (ProjectView) getParent();
    }
}
