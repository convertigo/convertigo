package com.twinsoft.convertigo.beans.steps;

import java.io.File;

import org.apache.commons.io.FileUtils;
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
	
	private String nodeName = "hash";

		
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
		if (isEnable()) {
			try {
				Engine.logBeans.info("Hashing file \"" + sourceFilePath + "\"");

				File sourceFile = new File(sourceFilePath);
				if (!sourceFile.exists()) {
					throw new EngineException("Source file does not exist: " + sourceFilePath);
				}

				if (!sourceFile.isFile()) {
					throw new EngineException("Source file is not a file: " + sourceFilePath);
				}

				byte[] path = null;
				path = FileUtils.readFileToByteArray(sourceFile);
				
				String hash = null;
				if (hashAlgorithm == HashAlgorithm.MD5) {
					hash = org.apache.commons.codec.digest.DigestUtils.md5Hex(path);
				} else if (hashAlgorithm == HashAlgorithm.SHA1) {
					hash = org.apache.commons.codec.digest.DigestUtils.shaHex(path);
				}
				Engine.logBeans.info("File \"" + sourceFilePath	+ "\" has been hashed.");
				
//				Node hashNode = doc.createElement("hash");
//				hashNode.appendChild(doc.createTextNode(hash));
//				stepNode.appendChild(hashNode);
				
				Node text = doc.createTextNode(hash);
				stepNode.appendChild(text);
				
			} catch (Exception e) {
				setErrorStatus(true);
				throw new EngineException("Unable to compute hash code", e);
			}
		}
	}
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
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
				
				if (super.stepExecute(javascriptContext, scope)) {
					return true;
				}
				
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
	
	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		element.setSchemaTypeName(Constants.XSD_STRING);
		return element;
	}
	
	public boolean isGenerateElement() {
		return true;
	}
}
