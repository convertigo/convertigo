package com.twinsoft.convertigo.beans.core;


public abstract class Reference extends DatabaseObject {
	private static final long serialVersionUID = -1201316885732909011L;

	public Reference() {
		super();
		databaseType = "Reference";
	}
}
