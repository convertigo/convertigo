package com.twinsoft.convertigo.beans.steps;

import com.twinsoft.convertigo.engine.EngineException;

public class StepException extends EngineException {
	private static final long serialVersionUID = -3021122246529079529L;

	public String message;
	public String details;

	public StepException(String message, String details) {
		super(null);
		
		this.message = message;
		this.details = details;
	}
	
	@Override
	public String getMessage() {
		return message + "\nDetails: " + details;
	}
}