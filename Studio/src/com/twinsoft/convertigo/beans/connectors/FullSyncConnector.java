package com.twinsoft.convertigo.beans.connectors;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchClient;

public class FullSyncConnector extends CouchDbConnector {
	private static final long serialVersionUID = 4063707392313093177L;
	
	@Override
	public CouchClient getCouchClient() {
		return Engine.theApp.couchDbManager.getCouchClient();
	}
}
