package com.twinsoft.convertigo.engine.studio.views.projectexplorer;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.studio.AbstractRunnableAction;
import com.twinsoft.convertigo.engine.studio.responses.XmlResponseFactory;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapDatabaseObject;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapObject;

public class CheStudio extends Studio {

	private Document document;

	public CheStudio(Document document) {
		setDocument(document);
	}

	public Document getDocument() {
	    return document;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public void createResponse(Element element) {
	    if (element != null) {
	        document
                .getDocumentElement()
                .appendChild(element);
	    }
	}

	public void runAction(AbstractRunnableAction action) throws DOMException, Exception {
		// Get all qnames to generate the response later
		List<String> qnames = new ArrayList<>(getSelectedObjects().size());
		for (WrapObject wrapObject: getSelectedObjects()) {
			WrapDatabaseObject wrapDbo = (WrapDatabaseObject) wrapObject;
			qnames.add(((DatabaseObject) wrapDbo.getObject()).getQName());			
		}

		action.run();
		isActionDone = action.isDone();

		// Generate responses
		for (String qname: qnames) {
		    Element xmlResponse = action.toXml(document, qname);
		    createResponse(xmlResponse);
		}

		// End of the action: notify
		synchronized (this) {
			notify();
		}
	}

	@Override
	public int openMessageDialog(String title, Object object, String msg, String string, String[] buttons, int defaultIndex) {
		try {
		    createResponse(XmlResponseFactory.createMessageDialogResponse(document, null, title, msg, buttons));
		}
		catch (Exception e1) {
		}

		synchronized (this) {
			notify();
			try {
				wait();
			}
			catch (InterruptedException e) {
			}

			return response;
		}
	}

	@Override
	public int openMessageBox(String title, String msg, String[] buttons) {
		try {
		    createResponse(XmlResponseFactory.createMessageDialogResponse(document, null, title, msg, buttons));
		}
		catch (Exception e1) {
		}

		synchronized (this) {
			notify();
			try {
				wait();
			}
		    catch (InterruptedException e) {
			}

			return response;
		}
	}
}
