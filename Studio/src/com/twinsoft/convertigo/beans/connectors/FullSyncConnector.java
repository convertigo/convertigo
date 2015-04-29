package com.twinsoft.convertigo.beans.connectors;

import com.twinsoft.convertigo.beans.transactions.couchdb.AbstractDatabaseTransaction;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchClient;
import com.twinsoft.convertigo.engine.providers.couchdb.FullSyncContext;

public class FullSyncConnector extends CouchDbConnector {
	private static final long serialVersionUID = 4063707392313093177L;
	
	@Override
	public CouchClient getCouchClient() {
		return Engine.theApp.couchDbManager.getFullSyncClient();
	}
	
	@Override
	public String getDatabaseName() {
		return getName();
	};
	
	@Override
	public String getTargetDatabase(AbstractDatabaseTransaction couchDbTransaction) {
		return getDatabaseName();
	}
	
	@Override
	public void setName(String name) throws EngineException {
		if (!name.matches("^[a-z][a-z0-9_$()+/-]*$")) {
			throw new EngineException("Must begin with a letter with only lowercase characters, digits and _");
		}
		super.setName(name);
	}

	@Override
	public void beforeTransactionInvoke() {
		FullSyncContext.set(context.getAuthenticatedUser());
	}

	@Override
	public void afterTransactionInvoke() {
		FullSyncContext.unset();
	}
		
}
