/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it under the terms of the GNU Affero General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 */

package com.twinsoft.convertigo.engine.studio;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.IteratorStep;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.SmartType;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.SchemaManager.Option;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.CachedIntrospector.Property;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class SourcePickerModel {
	private static final String REGEXP_FOR_PREDICATES = "\\[\\D{1,}\\]";

	public static class SourceModel {
		public String ownerId = "";
		public String sourceId = "";
		public String sourceName = "";
		public String sourcePriority = "";
		public String schemaSourceId = "";
		public String schemaSourceName = "";
		public String anchor = "";
		public String xpath = ".";
		public String displayXpath = "";
		public boolean available = false;
		public String message = "";
		public NodeModel tree = null;
		public NodeModel result = null;
		public NodeModel jsonTree = null;
		public NodeModel jsonResult = null;
	}

	public static class NodeModel {
		public String type = "";
		public String label = "";
		public String name = "";
		public String value = "";
		public String xpath = "";
		public String displayXpath = "";
		public List<NodeModel> children = new ArrayList<>();
	}

	private static class SourceSelection {
		private Step step = null;
		private String sourcePriority = "";
		private String xpath = ".";
	}

	public static SourceModel get(String ownerId, String sourcePriority, String xpath) throws Exception {
		SourceModel model = makeModel(ownerId, sourcePriority, xpath);
		if (model.available) {
			evaluate(model, model.displayXpath);
		}
		return model;
	}

	public static SourceModel evaluate(String ownerId, String sourcePriority, String xpath) throws Exception {
		SourceModel model = makeModel(ownerId, sourcePriority, xpath);
		if (model.available) {
			evaluate(model, model.displayXpath);
		}
		return model;
	}

	private static SourceModel makeModel(String ownerId, String sourcePriority, String xpath) throws Exception {
		SourceModel model = new SourceModel();
		model.ownerId = ownerId == null ? "" : ownerId;
		model.xpath = normalizeXPath(xpath);

		Step ownerStep = findOwnerStep(ownerId);
		if (ownerStep == null) {
			model.message = "No step selected";
			return model;
		}

		SourceSelection selection = resolveSourceSelection(ownerStep, sourcePriority, xpath);
		Step sourceStep = selection.step;
		if (sourceStep == null) {
			model.message = "Source step not found";
			return model;
		}

		model.sourceId = sourceStep.getFullQName();
		model.sourceName = sourceStep.getName();
		model.sourcePriority = selection.sourcePriority.isBlank() ? Long.toString(sourceStep.priority)
				: selection.sourcePriority;
		model.xpath = normalizeXPath(selection.xpath);

		Step schemaStep = sourceStep;
		while (schemaStep instanceof IteratorStep) {
			Step targetStep = getTargetStep(schemaStep);
			if (targetStep == null) {
				break;
			}
			schemaStep = targetStep;
		}
		model.schemaSourceId = schemaStep.getFullQName();
		model.schemaSourceName = schemaStep.getName();

		Document dom = buildDom(sourceStep, schemaStep);
		if (dom == null) {
			model.message = "No schema DOM available for this source";
			return model;
		}

		model.anchor = sourceStep.getAnchor();
		model.displayXpath = toDisplayXPath(model.anchor, model.xpath);
		model.xpath = toRelativeXPath(dom, model.anchor, model.displayXpath);
		model.displayXpath = toDisplayXPath(model.anchor, model.xpath);
		model.available = true;
		model.tree = toNodeModel(dom.getDocumentElement(), model.anchor);
		model.jsonTree = toJsonNodeModel(dom.getDocumentElement());
		return model;
	}

	private static String normalizeXPath(String xpath) {
		if (xpath == null || xpath.isBlank()) {
			return ".";
		}
		return xpath.trim();
	}

	private static Step findOwnerStep(String ownerId) throws Exception {
		DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(ownerId);
		if (dbo instanceof Step step) {
			return step;
		}
		if (dbo != null && dbo.getParent() instanceof Step step) {
			return step;
		}
		return null;
	}

	private static Step findSourceStep(Step ownerStep, String sourcePriority) {
		if (sourcePriority != null && !sourcePriority.isBlank()) {
			try {
				Step step = ownerStep.getSequence().loadedSteps.get(Long.valueOf(sourcePriority));
				if (step != null) {
					return step;
				}
			} catch (Exception e) {
				// Fall back to the owner step below.
			}
		}
		return ownerStep;
	}

	private static SourceSelection resolveSourceSelection(Step ownerStep, String sourcePriority, String xpath)
			throws Exception {
		SourceSelection selection = new SourceSelection();
		selection.sourcePriority = sourcePriority == null ? "" : sourcePriority.trim();
		selection.xpath = normalizeXPath(xpath);

		if (selection.sourcePriority.isBlank()) {
			XMLVector<String> sourceDefinition = findExistingSourceDefinition(ownerStep);
			if (sourceDefinition != null && !sourceDefinition.isEmpty()) {
				selection.sourcePriority = sourceDefinition.get(0);
				if (xpath == null || xpath.isBlank()) {
					selection.xpath = normalizeXPath(sourceDefinition.size() > 1 ? sourceDefinition.get(1) : ".");
				}
			}
		}

		selection.step = findSourceStep(ownerStep, selection.sourcePriority);
		return selection;
	}

	private static XMLVector<String> findExistingSourceDefinition(Step step) throws Exception {
		for (PropertyDescriptor descriptor : CachedIntrospector.getPropertyDescriptors(step, Property.smartType)) {
			Method getter = descriptor.getReadMethod();
			if (getter == null || descriptor.isHidden()) {
				continue;
			}
			Object value = getter.invoke(step);
			if (value instanceof SmartType smartType && smartType.isUseSource()) {
				XMLVector<String> sourceDefinition = smartType.getSourceDefinition();
				if (hasSourcePriority(sourceDefinition)) {
					return sourceDefinition;
				}
			}
		}

		for (PropertyDescriptor descriptor : CachedIntrospector.getPropertyDescriptors(step,
				Property.sourceDefinition)) {
			Method getter = descriptor.getReadMethod();
			if (getter == null || descriptor.isHidden()) {
				continue;
			}
			XMLVector<String> sourceDefinition = copySourceDefinition(getter.invoke(step));
			if (hasSourcePriority(sourceDefinition)) {
				return sourceDefinition;
			}
		}

		for (PropertyDescriptor descriptor : CachedIntrospector.getPropertyDescriptors(step,
				Property.sourcesDefinition)) {
			Method getter = descriptor.getReadMethod();
			if (getter == null || descriptor.isHidden()) {
				continue;
			}
			if (getter.invoke(step) instanceof XMLVector<?> rows) {
				for (Object row : rows) {
					if (row instanceof XMLVector<?> sourceRow && sourceRow.size() > 1) {
						XMLVector<String> sourceDefinition = copySourceDefinition(sourceRow.get(1));
						if (hasSourcePriority(sourceDefinition)) {
							return sourceDefinition;
						}
					}
				}
			}
		}

		return null;
	}

	private static boolean hasSourcePriority(XMLVector<String> sourceDefinition) {
		return sourceDefinition != null && !sourceDefinition.isEmpty()
				&& sourceDefinition.get(0) != null && !sourceDefinition.get(0).isBlank();
	}

	private static XMLVector<String> copySourceDefinition(Object value) {
		XMLVector<String> copy = new XMLVector<>();
		if (value instanceof XMLVector<?> sourceDefinition) {
			for (Object part : sourceDefinition) {
				copy.add(part == null ? "" : String.valueOf(part));
			}
		}
		return copy;
	}

	private static Step getTargetStep(Step step) throws EngineException {
		if (step instanceof IStepSourceContainer sourceContainer) {
			com.twinsoft.convertigo.beans.core.StepSource source = new com.twinsoft.convertigo.beans.core.StepSource(step,
					sourceContainer.getSourceDefinition());
			return source.isEmpty() ? null : source.getStep();
		}
		return step;
	}

	private static Document buildDom(Step sourceStep, Step schemaStep) throws Exception {
		XmlSchemaObject xso = getSchemaObject(sourceStep.getProject(), schemaStep);
		if (xso == null) {
			return null;
		}
		Document stepDoc = XmlSchemaUtils.getDomInstance(xso);
		if (stepDoc == null) {
			return null;
		}
		boolean shouldDisplayDom = !(!sourceStep.isXml() && sourceStep instanceof StepWithExpressions
				&& !(sourceStep instanceof IteratorStep));
		if (!shouldDisplayDom) {
			return null;
		}
		Document doc = sourceStep.getSequence().createDOM();
		Element root = (Element) doc.importNode(stepDoc.getDocumentElement(), true);
		doc.replaceChild(root, doc.getDocumentElement());
		removeUserDefinedNodes(doc.getDocumentElement());
		return doc;
	}

	private static XmlSchemaObject getSchemaObject(Project project, Step step) throws Exception {
		String projectName = project.getName();
		if (step instanceof SequenceStep) {
			XmlSchema schema = Engine.theApp.schemaManager.getSchemaForProject(projectName);
			XmlSchemaCollection collection = SchemaMeta.getCollection(schema);
			XmlSchemaObject xso = step.getXmlSchemaObject(collection, schema);
			if (xso != null) {
				SchemaMeta.setSchema(xso, schema);
			}
			return xso;
		}
		XmlSchema schema = Engine.theApp.schemaManager.getSchemaForProject(projectName, Option.fullSchema);
		return SchemaMeta.getXmlSchemaObject(schema, step);
	}

	private static void removeUserDefinedNodes(Element parent) {
		List<Node> toRemove = new ArrayList<>();
		NodeList list = parent.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				((Element) node).removeAttribute("done");
				((Element) node).removeAttribute("hashcode");
				if ("schema-type".equals(node.getNodeName())) {
					toRemove.add(node);
				} else {
					removeUserDefinedNodes((Element) node);
				}
			}
		}
		for (Node node : toRemove) {
			parent.removeChild(node);
		}
	}

	private static void evaluate(SourceModel model, String xpath) throws Exception {
		Document dom = buildDom(findSourceStep(findOwnerStep(model.ownerId), model.sourcePriority),
				findSchemaStep(model.ownerId, model.sourcePriority));
		if (dom == null) {
			return;
		}
		String displayXpath = toDisplayXPath(model.anchor, xpath);
		model.xpath = toRelativeXPath(dom, model.anchor, displayXpath);
		model.displayXpath = toDisplayXPath(model.anchor, model.xpath);

		Document resultDocument = XMLUtils.getDefaultDocumentBuilder().newDocument();
		Element root = resultDocument.createElement("root");
		resultDocument.appendChild(root);

		TwsCachedXPathAPI xpathApi = new TwsCachedXPathAPI();
		NodeList nodes = xpathApi.selectNodeList(dom, displayXpath);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = resultDocument.importNode(nodes.item(i), true);
			if (node.getNodeType() == Node.ELEMENT_NODE || node.getNodeType() == Node.TEXT_NODE) {
				root.appendChild(node);
			} else if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
				root.setAttribute(node.getNodeName() + "_" + i, node.getNodeValue());
			}
		}
		model.result = toNodeModel(root, "");
		model.jsonResult = toJsonNodeModel(root);
	}

	private static Step findSchemaStep(String ownerId, String sourcePriority) throws Exception {
		Step sourceStep = findSourceStep(findOwnerStep(ownerId), sourcePriority);
		Step schemaStep = sourceStep;
		while (schemaStep instanceof IteratorStep) {
			Step targetStep = getTargetStep(schemaStep);
			if (targetStep == null) {
				break;
			}
			schemaStep = targetStep;
		}
		return schemaStep;
	}

	private static NodeModel toNodeModel(Node node, String anchor) {
		NodeModel model = new NodeModel();
		model.type = nodeType(node);
		model.name = nodeName(node);
		model.label = nodeLabel(node);
		model.value = nodeValue(node);
		if (node.getNodeType() != Node.DOCUMENT_NODE) {
			model.xpath = XMLUtils.calcXpath(node);
			model.displayXpath = absoluteXPath(model.xpath);
			if (anchor != null && !anchor.isBlank() && model.displayXpath.startsWith(anchor)) {
				model.displayXpath = anchor + model.displayXpath.substring(anchor.length());
			}
		}

		if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
			return model;
		}

		if (node.hasAttributes()) {
			NodeModel attrs = new NodeModel();
			attrs.type = "attributes";
			attrs.name = "Attributes";
			attrs.label = "Attributes";
			NamedNodeMap map = node.getAttributes();
			for (int i = 0; i < map.getLength(); i++) {
				attrs.children.add(toNodeModel(map.item(i), anchor));
			}
			model.children.add(attrs);
		}

		NodeList children = node.getChildNodes();
		boolean hasElementChild = false;
		boolean hasTextChild = false;
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.TEXT_NODE && child.getNodeValue().trim().isEmpty()) {
				continue;
			}
			hasElementChild |= child.getNodeType() == Node.ELEMENT_NODE;
			hasTextChild |= child.getNodeType() == Node.TEXT_NODE;
			model.children.add(toNodeModel(child, anchor));
		}
		if (node.getNodeType() == Node.ELEMENT_NODE && !hasElementChild && !hasTextChild) {
			model.children.add(toTextNodeModel(model));
		}
		return model;
	}

	private static NodeModel toTextNodeModel(NodeModel parent) {
		NodeModel model = new NodeModel();
		model.type = "text";
		model.name = "text()";
		model.label = "TxT";
		model.xpath = parent.xpath == null || parent.xpath.isBlank() ? "text()" : parent.xpath + "/text()";
		model.displayXpath = parent.displayXpath == null || parent.displayXpath.isBlank() ? "text()"
				: parent.displayXpath + "/text()";
		return model;
	}

	private static NodeModel toJsonNodeModel(Element element) throws Exception {
		String json = XMLUtils.XmlToJson(element, true, true);
		Object value;
		String trimmed = json.trim();
		if (trimmed.startsWith("{")) {
			value = new JSONObject(trimmed);
		} else if (trimmed.startsWith("[")) {
			value = new JSONArray(trimmed);
		} else {
			value = trimmed;
		}
		NodeModel model = toJsonNodeModel("root", value);
		if (model.children.size() == 1) {
			return model.children.get(0);
		}
		return model;
	}

	private static NodeModel toJsonNodeModel(String label, Object value) throws Exception {
		NodeModel model = new NodeModel();
		model.label = label;
		model.name = label;
		if (value instanceof JSONObject object) {
			model.type = "object";
			for (Iterator<?> i = object.keys(); i.hasNext();) {
				String key = String.valueOf(i.next());
				model.children.add(toJsonNodeModel(key, object.get(key)));
			}
		} else if (value instanceof JSONArray array) {
			model.type = "array";
			for (int i = 0; i < array.length(); i++) {
				model.children.add(toJsonNodeModel("[" + i + "]", array.get(i)));
			}
		} else {
			model.type = "value";
			model.value = value == null || JSONObject.NULL.equals(value) ? "null" : String.valueOf(value);
		}
		return model;
	}

	private static String nodeType(Node node) {
		return switch (node.getNodeType()) {
		case Node.ELEMENT_NODE -> "element";
		case Node.ATTRIBUTE_NODE -> "attribute";
		case Node.TEXT_NODE -> "text";
		default -> "node";
		};
	}

	private static String nodeName(Node node) {
		return switch (node.getNodeType()) {
		case Node.ATTRIBUTE_NODE -> "@" + node.getNodeName();
		case Node.TEXT_NODE -> "text()";
		default -> node.getNodeName();
		};
	}

	private static String nodeLabel(Node node) {
		return switch (node.getNodeType()) {
		case Node.ATTRIBUTE_NODE -> node.getNodeName() + "=\"" + node.getNodeValue() + "\"";
		case Node.TEXT_NODE -> "TxT";
		default -> node.getNodeName();
		};
	}

	private static String nodeValue(Node node) {
		if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
			return "";
		}
		if (node.getNodeType() == Node.TEXT_NODE) {
			return node.getNodeValue() == null ? "" : node.getNodeValue().trim();
		}
		Node firstChild = node.getFirstChild();
		if (firstChild != null && firstChild.getNodeType() == Node.TEXT_NODE) {
			return firstChild.getNodeValue() == null ? "" : firstChild.getNodeValue().trim();
		}
		return "";
	}

	private static String absoluteXPath(String xpath) {
		if (xpath == null || xpath.isBlank()) {
			return "";
		}
		return xpath.startsWith("/") ? xpath : "//" + xpath;
	}

	private static String toDisplayXPath(String anchor, String xpath) {
		String value = normalizeXPath(xpath);
		if (value.startsWith(".")) {
			return (anchor == null || anchor.isBlank()) ? value : anchor + value.substring(1);
		}
		return absoluteXPath(value);
	}

	private static String toRelativeXPath(Document dom, String anchor, String xpath) throws TransformerException {
		String displayXpath = toDisplayXPath(anchor, xpath);
		if (anchor == null || anchor.isBlank() || ".".equals(displayXpath)) {
			return displayXpath;
		}
		if (displayXpath.equals(anchor)) {
			return ".";
		}
		if (displayXpath.startsWith(anchor + "/")) {
			return "." + displayXpath.substring(anchor.length());
		}
		return "." + calcRelativeXPath(dom, anchor, displayXpath);
	}

	private static String calcRelativeXPath(Node dom, String anchor, String xpath) throws TransformerException {
		TwsCachedXPathAPI xpathApi = new TwsCachedXPathAPI();
		String anchorPath = anchor.replaceAll(REGEXP_FOR_PREDICATES, "");
		String nodePath = xpath.replaceAll(REGEXP_FOR_PREDICATES, "");
		Node parentAnchor = xpathApi.selectSingleNode(dom, anchorPath);
		Node node = xpathApi.selectSingleNode(dom, nodePath);
		Node previousAnchor = parentAnchor;
		int level = 0;

		while (parentAnchor != null) {
			Node parentNode = node;
			Node previousNode = node;
			while (parentNode != null) {
				if (parentAnchor.equals(parentNode)) {
					String relative = "";
					NodeList anchorList = parentNode.getChildNodes();
					int anchorIndex = -1;
					int nodeIndex = -1;
					int elementIndex = 0;
					for (int i = 0; i < anchorList.getLength() && (anchorIndex == -1 || nodeIndex == -1); i++) {
						Node item = anchorList.item(i);
						if (item.getNodeType() == Node.ELEMENT_NODE) {
							if (item == previousAnchor) {
								anchorIndex = elementIndex;
							}
							if (item == previousNode) {
								nodeIndex = elementIndex;
							}
							elementIndex++;
						}
					}
					for (int i = 0; i < level - 1; i++) {
						relative += "/..";
					}
					if (anchorIndex != -1 && nodeIndex != -1) {
						relative += anchorIndex < nodeIndex
								? "/following-sibling::*[" + (nodeIndex - anchorIndex) + "]"
								: "/preceding-sibling::*[" + (anchorIndex - nodeIndex) + "]";
					} else if (anchorIndex != -1 || level == 1) {
						relative += "/..";
					}
					Node parent = xpathApi.selectSingleNode(dom, anchorPath + relative);
					String xpathFromParent = XMLUtils.calcXpath(node, parent);
					return relative + (xpathFromParent.length() > 0 ? "/" + xpathFromParent : "");
				}
				previousNode = parentNode;
				parentNode = getParentOrOwner(parentNode);
			}
			previousAnchor = parentAnchor;
			parentAnchor = getParentOrOwner(parentAnchor);
			level++;
		}
		return "";
	}

	private static Node getParentOrOwner(Node node) {
		return node instanceof Attr attr ? attr.getOwnerElement() : node.getParentNode();
	}
}
