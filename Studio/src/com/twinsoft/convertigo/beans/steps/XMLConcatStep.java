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

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class XMLConcatStep extends XMLActionStep {

	private static final long serialVersionUID = 2246207792295809623L;

	private String separator = " ";
	
	public XMLConcatStep() {
		super();
	}

	@Override
    public XMLConcatStep clone() throws CloneNotSupportedException {
    	XMLConcatStep clonedObject = (XMLConcatStep) super.clone();
        return clonedObject;
    }

	@Override
    public XMLConcatStep copy() throws CloneNotSupportedException {
    	XMLConcatStep copiedObject = (XMLConcatStep) super.copy();
        return copiedObject;
    }
	
	protected String getActionName() {
		return "Concat";
	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	@Override
	protected String getActionValue() throws EngineException {
		String nodeValue = super.getActionValue();
		XMLVector<XMLVector<Object>> sourcesDefinition = getSourcesDefinition();
		if (sourcesDefinition.size() > 0) {
			StepSource source = null;
			NodeList list = null;
			for (int i=0; i<sourcesDefinition.size();i++) {
				nodeValue += nodeValue.equals("") ? "":separator;
				source = getDefinitionsSource(i);
				if (source != null) {
					list = source.getContextValues();
					if (list != null) {
						int len = list.getLength();
						for (int j=0; j<len; j++) {
							String text = getNodeValue(list.item(j));
							nodeValue += ((text == null) ? getDefinitionsDefaultValue(i): text);
							nodeValue += ((j<len-1) ? separator:"");
						}
					}
					else {
						nodeValue += getDefinitionsDefaultValue(i);
					}
				}
				else {
					nodeValue += getDefinitionsDefaultValue(i);
				}
			}
		}
		return nodeValue;
	}
	
	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		element.setSchemaTypeName(XmlSchemaUtils.getSchemaDataTypeName(getSchemaDataType()));
		return element;
	}
}
