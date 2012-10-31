package com.twinsoft.convertigo.engine;

public class NoSuchSecurityTokenException extends SecurityTokenException {

	private static final long serialVersionUID = 577212443061751865L;

	public NoSuchSecurityTokenException(String tokenID) {
		super(tokenID);
	}

	@Override
	public String getMessage() {
		return "Unknown token ID: " + "'" + tokenID + "'";
	}
}
