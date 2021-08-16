/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.twinsoft.convertigo.beans.core.IComplexTypeAffectation;
import com.twinsoft.convertigo.beans.core.IStepSmartTypeContainer;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.engine.ConvertigoError;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.ErrorType;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class XMLErrorStep extends StepWithExpressions implements IStepSmartTypeContainer, IComplexTypeAffectation {

	private static final long serialVersionUID = 7008868210812220725L;
	
	private SmartType code = new SmartType();
	private SmartType message = new SmartType();
	private SmartType details = new SmartType();
	
	public XMLErrorStep() {
		super();
		setOutput(true);
		this.xml = true;
	}

	@Override
	public QName getComplexTypeAffectation() {
		return new QName(getProject().getTargetNamespace(), "ConvertigoError");
	}

	@Override
    public XMLErrorStep clone() throws CloneNotSupportedException {
    	XMLErrorStep clonedObject = (XMLErrorStep) super.clone();
    	clonedObject.smartTypes = null;
		clonedObject.code = code.clone();
		clonedObject.message = message.clone();
		clonedObject.details = details.clone();
        return clonedObject;
    }

	@Override
    public XMLErrorStep copy() throws CloneNotSupportedException {
    	XMLErrorStep copiedObject = (XMLErrorStep) super.copy();
        return copiedObject;
    }

	@Override
	public String getStepNodeName() {
		return "error";
	}
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			evaluate(javascriptContext, scope, code);
			evaluate(javascriptContext, scope, message);
			evaluate(javascriptContext, scope, details);
			
			return super.stepExecute(javascriptContext, scope);
		}
		return false;
	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		try {
			String sCode, eMessage, eDetails;
			int eCode = (sCode = code.getSingleString(this)) == null ? -1 : sCode.equals("") ? -1:Integer.parseInt(sCode);
			eMessage = (eMessage = message.getSingleString(this)) == null ? "" : eMessage;
			eDetails = (eDetails = details.getSingleString(this)) == null ? "" : eDetails;
			ConvertigoError err = ConvertigoError.initError(eCode, ErrorType.Project, new StepException(eMessage, eDetails));
			err.appendOutputNodes(stepNode, getSequence().context, false);
			try {
				Element exception = (Element) stepNode.getElementsByTagName("exception").item(0);
				stepNode.replaceChild(doc.createElement("exception"), exception);
				Element stacktrace = (Element) stepNode.getElementsByTagName("stacktrace").item(0);
				stepNode.replaceChild(doc.createElement("stacktrace"), stacktrace);
			}
			catch (Exception e) {}
			
		} catch (Exception e) {
			setErrorStatus(true);
			Engine.logBeans.error("An error occured while generating values from XMLErrorStep", e);
			//throw new EngineException("Unable to generate XMLErrorStep",e);
		}
	}
	
	public SmartType getCode() {
		return code;
	}

	public void setCode(SmartType code) {
		this.code = code;
	}
	
	public SmartType getMessage() {
		return message;
	}
	
	public void setMessage(SmartType message) {
		this.message = message;
	}

	public SmartType getDetails() {
		return details;
	}

	public void setDetails(SmartType details) {
		this.details = details;
	}
	
	@Override
	public String toString() {
		String str = "<error> [" + StringUtils.reduce(code.toString(this), 8) + "] " + StringUtils.reduce(message.toString(this), 30);
		try {
			return str + getLabel();
		} catch (Exception e) {
			return str;
		}
	}

	private transient Set<SmartType> smartTypes = null;
	
	@Override
	public Set<SmartType> getSmartTypes() {
		if (smartTypes != null) {
			if  (!hasChanged)
				return smartTypes;
			else
				smartTypes.clear();
		}
		else {
			smartTypes = new HashSet<SmartType>();
		}
		smartTypes.add(code);
		smartTypes.add(message);
		smartTypes.add(details);
		return smartTypes;
	}
}
