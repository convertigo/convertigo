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

package com.twinsoft.convertigo.beans.common;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.extractionrules.HtmlExtractionRule;
import com.twinsoft.convertigo.engine.Context;

public class XMLHttpUrl extends HtmlExtractionRule {

	private static final long serialVersionUID = 6419023789584983524L;

	private String tagName = null;
	
	public XMLHttpUrl() {
		super();
		tagName = "HttpUrl";
	}

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}
	
	@Override
	public boolean apply(Document xmlDom, Context context) {
		try {
			Document doc = context.outputDocument;
			Element root = doc.getDocumentElement();
			Element eurl = doc.createElement((tagName.equals("") ? getName():tagName));
			eurl.appendChild(doc.createTextNode(getReferer(context)));
			root.appendChild(eurl);
			return true;
		}
		catch (Exception ex) {}
		return false;
	}
	
	public String getSchema(String tns) {
		return "<xsd:element minOccurs=\"0\" maxOccurs=\"1\" name=\""+ (tagName.equals("") ? getName():tagName) + "\" type=\"xsd:string\" />\n";
	}
	
	public String getSchemaElementName() {
		return (tagName.equals("") ? getName():tagName);
	}

	public String getSchemaElementType() {
		return "string";
	}
	
	public String getSchemaElementNSType(String tns) {
		return "xsd:string";
	}

}
