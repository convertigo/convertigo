package com.twinsoft.convertigo.engine;

public class ExpiredSecurityTokenException extends SecurityTokenException {

	private static final long serialVersionUID = 577212443061751865L;

	public ExpiredSecurityTokenException(String tokenID) {
		super(tokenID);
	}

	@Override
	public String getMessage() {
		return "Expired token ID: " + "'" + tokenID + "'";
	}
}
