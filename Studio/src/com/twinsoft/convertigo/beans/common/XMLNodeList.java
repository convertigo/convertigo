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

import com.twinsoft.convertigo.beans.extractionrules.HtmlExtractionRule;

/**
 * 
 * This class simply extract a nodelist.
 *
 */
public class XMLNodeList extends HtmlExtractionRule {

	private static final long serialVersionUID = 6345966543290198172L;

	public XMLNodeList() {
		super();
	}

	public String getSchema(String tns) {
		return "<xsd:any minOccurs=\"0\" maxOccurs=\"unbounded\" namespace=\"##any\"/>\n";
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
