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
public class XMLAddImage extends XMLAddText {

	private static final long serialVersionUID = 8151719137075322727L;

	private String imageUrl = null;
		
	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public XMLAddImage() {
		super();
		imageUrl = "";
	}
	
	protected Element generateContent(Document dom) {
		// IMG element creation
		Element imgElem = dom.createElement("IMG");
		// add of attributes
		imgElem.setAttribute("src", getImageUrl());
		imgElem.setAttribute("alt", getText());
		imgElem.setAttribute("title", getText());
		return imgElem;
	}
	
}
