package com.twinsoft.convertigo.engine;

public abstract class SecurityTokenException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4787084718087880623L;

	protected String tokenID;
	
	public SecurityTokenException(String tokenID) {
		super();
		this.tokenID = tokenID;
	}
}
