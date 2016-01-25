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
	
	public enum JsonRoot {
		docNode("document node"),
		docChildNodes("document child nodes"),
		docAttrAndChildNodes("document attributes and child nodes");
		
		private final String label;
		
		private JsonRoot(String label) {
			this.label = label;
		}
		
		public String toString() {
			return label;
		}
	}
}
