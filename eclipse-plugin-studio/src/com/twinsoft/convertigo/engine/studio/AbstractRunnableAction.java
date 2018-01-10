package com.twinsoft.convertigo.engine.studio;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.ConvertigoException;

public abstract class AbstractRunnableAction {

	protected WrapStudio studio;
	private boolean isDone = false;

	public AbstractRunnableAction(WrapStudio studio) {
		this.studio = studio;
	}

	public void run() throws Exception {
		run2();
		isDone = true;
	}

	protected abstract void run2() throws Exception;

	public Element toXml(Document document, String qname) throws ConvertigoException, Exception {
		// Can't generate XML while the action is not finished yet
		if (!isDone) {
			throw new ConvertigoException("The action is not finished yet.");
		}

		return null;
	}

	public boolean isDone() {
		return isDone;
	}
}
