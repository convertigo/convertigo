package com.twinsoft.convertigo.engine.enums;

public enum FullSyncAclPolicy {
	fromAuthenticatedUser("From authenticated user"),
	anonymous("Anonymous (public)"),
	noOp("Don't modify existing ACL"),
	fromKeyC8oAcl("From _c8oAcl key");
	
	String toString;
	
	FullSyncAclPolicy () {
		toString = name();
	}
	
	FullSyncAclPolicy (String toString) {
		this.toString = toString;
	}
	
	public String toString() {
		return toString;
	}
}
