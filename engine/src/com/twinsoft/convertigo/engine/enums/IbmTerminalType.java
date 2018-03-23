package com.twinsoft.convertigo.engine.enums;

public enum IbmTerminalType {
	NONE(""),
	IBM3278("IBM-3278"),
	IBM3279("IBM-3279");
	
	String toString;
	
	IbmTerminalType () {
		toString = name();
	}
	
	IbmTerminalType (String toString) {
		this.toString = toString;
	}
	
	@Override
	public String toString() {
		return toString;
	}
}
