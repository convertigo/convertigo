package com.twinsoft.convertigo.beans.steps;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class InputVariablesStep extends Step {

	private static final long serialVersionUID = 3276050659362959158L;
	
	protected String expression = "";
	protected String nodeName = "inputVars";
	protected String nodeText = "";
	
	transient Map<String, Object> variables = new HashMap<String, Object>(); 
	
	public InputVariablesStep() throws EngineException {
		super();
		this.output = true;
		this.xml = true;
	}
	
    public Object clone() throws CloneNotSupportedException {
    	InputVariablesStep clonedObject = (InputVariablesStep) super.clone();
        return clonedObject;
    }
	
    public Object copy() throws CloneNotSupportedException {
    	InputVariablesStep copiedObject = (InputVariablesStep) super.copy();
        return copiedObject;
    }
 
	public String toString() {
		String text = this.getComment();
		String tag = "<"+ nodeName +">";
		return tag + (!text.equals("") ? " // "+text:"");
	}
		
	protected boolean workOnSource() {
		return false;
	}

	protected StepSource getSource() {
		return null;
	}
	
	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
	
	public String getStepNodeName() {
		return getNodeName();
	}

	@Override
	public String toJsString() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	@Override
	protected boolean stepExcecute(Context javascriptContext, Scriptable scope) throws EngineException {
		variables.clear();

		if (isEnable) {
			for(RequestableVariable var : getParentSequence().getAllVariables()) {
				try {
					evaluate(javascriptContext, scope, var.getName(), "expression", true);
					if (evaluated != null && !(evaluated instanceof Undefined)) {
						
						if (evaluated instanceof NativeJavaArray) {
							NativeJavaArray jsArray = (NativeJavaArray) evaluated; 
							Object javaArray = jsArray.unwrap();
							
							if (javaArray.getClass().isArray()) {
								String[] objects = (String[]) javaArray;
								variables.put(var.getName(), objects);
							}
						}
						else {
							variables.put(var.getName(), evaluated);
						}						
					}
				}
				catch (Exception e) {
					evaluated = null;
					Engine.logBeans.warn(e.getMessage());
				}
			}
			return super.stepExcecute(javascriptContext, scope);
		}
		return false;
	}

	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		String nodeValue = nodeText;
		XMLVector<Object> nodeValues = null;
		String [] sNodevalues = null;

		Element var = null;
		Node text = null;

		for(Map.Entry<String, Object> entry : variables.entrySet()){
		   String key = entry.getKey();
		   Object value = entry.getValue();

		   if (value instanceof XMLVector) {
			   	nodeValues = GenericUtils.cast(value);
			   	
			   	for (Object object :nodeValues) {
					var = doc.createElement(key.toString());
					text = doc.createTextNode(object.toString());
					var.appendChild(text);
					stepNode.appendChild(var);
			   	}
		   }
		   else if (value instanceof String[]) {
			   sNodevalues = GenericUtils.cast(value);
			   for (Object object :sNodevalues) {
					var = doc.createElement(key.toString());
					text = doc.createTextNode(object.toString());
					var.appendChild(text);
					stepNode.appendChild(var);
			   	}	   
		   }
		   else {
			   	nodeValue = value.toString();
				var = doc.createElement(key.toString());
				text = doc.createTextNode(nodeValue);
				var.appendChild(text);
				stepNode.appendChild(var);
		   }
		}
	}	
}