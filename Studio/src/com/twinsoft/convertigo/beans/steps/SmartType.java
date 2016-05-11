package com.twinsoft.convertigo.beans.steps;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.common.XMLizable;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.ParameterUtils;

public class SmartType implements XMLizable, Serializable, Cloneable {
	private static final long serialVersionUID = 6063228569094166129L;
	
	public enum Mode {
		PLAIN("TX", "Plain text", ""),
		JS("JS", "JavaScript expression", "="),
		SOURCE("SC", "Source definition", "@");
		
		String label;
		String tooltip;
		String prefix;
		
		Mode(String label, String tooltip, String prefix) {
			this.label = label;
			this.tooltip = tooltip;
			this.prefix = prefix;
		}
		
		public String label() {
			return label;
		}
		
		public String tooltip() {
			return tooltip;
		}
		
		public String prefix() {
			return prefix;
		}
	}
	
	private Mode mode = Mode.PLAIN;

	private XMLVector<String> sourceDefinition = new XMLVector<String>();
	private String expression = "";
	transient Object evaluated = null;

	public void readXml(Node node) throws Exception {
		try {
			Element self = (Element) node;
			mode = Mode.valueOf(self.getAttribute("mode"));
			if (isUseExpression()) {
				expression = self.getTextContent();
			} else {
				sourceDefinition.readXml(XPathAPI.selectSingleNode(self, "*"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Node writeXml(Document document) throws Exception {
		Element self = document.createElement(getClass().getSimpleName());
		self.setAttribute("mode", mode.name());
		if (isUseExpression()) {
			self.setTextContent(expression);
		} else {
			self.appendChild(sourceDefinition.writeXml(document));
		}
		return self;
	}
	
	public String getSingleString(Step owner) throws EngineException {
		String result = null;
		
		if (isUseExpression() && evaluated != null) {
			result = ParameterUtils.toString(evaluated);
		} else if (isUseSource()) {
			NodeList nodeList = new StepSource(owner, sourceDefinition).getContextValues();
			if (nodeList != null && nodeList.getLength() > 0) {
				Node node = nodeList.item(0);
				result = node instanceof Element ? ((Element) node).getTextContent() : node.getNodeValue();
			}
		}
		
		return result;
	}
	
	public List<String> getStringList(Step owner) throws EngineException {
		List<String> result;
		
		if (isUseExpression() && evaluated != null) {
			result = ParameterUtils.toStringList(evaluated);
		} else if (isUseSource()) {
			NodeList nodeList = new StepSource(owner, sourceDefinition).getContextValues();
			int len = nodeList == null ? 0 : nodeList.getLength();
			result = new ArrayList<String>(len);
			for (int i = 0; i < len; i++) {
				Node node = nodeList.item(i);
				result.add(node instanceof Element ? ((Element) node).getTextContent() : node.getNodeValue());
			}
		} else {
			result = Collections.emptyList();
		}
		
		return result;
	}
	
	public String[] getStringArray(Step owner) throws EngineException {
		List<String> list = getStringList(owner);
		return list.toArray(new String[list.size()]);
	}
	
	public Mode getMode() {
		return mode;
	}
	
	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public XMLVector<String> getSourceDefinition() {
		return sourceDefinition;
	}

	public void setSourceDefinition(XMLVector<String> sourceDefinition) {
		this.sourceDefinition = sourceDefinition;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}
	
	@Override
	public String toString() {
		return mode.prefix() + toStringContent();
	}
	
	public String toStringContent() {
		return mode == Mode.SOURCE ? sourceDefinition.toString() : expression.toString();
	}
	
	public String toString(Step owner) {
		return mode.prefix() + toStringContent(owner);
	}
	
	public String toStringContent(Step owner) {
		if (mode == Mode.SOURCE) {
			StepSource source = new StepSource(owner, sourceDefinition);
			try {
				return "(" + source.getLabel() + ")";
			} catch (EngineException e) {
				
			}
		}
		return toStringContent();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SmartType) {
			SmartType smartType = (SmartType) obj;
			return expression.equals(smartType.expression) &&
					mode.equals(smartType.mode) &&
					sourceDefinition.equals(smartType.sourceDefinition);
		}
		return false;
	}

	@Override
	public SmartType clone() {
		try {
			return (SmartType) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	public void pack() {
		if (mode == Mode.SOURCE) {
			expression = "";
		} else {
			sourceDefinition.clear();
		}
	}
	
	public boolean isUseExpression() {
		return mode == Mode.JS || mode == Mode.PLAIN;
	}
	
	public boolean isUseSource() {
		return mode == Mode.SOURCE;
	}
	
	public Object getEvaluated() {
		return evaluated;
	}
	
	public void setEvaluated(Object evaluated) {
		this.evaluated = evaluated;
	}
}
