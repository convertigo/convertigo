/*
 * Copyright (c) 2001-2021 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.studio;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.studio.responses.XmlResponseFactory;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapDatabaseObject;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapObject;
import com.twinsoft.convertigo.engine.studio.views.sourcepicker.SourcePickerViewWrap;

public class CheStudio extends Studio {

	private Document document;
	private static SourcePickerViewWrap sourcePickerViewWrap;

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

	public void runAction(AbstractRunnableAction action) throws Exception {
	    try {
    		// Get all qnames to generate the response later
    		List<String> qnames = new ArrayList<>(getSelectedObjects().size());
    		for (WrapObject wrapObject: getSelectedObjects()) {
    			WrapDatabaseObject wrapDbo = (WrapDatabaseObject) wrapObject;
    			qnames.add(((DatabaseObject) wrapDbo.getObject()).getQName());			
    		}

    		action.run();
    		synchronized (this) {
        		isActionDone = action.isDone();

        		// Generate responses
        		for (String qname: qnames) {
        		    Element xmlResponse = action.toXml(document, qname);
        		    createResponse(xmlResponse);
        		}
    
        		// End of the action: notify
    			notify();
    		}
	    }
		catch (Exception e) {
            isActionDone = true;
            throw e;
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

    @Override
    public SourcePickerViewWrap getSourcePickerView() {
        if (sourcePickerViewWrap == null) {
            sourcePickerViewWrap = new SourcePickerViewWrap(this);
        }

        sourcePickerViewWrap.updateStudio(this);
        return sourcePickerViewWrap;
    }
}
