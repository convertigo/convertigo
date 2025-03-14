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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.IContextMaintainer;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.IVariableContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepEvent;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.ProjectUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class TransactionStep extends RequestableStep implements ITagsProperty {

	private static final long serialVersionUID = -8658842486320491604L;

	protected transient String connectorName = "";
	protected transient String transactionName = "";

	private String sourceTransaction = "";

	private XMLVector<String> connectionStringDefinition = new XMLVector<String>();

	public TransactionStep() {
		super();
	}

	public TransactionStep(boolean synchronous) {
		super();
	}

	@Override
	public TransactionStep clone() throws CloneNotSupportedException {
		TransactionStep clonedObject = (TransactionStep) super.clone();
		clonedObject.setSourceTransaction(sourceTransaction);
		return clonedObject;
	}

	@Override
	public TransactionStep copy() throws CloneNotSupportedException {
		TransactionStep copiedObject = (TransactionStep) super.copy();
		copiedObject.setSourceTransaction(sourceTransaction);
		return copiedObject;
	}

	@Override
	public String getStepNodeName() {
		return "transaction";
	}

	public String getConnectorName() {
		return connectorName;
	}

	public String getTransactionName() {
		return transactionName;
	}

	public XMLVector<String> getConnectionStringDefinition() {
		return connectionStringDefinition;
	}

	public void setConnectionStringDefinition(XMLVector<String> connectionStringDefinition) {
		this.connectionStringDefinition = connectionStringDefinition;
	}

	public String getConnectionStringValue() throws EngineException {
		String connectionStringValue = "";
		if (connectionStringDefinition.size() != 0) {
			StepSource source = new StepSource(this, connectionStringDefinition);
			Object value = source.getContextValues();
			if (value != null) {
				if (value instanceof NodeList) {
					NodeList list = (NodeList) value;
					if ((list != null) && (list.getLength() > 0)) {
						connectionStringValue = getNodeValue(list.item(0));
					}
				} else {
					connectionStringValue = value.toString();
				}
			}
		}
		return connectionStringValue;
	}

	@Override
	protected void stepDone() {
		// Remove transaction's context if needed
		removeTransactionContext();

		super.stepDone();
	}

	private void removeTransactionContext() {
		if (Engine.isEngineMode()) {
			if (parent instanceof ParallelStep) {
				if (sequence.useSameJSessionForSteps()) {
					String contextName = InternalRequester.getString(request, Parameter.Context.getName());
					if (contextName.startsWith("Container-")) { // Only remove context automatically named
						String contextID = sequence.getSessionId() + "_" + contextName;
						Engine.logBeans.debug("Executing deletion of transaction's context of TransactionStep \"" + getName() + "\"");
						Engine.theApp.contextManager.remove(contextID);
						Engine.logBeans.debug("Deletion of transaction's context of TransactionStep \"" + getName() + "\" done");
					}
				} else {
					if (httpState != null) {
						Cookie[] httpCookies = httpState.getCookies();
						int len = httpCookies.length;
						Cookie cookie = null;
						for (int i = 0; i < len; i++) {
							cookie = httpCookies[i];
							if (cookie.getName().equalsIgnoreCase("JSESSIONID")) {
								Engine.logBeans
										.debug("Executing deletion of transaction's context of TransactionStep \""
												+ getName() + "\"");
								Engine.theApp.contextManager.removeAll(cookie.getValue());
								Engine.logBeans
										.debug("Deletion of transaction's context of TransactionStep \""
												+ getName() + "\" done");
								break;
							}
						}
					}
				}
			}
		}
	}

	protected void prepareForRequestable(Context javascriptContext, Scriptable scope) throws MalformedURLException, EngineException {
		Transaction targetTransaction = getTargetTransaction();
		Connector targetConnector = targetTransaction.getConnector();

		String ctxName = getContextName(javascriptContext, scope);
		boolean useSequenceJSession = sequence.useSameJSessionForSteps();

		String connectionStringValue = (String) getConnectionStringValue();

		if (isInternalInvoke()) {
			request.put(Parameter.Project.getName(), new String[] { projectName });
			// request.put(Parameter.Pool.getName(), new String[] { "" });
			request.put(Parameter.MotherSequenceContext.getName(), new String[] { sequence.context.contextID });
			request.put(Parameter.Transaction.getName(), new String[] { targetTransaction.getName() });
			request.put(Parameter.Connector.getName(), new String[] { targetConnector.getName() });
			request.put(Parameter.Context.getName(), new String[] { sequence.addStepContextName(ctxName) });
			request.put(Parameter.SessionId.getName(), new String[] { getTransactionSessionId() });
			if (!connectionStringValue.equals(""))
				request.put(Parameter.ConnectorConnectionString.getName(),
						new String[] { connectionStringValue });
			getPostQuery(scope);
		} else {
			targetUrl = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
			targetUrl += "/projects/" + projectName + "/.xml?";

			URL url = new URL(targetUrl);
			String host = url.getHost();
			int port = url.getPort();

			Engine.logBeans.trace("(TransactionStep) Host: " + host + ":" + port);
			hostConfiguration.setHost(host, port);

			method = new PostMethod(targetUrl);
			HeaderName.ContentType.setRequestHeader(method, MimeType.WwwForm.value());

			// Set transaction sessionId from context maintainer
			String sessionId = getTransactionSessionId();
			if (useSequenceJSession) {
				Engine.logBeans.trace("(TransactionStep) JSESSIONID required : " + sessionId);
				if (sessionId != null) {
					method.setRequestHeader("Cookie", "JSESSIONID=" + sessionId + ";");
					Engine.logBeans.trace("(TransactionStep) JSESSIONID used : " + sessionId);
				} else {
					Engine.logBeans.trace("(TransactionStep) JSESSIONID is null");
				}
			} else {
				if (sessionId != null) {
					method.setRequestHeader("Cookie", "JSESSIONID=" + sessionId + ";");
					Engine.logBeans.trace("(TransactionStep) Transaction JSESSIONID used : " + sessionId);
				} else {
					Engine.logBeans.trace("(TransactionStep) Transaction JSESSIONID is null");
				}
			}

			String postQuery = getPostQuery(scope);
			if (postQuery.indexOf(Parameter.Connector.getName()) == -1)
				postQuery = addParamToPostQuery(Parameter.Connector.getName(), targetConnector.getName(),
						postQuery);
			if (postQuery.indexOf(Parameter.Transaction.getName()) == -1)
				postQuery = addParamToPostQuery(Parameter.Transaction.getName(), targetTransaction.getName(),
						postQuery);
			if (postQuery.indexOf(Parameter.MotherSequenceContext.getName()) == -1)
				postQuery = addParamToPostQuery(Parameter.MotherSequenceContext.getName(),
						sequence.context.contextID, postQuery);
			if (postQuery.indexOf(Parameter.Context.getName()) == -1)
				postQuery = addParamToPostQuery(Parameter.Context.getName(),
						sequence.addStepContextName(ctxName), postQuery);
			if (!connectionStringValue.equals(""))
				postQuery = addParamToPostQuery(Parameter.ConnectorConnectionString.getName(),
						connectionStringValue, postQuery);

			if (Engine.logBeans.isTraceEnabled())
				Engine.logBeans.trace("(TransactionStep) postQuery :"
						+ Visibility.Logs.replaceVariables(getVariables(), postQuery));

			try {
				method.setRequestEntity(new StringRequestEntity(postQuery, null, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new EngineException("Encoding error", e);
			}
		}
	}

	private String getContextName(Context javascriptContext, Scriptable scope) {
		boolean useSameJSessionForSteps = sequence.useSameJSessionForSteps();
		String ctxName = getStepContextName();

		// Auto context name
		if (ctxName.equals("")) {
			ctxName = useSameJSessionForSteps ? "error_" + priority : "default";
			IContextMaintainer transactionContextMaintainer = getTransactionContextMaintainer();
			if (transactionContextMaintainer != null) {
				// Get context name for transaction (inherited or not)
				ctxName = transactionContextMaintainer.getInheritedContextName();
				ctxName = (ctxName == null) ? transactionContextMaintainer.getContextName() : ctxName;
				if (useSameJSessionForSteps) {
					if (transactionContextMaintainer instanceof ParallelStep) {
						// Transaction requires new context
						ctxName += "-" + priority;
					}
				}
			}
		}
		// Defined context name
		else {
			try {
				evaluate(javascriptContext, scope, ctxName, "ctxName", true);
			} catch (Exception e) {
				evaluated = null;
				Engine.logBeans.trace("(TransactionStep) " + e.getMessage());
			}
			ctxName = (evaluated != null) ? evaluated.toString() : (useSameJSessionForSteps ? "error_"
					+ priority : "default");
		}
		return ctxName;
	}

	private String getTransactionSessionId() {
		if (sequence.useSameJSessionForSteps()) {
			return sequence.getSessionId();
		} else {
			IContextMaintainer transactionContextMaintainer = getTransactionContextMaintainer();
			if (transactionContextMaintainer != null) {
				return transactionContextMaintainer.getTransactionSessionId();
			}
			return null;
		}
	}

	private void setTransactionSessionId(HttpState state) {
		if (sequence.useSameJSessionForSteps())
			return;

		IContextMaintainer transactionContextMaintainer = getTransactionContextMaintainer();
		if (transactionContextMaintainer != null) {
			transactionContextMaintainer.setTransactionSessionId(state);
		}
	}

	public String[] getConnectorNames() {
		try {
			if (!projectName.equals("")) {
				Project p = getTargetProject(projectName);
				List<Connector> v = new ArrayList<Connector>(p.getConnectorsList());
				v = GenericUtils.cast(sort((Vector<?>) v, true));
				String[] connectorNames = new String[v.size() + 1];
				connectorNames[0] = "";
				int i = 0;
				for (Connector connector : v) {
					connectorNames[i + 1] = connector.getName();
					i++;
				}
				return connectorNames;
			}
		} catch (EngineException e) {
		}
		return new String[] {};
	}

	public String[] getTransactionNames() {
		try {
			if (!projectName.equals("") && !connectorName.equals("")) {
				Project p = getTargetProject(projectName);
				Connector connector = p.getConnectorByName(connectorName);
				List<Transaction> v = new ArrayList<Transaction>(connector.getTransactionsList());
				v = GenericUtils.cast(sort((Vector<?>) v, true));
				String[] transactionNames = new String[v.size() + 1];
				transactionNames[0] = "";
				int i = 0;
				for (Transaction transaction : v) {
					transactionNames[i + 1] = transaction.getName();
					i++;
				}
				return transactionNames;
			}
		} catch (EngineException e) {
		}
		return new String[] {};
	}

	public void importVariableDefinition() throws EngineException {
		Transaction targetTransaction = getTargetTransaction();
		if (targetTransaction != null && targetTransaction instanceof TransactionWithVariables) {
			importVariableDefinition(targetTransaction);
		}
	}

	public Transaction getTargetTransaction() throws EngineException {
		Project p = getTargetProject(projectName);
		Connector connector = (connectorName.equals("") ? p.getDefaultConnector() : p
				.getConnectorByName(connectorName));
		Transaction targetTransaction = (transactionName.equals("") ? connector.getDefaultTransaction() : connector
				.getTransactionByName(transactionName));
		return targetTransaction;
	}
	
	protected byte[] executeMethod() throws IOException, URIException, MalformedURLException, EngineException {
		Header[] requestHeaders, responseHeaders = null;
		byte[] result = null;
		String contents = null;
		int statuscode = -1;

		if (sequence.runningThread.bContinue) {
			Engine.logBeans.debug("(TransactionStep) Executing method - " + method.getName() + "("
					+ method.getPath() + ")");

			String ts = sequence.context.statistics.start(EngineStatistics.EXECUTE_SEQUENCE_CALLS);

			try {
				requestHeaders = method.getRequestHeaders();
				if (Engine.logBeans.isTraceEnabled())
					Engine.logBeans.trace("(TransactionStep) Request headers :\n"
							+ Arrays.asList(requestHeaders).toString());

				statuscode = doExecuteMethod();

				Engine.logBeans.debug("(TransactionStep) Status: " + method.getStatusLine().toString());

				// stores transaction sessionId in context maintainer
				setTransactionSessionId(httpState);

				responseHeaders = method.getResponseHeaders();
				if (Engine.logBeans.isTraceEnabled())
					Engine.logBeans.trace("(TransactionStep) Response headers:\n"
							+ Arrays.asList(responseHeaders).toString());

				if (statuscode != -1) {
					InputStream in = method.getResponseBodyAsStream();
					if (in != null) {
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						byte[] buf = new byte[1024];
						int len;
						while ((len = in.read(buf)) > 0) {
							bos.write(buf, 0, len);
						}
						result = bos.toByteArray();
						in.close();
						bos.close();
					}

					if (Engine.logBeans.isTraceEnabled()) {
						contents = new String((result != null) ? result : new byte[] {});
						Engine.logBeans.trace("(TransactionStep) Response content:\n" + contents);
					}

				}

				if (statuscode >= 300) {
					throw new EngineException("(TransactionStep) HTTP response returned status :"
							+ ((method != null) ? method.getStatusLine().toString()
									: String.valueOf(statuscode)));
				}
			} finally {
				if (method != null)
					method.releaseConnection();

				sequence.context.statistics.stop(ts, sequence.getCurrentChildStep() != 0);
			}
		}
		return result;
	}

	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);

		String version = element.getAttribute("version");

		if (version == null) {
			String s = XMLUtils.prettyPrintDOM(element);
			EngineException ee = new EngineException(
					"Unable to find version number for the database object \"" + getName() + "\".\nXML data: "
							+ s);
			throw ee;
		}

		try {
			if (VersionUtils.compare(version, "6.0.3") < 0) {
				String projectName = (String) XMLUtils.findPropertyValue(element, "projectName");
				// Handle wrong project name
				if (projectName.equals("")) {
					projectName = "unknown_project";
				}
				
				String connectorName = (String) XMLUtils.findPropertyValue(element, "connectorName");
				// If the default connector has been set, find the explicit default connector name
				if (connectorName.equals("")) {
					NodeList connectorsNodeList = element.getOwnerDocument().getElementsByTagName("connector");
					Node connectorNode = XMLUtils.findNodeByAttributeValue(connectorsNodeList, "default", "true");
					if (connectorNode != null) {
						connectorName = (String) XMLUtils.findPropertyValue((Element) connectorNode, "name");
					}
					else {
						throw new EngineException("Unable to find the default connector for the project '" + projectName + "'");
					}
				}
				
				String transactionName = (String) XMLUtils.findPropertyValue(element, "transactionName");
				// If the default transaction has been set, find the explicit default transaction name
				if (transactionName.equals("")) {
					NodeList connectorsNodeList = element.getOwnerDocument().getElementsByTagName("connector");
					int nlLen = connectorsNodeList.getLength();
					for (int i = 0; i < nlLen; i++) {
						Element connectorElement = (Element) connectorsNodeList.item(i);
						String connectorNameElement = (String) XMLUtils.findPropertyValue(connectorElement, "name");
						if (connectorName.equals(connectorNameElement)) {
							NodeList transactionsNodeList = connectorElement.getElementsByTagName("transaction");
							Node transactionNode = XMLUtils.findNodeByAttributeValue(transactionsNodeList, "default", "true");
							if (transactionNode != null) {
								transactionName = (String) XMLUtils.findPropertyValue((Element) transactionNode, "name");
								break;
							}
							throw new EngineException("Unable to find the default transaction for the connector '" +
									connectorName + "' from project '" + projectName + "'");
						}
					}
				}
				
				String sourceTransaction = projectName + SequenceStep.SOURCE_SEPARATOR + connectorName
						+ SequenceStep.SOURCE_SEPARATOR + transactionName;

				setSourceTransaction(sourceTransaction);

				hasChanged = true;
				Engine.logBeans.warn("[SequenceStpe] The object \"" + getName()
						+ "\" has been updated to version 6.0.3; source transaction: " + sourceTransaction);
			}
		} catch (Exception e) {
			throw new EngineException("Unable to migrate the source definition for CallTransaction step \""
					+ getName() + "\".", e);
		}
	}

	@Override
	public Element toXml(Document document) throws EngineException {
		Element element = super.toXml(document);

		// Storing the transaction WSDL type
		try {
			Element wsdlTypeElement = document.createElement("wsdltype");
			if (wsdlType != null) {
				CDATASection cDATASection = document.createCDATASection(wsdlType);
				wsdlTypeElement.appendChild(cDATASection);
				element.appendChild(wsdlTypeElement);
			}
		} catch (NullPointerException e) {
			// Silently ignore
		}

		return element;
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("sourceTransaction")) {
			List<String> transactionsList = new ArrayList<String>();

			try {
				List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
				for (String projectName : projectNames) {
					Project project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
					
					List<Connector> connectors = project.getConnectorsList();
					for (Connector connector : connectors) {
						String connectorName = connector.getName();

						List<Transaction> transactions = connector.getTransactionsList();

						for (Transaction transaction : transactions) {
							transactionsList.add(projectName + SOURCE_SEPARATOR + connectorName
									+ SOURCE_SEPARATOR + transaction.getName());
						}
					}
				}
			} catch (EngineException e) {
				// Just ignore, should never happen
			}

			return transactionsList.toArray(new String[] {});
		}
		return super.getTagsForProperty(propertyName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.twinsoft.convertigo.beans.core.RequestableStep#getSpecificLabel()
	 */
	@Override
	protected String getSpecificLabel() throws EngineException {
		String label = super.getSpecificLabel();
		if (label.equals("")) {// normal case
			// Check for project
			if (projectName.equals("")) {
				label = "! broken project !";
			} else {
				// Check for project
				try {
					if (ProjectUtils.existProjectSchemaReference(getProject(), projectName)) {
						Project p = getTargetProject(projectName);
						if (p == null) {
							label = "! broken project!";
						} else {
							// Check for connector
							Connector connector = connectorName.equals("") ? p.getDefaultConnector() : null;
							if (connector == null) {
								try {
									connector = p.getConnectorByName(connectorName);
								} catch (EngineException e) {
								}
							}
									
							if (connector == null) {
								label = "! broken connector !";
							} else {
								// Check for transaction
								Transaction transaction = (transactionName.equals("") ? connector
										.getDefaultTransaction() : connector.getTransactionByName(transactionName));
								if (transaction == null) {
									label = "! broken transaction !";
								}
								else {
									// Check for connection string
									StepSource source = new StepSource(this, connectionStringDefinition);
									if (source != null && !source.isEmpty()) {
										String csl = source.getLabel();
										if (csl.equals("! broken source !")) {
											label = " (! broken source in Connection string !)";
										}
									}
								}
							}
						}
					}
					else {
						label = "! broken reference !";
					}
				} catch (Exception e) {
					label = "! broken project !";
				}
			}
		}
		return label;
	}

	@Override
	public String toString() {
		String label = "";
		try {
			label += getLabel();
		} catch (EngineException e) { }
		return StringUtils.normalize("Call_" + getSourceTransaction()) + (label.equals("") ? "" : " ") + label;
	}
	
	public String getSourceTransaction() {
		return sourceTransaction;
	}

	public void setSourceTransaction(String sourceTransaction) {
		this.sourceTransaction = sourceTransaction;
		StringTokenizer st = new StringTokenizer(sourceTransaction, TransactionStep.SOURCE_SEPARATOR);
		try {
			projectName = st.nextToken();
			connectorName = st.nextToken();
			transactionName = st.nextToken();
		}
		catch (Exception e) {}
	}
	
	protected String getRequestableName() {
		return connectorName + "__" + transactionName;
	}

	@Override
	public void stepMoved(StepEvent stepEvent) {
		super.stepMoved(stepEvent);
		
		StepSource source = new StepSource(this, connectionStringDefinition);
		if (source != null && !source.isEmpty()) {
			source.updateTargetStep((Step)stepEvent.getSource(), (String)stepEvent.data);
		}
	}

	@Override
	protected IVariableContainer getTargetVariableContainer() throws EngineException {
		Transaction transaction = getTargetTransaction();
		if (transaction instanceof IVariableContainer tr) {
			return tr;
		}
		return null;
	}
	
}
