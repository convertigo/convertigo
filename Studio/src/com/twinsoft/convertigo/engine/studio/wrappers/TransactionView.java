package com.twinsoft.convertigo.engine.studio.wrappers;

import com.twinsoft.convertigo.beans.core.Transaction;

public class TransactionView extends DatabaseObjectView {

    public TransactionView(Transaction transaction, WrapStudio studio) {
        super(transaction, studio);
    }

    public ConnectorView getConnectorTreeObject() {
        return new ConnectorView(getObject().getConnector(), studio);
    }

    @Override
    public Transaction getObject() {
        return (Transaction) super.getObject();
    }
}
