/**
 * 
 */
package com.twinsoft.convertigo.engine.util;

public class Replacement {
	private String strSource;
	private String strTarget;
	private String startsWith;
	
	public Replacement() {
		this("","");
	}
	
	public Replacement(String strSource, String strTarget) {
		this(strSource, strTarget, null);
	}
	
	public Replacement(String strSource, String strTarget, String startsWith) {
		this.setSource(strSource);
		this.setTarget(strTarget);
		this.setStartsWith(startsWith);
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
	
	public String getStartsWith() {
		return startsWith;
	}

	public void setStartsWith(String startsWith) {
		this.startsWith = startsWith;
	}
}
