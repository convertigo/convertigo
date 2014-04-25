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

package com.twinsoft.convertigo.beans.transactions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JavaScriptException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.beans.extractionrules.HtmlExtractionRule;
import com.twinsoft.convertigo.beans.screenclasses.HtmlScreenClass;
import com.twinsoft.convertigo.beans.statements.ContextAddTextNodeStatement;
import com.twinsoft.convertigo.beans.statements.HTTPStatement;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.engine.AttachmentManager.Status;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.IdToXpathManager;
import com.twinsoft.convertigo.engine.ObjectWithSameNameException;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.parsers.IDownloader;
import com.twinsoft.convertigo.engine.parsers.events.AbstractEvent;
import com.twinsoft.convertigo.engine.parsers.events.IEvent;
import com.twinsoft.convertigo.engine.parsers.events.InputCheckEvent;
import com.twinsoft.convertigo.engine.parsers.events.InputSelectEvent;
import com.twinsoft.convertigo.engine.parsers.events.InputValueEvent;
import com.twinsoft.convertigo.engine.parsers.events.MouseEvent;
import com.twinsoft.convertigo.engine.parsers.events.NavigationBarEvent;
import com.twinsoft.convertigo.engine.parsers.events.SimpleEvent;
import com.twinsoft.convertigo.engine.parsers.triggers.AbstractTrigger;
import com.twinsoft.convertigo.engine.parsers.triggers.DocumentCompletedTrigger;
import com.twinsoft.convertigo.engine.parsers.triggers.TriggerXMLizer;
import com.twinsoft.convertigo.engine.parsers.triggers.WaitTimeTrigger;
import com.twinsoft.convertigo.engine.util.Base64v21;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.util.StringEx;

public class HtmlTransaction extends HttpTransaction {

	private static final long serialVersionUID = 5647248191056845273L;

	/**
	 * Asks the algorithm to detect (again) the current screen class,
	 * and to apply entry handler for the new detected screen class.
	 */
	public static final String RETURN_REDETECT = "redetect";

	/**
	 * Asks the algorithm to skip the current screen class,
	 * i.e. do not apply extraction rules.
	 */
	public static final String RETURN_SKIP = "skip";

	/**
	 * Asks the algorithm to continue on current screen class,
	 * i.e. do not redetect screen class and apply extraction rules.
	 */
	public static final String RETURN_CONTINUE = "continue";

	/**
	 * Asks the algorithm to accumulate results into the same XML document,
	 * i.e. to restart the algorithm.
	 */
	public static final String RETURN_ACCUMULATE = "accumulate";


	public static final String EVENT_ENTRY_HANDLER = "Entry";
	public static final String EVENT_EXIT_HANDLER = "Exit";

	transient private Document currentXmlDocument = null;

	transient private List<Statement> vStatements = new LinkedList<Statement>();

	transient public Statement currentStatement = null;

	/** Holds value of property stateFull **/
	private boolean stateFull = false;

	private TriggerXMLizer trigger = new TriggerXMLizer(new DocumentCompletedTrigger(1,60000));

	transient private boolean alreadyConnected = false;

	transient public boolean handlePriorities = true;

	public HtmlTransaction() {
		super();
		handlers = "// handlers are handled by Statements for HTML Transaction.";
		alreadyConnected = false;
	}

    @Override
	public HtmlTransaction clone() throws CloneNotSupportedException {
		HtmlTransaction clonedObject = (HtmlTransaction) super.clone();
		clonedObject.vStatements = new LinkedList<Statement>();
		clonedObject.alreadyConnected = false;
		clonedObject.handlePriorities = handlePriorities;
		return clonedObject;
	}

	/**
	 * @return the stateFull
	 */
	public boolean isStateFull() {
		return stateFull;
	}

	/**
	 * @param stateFull the stateFull to set
	 */
	public void setStateFull(boolean stateFull) {
		this.stateFull = stateFull;
	}

	public TriggerXMLizer getTrigger() {
		return trigger;
	}

	public void setTrigger(TriggerXMLizer trigger) {
		this.trigger = trigger;
	}

    @Override
	public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof Statement) {
			addStatement((Statement) databaseObject);
		}
		else {
			super.add(databaseObject);
		}
	}

    @Override
	public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof Statement) {
			removeStatement((Statement) databaseObject);
			((Statement) databaseObject).setParent(null);
		}
		else {
			super.remove(databaseObject);
		}
	}

	public List<Statement> getStatements() {
		checkSubLoaded();
		return sort(vStatements);
	}

	public boolean hasStatements() {
		checkSubLoaded();
		
		return (vStatements.size() > 0);
	}

	public HandlerStatement getHandlerStatement(String stName) {
		checkSubLoaded();
		
		HandlerStatement handlerStatement = null, st;
		for (int i=0; i < vStatements.size(); i++) {
			Object ob = vStatements.get(i);
			if (ob instanceof HandlerStatement) {
				st = (HandlerStatement)ob;
				if (st.getName().equals(stName)) {
					handlerStatement = st;
					break;
				}
			}
		}
		return handlerStatement;
	}

	public void addStatement(Statement statement) throws EngineException {
		checkSubLoaded();
		
		// Do not use getChildBeanName here because of ScHandlerStatement!!
		String newDatabaseObjectName = statement.getName();
		for (Statement st : vStatements) {
			if (newDatabaseObjectName.equals(st.getName())) {
				throw new ObjectWithSameNameException("Unable to add the statement \"" + newDatabaseObjectName + "\" to the html transaction class because a statement with the same name already exists.");
			}
		}

		vStatements.add(statement);

		statement.setParent(this);// do not call super.add otherwise it will generate an exception

		if (!statement.bNew && !handlePriorities) {
			statement.newPriority = 0;
			statement.hasChanged = true;
		}
		else if (handlePriorities && (statement.priority != 0)) {
			statement.priority = 0;
			statement.newPriority = 0;
			statement.hasChanged = true;
		}
	}

	public void removeStatement(Statement statement) {
		checkSubLoaded();
		
		vStatements.remove(statement);
	}

	transient private List<String> vExtractionRulesInited;
	transient private HtmlScreenClass screenClass;
	transient private String normalizedScreenClassName = "";
	transient private boolean bNotFirstLoop1 = false;
	transient private boolean bNotFirstLoop2 = false;
	transient private boolean bDispatching = false;
	transient private IEvent wcEvent;
	transient private List<AbstractEvent> wcFields;
	transient private AbstractTrigger wcTrigger;

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.transactions.HttpTransaction#parseInputDocument(com.twinsoft.convertigo.engine.Context)
	 */
    @Override
	public void parseInputDocument(com.twinsoft.convertigo.engine.Context context) {
		super.parseInputDocument(context);

		// TODO : voir si on garde cela
		// Overrides statefull mode using given __statefull request parameter
		NodeList stateNodes = context.inputDocument.getElementsByTagName("statefull");
		if (stateNodes.getLength() == 1) {
			Element node = (Element) stateNodes.item(0);
			String value  = node.getAttribute("value");
			if (!value.equals("")) {
				setStateFull(value.equalsIgnoreCase("true"));
			}
		}
		
		stateNodes = context.inputDocument.getElementsByTagName("webviewer-action");
		if (stateNodes.getLength() == 1 && stateNodes.item(0).getChildNodes().getLength() > 0) {
			Element webviewerAction = (Element)stateNodes.item(0);
			IdToXpathManager idToXpathManager = context.getIdToXpathManager();
			NodeList fieldNodes = context.inputDocument.getElementsByTagName("field");
			wcEvent = null;
			wcTrigger = null;
			wcFields = new ArrayList<AbstractEvent>(fieldNodes.getLength());
			for (int i=0;i < fieldNodes.getLength(); i++) {
				Element field = (Element)fieldNodes.item(i);
				String id = field.getAttribute("name");
				id = id.substring(id.lastIndexOf('_') + 1, id.length());
				String value = field.getAttribute("value");
				String xPath = idToXpathManager.getXPath(id);
				Node node = idToXpathManager.getNode(id);
				
				if (node != null && node instanceof Element) { 
					Element el = (Element)node;
					String tagname = el.getTagName();
					AbstractEvent evt = null;
					if (tagname.equalsIgnoreCase("input")) {
						String type = el.getAttribute("type");
						if (type.equalsIgnoreCase("checkbox") || type.equalsIgnoreCase("radio")) {
							evt = new InputCheckEvent(xPath, true, Boolean.valueOf(value).booleanValue());
						} else {
							evt = new InputValueEvent(xPath, true, value);
						}
					} else if (tagname.equalsIgnoreCase("select")) {
						evt = new InputSelectEvent(xPath, true, InputSelectEvent.MOD_INDEX, value.split(";"));
					} else if (tagname.equalsIgnoreCase("textarea")) {
						evt = new InputValueEvent(xPath, true, value); 
					}
					if (evt!=null) {
						wcFields.add(evt);
						Engine.logBeans.trace("Xpath: " + xPath + " will be set to :" + value);
					}
				}
			}

			NodeList actionNodes = webviewerAction.getElementsByTagName("action");
			if (actionNodes.getLength() == 1) {
				String action = ((Element)actionNodes.item(0)).getAttribute("value");
				if (action.equals("click")) {
					int screenX, screenY, clientX, clientY;
					screenX=screenY=clientX=clientY = -1;

					boolean ctrlKey, altKey, shiftKey, metKey;
					ctrlKey=altKey=shiftKey=metKey = false;

					short button = 0;
					String xPath = null;

					NodeList eventNodes = webviewerAction.getElementsByTagName("event");
					for(int i=0;i<eventNodes.getLength();i++){
						String name = ((Element)eventNodes.item(i)).getAttribute("name");
						String value = ((Element)eventNodes.item(i)).getAttribute("value");

						if(name.equals("x")||name.equals("y")||name.startsWith("client")){
							int valueInt = Integer.parseInt(value);
							if(name.equals("x")) screenX = valueInt;
							else if(name.equals("y")) screenY = valueInt;
							else if(name.equals("clientX")) clientX = valueInt;
							else if(name.equals("clientY")) clientY = valueInt;
						}else if(name.endsWith("Key")){
							boolean valueBool = Boolean.getBoolean(value);
							if(name.equals("ctrlKey")) ctrlKey = valueBool;
							else if(name.equals("altKey")) altKey = valueBool;
							else if(name.equals("shiftKey")) altKey = valueBool;
							else if(name.equals("metKey")) altKey = valueBool;
						}else if(name.equals("srcid")){
							xPath = idToXpathManager.getXPath(value);
						}
					}
					if(xPath!=null){
						wcEvent = new MouseEvent(xPath, action,
								screenX, screenY, clientX, clientY,
								ctrlKey, altKey, shiftKey, metKey, button);
						Engine.logBeans.debug("Created an click event from webviewer-action on : " +xPath);
					}
				}else if(action.startsWith("navbar_")){
					action = action.substring(7, action.length());
					wcEvent = new NavigationBarEvent(action);
				}else{
					String xPath = null;
					NodeList eventNodes = webviewerAction.getElementsByTagName("event");
					for(int i=0;i<eventNodes.getLength();i++){
						String name = ((Element)eventNodes.item(i)).getAttribute("name");
						String value = ((Element)eventNodes.item(i)).getAttribute("value");
						if(name.equals("srcid")) xPath = idToXpathManager.getXPath(value);
					}
					if(xPath!=null)	wcEvent = new SimpleEvent(xPath, action);
				}
				bDispatching = (wcEvent!=null);
				if(bDispatching && wcTrigger==null)
					// wcTrigger = new  WaitTimeTrigger(2000);
					// wcTrigger = new  XpathTrigger("*", 10000);
					// wcTrigger = new  DocumentCompletedTrigger(1, 10000);
					wcTrigger = getTrigger().getTrigger();
			}

		}
	}

    @Override
	public void setStatisticsOfRequestFromCache() {
		context.statistics.add(EngineStatistics.GET_CURRENT_SCREEN_CLASS, 0);
		context.statistics.add(EngineStatistics.APPLY_EXTRACTION_RULES, 0);
		context.statistics.add(EngineStatistics.HTTP_CONNECT, 0);
		context.statistics.add(EngineStatistics.GET_XUL_DOCUMENT, 0);
	}
    
    @Override
	public void runCore() throws EngineException {
		try {
			HtmlConnector connector = (HtmlConnector)parent;
			vExtractionRulesInited = new LinkedList<String>();

			if (!runningThread.bContinue) {
				return;
			}
			
			if (isStateFull() || connector.getHtmlParser().isConnected()){
				alreadyConnected = true;
			}
			// TODO
			//else{
			//	context.httpState = null; // clear cookies
			//}

			if (!alreadyConnected) {
				connector.setCurrentXmlDocument(null);
				applyUserRequest(connector);
			} else {
				setCurrentXmlDocument(connector.getHtmlParser().getDom(context));
			}

			if (bDispatching) {
				applyUserEvents();
			}

			bNotFirstLoop1 = false;
			bNotFirstLoop2 = false;

			if (!runningThread.bContinue) {
				return;
			}

			if (isContentTypeHTML()) {
				latestCalledHandler = null;
				do {
					do {
						String t = context.statistics.start(EngineStatistics.GET_CURRENT_SCREEN_CLASS);
						try {
							screenClass = ((HtmlConnector) connector).getCurrentScreenClass();

							normalizedScreenClassName = StringUtils.normalize(screenClass.getName());
							context.lastDetectedObject = screenClass;
							score +=1;
							Engine.logBeans.info("Detected screen class: '" + screenClass.getName() + "'");

							// The 2 next lines was removed at r19919
							//String connectionString = "[" + context.contextID + "] Screen class detected: "+ screenClass.getName() + ", project: " + context.projectName + ", transaction: " + getName();
							//Engine.connectionsLog.message(connectionString);

							// We fire engine events only in studio mode.
							if (Engine.isStudioMode()) {
								Engine.theApp.fireObjectDetected(new EngineEvent(screenClass));
							}
						}
						finally {
							context.statistics.stop(t, bNotFirstLoop1);
						}
						
						// We execute the entry handler for the detected screen class
						executeHandler(EVENT_ENTRY_HANDLER, ((RequestableThread) Thread.currentThread()).javascriptContext);
						bNotFirstLoop1 = true;
						
						// while re detecting, a new page could be loaded by async client javascript, if this happens, 
						// we refresh our current dom. Normally this is done by each statement, refreshing the dom here seems to be useless
						// except in the case explained above. We rely on the the DOM cache to speed up things as the
						// isDirty flag would not be set in most of the cases
						setCurrentXmlDocument(connector.getHtmlParser().getDom(context));
					} while (runningThread.bContinue && (handlerResult.equalsIgnoreCase(RETURN_REDETECT)));

					if (!handlerResult.equalsIgnoreCase(RETURN_SKIP)) {

						// We fire engine events only in studio mode.
						if (Engine.isStudioMode()) {
							//Engine.theApp.fireObjectDetected(new EngineEvent(blockFactory));
						}

						if (!runningThread.bContinue) {
							return;
						}

						if (currentXmlDocument == null) {
							throw new EngineException(alreadyConnected ? "Connector did not reconnect, please verify transaction statefull mode.":"" + " Current document is null. Can not apply extraction rules.");
						}

						applyExtractionRules(screenClass, bNotFirstLoop2);
						Engine.logBeans.debug("(HtmlTransaction) Extraction rules executed ...");

					}

					if (!runningThread.bContinue) {
						return;
					}

					// We execute the exit handler for the current screen class
					executeHandler(EVENT_EXIT_HANDLER, ((RequestableThread) Thread.currentThread()).javascriptContext);
					bNotFirstLoop2 = true;

				} while (runningThread.bContinue && (handlerResult.equalsIgnoreCase(RETURN_ACCUMULATE)));

				if (!runningThread.bContinue) {
					return;
				}
			}
			else if(currentXmlDocument!=null){
				XMLUtils.copyDocument(currentXmlDocument, context.outputDocument);
			} else {
				throw new EngineException("Connector doesn't retrieve xmlizable content. Verify your url.");
			}
		}
		finally {
			alreadyConnected = false;
			//restoreVariablesDefinition();
			restoreVariables();

			if ((runningThread == null) || (!runningThread.bContinue)) {
				Engine.logBeans.warn("(HtmlTransaction) The transaction \"" + getName() + "\" has been successfully interrupted.");
			}
			else {
				Engine.logBeans.debug("(HtmlTransaction) The transaction \"" + getName() + "\" has successfully finished.");
			}
		}
	}

	public void applyUserRequest(Object object) throws EngineException {
		final HtmlConnector connector = (HtmlConnector) parent;

		if (!runningThread.bContinue) {
			return;
		}

		Cookie[] cookies = new Cookie[]{};
		final byte[][] httpData = {null};
		try {
			Document dom = null;
			TriggerXMLizer triggerXML = null;
			
			String t = context.statistics.start(EngineStatistics.HTTP_CONNECT);

			try {
				// Retrieving Data
				if (object instanceof HtmlConnector) {
					Engine.logBeans.trace("(HtmlTransaction) Retrieving data from connector ...");
					httpData[0] = connector.getData(context);
					triggerXML = trigger;
				}
				if (object instanceof HTTPStatement) {
					Engine.logBeans.trace("(HtmlTransaction) Retrieving data from http statement ...");
					connector.prepareForHTTPStatement(context);
					httpData[0] = connector.getData(context);
					triggerXML = ((HTTPStatement)object).getTrigger();
				}
				if (!alreadyConnected)
					alreadyConnected = true;
	
				Engine.logBeans.trace("(HtmlTransaction) Data retrieved!");
				//Engine.logBeans.trace("(HtmlTransaction) Data html:\n"+ new String(httpData));
			}
			finally {
				context.statistics.stop(t);
			}

			// Retrieve cookies from HttpClient response
			if (connector.handleCookie) {
				cookies = connector.getCookies();
				if (Engine.logBeans.isTraceEnabled()) {
					Engine.logBeans.trace("(HtmlTransaction) cookies from HttpClient response :" + Arrays.asList(cookies).toString());
				}
			}

			if (!runningThread.bContinue) {
				return;
			}
			
			// Parse response : push data into HTML parser
			if (!isContentTypeHTML()) {
				final String[] filename = {connector.getReferer()};
				int id;
				if ((id = filename[0].indexOf('?')) != -1) {
					filename[0] = filename[0].substring(0, id);
				}
				if ((id = filename[0].lastIndexOf('/')) != -1) {
					filename[0] = filename[0].substring(id+1);
				}
				connector.getHtmlParser().downloadRequest(context, new IDownloader() {
					public String getUri() {
						return connector.getReferer();
					}
					
					public String getReferrer() {
						return connector.getReferer();
					}
					
					public String getFilename() {
						return filename[0];
					}
					
					public byte[] getData(long timeout, long threshold) {
						return httpData[0];
					}
					
					public String getContentType() {
						return HtmlTransaction.this.getContentType();
					}
					
					public void cancel() {}
					
					public Status getStatus() {
						return Status.direct;
					}
				});
				return;
			}
			
			dom = connector.parseData(httpData[0], connector.getReferer(), connector.getCharset(), triggerXML.getTrigger());

			if (!runningThread.bContinue)
				return;

			setCurrentXmlDocument(dom);
			
			if (Engine.logBeans.isTraceEnabled())
				Engine.logBeans.trace("(HtmlTransaction) Parser result dom:\n"+ XMLUtils.prettyPrintDOM(dom));

			if (!runningThread.bContinue)
				return;
			
			// Modify input document if needed
			modifyInputDocument();

		} catch (IOException e) {
			throw new EngineException("An IO exception occured while trying to connect to the URL.\nURL: " + connector.sUrl + "\nPost query: " + connector.postQuery, e);
		}
		catch(Exception e) {
			throw new EngineException("An unexpected exception occured while trying to get the document via HTTP.", e);
		}		

		if (!runningThread.bContinue)
			return;
		
		// Applying handler
		executeHandler(EVENT_DATA_RETRIEVED, ((RequestableThread) Thread.currentThread()).javascriptContext);
	}

	public void modifyInputDocument() throws Exception {
		Element root, statementVariablesElement, form, formElement;
		NodeList nodeList, formList, formElementList;
		String statement_name, form_name, form_action, http_form_name;
		Node importedForm;
		int len;

		// Inspect resulting parsed document to retrieve Form data values
		//formList = currentXmlDocument.getElementsByTagName("form");
		formList = currentXmlDocument.getElementsByTagName("FORM"); // tagname in uppercase with MOZPARSER!!
		len = formList.getLength();
		if (len > 0) {
			Engine.logBeans.trace("(HtmlTransaction) Modifying input document for HTTPStatement");
			statementVariablesElement = null;
			statement_name = "none";

			if (currentStatement != null)
				statement_name = currentStatement.getName();

			root = (Element)context.inputDocument.getElementsByTagName("input").item(0);
			if (root != null) {
				nodeList = root.getElementsByTagName("httpstatement-variables");
				if ((nodeList != null) && (nodeList.getLength() > 0)) {
					statementVariablesElement = (Element)nodeList.item(0);
					//root.removeChild(statementVariablesElement);
					//statementVariablesElement = null;
				}
				if (statementVariablesElement == null) {
					statementVariablesElement = context.inputDocument.createElement("httpstatement-variables");
					//statementVariablesElement.setAttribute("statement", statement_name);
					root.appendChild(statementVariablesElement);
				}
				/*if (statementVariablesElement != null)
					statementVariablesElement.setAttribute("statement", statement_name);*/

				for (int i=0; i<len; i++) {
					formElement = null;
					form = (Element)formList.item(i);
					form_action = form.getAttribute("action");
					form_name = form.getAttribute("name");
					http_form_name = "http-"+ form_name;

					formElementList = statementVariablesElement.getElementsByTagName(http_form_name);
					if ((formElementList != null) && (formElementList.getLength() > 0)) {
						formElement = (Element)formElementList.item(0);
						statementVariablesElement.removeChild(formElement);
						formElement = null;
					}
					if (formElement == null) {
						formElement = context.inputDocument.createElement(http_form_name);
						formElement.setAttribute("name", form_name);
						formElement.setAttribute("action", form_action);
						formElement.setAttribute("statement", statement_name);
						statementVariablesElement.appendChild(formElement);
					}

					if (formElement != null) {
						importedForm = context.inputDocument.importNode(form, true);
						//statementVariablesElement.appendChild(importedForm);
						formElement.appendChild(importedForm);
					}
				}
			}

			Engine.logBeans.trace("(HtmlTransaction) Input document modified for HTTPStatement");
			if (Engine.logBeans.isTraceEnabled()) {
				Document printDoc = (Document)Visibility.Logs.replaceVariables(getVariablesList(), context.inputDocument);
				Engine.logBeans.trace("(HtmlTransaction) \n"+ XMLUtils.prettyPrintDOM(printDoc));
			}
		}
	}

    @Override
	protected void insertObjectsInScope() throws EngineException {
		super.insertObjectsInScope();
	}

	public synchronized void setCurrentXmlDocument(Document document) {
		currentXmlDocument = document;
		((HtmlConnector)parent).setCurrentXmlDocument(currentXmlDocument);
	}

    @Override
	protected void executeHandlerCore(String handlerType, Context javascriptContext) throws EcmaError, EvaluatorException, JavaScriptException, EngineException {
		if (!EVENT_ENTRY_HANDLER.equals(handlerType) && !EVENT_EXIT_HANDLER.equals(handlerType)) {
			super.executeHandlerCore(handlerType, javascriptContext);
			return;
		}
		handlerResult = "";
		handlerName = "on" + normalizedScreenClassName + handlerType;
		Engine.logBeans.trace("(HtmlTransaction) Search of the " + handlerType + " handler (" + handlerName + ")");

		HandlerStatement handlerStatement = getHandlerStatement(handlerName);

		if ((handlerStatement == null) || ((handlerStatement != null) && !handlerStatement.isEnable())) {
			if (handlerStatement == null)
				Engine.logBeans.debug("(HtmlTransaction) No " + handlerType + " handler (" + handlerName + ") found for the screen class '" + screenClass.getName() + "'; searching for the transaction default handler...");
			else
				Engine.logBeans.debug("(HtmlTransaction) handler (" + handlerName + ") disabled for the screen class '" + screenClass.getName() + "'; searching for the transaction default handler...");
			
			handlerName = "onTransactionDefaultHandler" + handlerType;
			handlerStatement = getHandlerStatement(handlerName);

			if (handlerStatement == null) {
				Engine.logBeans.debug("(HtmlTransaction) No " + handlerType + " transaction default handler found");
				return;
			}

			if (!handlerStatement.isEnable()) {
				Engine.logBeans.debug("(HtmlTransaction) " + handlerType + " transaction default handler disabled");
				return;
			}

			Engine.logBeans.debug("(HtmlTransaction) Execution of the " + handlerType + " transaction default handler");
		}
		else {
			Engine.logBeans.debug("(HtmlTransaction) Execution of the " + handlerType + " handler (" + handlerName + ") for the screen class '" + screenClass.getName() + "'");
		}

		Engine.logBeans.debug(">> " + handlerName + "()");

		// See ticket #819 (Fix the convertigo statistics for HTML connector)
//		String th = "";
//		if (bStatistics) {
//			th = context.statistics.start(EngineStatistics.APPLY_SCREENCLASS_HANDLERS);
//		}
		handlerStatement.checkSymbols();
		
		Object returnedValue = null;
		try {
			handlerStatement.execute(javascriptContext, scope);
			testLoop(handlerStatement);
			returnedValue = handlerStatement.getReturnedValue();
		}
		finally {
			// See ticket #819 (Fix the convertigo statistics for HTML connector)
//			if (bStatistics) {
//				context.statistics.stop(th, bNotFirstLoop1);
//			}
		}

		if (returnedValue instanceof org.mozilla.javascript.Undefined) {
			handlerResult = "";
		}
		else if (returnedValue instanceof String) {
			handlerResult = (String) returnedValue;
			if (EVENT_ENTRY_HANDLER.equals(handlerType)) {
				if ((!handlerResult.equalsIgnoreCase(RETURN_REDETECT)) && (!handlerResult.equalsIgnoreCase(RETURN_SKIP)) && (!handlerResult.equalsIgnoreCase(RETURN_CONTINUE)) && (!handlerResult.equals(""))) {
					EngineException ee = new EngineException(
							"Wrong returned code for the " + handlerType + " handler: " + handlerResult + ".\n" +
							"Transaction: \"" + getName() + "\"\n" +
							"Screen class: \"" + screenClass.getName() + "\""
					);
					throw ee;
				}
			}
			else {
				if (!handlerResult.equalsIgnoreCase(RETURN_ACCUMULATE) && !handlerResult.equals("")) {
					EngineException ee = new EngineException(
							"Wrong returned code for the " + handlerType + " handler: " + handlerResult + ".\n" +
							"Transaction: \"" + getName() + "\"\n" +
							"Screen class: \"" + screenClass.getName() + "\""
					);
					throw ee;
				}
			}
		}
		else {
			EngineException ee = new EngineException(
					"Wrong returned code for the " + handlerType + " handler: " + handlerResult + ".\n" +
					"Transaction: \"" + getName() + "\"\n" +
					"Screen class: \"" + screenClass.getName() + "\"" +
					"Returned value: \"" + returnedValue.toString() + "\"" +
					"Handler result: \"" + handlerResult.toString() + "\""
			);
			throw ee;
		}

		Engine.logBeans.debug("<< " + handlerName + "(): \"" + handlerResult + "\"");
	}

    @Override
	protected void executeSimpleHandlerCore(String handlerType, Context myJavascriptContext) throws EcmaError, EvaluatorException, JavaScriptException, EngineException {

		handlerName = "on" + handlerType;
		Engine.logBeans.trace("(HtmlTransaction) Searching the " + handlerType + " handler (" + handlerName + ")");
		HandlerStatement handlerStatement = getHandlerStatement(handlerName);

		handlerResult = "";
		if (handlerStatement != null) {
			
			if (!handlerStatement.isEnable())
				return;
			
			Engine.logBeans.debug("(HtmlTransaction) Execution of the " + handlerType + " handler  (" + handlerName + ") for the transaction '" + getName() + "'");
			handlerStatement.execute(myJavascriptContext, scope);
			Object returnedValue = handlerStatement.getReturnedValue();
			if (returnedValue instanceof org.mozilla.javascript.Undefined) {
				handlerResult = "";
			}
			else {
				handlerResult = returnedValue.toString();
			}
		}
		else {
			Engine.logBeans.debug("(HtmlTransaction) No " + handlerType + " handler  (" + handlerName + ") found");
		}
	}

	public void applyExtractionRules(HtmlScreenClass screenClass, boolean bNotFirstLoop) throws EngineException {
		String t = context.statistics.start(EngineStatistics.APPLY_EXTRACTION_RULES);

		try {
			// We apply the extraction rules for this screen class
			int extractionRuleInitReason;

			List<ExtractionRule> vExtractionRules = screenClass.getExtractionRules();

			for (ExtractionRule extractionRule : vExtractionRules) {
				HtmlExtractionRule htmlExtractionRule = (HtmlExtractionRule) extractionRule;

				if (!runningThread.bContinue) break;

				if (!extractionRule.isEnabled()) {
					Engine.logBeans.trace("(HtmlTransaction) Skipping the extraction rule \"" + extractionRule.getName() + "\" because it has been disabled.");
					continue;
				}

				Engine.logBeans.debug("(HtmlTransaction) Applying the extraction rule \"" + extractionRule.getName() + "\"");
				extractionRule.checkSymbols();
				
				String extractionRuleQName = extractionRule.getQName();
				if (vExtractionRulesInited.contains(extractionRuleQName)) {
					extractionRuleInitReason = ExtractionRule.ACCUMULATING;
				}
				else {
					extractionRuleInitReason = ExtractionRule.INITIALIZING;
					vExtractionRulesInited.add(extractionRuleQName);
				}

				Engine.logBeans.trace("(HtmlTransaction) Initializing extraction rule (reason = " + extractionRuleInitReason + ")...");
				extractionRule.init(extractionRuleInitReason);

				// We fire engine events only in studio mode.
				if (Engine.isStudioMode()) {
					Engine.theApp.fireObjectDetected(new EngineEvent(extractionRule));
				}

				boolean hasMatched = htmlExtractionRule.apply(currentXmlDocument, context);
				if (hasMatched) {
					htmlExtractionRule.addToScope(scope);
					Engine.logBeans.trace("(HtmlTransaction) Applying extraction rule '" + extractionRule.getName() + "': matching");
				}
				else
					Engine.logBeans.trace("(HtmlTransaction) Applying extraction rule '" + extractionRule.getName() + "': not matching");

				// We fire engine events only in studio mode.
				if (Engine.isStudioMode()) {
					Engine.logBeans.debug("(HtmlTransaction) Step reached after having applied the extraction rule \"" + extractionRule.getName() + "\".");
					Engine.theApp.fireStepReached(new EngineEvent(extractionRule));
				}

				extractionRule = null;
			}

			vExtractionRules = null;
		}
		finally {
			context.statistics.stop(t, bNotFirstLoop);
		}
	}

    @Override
    public String migrateToXsdTypes() {
    	String xsdTypes = null;
    	try {
			// Retrieve backup wsdlTypes
			String backupWsdlTypes = getBackupWsdlTypes();
			if (backupWsdlTypes != null) {
				String types = backupWsdlTypes;
				if (isDefault) {
	    			/* Generate again : correct bug of ref in group */
	    			types = generateWsdlType(null);
	    			
			    	if (!isPublicAccessibility()) {
		    			HtmlConnector connector = (HtmlConnector)getParent();
		    			String prefix = (connector.isDefault ? "":connector.getName() + "__");
		    			String transactionName = StringUtils.normalize(prefix + getName(), true) + "Response";
		    			/* remove complexType for transaction*/
		    			int i = types.indexOf("<xsd:complexType name=\""+ transactionName +"\">");
		    			if (i != -1) {
			    			int j = types.indexOf("</xsd:complexType>", i);
			    			if (j != -1)
			    				types = types.substring(0, i) + types.substring(j+ "</xsd:complexType>\n".length());
		    			}
			    	}
	    		}
		    	// Replace xxxResponse by yyy__xxxResponseData
				StringEx sx = new StringEx(types);
		    	sx.replace("\""+ getName() + "Response\"", "\"" + getXsdTypePrefix() + getName() + "ResponseData\"");
		    	sx.replace(":"+ getName() + "Response\"", ":" + getXsdTypePrefix() + getName() + "ResponseData\"");
		    	sx.replace("__"+ getName() + "Response\"", "__" + getName() + "ResponseData\"");
		    	sx.replaceAll("tns:", getProject().getName() + "_ns:");
		    	xsdTypes = generateXsdRequestData() + " " + sx.toString();
			}
    	}
		catch (Exception e) {
			Engine.logBeans.warn("Unable to migrate to XSD types for requestable \""+ getName() +"\"", e);
		}
    	return xsdTypes;
    }
    
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.Transaction#generateWsdlType(org.w3c.dom.Document)
	 */
    @Override
	public String generateWsdlType(Document document) throws Exception {
		
		HtmlConnector connector = (HtmlConnector)getParent();
		String prefix = getXsdTypePrefix();
		
		// First regenerates wsdltype for default transaction : will contains all types!!
		if (!isDefault) {
			HtmlTransaction defaultTransaction = (HtmlTransaction)connector.getDefaultTransaction();
			defaultTransaction.generateWsdlType(document);
			defaultTransaction.hasChanged = true;
		}

		// Retrieve extraction rules schemas
		List<HtmlScreenClass> screenClasses = connector.getAllScreenClasses();
		Map<String, ScreenClass> ht = new Hashtable<String, ScreenClass>(screenClasses.size());
		String normalizedScreenClassName;
		int i;

		for (ScreenClass screenClass : screenClasses) {
			normalizedScreenClassName = StringUtils.normalize(screenClass.getName());
			ht.put(normalizedScreenClassName, screenClass);
		}

		Hashtable<String, String> names = new Hashtable<String, String>();
		Hashtable<String, String> types = new Hashtable<String, String>();
		
		List<String> schemas = new LinkedList<String>();
		screenClass = connector.getDefaultScreenClass();
		if (screenClass != null) {
			addExtractionRuleShemas(names, types, schemas, screenClass);
		}
		
		// Retrieve statements schemas
        for (Statement statement: getStatements()) {
        	addStatementSchemas(schemas, statement);
        }
		
		// Construct transaction schema
		String transactionName = StringUtils.normalize(prefix + getName(), true) + "Response";
		
		String 	schema = "<?xml version=\"1.0\" encoding=\""+ getEncodingCharSet() +"\" ?>\n";
		schema += "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n";
		
		String all, obSchema;
		all = "";
		for (i=0; i<schemas.size(); i++) {
			obSchema = schemas.get(i);
			all += obSchema;
		}
		
		if (isDefault) {
			String group = "";
			String groupName = StringUtils.normalize(connector.getName(), true) + "Types";
			group += "<xsd:group name=\""+groupName+"\">\n";
			group += "<xsd:sequence>\n";
			group += all;
			group += "</xsd:sequence>\n";
			group += "</xsd:group>\n";
			
			schema += "<xsd:complexType name=\""+ transactionName +"\">\n";
			schema += "<xsd:sequence>\n";
			schema += "<xsd:element minOccurs=\"0\" maxOccurs=\"1\" name=\"error\" type=\"p_ns:ConvertigoError\"/>\n";
			schema += "<xsd:group minOccurs=\"0\" maxOccurs=\"1\" ref=\"p_ns:"+groupName+"\"/>\n";
			schema += "</xsd:sequence>\n";
			schema += "</xsd:complexType>\n";
			
			schema += group;
			for (Enumeration<String> e = types.keys(); e.hasMoreElements();) {
				String typeSchema = (String) types.get(e.nextElement());
				schema += typeSchema;
			}
		}
		else {
			schema += "<xsd:complexType name=\""+ transactionName +"\">\n";
			schema += "<xsd:sequence>\n";
			schema += "<xsd:element minOccurs=\"0\" maxOccurs=\"1\" name=\"error\" type=\"p_ns:ConvertigoError\"/>\n";
			schema += all;
			schema += "</xsd:sequence>\n";
			schema += "</xsd:complexType>\n";
		}
		schema += "</xsd:schema>\n";

		String prettyPrintedText = XMLUtils.prettyPrintDOM(schema);
		int index = prettyPrintedText.indexOf("<xsd:schema") + "<xsd:schema".length();
		index = prettyPrintedText.indexOf('\n', index);
		prettyPrintedText = prettyPrintedText.substring(index + 1);
		prettyPrintedText = prettyPrintedText.substring(0,prettyPrintedText.indexOf("</xsd:schema>"));
		//prettyPrintedText = removeTab(prettyPrintedText);
		return prettyPrintedText;
	}

    private void addStatementSchemas(List<String> schemas, Statement statement) {
		if (statement.isEnable()) {
			if (statement instanceof ContextAddTextNodeStatement) {
				String eltName = ((ContextAddTextNodeStatement)statement).getTagName();
				String stSchema = "<xsd:element minOccurs=\"0\" maxOccurs=\"1\" name=\""+eltName+"\" type=\"xsd:string\"/>\n";
				if (!schemas.contains(stSchema)) {
					schemas.add(stSchema);
				}
			} else if (statement instanceof StatementWithExpressions) {
				for (Statement st: ((StatementWithExpressions)statement).getStatements()) {
					addStatementSchemas(schemas, st);
				}
			}
		}
	}

	private void addExtractionRuleShemas(Map<String, String> names, Map<String, String> types, List<String> schemas, HtmlScreenClass screenClass) throws Exception {
		HtmlExtractionRule htmlExtractionRule = null;
		String typeSchema, typeName;
		String erSchema, erSchemaEltName, erSchemaEltNSType;
		Map<String, String> type;
		
		if (screenClass != null) {
			for (ExtractionRule extractionRule: screenClass.getExtractionRules()) {
				htmlExtractionRule = (HtmlExtractionRule) extractionRule;
				if (htmlExtractionRule.isEnabled()) {
					erSchemaEltName = htmlExtractionRule.getSchemaElementName();
					erSchemaEltNSType = htmlExtractionRule.getSchemaElementNSType("p_ns");
					if (!names.containsKey(erSchemaEltName)) {
						names.put(erSchemaEltName, erSchemaEltNSType);
					} else {
						typeSchema = (String)names.get(erSchemaEltName);
						if (!typeSchema.equals(erSchemaEltNSType)) {
							throw new Exception("Transaction may generate at least two extraction rules named '"+erSchemaEltName+"' with different type : '"+typeSchema+"' and '"+erSchemaEltNSType+"'.\nPlease correct by changing tagname or name if tagname is empty");
						}
					}

					erSchema = htmlExtractionRule.getSchema("p_ns");
					if (!schemas.contains(erSchema)) {
						schemas.add(erSchema);
					}
					type = htmlExtractionRule.getSchemaTypes();
					for (Entry<String, String> entry : type.entrySet()) {
						typeName = entry.getKey();
						typeSchema = entry.getValue();
						types.put(typeName, typeSchema);
					}
				}
			}
			
	        List<ScreenClass> visc = screenClass.getInheritedScreenClasses();
	        for (ScreenClass inheritedScreenClass : visc) {
	        	addExtractionRuleShemas(names, types, schemas, (HtmlScreenClass) inheritedScreenClass);
	        }
		}
	}
	
	protected String removeTab(String s) throws IOException {
		String s2 = "";
		BufferedReader reader = new BufferedReader(new StringReader(s));
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.substring(4);
			s2 += line + "\n";
		}
		return s2;
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#configure(org.w3c.dom.Element)
	 */
    @Override
	public void configure(Element element) throws Exception {
		super.configure(element);

		try {
			String attribute = element.getAttribute("handlePriorities");
			if (attribute.equals("")) throw new Exception("Missing \"handlePriorities\" attribute.");
			handlePriorities = new Boolean(attribute).booleanValue();
			if (!handlePriorities)
				hasChanged = true;

		}
		catch(Exception e) {
			handlePriorities = false;
			Engine.logBeans.warn("The "+getClass().getName() +" object \"" + getName() + "\" has been updated to version \"4.0.1\"");
			hasChanged = true;
		}
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#write(java.lang.String)
	 */
    @Override
	public void write(String databaseObjectQName) throws EngineException {
		boolean b = handlePriorities;
		if (hasChanged && !isImporting)
			handlePriorities = true;
		try {
			super.write(databaseObjectQName);
		}
		catch (EngineException e) {
			handlePriorities = b;
			throw e;
		}
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#toXml(org.w3c.dom.Document)
	 */
    @Override
	public Element toXml(Document document) throws EngineException {
		Element element =  super.toXml(document);

		// Storing the transaction "handlePriorities" flag
		element.setAttribute("handlePriorities", new Boolean(handlePriorities).toString());

		return element;
	}

	protected String getContentType(){
		String contentType = null;
		Header[] heads = context.getResponseHeaders();

		for(int i=0;i<heads.length && contentType==null ;i++)
			if(heads[i].getName().equalsIgnoreCase("Content-Type"))
				contentType = heads[i].getValue();

		return contentType==null?"":contentType;
	}

	protected boolean isContentTypeHTML(){
		String contentType = getContentType();

		// content type may be empty , default it to text/
		if (contentType.length() == 0)
			contentType = "text/";

		return (contentType.indexOf("text/") != -1);
	}

	protected Document makeBlob(byte[] data){
		Document document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		Element blob = document.createElement("blob");
		document.appendChild(blob);

		Header[] heads = context.getResponseHeaders();

		for(int i=0;i<heads.length;i++){
			if(heads[i].getName().equals("Content-Type") ||
					heads[i].getName().equals("Content-Length") ){
				blob.setAttribute(heads[i].getName(), heads[i].getValue());
			}
		}
		blob.setAttribute("Referer", ((HtmlConnector)context.getConnector()).getReferer());

		blob.appendChild(document.createTextNode(Base64v21.encodeBytes(data)));

		return document;
	}

	protected void applyUserEvents() throws EngineException {
		HtmlConnector connector = (HtmlConnector)parent;

		if (!runningThread.bContinue)
			return;

		try {
			Document dom = null;

			for(int i=0;i<wcFields.size();i++){
				AbstractEvent event = (AbstractEvent)wcFields.get(i);
				connector.dispatchEvent(event, context, new WaitTimeTrigger(0, false));
			}
			String comment = (wcEvent instanceof AbstractEvent)?((AbstractEvent)wcEvent).getXPath():"on browser";
			Engine.logBeans.trace("(HtmlTransaction) Dispatch Event: " + comment);
			boolean dispatch = connector.dispatchEvent(wcEvent, context, wcTrigger);
			dom = connector.getHtmlParser().getDom(context);
			Engine.logBeans.trace("(HtmlTransaction) Event: " + comment + (dispatch?"":" not") +" dispatched");

			setCurrentXmlDocument(dom);
			if (Engine.logBeans.isTraceEnabled())
				Engine.logBeans.trace("(HtmlTransaction) Parse result dom:\n"+ XMLUtils.prettyPrintDOM(dom));
		}
		catch(Exception e) {
			throw new EngineException("An unexpected exception occured while trying to get the document via HTTP.", e);
		}		
		finally {
			bDispatching = false;
		}

		if (!runningThread.bContinue)
			return;
	}
	
	private transient int handlerExecutionCounter = 0;
	private transient String latestCalledHandler = null;
	private transient long[] handlersCallTimeWindow;
	private transient int handlerExecutionCountLimit;
	private transient long handlerExecutionMaxTime;
	
	public void testLoop(HandlerStatement handler) throws EngineException {
		if (handler != null) {
			handlerName = handler.getName();
			if (handler.preventFromLoops()) {
				if (!handlerName.equals(latestCalledHandler)) {
					Engine.logBeans.trace("first loop for " + handlerName);
					
					handlerExecutionCounter = 1;
					latestCalledHandler = handlerName;
					
					handlerExecutionCountLimit = 3;
					handlerExecutionMaxTime = 150;
					
					if (handlerName.endsWith("Exit")) {
						handlerExecutionCountLimit = 7;
						handlerExecutionMaxTime = 2000;
					}
					
					Engine.logBeans.trace("handlerExecutionCountLimit=" + handlerExecutionCountLimit);
					Engine.logBeans.trace("handlerExecutionMaxTime=" + handlerExecutionMaxTime);

					handlersCallTimeWindow = new long[handlerExecutionCountLimit];
					Arrays.fill(handlersCallTimeWindow, System.currentTimeMillis());
				} else {
					Engine.logEngine.trace("loop call for " + handlerName + " loop=" + handlerExecutionCounter);
					
					handlerExecutionCounter++;
					
					long handlerCurrentCallTime = System.currentTimeMillis();
					handlersCallTimeWindow[(handlerExecutionCounter - 1) % handlerExecutionCountLimit] = handlerCurrentCallTime;
					
					long handlerFirstCallInWindowTime = handlersCallTimeWindow[handlerExecutionCounter % handlerExecutionCountLimit];

					long deltaTime = handlerCurrentCallTime - handlerFirstCallInWindowTime;
					
					Engine.logBeans.trace("handlerCurrentCallTime      =" + handlerCurrentCallTime);
					Engine.logBeans.trace("handlerFirstCallInWindowTime=" + handlerFirstCallInWindowTime);
					Engine.logBeans.trace("deltaTime=" + deltaTime);
					
					if ((handlerExecutionCounter > handlerExecutionCountLimit) &&
							(deltaTime < handlerExecutionMaxTime)) {
						throw new EngineException(
								"An unexpected handler loop has been detected: "
										+ handlerName
										+ " has been called "
										+ handlerExecutionCounter
										+ " times in "
										+ deltaTime
										+ " ms; you can authorize this behaviour by setting the \"Infinite loop protection\" handler's property to \"false\"");
					}
				}
			}
		}
	}
}