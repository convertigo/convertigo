package com.twinsoft.convertigo.engine.studio.wrappers;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;

public class DatabaseObjectView implements WrapDatabaseObject {

	private DatabaseObject dbo;
	
	public DatabaseObjectView(DatabaseObject dbo) {
		this.dbo = dbo;
	}

	@Override
	public DatabaseObject getObject() {
		return dbo;
	}
	
	@Override
	public WrapDatabaseObject getParent() {
		return new DatabaseObjectView(dbo.getParent());
	}
	
	@Override
	public String getName() {
		return dbo.getName();
	}
	
	@Override
	public void hasBeenModified(boolean hasBeenModified) {
	}
	
	@Override
	public void closeAllEditors() {
	}
	
	@Override
	public void closeSequenceEditors(Sequence object) {
	}
	
	@Override
	public void closeConnectorEditors(Connector object) {		
	}

}
