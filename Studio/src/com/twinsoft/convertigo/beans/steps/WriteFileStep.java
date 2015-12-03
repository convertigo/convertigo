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
import java.util.Date;
import java.util.List;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.constants.Constants;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public abstract class WriteFileStep extends Step implements IStepSourceContainer {
	private static final long serialVersionUID = 1935459983330667718L;
	
	private XMLVector<String> sourceDefinition = new XMLVector<String>();
	private boolean appendTimestamp = false;
	private String dataFile = "";	
	private String encoding= "UTF-8";
	private boolean appendResult=false;
	private transient String filePath;
	
	public WriteFileStep() {
		super();
	}
	
	@Override
    public WriteFileStep clone() throws CloneNotSupportedException {
    	WriteFileStep clonedObject = (WriteFileStep) super.clone();
        return clonedObject;
    }
	
	@Override
    public WriteFileStep copy() throws CloneNotSupportedException {
    	WriteFileStep copiedObject = (WriteFileStep) super.copy();
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
			Engine.logBeans.warn("[WriteFileStep] The object \"" + getName()+ "\" has been updated to .m003 version");
        }
	}

	public String getDataFile() {
		return dataFile;
	}
	
	public void setDataFile(String dataFile) {
		this.dataFile = dataFile;
	}
	
	public boolean isAppendTimestamp() {
		return appendTimestamp;
	}
	
	public void setAppendTimestamp(boolean appendTimestamp) {
		this.appendTimestamp = appendTimestamp;
	}
		
	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	
	public XMLVector<String> getSourceDefinition() {
		return sourceDefinition;
	}

	public void setSourceDefinition(XMLVector<String> sourceDefinition) {
		this.sourceDefinition = sourceDefinition;
	}

	@Override
	protected String getLabel() throws EngineException {
		String label = "";
		try {
			label += (sourceDefinition.size() > 0) ? " @("+ super.getLabel()+")":" @(??)";
		} catch (EngineException e) {}
		return label;
	}

	@Override
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += getLabel();
		} catch (EngineException e) {}
		return "WriteFile" + label + (!text.equals("") ? " // "+text:"");
	}

	@Override
	public String toJsString() {
		return "";
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {

				filePath = null;
				try {
					StepSource stepSource = getSource();
					if (!stepSource.inError()) {
						filePath = evaluateDataFileName(javascriptContext, scope);
						//NodeList nl = stepSource.getContextOutputNodes();
						List<Node> nl = stepSource.getContextOutputNodes();
						writeFile(filePath, nl);
						filePath = getAbsoluteFilePath(filePath);
					}
				} catch (Exception e) {
		        	setErrorStatus(true);
		            Engine.logBeans.error("An error occured while writing to file", e);
		        }
		        return super.stepExecute(javascriptContext, scope);
			
		}
		return false;
	}
	
	//protected abstract void writeFile(String filePath, NodeList nodeList) throws EngineException;
	protected abstract void writeFile(String filePath, List<Node> nodeList) throws EngineException;
	
	private String evaluateDataFileName(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, dataFile, "dataFile", true);
	}
	
	protected String getAbsoluteFilePath(String entry) throws EngineException {
		if (entry.equals(""))
			throw new EngineException("The file name is empty");
		
		String path = entry;
			
		if (appendTimestamp) {
			java.text.SimpleDateFormat formater = new java.text.SimpleDateFormat( ".yyyymmddHHmmssSSS" );
			String date = formater.format(new Date());
			
			int id = path.lastIndexOf(".");
			if (id > path.lastIndexOf('/') || id > path.lastIndexOf('\\')) {
				path = path.substring(0, id) + date + path.substring(id);
			} else {
				path = path + date;
			}
		}
		
		return Engine.theApp.filePropertyManager.getFilepathFromProperty(path, getProject().getName());
	}

	public boolean isAppendResult() {
		return appendResult;
	}

	public void setAppendResult(boolean appendResult) {
		this.appendResult = appendResult;
	}
	
	protected boolean isReallyAppend(String fullFilePath) throws EngineException{
		return appendResult && new File(fullFilePath).exists();
	}
	
	@Override
	public String getStepNodeName() {
		return "fileInfo";
	}
	
	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		if (filePath != null) {
			String projectPath = getProject().getDirPath() + "/";
			
			filePath = filePath.replace('\\', '/');
			projectPath = projectPath.replace('\\', '/');
			
			// Relative file path
			if (filePath.startsWith(projectPath)) { 
				stepNode.setAttribute("relative-url", filePath.substring(projectPath.length()));
			}
			// Absolute file path
			if (new File(filePath).exists()) {
				stepNode.setAttribute("absolute-path", filePath);
			}
			// File-url
			if (filePath.startsWith(Engine.PROJECTS_PATH.replace('\\', '/') + "/")) {
				String fileurl = HttpUtils.convertigoRequestURL(getSequence().context.httpServletRequest);
				stepNode.setAttribute("file-url", fileurl + "/projects/" + filePath.substring(Engine.PROJECTS_PATH.length()+1));	
			}
		}
	}
	
	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		
		XmlSchemaComplexType cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		element.setType(cType);

		XmlSchemaSequence sequence = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
		cType.setParticle(sequence);
		SchemaMeta.setContainerXmlSchemaGroupBase(element, sequence);		
		
		XmlSchemaAttribute attr = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
		attr.setName("file-url");
		attr.setSchemaTypeName(Constants.XSD_STRING);
		cType.getAttributes().add(attr);
		
		attr = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
		attr.setName("absolute-path");
		attr.setSchemaTypeName(Constants.XSD_STRING);
		cType.getAttributes().add(attr);
		
		attr = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
		attr.setName("relative-path");
		attr.setSchemaTypeName(Constants.XSD_STRING);
		cType.getAttributes().add(attr);
		
		return element;
	}
}