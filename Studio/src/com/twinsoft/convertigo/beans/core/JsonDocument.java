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

package com.twinsoft.convertigo.beans.core;

import org.w3c.dom.Element;

import com.google.gson.JsonObject;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.Json;

public class JsonDocument extends Document {

	private static final long serialVersionUID = -158097183400259342L;

	transient protected JsonObject jsonDocument = new JsonObject();
	
	public JsonDocument() {
		super();
	}
	
	@Override
	public JsonDocument clone() throws CloneNotSupportedException {
		JsonDocument clonedObject =  (JsonDocument) super.clone();
		return clonedObject;
	}

	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
		// load document
		if (docdata != null) {
			jsonDocument = Json.newJsonObject(docdata);
		}
	}

	@Override
	public Element toXml(org.w3c.dom.Document document) throws EngineException {
    	// store document
		if (jsonDocument != null) {
    		docdata = jsonDocument.toString();
    	}

		return super.toXml(document);
	}

	public JsonObject getJsonObject() {
		return jsonDocument;
	}

	public void setJsonObject(JsonObject jsonObject) {
		jsonDocument = jsonObject;
	}
}
