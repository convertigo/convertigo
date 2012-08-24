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

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.EngineException;

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
				tag=(tags.elementAt(index)).elementAt(0);
			return tag.equals("") ? "split":tag;
		}
		return "split";
	}

	@Override
	protected Node createWsdlDom() throws EngineException {
		Element element = (Element)super.createWsdlDom();
		if (element != null) {
			int count = getTagsCount();
			if (count > 0) {
				for (int i=0; i<count; i++) {
					element.appendChild(wsdlDom.createElement(getTag(i)));
				}
			}
			else {
				element.appendChild(wsdlDom.createElement("split"));
			}
		}
		return element;
	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		NodeList list = getContextValues();
		if (list != null) {
			int len = list.getLength();
			for (int i = 0; i < len; i++) {
				Node node = list.item(i);
				String nodeValue = getNodeValue(node);
				String text = (nodeValue == null) ? getNodeText() : nodeValue;
				
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
					
					Element splits = null;
					if (len > 1) {
						splits = doc.createElement("splits");
					}

					// for all split string : create a node and add it
					for (int j = 0 ; j < splitString.size() ; j++) {
						String splitted = (String) splitString.elementAt(j);
						Element split = doc.createElement(getTag(j)); 
						split.appendChild(doc.createTextNode(splitted));
						if (splits == null) {
							stepNode.appendChild(split);
						} else {
							splits.appendChild(split);
						}
					}
					
					if (splits != null) {
						stepNode.appendChild(splits);
					}
				}
			}
		}
	}

	@Override
	public String getSchemaType(String tns) {
		return tns +":"+ getStepNodeName() + priority +"StepType";
	}
	
	@Override
	public void addSchemaType(HashMap<Long, String> stepTypes, String tns, String occurs) throws EngineException {
		int count = getTagsCount();
		
		String stepTypeSchema = "";
		stepTypeSchema += "\t<xsd:complexType name=\""+ getSchemaTypeName(tns) +"\">\n";
    	if (count > 0) {
			stepTypeSchema += "\t\t<xsd:element name=\"splits\">\n";
			stepTypeSchema += "\t\t\t<xsd:complexType>\n";
    	}
		stepTypeSchema += "\t\t\t<xsd:sequence>\n";
		for (int i=0; i<count; i++) {
			String tag = getTag(i);
			if (!tag.equals("split"))
				stepTypeSchema += "\t\t\t<xsd:element minOccurs=\"0\" name=\""+ tag +"\" type=\""+ getSchemaDataType(tns) +"\" />\n";
		}
		stepTypeSchema += "\t\t\t<xsd:element minOccurs=\"0\" name=\"split\" type=\""+ getSchemaDataType(tns) +"\" />\n";
		stepTypeSchema += "\t\t\t</xsd:sequence>\n";
		if (count > 0) {
	    	stepTypeSchema += "\t\t\t</xsd:complexType>\n";
	    	stepTypeSchema += "\t\t</xsd:element>\n";
    	}
		stepTypeSchema += "\t</xsd:complexType>\n";
		
		stepTypes.put(new Long(priority), stepTypeSchema);
	}

	@Override
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			XMLVector<String> sourceDefinition = getSourceDefinition();
			label += (sourceDefinition.size() > 0) ? "@("+ getLabel()+")":"@(??)";
		} catch (EngineException e) {
		}
		String nodeName = getNodeName();
		return "<"+ nodeName +">" + "Split("+ label +")"+ (!text.equals("") ? " // "+text:"");
	}
	
}
