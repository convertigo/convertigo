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
 * $URL:  $
 * $Author:  $
 * $Revision:  $
 * $Date:  $
 */

package com.twinsoft.convertigo.beans.core;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public abstract class Document extends DatabaseObject {

	private static final long serialVersionUID = 1291777029005995625L;
	
	transient protected String docdata = null;
	
	public Document() {
		super();
		databaseType = "Document";
	}

	public String getRenderer() {
		return "DocumentTreeObject";
	}
	
	@Override
	public Document clone() throws CloneNotSupportedException {
		Document clonedObject =  (Document) super.clone();
		return clonedObject;
	}
	
    @Override
    public void configure(Element element) throws Exception {
        super.configure(element);

		String version = element.getAttribute("version");
        
		if (version == null) {
			String s = XMLUtils.prettyPrintDOM(element);
			EngineException ee = new EngineException("Unable to find version number for the database object \"" + getName() + "\".\nXML data: " + s);
			throw ee;
		}
		
        try {
            NodeList childNodes = element.getElementsByTagName("docdata");
            int len = childNodes.getLength();
            if (len > 0) {
                Node childNode = childNodes.item(0);
                Node cdata = XMLUtils.findChildNode(childNode, Node.CDATA_SECTION_NODE);
                if (cdata != null) docdata = cdata.getNodeValue();
            }
        }
        catch(Exception e) {
            throw new EngineException("Unable to configure the data for the document \"" + getName() + "\".", e);
        }
    }
    
    @Override
    public Element toXml(org.w3c.dom.Document document) throws EngineException {
        Element element = super.toXml(document);
        
        // Storing the docdata
        try {
            Element dataElement = document.createElement("docdata");
            if (docdata != null) {
                CDATASection cDATASection = document.createCDATASection(docdata);
                dataElement.appendChild(cDATASection);
                element.appendChild(dataElement);
            }
        }
        catch(NullPointerException e) {
            // Silently ignore
        }
        
        return element;
    }
    
}
