package com.twinsoft.convertigo.beans.steps;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class GenerateHashCodeStep extends Step implements ITagsProperty {

	private static final long serialVersionUID = -2873578590344942963L;

	private static String MD5 = "MD5";
	private static String SHA1 = "SHA-1";
	
	private String sourcePath = "";
	private String hashAlgorithm = "MD5";

	public GenerateHashCodeStep() {
		super();
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		GenerateHashCodeStep clonedObject = (GenerateHashCodeStep) super.clone();
		return clonedObject;
	}

	@Override
	public Object copy() throws CloneNotSupportedException {
		GenerateHashCodeStep copiedObject = (GenerateHashCodeStep) super.copy();
		return copiedObject;
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.stepExecute(javascriptContext, scope)) {
				try {
					String sourceFilePath = getAbsoluteFilePath(evaluateToString(javascriptContext, scope, sourcePath, "sourcePath", false));

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
					if (MD5.equals(hashAlgorithm)) {
						hash = org.apache.commons.codec.digest.DigestUtils.md5Hex(path);
					} else if (SHA1.equals(hashAlgorithm)) {
						hash = org.apache.commons.codec.digest.DigestUtils.shaHex(path);
					}

					Document xmlDoc = XMLUtils.getDefaultDocumentBuilder().newDocument();
					Node hashNode = xmlDoc.createElement("hash");
					hashNode.appendChild(xmlDoc.createTextNode(hash));
					xmlDoc.appendChild(hashNode);

					if (isOutput()) {
						sequence.flushStepDocument(executeTimeID, xmlDoc);
					}
					Node rootNode = outputDocument.getDocumentElement();
					Node newChild = outputDocument.importNode(xmlDoc.getDocumentElement(), true);
					outputDocument.replaceChild(newChild, rootNode);

					Engine.logBeans.info("File \"" + sourceFilePath	+ "\" has been hashed.");
				} catch (Exception e) {
					setErrorStatus(true);
					throw new EngineException("Unable to compute hash code", e);
				}
				return true;
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
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("hashAlgorithm")) {
			return new String[] { MD5, SHA1 };
		}
		return super.getTagsForProperty(propertyName);
	}

	@Override
	protected boolean workOnSource() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected StepSource getSource() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toJsString() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getHashAlgorithm() {
		return hashAlgorithm;
	}

	public void setHashAlgorithm(String hashAlgorithm) {
		this.hashAlgorithm = hashAlgorithm;
	}
}
