package com.twinsoft.convertigo.beans.connectors;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.transactions.ExternalBrowserTransaction;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.EngineException;

public class ExternalBrowserConnector extends Connector {
	private static final long serialVersionUID = -9120542678913016863L;

	@Override
	public void prepareForTransaction(Context context) throws EngineException {
		// TODO Auto-generated method stub

	}

	@Override
	public ExternalBrowserTransaction newTransaction() {
		return new ExternalBrowserTransaction();
	}
}