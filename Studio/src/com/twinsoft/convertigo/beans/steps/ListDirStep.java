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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.beans.steps;

import java.io.File;
import java.util.HashMap;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class ListDirStep extends Step {

	private static final long serialVersionUID = 369619169845666287L;
	
	/** Holds the value for the  source directory */
	private String sourceDirectory = "";

	public ListDirStep() {
		super();
		setOutput(false);
		xml = true;
	}

	@Override
	public ListDirStep clone() throws CloneNotSupportedException {
		ListDirStep clonedObject = (ListDirStep)super.clone();
		return clonedObject;
	}
	
	@Override
	public ListDirStep copy() throws CloneNotSupportedException {
		ListDirStep copiedObject = (ListDirStep)super.copy();
		return copiedObject;
	}
	
	public String getSourceDirectory() {
		return sourceDirectory;
	}

	public void setSourceDirectory(String sourceDirectory) {
		this.sourceDirectory = sourceDirectory;
	}

	@Override
	protected StepSource getSource() {
		return null;
	}

	@Override
	public String toJsString() {
		return "";
	}

	@Override
	public String toString() {
		return "List "+ sourceDirectory;
	}
	
	@Override
	protected boolean workOnSource() {
		return false;
	}

	@Override
	public String getStepNodeName() {
		return "directory";
	}
	
	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		try {
			// Command line string
			if (evaluated instanceof org.mozilla.javascript.Undefined)
				throw new EngineException("Source directory path is empty.");
			String path = Engine.theApp.filePropertyManager.getFilepathFromProperty(evaluated.toString(), getProject().getName());
			
	        File fDir = new File(path);
	        File[] files = fDir.listFiles();
	        if (files == null)
	        	throw new EngineException("Source path \""+path+"\" isn't a directory.");
	        
	        String fileName, fileLastModified, fileSize;
	        Element element;
	        File file;
	        for (int i = 0 ; i < files.length ; i++) {
	            file = files[i];
	            if (file.isFile() && !file.isHidden()) {
	            	fileName = file.getName();
	            	fileSize = String.valueOf(file.length());
	            	fileLastModified = String.valueOf(file.lastModified());
	            	
	            	element = doc.createElement("file");
	            	element.appendChild(doc.createTextNode(fileName));
	            	element.setAttribute("lastModified", fileLastModified);
	            	element.setAttribute("size", fileSize);
	            	stepNode.appendChild(element);
	            }
	        }
			
		}
		catch (Exception e) {
			setErrorStatus(true);
			Engine.logBeans.warn("An error occured while listing directory.", e);
		}
	}
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			evaluate(javascriptContext, scope, getSourceDirectory(), "sourceDirectory", true);
			if (super.stepExecute(javascriptContext, scope)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getSchemaType(String tns) {
		return tns +":"+ getStepNodeName() + priority +"StepType";
	}
	
	@Override
	public void addSchemaType(HashMap<Long, String> stepTypes, String tns, String occurs) throws EngineException {
		String stepTypeSchema = "";
		stepTypeSchema += "\t<xsd:complexType name=\""+ getSchemaTypeName(tns) +"\">\n";
		stepTypeSchema += "\t\t<xsd:sequence>\n";
		stepTypeSchema += "\t\t\t<xsd:element minOccurs=\"0\" maxOccurs=\"unbounded\" name=\"file\" type=\"xsd:string\" >\n";
		stepTypeSchema += "\t\t\t\t<xsd:complexType>\n";
		stepTypeSchema += "\t\t\t\t\t<xsd:simpleContent>\n";
		stepTypeSchema += "\t\t\t\t\t\t<xsd:extension base=\"xsd:string\">\n";
		stepTypeSchema += "\t\t\t\t\t\t\t<xsd:attribute name=\"lastModified\" type=\"xsd:long\" />\n";
		stepTypeSchema += "\t\t\t\t\t\t\t<xsd:attribute name=\"size\" type=\"xsd:long\" />\n";
		stepTypeSchema += "\t\t\t\t\t\t</xsd:extension>\n";
		stepTypeSchema += "\t\t\t\t\t</xsd:simpleContent>\n";
		stepTypeSchema += "\t\t\t\t</xsd:complexType>\n";
		stepTypeSchema += "\t\t\t</xsd:element>\n";
		stepTypeSchema += "\t\t</xsd:sequence>\n";
		stepTypeSchema += "\t</xsd:complexType>\n";
		
		stepTypes.put(new Long(priority), stepTypeSchema);
	}
	
	@Override
	protected Node createWsdlDom() throws EngineException {
		Element element = (Element) super.createWsdlDom();
		Element file = wsdlDom.createElement("file");
		file.setAttribute("lastModified", "");
		file.setAttribute("size", "");
		element.appendChild(file);
		return element;
	}
}
