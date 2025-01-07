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

package com.twinsoft.convertigo.engine.translators;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.variables.TestCaseVariable;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class DefaultServletTranslator implements Translator {

	public void buildInputDocument(Context context, Object inputData) throws Exception {
        Engine.logContext.debug("Making input document");

		HttpServletRequest request = (HttpServletRequest) inputData;
		
		InputDocumentBuilder inputDocumentBuilder = new InputDocumentBuilder(context);
        
		// We transform the HTTP post data into XML data.
		Enumeration<?> parameterNames = request.getParameterNames();

		// Sometimes, because of a bug about bad recycling of request facade objects in Tomcat,
		// the HTTP parameters parsing is not realized, and then parameterNames is empty. In
		// such a case, a (bad) workaround is to decode by ourselves the query string in GET
		// or the HTTP body in POST...
//		Map<String, String[]> reparsedParameters = (Map<String, String[]>) request.getAttribute(ServletRequester.REPARSED_PARAMETERS_ATTRIBUTE);
//		boolean bReparsedParameters = (reparsedParameters != null); 
//		if (bReparsedParameters) {
//			parameterNames = reparsedParameters.keys();
//		}
		
		boolean isHandleComplex = "true".equals(request.getParameter("__handleComplex"));
		
		while (parameterNames.hasMoreElements()) {
			String parameterName = (String) parameterNames.nextElement();
			String[] parameterValues = request.getParameterValues(parameterName);

			if (!inputDocumentBuilder.handleSpecialParameter(parameterName, parameterValues)) {
				inputDocumentBuilder.addVariable(parameterName, parameterValues, isHandleComplex);
			}
		}
		
		TestCase tc = TestCase.getTestCase(request, context.projectName);
		if (tc != null) {
			for (TestCaseVariable var: tc.getVariables()) {
				String parameterName = var.getName();
				if (request.getParameter(parameterName) == null) {
					Object parameterObject = var.getValueOrNull();
					String[] parameterValues = (parameterObject instanceof XMLVector<?>) ?
							((XMLVector<?>) parameterObject).toArray(new String[0]) : new String[] {(String) parameterObject};
	
					if (!inputDocumentBuilder.handleSpecialParameter(parameterName, parameterValues)) {
						inputDocumentBuilder.addVariable(parameterName, parameterValues, isHandleComplex);
					}
				}
			}
		}

		Engine.logContext.debug("Input document created");
    }

	public Object buildOutputData(Context context, Object convertigoResponse) throws Exception {
		if (convertigoResponse instanceof String) {
			String encodingCharSet = "UTF-8";
			if (context.requestedObject != null) {
				encodingCharSet = context.requestedObject.getEncodingCharSet();
			}
			return ((String) convertigoResponse).getBytes(encodingCharSet);
		}
		return convertigoResponse;
	}

	public String getContextName(byte[] data) throws Exception {
		throw new EngineException("The DefaultServletTranslator translator does not support the getContextName() method");
	}

	public String getProjectName(byte[] data) throws Exception {
		throw new EngineException("The DefaultServletTranslator translator does not support the getProjectName() method");
	}

}
