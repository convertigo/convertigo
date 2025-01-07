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

package com.twinsoft.convertigo.beans.steps;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.comparator.NameFileComparator;
import org.apache.commons.io.comparator.SizeFileComparator;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.constants.Constants;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.FileSortByPolicy;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class ListDirStep extends Step {

	private static final long serialVersionUID = 369619169845666287L;
	
	/** Holds the value for the  source directory */
	private String sourceDirectory = "";

	private FileSortByPolicy fileSortByPolicy = FileSortByPolicy.fileName;
	
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
	
	public FileSortByPolicy getFileSortByPolicy() {
		return fileSortByPolicy;
	}
	
	public void setFileSortByPolicy(FileSortByPolicy fileSortByPolicy) {
		this.fileSortByPolicy = fileSortByPolicy;
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
	        
	        if (fileSortByPolicy.equals(FileSortByPolicy.fileLastModified)) {
	        	Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
	        } else if (fileSortByPolicy.equals(FileSortByPolicy.fileSize)) {
	        	Arrays.sort(files, SizeFileComparator.SIZE_COMPARATOR);
	        } else {
	        	Arrays.sort(files, NameFileComparator.NAME_SYSTEM_COMPARATOR);
	        }
	        
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
		if (isEnabled()) {
			evaluate(javascriptContext, scope, getSourceDirectory(), "sourceDirectory", true);
			if (super.stepExecute(javascriptContext, scope)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);

		XmlSchemaComplexType cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		element.setType(cType);

		XmlSchemaSequence sequence = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
		cType.setParticle(sequence);
		
		XmlSchemaElement elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		sequence.getItems().add(elt);
		elt.setName("file");
		elt.setMinOccurs(0);
		elt.setMaxOccurs(Long.MAX_VALUE);
		
		cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		elt.setType(cType);
		
		XmlSchemaSimpleContent sContent = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSimpleContent());
		cType.setContentModel(sContent);
		
		XmlSchemaSimpleContentExtension sContentExt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSimpleContentExtension());
		sContent.setContent(sContentExt);
		sContentExt.setBaseTypeName(Constants.XSD_STRING);
		
		XmlSchemaAttribute attr = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
		attr.setName("lastModified");
		attr.setSchemaTypeName(Constants.XSD_NONNEGATIVEINTEGER);
		sContentExt.getAttributes().add(attr);
		
		attr = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
		attr.setName("size");
		attr.setSchemaTypeName(Constants.XSD_NONNEGATIVEINTEGER);
		sContentExt.getAttributes().add(attr);
		
		return element;
	}
	
	@Override
	protected void onBeanNameChanged(String oldName, String newName) {
		if (oldName.startsWith(StringUtils.normalize(sourceDirectory))) {
			sourceDirectory = newName;
			hasChanged = true;
		}
	}
	
	@Override
	protected String defaultBeanName(String displayName) {
		return "dir";
	}
}
