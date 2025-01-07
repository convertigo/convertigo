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
import java.io.FileInputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.constants.Constants;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.ISchemaParticleGenerator;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class GenerateHashCodeStep extends Step implements ISchemaParticleGenerator {

	private static final long serialVersionUID = -2873578590344942963L;

	public enum HashAlgorithm {
		MD5("MD5"),
		SHA1("SHA-1");
		
		private final String label;
		
		private HashAlgorithm(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}
	
	private String sourcePath = "";
	private HashAlgorithm hashAlgorithm = HashAlgorithm.MD5;

	private transient String sourceFilePath = "";
	
	private String nodeName = getName();
	
	private String offset = "0";
	
	private transient long currentOffset = 0;

		
	public GenerateHashCodeStep() {
		super();
		this.xml = true;
	}

	@Override
	public GenerateHashCodeStep clone() throws CloneNotSupportedException {
		GenerateHashCodeStep clonedObject = (GenerateHashCodeStep) super.clone();
		clonedObject.sourceFilePath = "";
		return clonedObject;
	}

	@Override
	public GenerateHashCodeStep copy() throws CloneNotSupportedException {
		GenerateHashCodeStep copiedObject = (GenerateHashCodeStep) super.copy();
		copiedObject.sourceFilePath = "";
		return copiedObject;
	}

	@Override
	public String toString() {
		return "<" + nodeName + ">" + getName();
	}
	
	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		if (isEnabled()) {
			try {
				Engine.logBeans.info("Hashing file \"" + sourceFilePath + "\"");

				File sourceFile = new File(sourceFilePath);
				if (!sourceFile.exists()) {
					throw new EngineException("Source file does not exist: " + sourceFilePath);
				}

				if (!sourceFile.isFile()) {
					throw new EngineException("Source file is not a file: " + sourceFilePath);
				}
				
				if (currentOffset > sourceFile.length()) {
					throw new EngineException("The currentOffset of " + currentOffset + "(/" + sourceFile.length() + ") for the file: " + sourceFilePath);
				}
				
				try (FileInputStream fis = new FileInputStream(sourceFile)) {
					String hash = null;

					if (currentOffset > 0) {
						Engine.logBeans.debug("Skipping " + currentOffset + " bytes of the file \"" + sourceFilePath	+ "\" before hash.");
						fis.skip(currentOffset);
					}
					
					switch (hashAlgorithm) {
					case MD5:
						hash = DigestUtils.md5Hex(fis); break;
					case SHA1:
						hash = DigestUtils.sha1Hex(fis); break;
					}
					
					Engine.logBeans.info("File \"" + sourceFilePath	+ "\" has been hashed.");

					Node text = doc.createTextNode(hash);
					stepNode.appendChild(text);
				}
			} catch (Exception e) {
				setErrorStatus(true);
				throw new EngineException("Unable to compute hash code", e);
			}
		}
	}
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			try {
				sourceFilePath = getAbsoluteFilePath(evaluateToString(javascriptContext, scope, sourcePath, "sourcePath", false));

				Engine.logBeans.info("Hashing file \"" + sourceFilePath + "\"");

				File sourceFile = new File(sourceFilePath); 
				if (!sourceFile.exists()) {
					throw new EngineException("Source file does not exist: " + sourceFilePath);
				}

				if (!sourceFile.isFile()) {
					throw new EngineException("Source file is not a file: " + sourceFilePath);
				}
				
				currentOffset = Math.max(evaluateToLong(javascriptContext, scope, offset, "offset", false), 0);
				
				if (super.stepExecute(javascriptContext, scope)) {
					return true;
				}
			} catch (EngineException e) {
				throw e;
			} catch (Exception e) {
				setErrorStatus(true);
				throw new EngineException("Unable to compute hash code", e);
			}
		}
		return false;
	}

	protected String getAbsoluteFilePath(String entry) throws EngineException {
		if (entry.equals("")) {
			throw new EngineException("Please fill the Source property field.");
		}

		return Engine.theApp.filePropertyManager.getFilepathFromProperty(entry,	getProject().getName());
	}

	@Override
	public String toJsString() {
		return null;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public HashAlgorithm getHashAlgorithm() {
		return hashAlgorithm;
	}

	public void setHashAlgorithm(HashAlgorithm hashAlgorithm) {
		this.hashAlgorithm = hashAlgorithm;
	}
	
	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	@Override
	public String getStepNodeName() {
		return getNodeName();
	}
	
	public String getOffset() {
		return offset;
	}
	
	public void setOffset(String offset) {
		this.offset = offset;
	}
	
	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		element.setSchemaTypeName(Constants.XSD_STRING);
		return element;
	}
	
	public boolean isGenerateElement() {
		return true;
	}
	
	@Override
	protected void onBeanNameChanged(String oldName, String newName) {
		if (oldName.startsWith(nodeName)) {
			nodeName = StringUtils.normalize(newName);
			hasChanged = true;
		}
	}
	
	@Override
	protected String defaultBeanName(String displayName) {
		return "hash";
	}
}
