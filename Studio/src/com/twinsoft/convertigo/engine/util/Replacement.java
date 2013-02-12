/**
 * 
 */
package com.twinsoft.convertigo.engine.util;

public class Replacement {
	private String strSource;
	private String strTarget;
	
	public Replacement() {
		this("","");
	}
	
	public Replacement(String strSource, String strTarget) {
		this.setSource(strSource);
		this.setTarget(strTarget);
	}

	public String getSource() {
		return strSource;
	}

	public void setSource(String strSource) {
		this.strSource = strSource;
	}

	public String getTarget() {
		return strTarget;
	}

	public void setTarget(String strTarget) {
		this.strTarget = strTarget;
	}
}
