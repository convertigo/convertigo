package com.twinsoft.convertigo.beans.core;

import com.twinsoft.convertigo.engine.EngineException;

public class SourceNotFoundException extends EngineException {
	private static final long serialVersionUID = 1882501591683615914L;

	public SourceNotFoundException(String message) {
		super(message);
	}
	
	public SourceNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
