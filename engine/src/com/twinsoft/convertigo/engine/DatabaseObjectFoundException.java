package com.twinsoft.convertigo.engine;

import com.twinsoft.convertigo.beans.core.DatabaseObject;

public class DatabaseObjectFoundException extends Exception {
	private static final long serialVersionUID = 971053121402061961L;

	DatabaseObject databaseObject;
	
	public DatabaseObjectFoundException(DatabaseObject databaseObject) {
		this.databaseObject = databaseObject;
	}
	
	public DatabaseObject getDatabaseObject() {
		return databaseObject;
	}
}
