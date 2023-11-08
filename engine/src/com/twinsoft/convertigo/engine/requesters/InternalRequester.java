/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.engine.requesters;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.NativeJavaObject;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.variables.TestCaseVariable;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.translators.DefaultInternalTranslator;
import com.twinsoft.convertigo.engine.translators.Translator;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class InternalRequester extends GenericRequester {

	private HttpServletRequest httpServletRequest;

	boolean bStrictMode = false;

	protected String subPath = null;

	public InternalRequester(Map<String, Object> request) throws EngineException {
		this(request, null);
	}

	public InternalRequester(Map<String, Object> request, HttpServletRequest httpServletRequest)
			throws EngineException {
		String projectName = getString(request, Parameter.Project.getName());
		DatabaseObjectsManager dbom = "true".equals(RequestAttribute.system.string(httpServletRequest))
				? Engine.theApp.getSystemDatabaseObjectsManager()
				: Engine.theApp.databaseObjectsManager;
		bStrictMode = dbom.getOriginalProjectByName(projectName).isStrictMode();
		inputData = request;
		this.httpServletRequest = httpServletRequest == null ? new InternalHttpServletRequest() : httpServletRequest;

		if (this.httpServletRequest instanceof InternalHttpServletRequest) {
			((InternalHttpServletRequest) this.httpServletRequest).setInternalRequester(this);
		}
	}

	public Object processRequest() throws Exception {
		try {
			return processRequest(inputData);
		} finally {
			Map<String, Object> request = GenericUtils.cast(inputData);
			processRequestEnd(request);
			onFinally(request);
		}
	}

	void processRequestEnd(Map<String, Object> request) {
		request.put("convertigo.cookies", context.getCookieStrings());

		String trSessionId = context.getSequenceTransactionSessionId();
		if (trSessionId != null) {
			request.put("sequence.transaction.sessionid", trSessionId);
		}

		boolean isNew = true;
		HttpSession session = httpServletRequest.getSession();
		if (session != null) {
			if (SessionAttribute.isNew.has(session)) {
				isNew = false;
			} else {
				SessionAttribute.isNew.set(session, true);
			}
		}

		if (context.requireEndOfContext || (isNew && context.isErrorDocument) || context.project == null) {
			request.put("convertigo.requireEndOfContext", Boolean.TRUE);
		}

		if (request.get("convertigo.contentType") == null) { // if
			// contentType
			// set by
			// webclipper
			// servlet
			// (#320)
			request.put("convertigo.contentType", context.contentType);
		}

		request.put("convertigo.cacheControl", context.cacheControl);
		request.put("convertigo.context", context);
		request.put("convertigo.isErrorDocument", Boolean.valueOf(context.isErrorDocument));
		request.put("convertigo.context.removalRequired", Boolean.valueOf(context.removalRequired()));
		if (request.get("convertigo.charset") == null) {
			request.put("convertigo.charset", "UTF-8");
		}

	}

	void onFinally(Map<String, Object> request) {
		// Removes context when finished
		// Note: case of context.requireEndOfContext has been set in scope
		if (getString(request.get("convertigo.requireEndOfContext")) != null) {
			removeContext();
		}

		// Removes context when finished
		String removeContextParam = getString(request, Parameter.RemoveContext.getName());
		if (removeContextParam == null) {
			// case of a mother sequence (context is removed by default)
			Boolean removeContextAttr = Boolean
					.valueOf(getString(request.get("convertigo.context.removalRequired")));
			if ((removeContextAttr != null) && removeContextAttr.booleanValue()) {
				removeContext();
			}
		} else {
			// other cases (remove context if exist __removeContext
			// or __removeContext=true/false)
			if (!("false".equals(removeContextParam))) {
				removeContext();
			}
		}
	}

	protected void removeContext() {
		if (Engine.isEngineMode()) {
			if (context != null) {
				Engine.logContext.debug(
						"(InternalRequester) End of context " + context.contextID + " required => removing context");
				Engine.theApp.contextManager.remove(context);
			}
		}
	}

	public String getName() {
		return "InternalRequester";
	}

	public boolean isInternal() {
		return true;
	}

	protected void initInternalVariables() throws EngineException {
		Map<String, Object> request = GenericUtils.cast(inputData);

		// Find the project name
		projectName = getString(request, Parameter.Project.getName());
		if (projectName != null) {
			Engine.logContext.debug("(InternalRequester) project name: " + projectName);
		}

		// Find the pool name
		poolName = getString(request, Parameter.Pool.getName());
		if (poolName != null) {
			Engine.logContext.debug("(InternalRequester) pool name: " + poolName);
		}

		// Find the sequence name
		sequenceName = getString(request, Parameter.Sequence.getName());
		if (sequenceName != null) {
			Engine.logContext.debug("(InternalRequester) sequence name: " + sequenceName);
		}

		// Find the connector name
		connectorName = getString(request, Parameter.Connector.getName());
		if (connectorName != null) {
			Engine.logContext.debug("(InternalRequester) connector name: " + connectorName);
		}
	}

	public Context getContext() throws Exception {
		if ("true".equals(RequestAttribute.system.string(httpServletRequest))) {
			initInternalVariables();
			return new Context("system");
		}

		Map<String, String[]> request = GenericUtils.cast(inputData);

		String contextName = getContextName();

		initInternalVariables();

		String sessionID = getString(request, Parameter.SessionId.getName());
		if (sessionID == null) {
			sessionID = httpServletRequest.getSession().getId();
		}
		Engine.logContext.debug("(ServletRequester) Requested execution sessionID: " + sessionID);

		context = Engine.theApp.contextManager.get(this, contextName, sessionID, poolName, projectName, connectorName,
				sequenceName);

		if (context.remoteAddr == null) {
			context.remoteAddr = httpServletRequest.getRemoteAddr();
		}
		return context;
	}

	public String getContextName() throws Exception {
		Map<String, Object> request = GenericUtils.cast(inputData);

		// Find the context name
		String contextName = getString(request, Parameter.Context.getName());

		if (StringUtils.isBlank(contextName)) {
			contextName = "default";
		} else if (contextName.equals("*")) {
			contextName = "default*";
		}

		Engine.logContext.debug("(InternalRequester) Context name: " + contextName);
		return contextName;
	}

	@Override
	public void initContext(Context context) throws Exception {
		if (httpServletRequest != null) {
			context.setRequest(httpServletRequest);
		}

		super.initContext(context);

		Map<String, Object> request = GenericUtils.cast(inputData);

		// We transform the HTTP post data into XML data.
		Set<String> parameterNames = request.keySet();
		boolean bConnectorGivenByUser = false;

		for (String parameterName : parameterNames) {
			String parameterValue;

			// Handle only convertigo parameters
			if (parameterName.startsWith("__")) {
				parameterValue = getString(request, parameterName);

				handleParameter(context, parameterName, parameterValue);

				if (parameterName.equals(Parameter.Connector.getName())) {
					bConnectorGivenByUser = true;
				}
			}
		}

		TestCase tc = TestCase.getTestCase(request, context.projectName);
		if (tc != null) {
			for (TestCaseVariable var : tc.getVariables()) {
				String parameterName = var.getName();
				String parameterValue;
				// Handle only convertigo parameters
				if (!request.containsKey(parameterName) && parameterName.startsWith("__")) {
					Object parameterObjectValue = var.getValueOrNull();
					parameterValue = getString(parameterObjectValue);

					if (parameterValue != null) {
						handleParameter(context, parameterName, parameterValue);

						if (parameterName.equals(Parameter.Connector.getName())) {
							bConnectorGivenByUser = true;
						}
					}
				}
			}
		}

		if (!bConnectorGivenByUser) {
			if (context.project != null && context.project.getName().equals(context.projectName)) {
				String defaultConnectorName = context.project.getDefaultConnector().getName();
				if (!defaultConnectorName.equals(context.connectorName)) {
					context.connectorName = defaultConnectorName;
				}
			}
		}

		Engine.logContext.debug("Context initialized!");
	}

	public static String getParameterValue(Object parameterObjectValue) {
		return getString(parameterObjectValue);
	}

	public Translator getTranslator() {
		DefaultInternalTranslator defaultInternalTranslator = new DefaultInternalTranslator();
		defaultInternalTranslator.setStrictMode(bStrictMode);
		return defaultInternalTranslator;
	}

	public void preGetDocument() {
		String contextID = context.contextID;
		Engine.logContext.debug("Context ID: " + contextID);

		context.servletPath = null;
		if (context.userAgent == null) {
			context.userAgent = "Convertigo engine internal requester";
		}
		if (context.remoteAddr == null) {
			context.remoteAddr = "127.0.0.1";
		}
		if (context.remoteHost == null) {
			context.remoteHost = "localhost";
		}
	}

	public void setStyleSheet(Document document) {
		// Do nothing
	}

	protected Object addStatisticsAsData(Object result) {
		return EngineStatistics.addStatisticsAsXML(context, result);
	}

	protected Object addStatisticsAsText(String stats, Object result) throws UnsupportedEncodingException {
		if (result != null) {
			if (stats == null)
				stats = context.statistics.printStatistics();
			if (result instanceof Document) {
				Document document = (Document) result;
				Comment comment = document.createComment("\n" + stats);
				document.appendChild(comment);
			} else if (result instanceof byte[]) {
				String encodingCharSet = "UTF-8";
				if (context.requestedObject != null)
					encodingCharSet = context.requestedObject.getEncodingCharSet();
				String sResult = new String((byte[]) result, encodingCharSet);
				sResult += "<!--\n" + stats + "\n-->";
				result = sResult.getBytes(encodingCharSet);
			}
		}
		return result;
	}

	public HttpServletRequest getHttpServletRequest() {
		return httpServletRequest;
	}

	public static String getString(Object o) {
		return getString(o, null);
	}

	public static String getString(Object o, String def) {
		String s = def;
		if (o == null) {
		} else if (o instanceof String) {
			s = (String) o;
		} else if (o.getClass().isArray()) {
			if (Array.getLength(o) > 0) {
				s = getString(Array.get(o, 0), def);
			}
		} else if (o instanceof NativeJavaObject) {
			s = getString(((NativeJavaObject) o).unwrap(), def);
		} else if (o instanceof Node) {
			s = ((Node) o).getTextContent();
		} else if (o instanceof NodeList) {
			NodeList v = (NodeList) o;
			if (v.getLength() > 0) {
				s = getString(v.item(0));
			}
		} else if (o instanceof Collection) {
			Collection<?> v = (Collection<?>) o;
			if (!v.isEmpty()) {
				s = getString(v.iterator().next());
			}
		} else if (def == null) {
			s = o.toString();
		}
		return s;
	}

	public static String getString(Map<String, Object> request, String key) {
		return getString(request, key, null);
	}

	public static String getString(Map<String, Object> request, String key, String def) {
		Object o = request.get(key);
		return getString(o, def);
	}
}
