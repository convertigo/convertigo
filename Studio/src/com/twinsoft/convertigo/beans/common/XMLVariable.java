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

import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.extractionrules.HtmlExtractionRule;
import com.twinsoft.convertigo.engine.Context;

public class XMLVariable extends HtmlExtractionRule {

	private static final long serialVersionUID = -3201130024615529551L;

	public XMLVariable() {
		super();
	}
	
	public boolean apply(Document xmlDom, Context context) {
		return super.apply(xmlDom, context);
	}

	protected void appendToOutputDom(NodeList nodeList, Document outputDom) {
		// Do not append to output document
	}

	public void addToScope(Scriptable scope) {
		scope.put(this.name, scope, this.resultList);
	}

	public String getSchema(String tns) {
		return "";
	}
	
	public String getSchemaElementName() {
		return "";
	}

	public String getSchemaElementType() {
		return "";
	}
	
	public String getSchemaElementNSType(String tns) {
		return "";
	}
}
