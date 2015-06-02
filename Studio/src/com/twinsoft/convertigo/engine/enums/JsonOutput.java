package com.twinsoft.convertigo.engine.enums;

public enum JsonOutput {
	verbose,
	useType("use type attributes");
	
	String toString;
	
	JsonOutput () {
		toString = name();
	}
	
	JsonOutput (String toString) {
		this.toString = toString;
	}
	
	public String toString() {
		return toString;
	}
}
