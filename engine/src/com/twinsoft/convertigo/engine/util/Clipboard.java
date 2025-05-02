/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.util;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.DatabaseObject.ExportOption;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;

public class Clipboard {
	
	private Document clipboardDocument;
	private Element clipboardRootElement;
	private DatabaseObject currentScreenClassOrTransaction;
	private List<DatabaseObject> dboList = new ArrayList<DatabaseObject>();
	
	public Clipboard() {
		init();
	}
	
	protected void init() {
		currentScreenClassOrTransaction = null;
		
		clipboardDocument = XMLUtils.getDefaultDocumentBuilder().newDocument();
		
		ProcessingInstruction pi = clipboardDocument.createProcessingInstruction("xml", "version=\"1.0\" encoding=\"ISO-8859-1\"");
		clipboardDocument.appendChild(pi);
		
		clipboardRootElement = clipboardDocument.createElement("convertigo-clipboard");
		clipboardDocument.appendChild(clipboardRootElement);
	}
	
	public void reset() {
		dboList.clear();
		init();
	}
	
	public void add(DatabaseObject dbo) {
		if (dbo != null) {
			dboList.add(dbo);
		}
	}
	
	public String toXml() throws EngineException {
		for (DatabaseObject dbo: dboList) {
			copyDatabaseObject(dbo);
		}
		dboList.clear();
		return XMLUtils.prettyPrintDOM(clipboardDocument);
	}

	private void copyDatabaseObject(DatabaseObject databaseObject) throws EngineException {
		currentScreenClassOrTransaction = null;
		
		try {
			new WalkHelper() {
				// recursion parameters
				Element parentElement;
				
				public void init(DatabaseObject databaseObject, Element parentElement) throws Exception {
					this.parentElement = parentElement;
					super.init(databaseObject);
				}

				@Override
				protected void walk(DatabaseObject databaseObject) throws Exception {
					// retrieve recursion parameters
					final Element parentElement = this.parentElement;
					
					// Remember the current screen class or transaction for detecting inherited objects.
					if ((databaseObject instanceof ScreenClass) || (databaseObject instanceof Transaction)) {
						currentScreenClassOrTransaction = databaseObject;
					}
					
					// We should not include inherited objects (only for tree pastes).
					else if ((databaseObject instanceof Criteria) || (databaseObject instanceof ExtractionRule) ||
							(databaseObject instanceof Sheet) || (databaseObject instanceof BlockFactory)) {
						if ((currentScreenClassOrTransaction != null) && (currentScreenClassOrTransaction != databaseObject.getParent())) {
							return;
						}
					}
					
					Element element = parentElement;
					element = databaseObject.toXml(clipboardDocument, ExportOption.bIncludeVersion);
					appendDndData(element, databaseObject);
					parentElement.appendChild(element);
					
					// new value of recursion parameters
					this.parentElement = element;
					super.walk(databaseObject);
					
					// restore recursion parameters
					this.parentElement = parentElement;
				}
				
			}.init(databaseObject, clipboardRootElement);
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Exception in copyDatabaseObject", e);
		}
	}
	
	private void appendDndData(Element element, DatabaseObject databaseObject) {
		Element dnd = clipboardDocument.createElement("dnd");
		Element e;
		try {
			if (databaseObject instanceof Sequence) {
				Sequence sequence = (Sequence)databaseObject;
				e = clipboardDocument.createElement("project");
				e.setAttribute("name", sequence.getProject().getName());
				dnd.appendChild(e);
			} else if (databaseObject instanceof Transaction) {
				Transaction transaction = (Transaction)databaseObject;
				e = clipboardDocument.createElement("project");
				e.setAttribute("name", transaction.getProject().getName());
				dnd.appendChild(e);
				
				e = clipboardDocument.createElement("connector");
				e.setAttribute("name", transaction.getConnector().getName());
				dnd.appendChild(e);
			} else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIComponent) {
				com.twinsoft.convertigo.beans.mobile.components.UIComponent uic = GenericUtils.cast(databaseObject);
				e = clipboardDocument.createElement("project");
				e.setAttribute("name", uic.getProject().getName());
				dnd.appendChild(e);
	
				e = clipboardDocument.createElement("mobileapplication");
				e.setAttribute("name", uic.getApplication().getParentName());
				dnd.appendChild(e);
	
				e = clipboardDocument.createElement("application");
				e.setAttribute("name", uic.getApplication().getName());
				dnd.appendChild(e);
			} else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIComponent) {
				com.twinsoft.convertigo.beans.ngx.components.UIComponent uic = GenericUtils.cast(databaseObject);
				e = clipboardDocument.createElement("project");
				e.setAttribute("name", uic.getProject().getName());
				dnd.appendChild(e);
	
				e = clipboardDocument.createElement("mobileapplication");
				e.setAttribute("name", uic.getApplication().getParentName());
				dnd.appendChild(e);
	
				e = clipboardDocument.createElement("application");
				e.setAttribute("name", uic.getApplication().getName());
				e.setAttribute("requiredTplVersion", uic.requiredTplVersion());
				dnd.appendChild(e);
			}
		} catch (Exception ex) {}
		element.appendChild(dnd);
	}
	
}
