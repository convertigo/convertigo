package com.twinsoft.convertigo.beans.steps;

import java.util.List;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.variables.RequestableMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class InputVariablesStep extends Step {

	private static final long serialVersionUID = 3276050659362959158L;
	
	protected String expression = "";
	protected String nodeName = "inputVars";
	protected String nodeText = "";
	protected Sequence parentSequence = null;
	
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
	
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		String nodeValue = nodeText;
		Vector<String> nodeValues = null;
		
		Sequence parentSequence = this.getParentSequence();
		List<DatabaseObject> childrens = parentSequence.getAllChildren();
		
		Element var = null;
		Node text = null;

		for (DatabaseObject child: childrens) {
			if (child instanceof Variable) {
				if (child instanceof RequestableMultiValuedVariable) {
					nodeValues = GenericUtils.cast(((Variable) child).getValueOrNull());
					for (String value: nodeValues) {
						var = doc.createElement(child.getName());
						text = doc.createTextNode(value);
						var.appendChild(text);
						stepNode.appendChild(var);
					}
				}
				else if (child instanceof RequestableVariable) {
					nodeValue = ((Variable) child).getValueOrNull().toString();
					var = doc.createElement(child.getName());
					text = doc.createTextNode(nodeValue);
					var.appendChild(text);
					stepNode.appendChild(var);
				}
			}
		}
	}
}
