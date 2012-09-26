package com.twinsoft.convertigo.beans.core;


public abstract class Reference extends DatabaseObject implements ISchemaImportGenerator {
	private static final long serialVersionUID = -1201316885732909011L;

	public boolean isGenerateSchema() {
		return true;
	}	
}
