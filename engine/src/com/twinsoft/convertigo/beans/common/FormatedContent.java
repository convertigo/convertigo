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

package com.twinsoft.convertigo.beans.common;

import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class FormatedContent implements XMLizable, Serializable, Cloneable {

	private static final long serialVersionUID = 2489157191050437443L;
	
	private String content = "";
	
	public FormatedContent() {
		
	}

	public FormatedContent(String content) {
		this.content = content;
	}

	public String getString() {
		return content;
	}
	
	public void setString(String content) {
		this.content = content;
	}
	
	@Override
	protected FormatedContent clone() throws CloneNotSupportedException {
		return (FormatedContent)super.clone();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FormatedContent) {
			FormatedContent fcs = (FormatedContent)obj;
			return fcs.getString().equals(getString());
		}
		return false;
	}

	@Override
	public Node writeXml(Document document) throws Exception {
		Element element = document.createElement(getClass().getName());
        element.appendChild(document.createCDATASection(FileUtils.CrlfToLf(content)));
		return element;
	}

	@Override
	public void readXml(Node node) throws Exception {
//        Node cdata = XMLUtils.findChildNode(node, Node.CDATA_SECTION_NODE);
//        if (cdata != null) {
//        	this.content = cdata.getNodeValue();
//        }
        this.content = XMLUtils.readXmlText(node); // fix #310
	}

}
