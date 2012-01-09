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

package com.twinsoft.convertigo.beans.steps;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class ReadXMLStep extends ReadFileStep {
	
	private static final long serialVersionUID = 9145682088577678134L;

	public ReadXMLStep() {
		super();
	}

    public Object clone() throws CloneNotSupportedException {
    	ReadXMLStep clonedObject = (ReadXMLStep) super.clone();
        return clonedObject;
    }
	
    public Object copy() throws CloneNotSupportedException {
    	ReadXMLStep copiedObject = (ReadXMLStep) super.copy();
        return copiedObject;
    }			
	
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += getLabel();
		} catch (EngineException e) {
		}
		
		return "ReadXML: " + label + (!text.equals("") ? " // "+text:"");
	}
    
	protected Document read(String filePath, boolean schema) {
		return readMyXML(filePath);
	}
	
	protected Document readMyXML(String filePath) {
		Document xmlDoc = null;
		
		try {
			File xmlFile = new File(getAbsoluteFilePath(filePath));
			if (!xmlFile.exists()) {
				//throw new EngineException("The XML file \""+ dataFile +"\" does not exist.");
				Engine.logBeans.warn("(ReadXML) XML File '" + filePath + "' does not exist.");
				
				xmlDoc = XMLUtils.getDefaultDocumentBuilder().newDocument();
				xmlDoc.appendChild(xmlDoc.createElement("readxml_error"));
				Element myEl = xmlDoc.createElement("message");
				myEl.appendChild(xmlDoc.createTextNode("File '" + filePath + "' not found." ));
				xmlDoc.getDocumentElement().appendChild(myEl);
				
				//Engine.logBeans.debug("(ReadXML) XML File content '" + com.twinsoft.convertigo.engine.util.XMLUtils.prettyPrintDOM(xmlDoc) + "'", sequence.context.log);
			}
			else {
				xmlDoc = XMLUtils.parseDOM(xmlFile);
				if (Engine.logBeans.isDebugEnabled())
					Engine.logBeans.debug("(ReadXML) XML File content '" + com.twinsoft.convertigo.engine.util.XMLUtils.prettyPrintDOM(xmlDoc) + "'");
			}
		} catch (Exception e1) {
			Engine.logBeans.warn("(ReadXML) Error while trying to parse XML file : " + e1.toString());
			try {
				xmlDoc = XMLUtils.getDefaultDocumentBuilder().newDocument();
				xmlDoc.appendChild(xmlDoc.createElement("document"));
				Element myEl = xmlDoc.createElement("message");
				myEl.appendChild(xmlDoc.createTextNode("Unable to parse file '" + filePath + "'." ));
				xmlDoc.getDocumentElement().appendChild(myEl);
			}
			catch (Exception e2) {
				Engine.logBeans.warn("(ReadXML) An error occured while building error xml document: " + e1.toString());
			}
			
			//throw new EngineException("Unable to parse XML file.",e);
		}
		
		return xmlDoc;
	}

}
	
	



	