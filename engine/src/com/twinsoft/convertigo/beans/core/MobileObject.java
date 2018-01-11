package com.twinsoft.convertigo.beans.core;


public abstract class MobileObject extends DatabaseObject {
	
	private static final long serialVersionUID = -2681872537732721040L;

	public MobileObject() {
		super();
		databaseType = "MobileObject";
	}

	@Override
	public MobileObject clone() throws CloneNotSupportedException {
		MobileObject cloned = (MobileObject) super.clone();
		return cloned;
	}
	
	
}
