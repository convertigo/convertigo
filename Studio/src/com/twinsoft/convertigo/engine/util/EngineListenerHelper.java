package com.twinsoft.convertigo.engine.util;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineListener;

public class EngineListenerHelper implements EngineListener {

	public void blocksChanged(EngineEvent engineEvent) {
		

	}

	public void objectDetected(EngineEvent engineEvent) {
		

	}

	public void documentGenerated(EngineEvent engineEvent) {
		Object source = engineEvent.getSource();
		if (source != null && source instanceof Document) {
			documentGenerated((Document) source);
		}
	}

	public void stepReached(EngineEvent engineEvent) {
		

	}

	public void transactionStarted(EngineEvent engineEvent) {
		

	}

	public void transactionFinished(EngineEvent engineEvent) {
		

	}

	public void sequenceStarted(EngineEvent engineEvent) {
		

	}

	public void sequenceFinished(EngineEvent engineEvent) {
		

	}

	public void clearEditor(EngineEvent engineEvent) {
		

	}
	
	public void documentGenerated(Document document) {
		
	}
}
