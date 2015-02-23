/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.couchdb;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.core.JsonDocument;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.CouchKey;

public class DesignDocument extends JsonDocument {

	private static final long serialVersionUID = -1523783503757936794L;
	
	public DesignDocument() {
		super();
	}

	@Override
	public DesignDocument clone() throws CloneNotSupportedException {
		DesignDocument clonedObject =  (DesignDocument) super.clone();
		return clonedObject;
	}

	@Override
	public String getRenderer() {
		return "DesignDocumentTreeObject";
	}

	@Override
	public Element toXml(Document document) throws EngineException {
		if (jsonDocument != null) {
			if (bNew) {
				CouchKey._rev.remove(jsonDocument);
				CouchKey._id.put(jsonDocument, CouchKey._design.key() + getName());
			}
		}
		return super.toXml(document);
	}

	//*********************************************************************************
	// DO NOT REMOVE following fake getters/setters of _id and _rev bean's properties
	// The properties values are handled by the renderer : see DesignDocumentTreeObject
	//---------------------------------------------------------------------------------
	public String getId() {
		return "";
	}

	public void setId(String id) {
		// does nothing
	}
	
	public String getRevision() {
		return "";
	}

	public void setRevision(String revision) {
		// does nothing
	}
	//*********************************************************************************
	
	@Override
	public CouchDbConnector getConnector() {
		return (CouchDbConnector) super.getConnector();
	}
}
