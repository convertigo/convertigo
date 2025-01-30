/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.common.XmlQName;
import com.twinsoft.convertigo.beans.core.IComplexTypeAffectation;
import com.twinsoft.convertigo.beans.core.IElementRefAffectation;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class XMLComplexStep extends StepWithExpressions implements IComplexTypeAffectation, IElementRefAffectation {

	private static final long serialVersionUID = 7002348210812220725L;

	private String nodeName = getName();
	
	public XMLComplexStep() {
		super();
		setOutput(true);
		this.xml = true;
	}

    public XMLComplexStep clone() throws CloneNotSupportedException {
    	XMLComplexStep clonedObject = (XMLComplexStep) super.clone();
        return clonedObject;
    }

	@Override
    public XMLComplexStep copy() throws CloneNotSupportedException {
    	XMLComplexStep copiedObject = (XMLComplexStep) super.copy();
        return copiedObject;
    }

	@Override
	public String toString() {
		XmlQName xmlQName = getXmlElementRefAffectation();
		xmlQName = xmlQName.isEmpty() ? getXmlComplexTypeAffectation():xmlQName;
		
		String tag = "<"+ getStepNodeName() +"> " + xmlQName.getQName();
		return tag;
	}
	
	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	@Override
	public String getStepNodeName() {
		if (!getXmlElementRefAffectation().isEmpty())
			return getXmlElementRefAffectation().getQName().getLocalPart();		
		return getNodeName();
	}

	@Override
	protected boolean executeNextStep(Context javascriptContext, Scriptable scope) throws EngineException {
    	if (isEnabled()) {
			if (inError()) {
				Engine.logBeans.info("Skipping step "+ this +" ("+ hashCode()+") because its source is in error");
				return true;
			}
    		return super.executeNextStep(javascriptContext, scope);
    	}
    	return false;
	}

	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		return (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
	}
	
	@Override
	protected void onBeanNameChanged(String oldName, String newName) {
		if (oldName.startsWith(nodeName)) {
			nodeName = StringUtils.normalize(newName);
			hasChanged = true;
		}
	}
	
	@Override
	protected String defaultBeanName(String displayName) {
		return "complex";
	}
}
