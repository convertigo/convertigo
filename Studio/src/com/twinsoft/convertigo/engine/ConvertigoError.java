/*
 * Copyright (c) 2001-2012 Convertigo SA.
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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.engine;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.engine.enums.ErrorType;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;
import com.twinsoft.util.Log;

public class ConvertigoError {
	
	private int errorCode = -1;
	private Throwable throwable;
	private ErrorType errorType;

	private ConvertigoError(int code, ErrorType errorType, Throwable t) {
		this.errorCode = code;
		this.errorType = errorType == null ? ErrorType.Convertigo : errorType;
		this.throwable = t == null ? new Exception("Unknown error") : t;
	}
	
	public ErrorType getErrorType() {
		return errorType;
	}
	
	public Throwable getThrowable() {
		return throwable;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		if (throwable instanceof ConvertigoException) {
			return ((ConvertigoException)throwable).getErrorMessage();
		}
		return throwable.getMessage();
	}
	
	public String getErrorDetails() {
		if (throwable instanceof ConvertigoException) {
			return ((ConvertigoException)throwable).getErrorDetails();
		}
		return "";
	}
	
	public Document buildErrorDocument(Requester requester, Context context) throws Exception {
		return buildErrorDocument(requester, context, false);
	}
	
	public Document buildErrorDocument(Requester requester, Context context, boolean bHide) throws Exception {
		Document document = requester.createDOM("UTF-8");

		Element doc = document.createElement("document");
		document.appendChild(doc);
		
		Element error = document.createElement("error");
		doc.appendChild(error);
		
		Text text;

		if (!bHide) {
			if (context != null) {
				if (context.projectName != null)
					error.setAttributeNS("","project",context.projectName);
				if (context.connectorName != null)
					error.setAttributeNS("","connector", context.connectorName);
				if (context.transactionName != null)
					error.setAttributeNS("","transaction",  context.transactionName);
				if (context.sequenceName != null)
					error.setAttributeNS("","sequence",  context.sequenceName);
			}
			error.setAttributeNS("","type", getErrorType().getType());
			
			Element code = document.createElement("code");
			text = document.createTextNode(String.valueOf(getErrorCode()));
			code.appendChild(text);
			error.appendChild(code);

			Element message = document.createElement("message");
			text = document.createTextNode(getErrorMessage());
			message.appendChild(text);
			error.appendChild(message);

			Element details = document.createElement("details");
			text = document.createTextNode(getErrorDetails());
			details.appendChild(text);
			error.appendChild(details);
			
			Element econtext = document.createElement("context");
			if (context != null) {
				for (String key : context.keys()) {
					Object value = context.get(key);
					if ((value != null) && (value instanceof String)) {
						Element variable = document.createElement("variable");
						variable.setAttribute("name", key);
						variable.setAttribute("value", (String) value);
						econtext.appendChild(variable);
					}
				}
			}
			
			error.appendChild(econtext);
	
			Element exception = document.createElement("exception");
			text = document.createTextNode(getThrowable().getClass().getName());
			exception.appendChild(text);
			error.appendChild(exception);
	
			Element stackTrace = document.createElement("stacktrace");
			String jss = Log.getStackTrace(getThrowable());
			jss = jss.replace('\r', ' ');
			text = document.createTextNode(jss);
			stackTrace.appendChild(text);
			error.appendChild(stackTrace);
	
		}
		return document;
	}
	
	public static void addXmlSchemaObjects(XmlSchema schema) {
		XmlSchemaComplexType cConvertigoErrorContextVariableType = new XmlSchemaComplexType(schema);
		cConvertigoErrorContextVariableType.setName("ConvertigoErrorContextVariable");
		XmlSchemaObjectCollection attributes = cConvertigoErrorContextVariableType.getAttributes();
		XmlSchemaAttribute aName = new XmlSchemaAttribute();
		aName.setName("name");
		aName.setSchemaTypeName(Constants.XSD_STRING);
		attributes.add(aName);
		XmlSchemaAttribute aValue = new XmlSchemaAttribute();
		aValue.setName("value");
		aValue.setSchemaTypeName(Constants.XSD_STRING);
		attributes.add(aValue);
		XmlSchemaUtils.add(schema, cConvertigoErrorContextVariableType);
	
		XmlSchemaComplexType cConvertigoErrorContextType = new XmlSchemaComplexType(schema);
		cConvertigoErrorContextType.setName("ConvertigoErrorContext");
		XmlSchemaSequence sequence = new XmlSchemaSequence();
		cConvertigoErrorContextType.setParticle(sequence);
		XmlSchemaElement eVariable = new XmlSchemaElement();
		eVariable.setName("variable");
		eVariable.setSchemaTypeName(cConvertigoErrorContextVariableType.getQName());
		eVariable.setMaxOccurs(Long.MAX_VALUE);
		eVariable.setMinOccurs(0);
		sequence.getItems().add(eVariable);
		XmlSchemaUtils.add(schema, cConvertigoErrorContextType);
	
		XmlSchemaSimpleType sConvertigoErrorType = new XmlSchemaSimpleType(schema);
		XmlSchemaSimpleTypeRestriction sConvertigoErrorTypeRestriction = new XmlSchemaSimpleTypeRestriction();
		sConvertigoErrorTypeRestriction.setBaseTypeName(Constants.XSD_STRING);
		XmlSchemaObjectCollection sConvertigoErrorTypeRestrictionFacets = sConvertigoErrorTypeRestriction.getFacets();
		sConvertigoErrorTypeRestrictionFacets.add(new XmlSchemaEnumerationFacet("c8o", false));
		sConvertigoErrorTypeRestrictionFacets.add(new XmlSchemaEnumerationFacet("project", false));
		sConvertigoErrorType.setContent(sConvertigoErrorTypeRestriction);
		
		XmlSchemaComplexType cConvertigoErrorType = new XmlSchemaComplexType(schema);
		cConvertigoErrorType.setName("ConvertigoError");
		attributes = cConvertigoErrorType.getAttributes();
		aName = new XmlSchemaAttribute();
		aName.setName("project");
		aName.setSchemaTypeName(Constants.XSD_STRING);
		attributes.add(aName);
		aName = new XmlSchemaAttribute();
		aName.setName("connector");
		aName.setSchemaTypeName(Constants.XSD_STRING);
		attributes.add(aName);
		aName = new XmlSchemaAttribute();
		aName.setName("transaction");
		aName.setSchemaTypeName(Constants.XSD_STRING);
		attributes.add(aName);
		aName = new XmlSchemaAttribute();
		aName.setName("sequence");
		aName.setSchemaTypeName(Constants.XSD_STRING);
		attributes.add(aName);
		aName = new XmlSchemaAttribute();
		aName.setName("type");
		aName.setSchemaType(sConvertigoErrorType);
		attributes.add(aName);
		
		sequence = new XmlSchemaSequence();
		sequence.setMinOccurs(0);
		cConvertigoErrorType.setParticle(sequence);
		XmlSchemaElement eCode = new XmlSchemaElement();
		eCode.setName("code");
		eCode.setSchemaTypeName(Constants.XSD_INTEGER);
		sequence.getItems().add(eCode);
		XmlSchemaElement eMessage = new XmlSchemaElement();
		eMessage.setName("message");
		eMessage.setSchemaTypeName(Constants.XSD_STRING);
		sequence.getItems().add(eMessage);
		XmlSchemaElement eDetails = new XmlSchemaElement();
		eDetails.setName("details");
		eDetails.setSchemaTypeName(Constants.XSD_STRING);
		sequence.getItems().add(eDetails);
		XmlSchemaElement eContext = new XmlSchemaElement();
		eContext.setName("context");
		eContext.setSchemaTypeName(cConvertigoErrorContextType.getQName());
		sequence.getItems().add(eContext);
		XmlSchemaElement eException = new XmlSchemaElement();
		eException.setName("exception");
		eException.setSchemaTypeName(Constants.XSD_STRING);
		sequence.getItems().add(eException);
		XmlSchemaElement eStacktrace = new XmlSchemaElement();
		eStacktrace.setName("stacktrace");
		eStacktrace.setSchemaTypeName(Constants.XSD_STRING);
		sequence.getItems().add(eStacktrace);
		XmlSchemaUtils.add(schema, cConvertigoErrorType);
	}

	public static void removeXmlSchemaObjects(XmlSchema schema) {
		XmlSchemaType type = null;
		if ((type = schema.getTypeByName("ConvertigoErrorContextVariable")) != null)
			XmlSchemaUtils.remove(schema, type);
		if ((type = schema.getTypeByName("ConvertigoErrorContext")) != null)
			XmlSchemaUtils.remove(schema, type);
		if ((type = schema.getTypeByName("ConvertigoError")) != null)
			XmlSchemaUtils.remove(schema, type);
	}
	
	public static void updateXmlSchemaObjects(XmlSchema schema) {
		removeXmlSchemaObjects(schema);
		addXmlSchemaObjects(schema);
	}
	
	public static ConvertigoError initError(ConvertigoException convertigoException) {
		return initError(ErrorType.Convertigo, convertigoException);
	}

	public static ConvertigoError initError(ErrorType errorType, ConvertigoException convertigoException) {
		return initError(-1, errorType, convertigoException);
	}
	
	public static ConvertigoError initError(int errorCode, ErrorType errorType, ConvertigoException convertigoException) {
		return new ConvertigoError(errorCode, errorType, convertigoException);
	}

	public static ConvertigoError get(Throwable t) {
		if (t instanceof ConvertigoException)
			return ((ConvertigoException)t).getError();
		return new ConvertigoError(-1, ErrorType.Convertigo, t);
	}	
}
