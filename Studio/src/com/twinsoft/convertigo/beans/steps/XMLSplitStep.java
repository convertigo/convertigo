/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.steps;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class XMLSplitStep extends XMLElementStep {

	private static final long serialVersionUID = -4205502933374092261L;

	private String regexp = "";
	private boolean keepSeparator = false;
	private XMLVector<XMLVector<String>> tags = new XMLVector<XMLVector<String>>();
		
	public XMLSplitStep() {
		super();
	}

	@Override
    public XMLSplitStep clone() throws CloneNotSupportedException {
    	XMLSplitStep clonedObject = (XMLSplitStep) super.clone();
        return clonedObject;
    }

	@Override
    public XMLSplitStep copy() throws CloneNotSupportedException {
    	XMLSplitStep copiedObject = (XMLSplitStep) super.copy();
        return copiedObject;
    }

	public String getRegexp() {
		return regexp;
	}

	public void setRegexp(String regexp) {
		this.regexp = regexp;
	}

	public boolean isKeepSeparator() {
		return keepSeparator;
	}

	public void setKeepSeparator(boolean keepSeparator) {
		this.keepSeparator = keepSeparator;
	}

	public XMLVector<XMLVector<String>> getTags() {
		return tags;
	}

	public void setTags(XMLVector<XMLVector<String>> tags) {
		this.tags = tags;
	}

	public  int getTagsCount() {
		return tags.size();
	}
	
	public String getTag(int index) {
		if (!tags.isEmpty()) {
			
			String tag =""; 
			if(tags.size()>index)
				tag=(tags.get(index)).get(0);
			return tag.equals("") ? "split":tag;
		}
		return "split";
	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		boolean useDefaultValue = true;
		NodeList list = getContextValues();
		if (list != null) {
			int len = list.getLength();
			useDefaultValue = (len == 0);
			if (!useDefaultValue) {
				for (int i = 0; i < len; i++) {
					Node node = list.item(i);
					String nodeValue = getNodeValue(node);
					String text = (nodeValue == null) ? getNodeText() : nodeValue;
					
					List<Element> elements = split(doc, (len > 1), text);
					for (Element element: elements) {
						stepNode.appendChild(element);
					}
				}
			}
		}
		if (useDefaultValue) {
			String text = getNodeText();
			List<Element> elements = split(doc, false, text);
			for (Element element: elements) {
				stepNode.appendChild(element);
			}
		}
	}

	private List<Element> split(Document doc, boolean withRoot, String text) {
		List<Element> list = new ArrayList<Element>();
		if (!text.equals("")) {
			Pattern myPattern = Pattern.compile(regexp);
			Matcher myMatcher = myPattern.matcher(text);
			
			XMLVector<String> splitString = new XMLVector<String>();
			int beginIndex = 0, startIndex, endIndex;
			while (myMatcher.find()) {
				startIndex = myMatcher.start();
				endIndex = myMatcher.end();
				if (beginIndex != startIndex) {
					splitString.add(new String (text.substring(beginIndex, startIndex)));
				}
				if (keepSeparator) {
					splitString.add(new String (text.substring(startIndex, endIndex)));
				}
				beginIndex = endIndex;
			}
			if (beginIndex != text.length()) {
				splitString.add(new String (text.substring(beginIndex, text.length())));
			}
			
			Element root = null;
			if (withRoot) {
				root = doc.createElement("splits");
				list.add(root);
			}
			
			// for all split string : create a node and add it
			for (int j = 0 ; j < splitString.size() ; j++) {
				String splitted = (String) splitString.get(j);
				Element split = doc.createElement(getTag(j)); 
				split.appendChild(doc.createTextNode(splitted));
				if (withRoot)
					root.appendChild(split);
				else
					list.add(split);
			}
		}
		return list;
	}
	
	@Override
	public String toString() {
		String label = "";
		try {
			XMLVector<String> sourceDefinition = getSourceDefinition();
			label += (sourceDefinition.size() > 0) ? "@("+ getLabel()+")":"@(??)";
		} catch (EngineException e) {
		}
		String nodeName = getNodeName();
		return "<" + nodeName + ">" + "Split(" + label + ")";
	}
	
	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		element.setSchemaTypeName(null);

		XmlSchemaComplexType cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		element.setType(cType);

		XmlSchemaSequence sequence = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
		cType.setParticle(sequence);
		
		int count = getTagsCount();
		
		for (int i = 0; i < count + 1; i++) {
			XmlSchemaElement elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
			elt.setName(getTag(i));
			elt.setMinOccurs(0);
			if (i == count) {
				elt.setMaxOccurs(Long.MAX_VALUE);
			}
			elt.setSchemaTypeName(getSimpleTypeAffectation());
			sequence.getItems().add(elt);
		}
		
		return element;
	}
}
