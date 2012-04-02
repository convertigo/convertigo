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

package com.twinsoft.convertigo.engine.requesters;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.servlets.WebServiceServlet;
import com.twinsoft.convertigo.engine.translators.Translator;
import com.twinsoft.convertigo.engine.translators.WebServiceTranslator;
import com.twinsoft.convertigo.engine.util.SOAPUtils;

public class WebServiceServletRequester extends ServletRequester {
	
	@Override
    public String getName() {
        return "WebServiceServletRequester";
    }
    
	@Override
	public Translator getTranslator() {
		return new WebServiceTranslator();
	}
	
	@Override
	public String getContextName() throws Exception {
 		Engine.logEngine.debug("[WebServiceServlet] Searching for context name");
 		
        HttpServletRequest request = (HttpServletRequest) inputData;

		SOAPMessage requestMessage = (SOAPMessage) request.getAttribute(WebServiceServlet.REQUEST_MESSAGE_ATTRIBUTE);
		SOAPPart sp = requestMessage.getSOAPPart();
		SOAPEnvelope se = sp.getEnvelope();
		SOAPBody sb = se.getBody();

		SOAPElement method, parameter;
		String contextName = "default";
		String parameterName, parameterValue;

		for (Iterator<?> iterator = sb.getChildElements();iterator.hasNext();) {
			Object element = iterator.next();
			if (element instanceof SOAPElement) {
				method = (SOAPElement) element;
				
				for (Iterator<?> iterator2 = method.getChildElements();iterator2.hasNext();) {
					element = iterator2.next();
					if (element instanceof SOAPElement) {
						parameter = (SOAPElement) element;
						parameterName = parameter.getElementName().getLocalName();
						parameterValue = parameter.getValue();
						if (parameterValue == null) parameterValue = "";

						if (Parameter.Context.getName().equalsIgnoreCase(parameterName)) {
							if (parameterValue.length() != 0) contextName = parameterValue;
							break;
						}
						else if ("context".equalsIgnoreCase(parameterName)) {
							// For compatibility with Convertigo 2.x
							if (parameterValue.length() != 0) contextName = parameterValue;
							// Do not break because we want __context to have higher priority
						}
					}
				}
				break;
			}
		}

		if (contextName.equals("*")) contextName = "default*";
		
		return contextName;
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.engine.requesters.ServletRequester#initInternalVariables()
	 */
	@Override
	protected void initInternalVariables() throws EngineException {
		
		super.initInternalVariables();
		
        HttpServletRequest request = (HttpServletRequest) inputData;

		SOAPMessage requestMessage = (SOAPMessage) request.getAttribute(WebServiceServlet.REQUEST_MESSAGE_ATTRIBUTE);

		try {
			SOAPPart sp = requestMessage.getSOAPPart();
			SOAPEnvelope se = sp.getEnvelope();
			SOAPBody sb = se.getBody();
	
			SOAPElement method;
			String methodName;
			int i;
			
			for (Iterator<?> iterator = sb.getChildElements();iterator.hasNext();) {
				Object element = iterator.next();
				if (element instanceof SOAPElement) {
					method = (SOAPElement) element;
					methodName = method.getElementName().getLocalName();
					i = methodName.indexOf("__");
					if (i == -1) {
						sequenceName = methodName;
					}
					else {
						connectorName = methodName.substring(0, i);
					}
				}
			}
		}
		catch (SOAPException e) {
			throw new EngineException("Unable to init internal variables",e);
		}
	
	}

	@Override
	public void setStyleSheet(Document document) {
		// Nothing to do		
	}

	@Override
    protected Object coreProcessRequest() throws Exception {
		Object result;
		
		try {
			if (!Engine.isSOA) {
				throw new EngineException("Web Services are not available with this version of Convertigo.");
			}
			
			makeInputDocument();

            preGetDocument();
			Document document = getDocument();
            result = postGetDocument(document);
		}
		catch(Throwable e) {
			context.isErrorDocument = true;
			result = SOAPUtils.writeSoapFault(e, context.requestedObject.getEncodingCharSet());
		}
		
		return result;
    }

	@Override
	public Object postGetDocument(Document document) throws Exception {
		return document;
	}
}
