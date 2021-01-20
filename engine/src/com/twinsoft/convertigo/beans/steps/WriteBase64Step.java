/*
 * Copyright (c) 2001-2021 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.steps;

import java.io.File;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.constants.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class WriteBase64Step extends WriteFileStep implements  IStepSourceContainer {

	private static final long serialVersionUID = 2781335492473421310L;
	
	private transient String ComputedFilePath;

	public WriteBase64Step() {
		super();
		this.xml = true;
	}

	@Override
    public WriteBase64Step clone() throws CloneNotSupportedException {
    	WriteBase64Step clonedObject = (WriteBase64Step) super.clone();
    	clonedObject.ComputedFilePath = null;
        return clonedObject;
    }

	@Override
    public WriteBase64Step copy() throws CloneNotSupportedException {
    	WriteBase64Step copiedObject = (WriteBase64Step) super.copy();
        return copiedObject;
    }

	@Override
	public String toString() {
		String label = "";
		try {
			label += super.getLabel();
		} catch (EngineException e) {}
		return "WriteBase64" + label;
	}

	protected void writeFile(String filePath, NodeList nodeList) throws EngineException {
		if (nodeList == null) {
			throw new EngineException("Unable to write to xml file: element is Null");
		}
		
		String fullPathName = getAbsoluteFilePath(filePath);
		ComputedFilePath = fullPathName;
		synchronized (Engine.theApp.filePropertyManager.getMutex(fullPathName)) {
			try {
				for (Node node : XMLUtils.toNodeArray(nodeList)) {
					try {
						String content = node instanceof Element ? ((Element) node).getTextContent() : node.getNodeValue();
						if (content != null && content.length() > 0) {
							byte[] bytes = Base64.decodeBase64(content);
							if (bytes != null && bytes.length > 0) {
								FileUtils.writeByteArrayToFile(new File(fullPathName), bytes);
								return;
							}
						}
					} catch (Exception e) {
						Engine.logBeans.info("(WriteBase64Step) Failed to decode and write base64 content : " + e.getClass().getCanonicalName());
					}
				}
			} finally {
				Engine.theApp.filePropertyManager.releaseMutex(fullPathName);
			}
		}
	}
	
	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		if (StringUtils.isNotEmpty(ComputedFilePath)) {
			Element keyElement = doc.createElement("filePath");
			keyElement.setTextContent(ComputedFilePath);
			stepNode.appendChild(keyElement);
		}
	}

	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		
		XmlSchemaComplexType cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		element.setType(cType);

		XmlSchemaSequence sequence = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
		cType.setParticle(sequence);
		SchemaMeta.setContainerXmlSchemaGroupBase(element, sequence);
		
		XmlSchemaElement elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		sequence.getItems().add(elt);
		elt.setName("filePath");
		elt.setMinOccurs(0);
		elt.setMaxOccurs(1);
		elt.setSchemaTypeName(Constants.XSD_STRING);
		
		return element;
	}

}
