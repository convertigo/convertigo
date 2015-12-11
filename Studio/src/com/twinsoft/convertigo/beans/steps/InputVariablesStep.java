package com.twinsoft.convertigo.beans.steps;

import java.lang.reflect.Array;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroupBase;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.ISchemaParticleGenerator;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class InputVariablesStep extends Step implements ISchemaParticleGenerator {

	private static final long serialVersionUID = 3276050659362959158L;

	private String nodeName = "inputVars";

	transient Map<String, Object> variables = new LinkedHashMap<String, Object>();

	public InputVariablesStep() throws EngineException {
		super();
		setOutput(false);
		this.xml = true;
	}

	@Override
	public InputVariablesStep clone() throws CloneNotSupportedException {
		InputVariablesStep clonedObject = (InputVariablesStep) super.clone();
		clonedObject.variables = new LinkedHashMap<String, Object>();
		return clonedObject;
	}

	@Override
	public InputVariablesStep copy() throws CloneNotSupportedException {
		InputVariablesStep copiedObject = (InputVariablesStep) super.copy();
		copiedObject.variables = new LinkedHashMap<String, Object>();
		return copiedObject;
	}

	@Override
	public String toString() {
		String text = this.getComment();
		String tag = "<" + nodeName + ">";
		return tag + (!text.equals("") ? " // " + text : "");
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
	public String toJsString() {
		return "";
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		variables.clear();

		if (isEnable()) {
			for (RequestableVariable var : getParentSequence().getAllVariables()) {
				try {
					evaluate(javascriptContext, scope, var.getName(), "expression", true);
					if (evaluated != null && !(evaluated instanceof Undefined)) {
						variables.put(var.getName(), evaluated);
					}
				} catch (Exception e) {
					evaluated = null;
					Engine.logBeans.warn(e.getMessage());
				}
			}
			return super.stepExecute(javascriptContext, scope);
		}
		return false;
	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		for (Map.Entry<String, Object> entry : variables.entrySet()) {
			createStepNodeSingleValue(doc, stepNode, entry.getKey(), entry.getValue());
		}
	}
	
	private void createStepNodeSingleValue(Document doc, Element stepNode, String key, Object value) {
		if (value instanceof NativeJavaObject) {
			value = ((NativeJavaObject) value).unwrap();
		}
		if (value instanceof XMLVector) {
			XMLVector<Object> nodeValues = GenericUtils.cast(value);
			for (Object object : nodeValues) {
				createStepNodeSingleValue(doc, stepNode, key, object);
			}
		} else if (value.getClass().isArray()) {
			int len = Array.getLength(value);
			for (int i = 0; i < len ; i++) {
				createStepNodeSingleValue(doc, stepNode, key, Array.get(value, i));
			}
		} else {
			Element var = doc.createElement(key.toString());
			stepNode.appendChild(var);

			// Structured variable
			if (value instanceof NodeList) {
				NodeList valueNodeList = (NodeList) value;
				int nlLen = valueNodeList.getLength();
				Document document = stepNode.getOwnerDocument();
				for (int i = 0; i < nlLen; i++) {
					Node nodeVarPart = valueNodeList.item(i);
					if (!nodeVarPart.getOwnerDocument().equals(document))
						nodeVarPart = document.importNode(nodeVarPart, true);
					var.appendChild(nodeVarPart);
				}
			} else {
				String nodeValue = value.toString();
				Node text = doc.createTextNode(nodeValue);
				var.appendChild(text);
			}
		}
	}

	public boolean isGenerateElement() {
		return isOutput();
	}
	
	protected XmlSchemaParticle getXmlSchemaParticle(XmlSchemaCollection collection, XmlSchema schema, XmlSchemaGroupBase group) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		XmlSchemaComplexType cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		SchemaMeta.setContainerXmlSchemaGroupBase(element, group);
		element.setType(cType);
		cType.setParticle(group);
		return element;
	}

	@Override
	public XmlSchemaParticle getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		List<RequestableVariable> variables = getParentSequence().getAllVariables();
		
		XmlSchemaSequence sequence = variables.size() > 0 ? XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence()) : null;
		
		for (RequestableVariable variable : variables) {
			XmlSchemaElement element = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
			element.setName(variable.getName());
			element.setSchemaTypeName(variable.getTypeAffectation());
			element.setMinOccurs(0);
			if (variable.isMultiValued()) {
				element.setMaxOccurs(Long.MAX_VALUE);
			}
			sequence.getItems().add(element);
		}
		
		if (sequence != null) {
			return getXmlSchemaParticle(collection, schema, sequence);
		} else {
			return (XmlSchemaParticle) super.getXmlSchemaObject(collection, schema);
		}
	}
}