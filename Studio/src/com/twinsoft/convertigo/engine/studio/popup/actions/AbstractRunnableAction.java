package com.twinsoft.convertigo.engine.studio.popup.actions;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.studio.responses.XmlResponseFactory;
import com.twinsoft.convertigo.engine.studio.wrappers.WrapStudio;

public abstract class AbstractRunnableAction {

	protected WrapStudio studio;
	private boolean isDone = false;
	protected Map<String, String> dboExceptionMessages;

	public AbstractRunnableAction(WrapStudio studio) {
		this.studio = studio;
	}

	public void run() {
		dboExceptionMessages = new HashMap<>();
		run2();
		isDone = true;
	}

	protected abstract void run2();

	public Element toXml(Document document, String qname) throws ConvertigoException, Exception {
		// Can't generate XML while the action is not finished yet
		if (!isDone) {
			throw new ConvertigoException("The action is not finished yet.");
		}

		String exceptionMessage = dboExceptionMessages.get(qname);
		return exceptionMessage == null ?
				// No exception thrown during the execution of the action
			    null :
				// Exception thrown
			    XmlResponseFactory.createMessageBoxResponse(document, qname, exceptionMessage);
	}

	public boolean isDone() {
		return isDone;
	}

}
