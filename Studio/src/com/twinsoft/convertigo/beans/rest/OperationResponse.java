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

package com.twinsoft.convertigo.beans.rest;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;

public class OperationResponse extends AbstractRestResponse {

	private static final long serialVersionUID = -8385527221074999901L;

	public OperationResponse() {
		super();
	}

	@Override
	public OperationResponse clone() throws CloneNotSupportedException {
		OperationResponse clonedObject = (OperationResponse)super.clone();
		return clonedObject;
	}
	
	private String xpath = "";
	
	public String getXpath() {
		return xpath;
	}

	public void setXpath(String xpath) {
		this.xpath = xpath;
	}
	
	public boolean isMatching(Document xmlDocument) {
		if (xmlDocument != null && !xpath.isEmpty()) {
			try {
				TwsCachedXPathAPI xpathApi = new TwsCachedXPathAPI();
				NodeList nodeList = xpathApi.selectNodeList(xmlDocument, xpath);
				int  length = nodeList.getLength();
				return (length > 0) ? true:false;
			} catch (Exception e) {}
		}
		return false;
	}
}
