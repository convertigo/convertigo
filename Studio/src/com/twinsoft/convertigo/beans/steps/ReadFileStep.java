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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public abstract class ReadFileStep extends Step {
	private static final long serialVersionUID = 6887234233606563336L;
	
	private static final Pattern removeQuote = Pattern.compile("^('|\")(.*)\\1$");
	
	private String dataFile = "";			
	
	public ReadFileStep() {
		super();
		xml = true;
	}

	@Override
    public ReadFileStep clone() throws CloneNotSupportedException {
    	ReadFileStep clonedObject = (ReadFileStep) super.clone();
        return clonedObject;
    }
	
	@Override
    public ReadFileStep copy() throws CloneNotSupportedException {
    	ReadFileStep copiedObject = (ReadFileStep) super.copy();	    	
        return copiedObject;
    }
	
	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
        String version = element.getAttribute("version");
        
        if (version == null) {
            String s = XMLUtils.prettyPrintDOM(element);
            EngineException ee = new EngineException(
                "Unable to find version number for the database object \"" + getName() + "\".\n" +
                "XML data: " + s
            );
            throw ee;
        }
        
        if (VersionUtils.compareMigrationVersion(version, ".m003") < 0) {
        	if (!dataFile.equals("")) dataFile = "'" + dataFile + "'";
			hasChanged = true;
			Engine.logBeans.warn("[ReadFileStep] The object \"" + getName()+ "\" has been updated to .m003 version");
        }
	}

	public String getDataFile() {						
		return dataFile;			
	}

	public void setDataFile(String dataFile) {
		this.dataFile = dataFile;
	}
	
	@Override
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += getLabel();
		} catch (EngineException e) {
		}
		
		return "ReadFile:" + label + (!text.equals("") ? " // "+text:"");
	}
	
	@Override
	protected String getSpecificLabel() throws EngineException {
		String label = getDataFileName();
		return label.equals("") ? "??":label;
	}

	public String toJsString() {
		return "";
	}

	protected boolean workOnSource() {
		return false;
	}

	protected StepSource getSource() {
		return null;
	}

	@Override
    public Document getWsdlDom() throws EngineException {
    	if (wsdlDomDirty || wsdlDom == null) {
    		generateWsdlDom();
    	}
    	return wsdlDom;
    }
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.stepExecute(javascriptContext, scope)) {
				try {
					String filePath = evaluateDataFileName(javascriptContext, scope);
					Document xmlDoc = read(filePath, false);
					flushDocument(xmlDoc);
					
		        } catch (Exception e) {
		        	setErrorStatus(true);
		            Engine.logBeans.error("An error occured while reading from file", e);
		        }
		        return true;
			}
		}
		return false;
	}
	
	private void flushDocument(Document xmlDoc) {
		if (sequence.runningThread.bContinue) {
			if (isOutput()) sequence.flushStepDocument(executeTimeID, xmlDoc);
			Node rootNode = outputDocument.getDocumentElement();
			Node stepNode = rootNode.getFirstChild();
			Node newChild = outputDocument.importNode(xmlDoc.getDocumentElement(), true);
			stepNode.appendChild(newChild);
		}
	}
	
	protected abstract Document read(String filePath, boolean schema) throws EngineException;
	
	@Override
	protected Node generateWsdlDom() throws EngineException {
		try {
			String filePath = evaluateDataFileName(null, null);
			Document schemaDoc = read(filePath, true);
			Element schemaRoot = schemaDoc.getDocumentElement();
			
			wsdlDomDirty = true;
			Element wsdlRoot = (Element) super.generateWsdlDom();
			wsdlDom = wsdlRoot.getOwnerDocument();
			Element newRoot = (Element) wsdlDom.importNode(schemaRoot, true);
			wsdlRoot.appendChild(newRoot);
			
			wsdlDomDirty = false;
			return wsdlDom.getDocumentElement();
		}
		catch (Exception e) {
    		wsdlDom = null;
    		throw new EngineException("Unable to generate WSDL document",e);
		}
	}
	
	private String evaluateDataFileName(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, dataFile, "dataFile", true);
	}
	
	protected String getDataFileName() {
		String fileName = dataFile;
		int index = dataFile.lastIndexOf("/");
		if (index != -1) {
			fileName = dataFile.substring(index+1);
		}
		return fileName;
	}
	
	protected String getAbsoluteFilePath(String entry) throws EngineException {
		if (entry.equals("")) {
			throw new EngineException("The file name is empty");
		}
				
		return Engine.theApp.filePropertyManager.getFilepathFromProperty(entry, getProject().getName());
	}
	
	protected File getFile() {
		Matcher matcher = removeQuote.matcher(dataFile);
		if (matcher.matches()) {
			String filePath = matcher.group(2);
			filePath = Engine.theApp.filePropertyManager.getFilepathFromProperty(filePath, getProject().getName());
			return new File(filePath);
		}
		return null;
	}

}
