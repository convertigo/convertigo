package com.twinsoft.convertigo.engine.enums;

public enum EndlineType {
	unix("\n"),
	windows("\r\n"),
	dynamic(System.getProperty("line.separator"));
	
	String endline;
	
	EndlineType(String endline) {
		this.endline = endline;
	}
	
	public String endline() {
		return endline;
	}
}
