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

package com.twinsoft.convertigo.beans.html;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 
 * This class simply removes nodes from ouputDom.
 *
 */
public class XMLAddButton extends XMLAddLink {

	private static final long serialVersionUID = 6093906527851500534L;

	private String imageUrl = null;
	
	public XMLAddButton() {
		super();
		imageUrl = "../../images/add_button.gif";
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	
	protected void setLinkContent(Element parentElem, Document dom) {
		// IMG element generation
		Element imgElem = dom.createElement("IMG");
		// add attributes
		imgElem.setAttribute("src", imageUrl);
		imgElem.setAttribute("alt", getText());
		imgElem.setAttribute("title", getText());
		// append to parent A element
		parentElem.appendChild(imgElem);
	}
}
