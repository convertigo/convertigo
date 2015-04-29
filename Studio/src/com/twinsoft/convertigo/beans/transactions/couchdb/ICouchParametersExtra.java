package com.twinsoft.convertigo.beans.transactions.couchdb;

import java.util.Collection;

import com.twinsoft.convertigo.engine.enums.CouchExtraVariable;

public interface ICouchParametersExtra {
	
	public abstract Collection<CouchExtraVariable> getCouchParametersExtra();
}
