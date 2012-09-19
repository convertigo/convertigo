package com.twinsoft.convertigo.beans.steps;

import java.io.File;
import java.util.HashMap;

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

import com.twinsoft.convertigo.beans.core.ISchemaElementGenerator;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class GenerateHashCodeStep extends Step implements ITagsProperty, ISchemaElementGenerator {

	private static final long serialVersionUID = -2873578590344942963L;

	private static String MD5 = "MD5";
	private static String SHA1 = "SHA-1";
	
	private String sourcePath = "";
	private String hashAlgorithm = "MD5";

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
		String text = this.getComment();
		return "<"+ nodeName +">" + getName()+ (!text.equals("") ? " // "+text:"");
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
				if (MD5.equals(hashAlgorithm)) {
					hash = org.apache.commons.codec.digest.DigestUtils.md5Hex(path);
				} else if (SHA1.equals(hashAlgorithm)) {
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
	
//	@Override
//	protected Node createWsdlDom() throws EngineException {
//		Element element = (Element) super.createWsdlDom();
//		element.appendChild(wsdlDom.createElement("hash"));
//		return element;
//	}
	
	@Override
	public String getSchemaType(String tns) {
		return tns +":"+ getStepNodeName() + priority +"StepType";
	}
	
	@Override
	public String getSchema(String tns, String occurs) throws EngineException {
		schema = "";
		String maxOccurs = (occurs == null) ? "":"maxOccurs=\""+occurs+"\"";
		schema += "\t\t\t<xsd:element minOccurs=\"0\" "+maxOccurs+" name=\""+ getStepNodeName()+"\" type=\""+ getSchemaType(tns) +"\">\n";
		schema += "\t\t\t\t<xsd:annotation>\n";
		schema += "\t\t\t\t\t<xsd:documentation>"+ XMLUtils.getCDataXml(getComment()) +"</xsd:documentation>\n";
		schema += "\t\t\t\t</xsd:annotation>\n";
		schema += "\t\t\t</xsd:element>\n";
		
		return isEnable() && isOutput() ? schema:"";
	}

	@Override
	public void addSchemaType(HashMap<Long, String> stepTypes, String tns, String occurs) throws EngineException {
		String stepTypeSchema = "";
		stepTypeSchema += "\t<xsd:complexType name=\""+ getSchemaTypeName(tns) +"\">\n";
		stepTypeSchema += "\t\t<xsd:sequence>\n";
		stepTypeSchema += "\t\t\t\t<xsd:element minOccurs=\"0\" name=\"hash\" type=\"xsd:string\"/>\n";
		stepTypeSchema += "\t\t</xsd:sequence>\n";
		stepTypeSchema += "\t</xsd:complexType>\n";
		
		stepTypes.put(new Long(priority), stepTypeSchema);
	}
	
	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		element.setSchemaTypeName(Constants.XSD_STRING);
		return element;
	}

	public boolean isGenerateSchema() {
		return isOutput();
	}
}
