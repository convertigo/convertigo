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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.DefaultBlockFactory;
import com.twinsoft.convertigo.beans.common.PanelBlockFactory;
import com.twinsoft.convertigo.beans.common.TabBox;
import com.twinsoft.convertigo.beans.common.XMLRectangle;
import com.twinsoft.convertigo.beans.connectors.ConnectionException;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.beans.screenclasses.JavelinScreenClass;
import com.twinsoft.convertigo.beans.sna.Nptui;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.twinj.Javelin;
import com.twinsoft.twinj.iJavelin;

public class JavelinTransaction extends TransactionWithVariables {

	private static final long serialVersionUID = 555167441622133279L;

	/**
     * Asks the algorithm to detect (again) the current screen class,
     * and to apply entry handler for the new detected screen class.
     */
    public static final String RETURN_REDETECT = "redetect";
    
    /**
     * Asks the algorithm to skip the current screen class,
     * i.e. do not create blocks and do not apply extraction rules.
     */
    public static final String RETURN_SKIP = "skip";
    
    /**
     * Asks the algorithm to accumulate results into the same XML document,
     * i.e. to restart the algorithm.
     */
    public static final String RETURN_ACCUMULATE = "accumulate";
    
    /**
     * Asks the algorithm to cancel the applyUserRequest() method.
     */
    public static final String RETURN_BYPASS = "bypass";
    
    /**
     * Asks the algorithm to process like an empty return.
     */
    public static final String RETURN_CONTINUE = "continue";
    
    
    public static final String EVENT_ENTRY_HANDLER = "Entry";
	public static final String EVENT_EXIT_HANDLER = "Exit";
    
	public void setStatisticsOfRequestFromCache() {
		context.statistics.add(EngineStatistics.GET_CURRENT_SCREEN_CLASS, 0);
		context.statistics.add(EngineStatistics.GET_JAVELIN_OBJECT, 0);
		context.statistics.add(EngineStatistics.APPLY_USER_REQUEST, 0);
		context.statistics.add(EngineStatistics.APPLY_BLOCK_FACTORY, 0);
		context.statistics.add(EngineStatistics.APPLY_EXTRACTION_RULES, 0);
		context.statistics.add(EngineStatistics.APPLY_SCREENCLASS_HANDLERS, 0);
	}
	
	/**
	 * Defines if the 'blocks' node should be removed from the response or not
	 * (useful in web services context).
	 */
	private boolean removeBlocksNode = false;
    
	public boolean isRemoveBlocksNode() {
		return removeBlocksNode;
	}

	public void setRemoveBlocksNode(boolean removeBlocksNode) {
		this.removeBlocksNode = removeBlocksNode;
	}

	/**
	 * Defines the delay in seconds before firing timeout during
	 * attempts to connect.
	 */
	private int timeoutForConnect = 20000;
    
	public int getTimeoutForConnect() {
		return timeoutForConnect;
	}
    
	public void setTimeoutForConnect(int timeoutForConnect) {
		this.timeoutForConnect = timeoutForConnect;
	}
    
    /**
     * Defines the delay in seconds before firing timeout during
     * attempts to retrieve datastable.
     */
    private int timeoutForDataStable = 10000;
    
    public int getTimeoutForDataStable() {
        return timeoutForDataStable;
    }
    
    public void setTimeoutForDataStable(int timeoutForDataStable) {
        this.timeoutForDataStable = timeoutForDataStable;
    }
    
    /**
     * Defines if the extraction rules should be executed in panels or not.
     */
    private boolean executeExtractionRulesInPanels = true;
    
    public boolean isExecuteExtractionRulesInPanels() {
		return executeExtractionRulesInPanels;
	}

	public void setExecuteExtractionRulesInPanels(
			boolean executeExtractionRulesInPanels) {
		this.executeExtractionRulesInPanels = executeExtractionRulesInPanels;
	}

	
	public JavelinTransaction() {
        super();
    }
    
    transient private Vector<String> vExtractionRulesInited;
    
    /** Holds value of property dataStableThreshold. */
    private int dataStableThreshold = 300;
    
    /** Getter for property dataStableThreshold.
     * @return Value of property dataStableThreshold.
     */
    public int getDataStableThreshold() {
        return dataStableThreshold;
    }
    
    /** Setter for property dataStableThreshold.
     * @param dataStableThreshold New value of property dataStableThreshold.
     */
    public void setDataStableThreshold(int dataStableThreshold) {
        this.dataStableThreshold = dataStableThreshold;
    }
    
    transient private JavelinScreenClass screenClass;
    transient private String normalizedScreenClassName = "";
    transient private boolean bNotFirstLoop1 = false;
    transient private boolean bNotFirstLoop2 = false;
    transient private Vector<Collection<Block>> blocks;
    
    /** Holds value of property onlyOnePage. */
    private boolean onlyOnePage;
    
    public void runCore() throws EngineException {
        try {
        	JavelinConnector connector = (JavelinConnector) parent;
        	Javelin javelin = ((JavelinConnector) connector).javelin;
        	
			vExtractionRulesInited = new Vector<String>(32);

			boolean bDocumentLogScreenDumps = (EnginePropertiesManager.getProperty(PropertyName.DOCUMENT_LOG_SCREEN_DUMPS).equals("true") ? true : false);
			
			if (handlerResult.equalsIgnoreCase(RETURN_CANCEL)) {
				// must detect screen class before exit
				String t = context.statistics.start(EngineStatistics.GET_CURRENT_SCREEN_CLASS);

                try {
                	/*
					if ((Engine.objectsProvider == null) && !javelin.isConnected()) {
						throw new ConnectionException("The emulator has been disconnected! See the emulator logs for more details...");
					}
					*/

                    screenClass = ((JavelinConnector) connector).getCurrentScreenClass();
                    
					normalizedScreenClassName = StringUtils.normalize(screenClass.getName());
                    context.lastDetectedObject = screenClass;
                    score +=1;
                    
                    // for compatibility with older javelin projects, set the legacy lastDetectedScreenClass context property
                    context.lastDetectedScreenClass = screenClass;
                    
                    Engine.logContext.info("Detected screen class: '" + screenClass.getName() + "'");

					if (bDocumentLogScreenDumps) {
						StringBuffer screenDump = new StringBuffer("");
						int sw = javelin.getScreenWidth();
						int sh = javelin.getScreenHeight();
						for (int i = 0 ; i < sh ; i++) {
							screenDump.append(javelin.getString(0, i, sw) + "\n");
						}
                    	
						Engine.logContext.info("Screen dump for screen class '" + screenClass.getName() + "':\n" + screenDump.toString());
					}
                    
                    // We fire engine events only in studio mode.
                    if (Engine.isStudioMode()) {
                        Engine.theApp.fireObjectDetected(new EngineEvent(screenClass));
                    }
                    return;
                }
                finally {
                    context.statistics.stop(t, bNotFirstLoop1);
                }
			}
			
			
			if (!handlerResult.equalsIgnoreCase(RETURN_BYPASS)) {
				applyUserRequest(javelin);
			}

            blocks = new Vector<Collection<Block>>(16);

            bNotFirstLoop1 = false;
            bNotFirstLoop2 = false;
            
            if (!runningThread.bContinue) {
            	return;
            }
		
            do {
                do {
                	String t = context.statistics.start(EngineStatistics.GET_CURRENT_SCREEN_CLASS);

                    try {
                    	/*
    					if ((Engine.objectsProvider == null) && !javelin.isConnected()) {
    						throw new ConnectionException("The emulator has been disconnected! See the emulator logs for more details...");
    					}
    					*/

                        screenClass = (JavelinScreenClass) ((JavelinConnector) connector).getCurrentScreenClass();
                        
    					normalizedScreenClassName = StringUtils.normalize(screenClass.getName());
                        context.lastDetectedObject = screenClass;
                        score +=1;
                        
                        // for compatibility with older javelin projects, set the legacy lastDetectedScreenClass context property
                        context.lastDetectedScreenClass = screenClass;
                        
                        Engine.logContext.info("Detected screen class: '" + screenClass.getName() + "'");

    					if (bDocumentLogScreenDumps) {
    						StringBuffer screenDump = new StringBuffer("");
    						int sw = javelin.getScreenWidth();
    						int sh = javelin.getScreenHeight();
    						for (int i = 0 ; i < sh ; i++) {
    							screenDump.append(javelin.getString(0, i, sw) + "\n");
    						}
                        	
    						Engine.logContext.info("Screen dump for screen class '" + screenClass.getName() + "':\n" + screenDump.toString());
    					}
                        
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

                } while (runningThread.bContinue && (handlerResult.equalsIgnoreCase(RETURN_REDETECT)));

                if (!handlerResult.equalsIgnoreCase(RETURN_SKIP)) {
                    BlockFactory blockFactory = screenClass.getBlockFactory();


                    // We fire engine events only in studio mode.
                    if (Engine.isStudioMode()) {
                        Engine.theApp.fireObjectDetected(new EngineEvent(blockFactory));
                    }

                    if (!runningThread.bContinue) {
                    	return;
                    }

                    applyBlockFactory(screenClass, blockFactory, javelin, bNotFirstLoop2);
                    Engine.logContext.debug("(JavelinTransaction) Block factory executed...");

                    if (!runningThread.bContinue) {
                    	return;
                    }

                    // first extraction rules execution on the block factory
                    applyExtractionRules(screenClass, blockFactory, javelin, bNotFirstLoop2);
                    
                    if (executeExtractionRulesInPanels) {
	                    // search panels and apply extraction rules on their content blocks
	                    searchPanelsAndApplyExtractionRules(screenClass, blockFactory, javelin, bNotFirstLoop2);
                    }
                    
                    Engine.logContext.debug("(JavelinTransaction) Extraction rules executed ...");

                    blocks.addElement(blockFactory.getAllBlocks());
                    Engine.logContext.debug("(JavelinTransaction) Blocks added to output");
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

            // Adding screen resolution information and cursor position information to the XML document
			Element documentElement = context.outputDocument.getDocumentElement();
			documentElement.setAttribute("screenWidth", Integer.toString(javelin.getScreenWidth()));
			documentElement.setAttribute("screenHeight", Integer.toString(javelin.getScreenHeight()));
			documentElement.setAttribute("cursorLine", Integer.toString(javelin.getCurrentLine()));
			documentElement.setAttribute("cursorColumn", Integer.toString(javelin.getCurrentColumn()));

            // Generating the blocks XML tags
			renderBlocksToXml(blocks);

			// Removing the "blocks" encompassing node (useful only in on-the-fly cases) if required
			// and move all sub blocks node to the root element.
        	if (isRemoveBlocksNode()) {
        		NodeList childNodes = documentElement.getChildNodes();
        		int len = childNodes.getLength();
        		for (int i = 0; i < len; i++) {
        			// Use index 0 because blocks node is removed at each loop round!
            		Node node = childNodes.item(0);
            		if (node.getNodeName().equalsIgnoreCase("blocks")) {
            			NodeList subNodesBlocks = node.getChildNodes();
            			int len2 = subNodesBlocks.getLength();
            			while (len2 > 0) {
            				documentElement.appendChild(subNodesBlocks.item(0));
                			subNodesBlocks = node.getChildNodes();
                			len2 = subNodesBlocks.getLength();
            			}
            			documentElement.removeChild(node);
            		}
        		}
        	}
        }
        finally {
			if (sessionMustBeDestroyed) {
                Engine.logContext.debug("(JavelinTransaction) Destroying the current session");
                Engine.theApp.sessionManager.removeSession(context.contextID);
			}

			if ((runningThread == null) || (!runningThread.bContinue)) {
				Engine.logContext.warn("(JavelinTransaction) The transaction \"" + getName() + "\" has been successfully interrupted.");
        	}
        	else {
				Engine.logContext.debug("(JavelinTransaction) The transaction \"" + getName() + "\" has successfully finished.");
        	}
        }
    }

	protected void insertObjectsInScope() throws EngineException {
		super.insertObjectsInScope();
		
		Connector connector = (Connector) parent;
		
		// Insert the Javelin object in the script scope
		Scriptable jsJavelin = org.mozilla.javascript.Context.toObject(((JavelinConnector) connector).javelin, scope);
		scope.put("javelin", scope, jsJavelin);
		
		// Insert the dataStableTimeout object in the script scope
		Scriptable jsDataStableTimeout = org.mozilla.javascript.Context.toObject(new Integer(timeoutForDataStable), scope);
		scope.put("timeout", scope, jsDataStableTimeout);
		
		// Insert the dataStableThreshold object in the script scope
		Scriptable jsDataStableThreshold = org.mozilla.javascript.Context.toObject(new Integer(dataStableThreshold), scope);
		scope.put("threshold", scope, jsDataStableThreshold);
	}
    
	protected void executeHandlerCore(String handlerType, org.mozilla.javascript.Context javascriptContext) throws EcmaError, EvaluatorException, JavaScriptException, EngineException {
		if (!EVENT_ENTRY_HANDLER.equals(handlerType) && !EVENT_EXIT_HANDLER.equals(handlerType)) {
			super.executeHandlerCore(handlerType, javascriptContext);
			return;
		}

		/*
		JavelinConnector connector = (JavelinConnector) parent;
		Javelin javelin = ((JavelinConnector) connector).javelin;
		
		if ((Engine.objectsProvider == null) && !javelin.isConnected()) {
			throw new ConnectionException("The emulator has been disconnected! See the emulator logs for more details...");
		}
		*/

		handlerName = "on" + normalizedScreenClassName + handlerType;
		Engine.logContext.debug("(JavelinTransaction) Search of the " + handlerType + " handler (" + handlerName + ")");
		Object object = scope.get(handlerName, scope);
		Engine.logContext.debug("(JavelinTransaction) Rhino returned: [" + object.getClass().getName() + "] " + object.toString());
        
		if (!(object instanceof Function)) {
			Engine.logContext.debug("(JavelinTransaction) No " + handlerType + " handler found for the screen class '" + screenClass.getName() + "'; searching for the transaction default handler...");

			handlerName = "onTransactionDefaultHandler" + handlerType;
			object = scope.get(handlerName, scope);

			if (!(object instanceof Function)) {
				Engine.logContext.debug("(JavelinTransaction) No " + handlerType + " transaction default handler found");
				return;
			}

			Engine.logContext.debug("(JavelinTransaction) Execution of the " + handlerType + " transaction default handler");
		}
		else {
			Engine.logContext.debug("(JavelinTransaction) Execution of the " + handlerType + " handler for the screen class '" + screenClass.getName() + "'");
		}

		Engine.logContext.debug(">> " + handlerName + "()");
		function = (Function) object;

		String th = context.statistics.start(EngineStatistics.APPLY_SCREENCLASS_HANDLERS);
    
		Object returnedValue = null;
		try {
			returnedValue = function.call(javascriptContext, scope, scope, null);
		}
		finally {
			context.statistics.stop(th, bNotFirstLoop1);
		}
    
		if (returnedValue instanceof org.mozilla.javascript.Undefined) {
			handlerResult = "";
		}
		else if (returnedValue instanceof String) {
			handlerResult = (String) returnedValue;
			if ("".equals(handlerResult) || RETURN_CONTINUE.equals(handlerResult)) {
				// handle emptry string "" and "continue" string as an undefined result, for entry and exit handlers
				handlerResult = "";
			} else if (EVENT_ENTRY_HANDLER.equals(handlerType)) {
				if ((!handlerResult.equalsIgnoreCase(RETURN_REDETECT)) && (!handlerResult.equalsIgnoreCase(RETURN_SKIP))) {
					EngineException ee = new EngineException(
					 "Wrong return code for the " + handlerType + " handler: " + handlerResult + ".\n" +
						"Transaction: \"" + getName() + "\"\n" +
						"Screen class: \"" + screenClass.getName() + "\""
					);
					throw ee;
				}
			} else {
				if (!handlerResult.equalsIgnoreCase(RETURN_ACCUMULATE)) {
					EngineException ee = new EngineException(
						"Wrong return code for the " + handlerType + " handler: " + handlerResult + ".\n" +
						"Transaction: \"" + getName() + "\"\n" +
						"Screen class: \"" + screenClass.getName() + "\""
					);
					throw ee;
				}
			}
		}
		else {
			EngineException ee = new EngineException(
				"Wrong return code for the " + handlerType + " handler: " + handlerResult + ".\n" +
				"Transaction: \"" + getName() + "\"\n" +
				"Screen class: \"" + screenClass.getName() + "\"" +
				"Returned value: \"" + returnedValue.toString() + "\"" +
				"Handler result: \"" + handlerResult.toString() + "\""
			);
			throw ee;
		}

		Engine.logContext.debug("<< " + handlerName + "(): \"" + handlerResult + "\"");
	}
    
    /**
     * Compares the value to the actual value of the field specified
     * by the line and column coordinates.
     *
     * @param blockName the block name to analize
     * @param value the value of the field to be compared to
     *
     * @return true if the value is different, false otherwise.
     */
    private boolean isFieldValueDifferent(String blockName, String value) {
    	Block block = context.previousFields.get(blockName);

    	if (block == null) {
			Engine.logContext.debug("(JavelinTransaction) Field " + blockName + " not found!");
			return true;
    	}
    	else {
			String text = block.getText();
			text = DefaultBlockFactory.rightTrim(text);
			if (value.compareTo(text) == 0) {
				return false;
			}
			else {
				Engine.logContext.debug("(JavelinTransaction) Value: '" + value + "' differs from : '" + text + "'");
				return true;
			}
    	}
    }
    
    transient private boolean sessionMustBeDestroyed;
    
    public void applyUserRequest(iJavelin javelin) throws EngineException {
        boolean fieldIsAutoEnter = false;
		String currentField = "null";
		sessionMustBeDestroyed = false;
        
		String t = context.statistics.start(EngineStatistics.APPLY_USER_REQUEST);

        /*
		if ((Engine.objectsProvider == null) && !javelin.isConnected()) {
			throw new ConnectionException("The emulator has been disconnected! See the emulator logs for more details...");
		}
		*/

        try {
            Engine.logContext.debug("(JavelinTransaction) Applying user request");

            // We apply the XML command to the Javelin object.
            if (context.inputDocument == null) {
                // Nothing to do: the user request is not relevant for this screen.
                Engine.logContext.debug("(JavelinTransaction) Request handling aborted: no user request");
                return;
            }

            NodeList nodeList;
			Node node, nodeAttribute;
			NamedNodeMap nodeAttributes;

			long documentSignature = 0;
			nodeList = context.inputDocument.getElementsByTagName("document-signature");
			if (nodeList.getLength() > 0) {
				node = nodeList.item(0);
				String value = ((Element) node).getAttribute("value");
				try {
					documentSignature = Long.parseLong(value);
				}
				catch(NumberFormatException e) {
					Engine.logContext.debug("(JavelinTransaction) Wrong document signature: " + value);
				}
			}
			Engine.logContext.debug("(JavelinTransaction) Received document signature:      " + documentSignature);
			Engine.logContext.debug("(JavelinTransaction) Last received document signature: " + context.documentSignatureReceived);
			Engine.logContext.debug("(JavelinTransaction) Last sent document signature:     " + context.documentSignatureSent);

            if (documentSignature < context.documentSignatureReceived) {
                // The user is trying to replay a previous request (probably by using the back
            	// functionality from the browser...): this is forbidden.
                Engine.logContext.warn("(JavelinTransaction) Request handling aborted: \"back\" protection");
    			context.outputDocument.getDocumentElement().setAttribute("back-attempt", "true");
                return;
            }
			
			nodeList = context.inputDocument.getElementsByTagName("current-field");
			if (nodeList.getLength() > 0) {
				node = nodeList.item(0);
				nodeAttributes = node.getAttributes();
				nodeAttribute = nodeAttributes.getNamedItem("name");
				currentField = nodeAttribute.getNodeValue();
			}
            
			nodeList = context.inputDocument.getElementsByTagName("action");
            if (nodeList.getLength() == 1) {
                Block block;
                String action, blockName;
                
                node = nodeList.item(0);
                nodeAttributes = node.getAttributes();
                nodeAttribute = nodeAttributes.getNamedItem("name");
                action = nodeAttribute.getNodeValue();
                action = action.trim();
                
                // Refreshing current XML document
                if (action.equalsIgnoreCase("convertigo_refresh")) {
                    Engine.logContext.debug("(JavelinTransaction) Refresh required");
                }
                else if (action.startsWith("convertigo_bench")) {
                	long sleepTime = 1000;
                	
                	if (action.length() > "convertigo_bench".length()) {
    					String s = action.substring(16);
                		try {
                			sleepTime = Long.parseLong(s);
                		}
                		catch(NumberFormatException e) {
                			// Ignore
                		}
                	}

                	Engine.logContext.debug("(JavelinTransaction) Bench option: sleep during " + sleepTime + " second(s)");
                    
                	try {
                    	Thread.sleep(sleepTime);
                    }
                    catch(InterruptedException e) {}
                }
                // Destroy the current session
                else if (action.equalsIgnoreCase("convertigo_destroy_session")) {
                    Engine.logContext.debug("(JavelinTransaction) Requiring destroy of the current session");
                	sessionMustBeDestroyed = true;
                }
//                // Disconnecting: is better handled inside the handlers
//                else if (action.equalsIgnoreCase("convertigo_disconnect")) {
//                    Engine.logContext.debug.debug("Disconnection required");
//                    javelin.disconnect();
//                }
                // Reconnecting
                else if (action.equalsIgnoreCase("convertigo_reconnect")) {
                    Engine.logContext.debug("(JavelinTransaction) Reconnection required");

					javelin.disconnect();
					if (javelin.isConnected()) {
						throw new ConnectionException("Unable to disconnect the session! See the emulator logs for more details...");
					}

					javelin.connect(timeoutForConnect);
					if (!javelin.isConnected()) {
						throw new ConnectionException("Unable to connect the session! See the emulator logs for more details...");
					}
					javelin.waitForDataStable(timeoutForDataStable, dataStableThreshold);
                }
                else {
                    if (javelin.getTerminalClass().equals(iJavelin.SNA) || javelin.getTerminalClass().equals(iJavelin.AS400)) {
                        // Special case for IBM emulators: before applying an action,
                        // do a RESET action on the emulator to clear a possible
                        // previous X System Status.
                        javelin.doAction("KEY_RESET");
                        Engine.logContext.debug("(JavelinTransaction) Action performed on the Javelin object: 'KEY_RESET'");
                    }
                    
                    // Applying fields only if sent document signature equals to the
                    // previously sent document signature
        			if ((documentSignature == 0) || (documentSignature == context.documentSignatureSent)) {
                        // Searching for fields to push into the Javelin object.
                        nodeList = context.inputDocument.getElementsByTagName("field");
                        int len = nodeList.getLength();
                        String value;
                        
                        // sort fields by line and column in the screen
                        Element elem;
                        ArrayList<ComparableFieldElement> liste = new ArrayList<ComparableFieldElement>();
                        for (int i = 0 ; i < len ; i++) {
                        	elem = (Element) nodeList.item(i);
                        	liste.add(new ComparableFieldElement(elem));
                        }
                        Engine.logContext.debug("(JavelinTransaction) Fields from inputDocument set in a list.");
                        Collections.sort(liste);
                        Engine.logContext.debug("(JavelinTransaction) Fields from inputDocument sorted.");

                        Iterator<ComparableFieldElement> it = liste.iterator();
                        while (it.hasNext()) {
                        	ComparableFieldElement cfElem = it.next();
                        	elem = cfElem.getFieldElement();
                        	value = elem.getAttribute("value");
                                                        
                            if (javelin.getTerminalClass().equals(iJavelin.VDX)) {
                                javelin.send(value);
                                Engine.logContext.debug("(JavelinTransaction) Characters sent to the Javelin object: '" + value + "'");
                            } else if (javelin.getTerminalClass().equals(iJavelin.VT)) {
                                javelin.send(value);
                                Engine.logContext.debug("(JavelinTransaction) VT Characters sent to the Javelin object: '" + value + "'");
    						} else {
                            	// Trying to find the requested block
    							blockName = elem.getAttribute("name");

    							Engine.logContext.debug("(JavelinTransaction) Analyzing field \"" + blockName + "\"");
    							
    							block = context.previousFields.get(blockName);
    							
    							int column = 0;
    							int line = 0;
    							
    							try {
									int index1 = blockName.indexOf("_c");
									int index2 = blockName.indexOf("_l");
									column = Integer.parseInt(blockName.substring(index1 + 2, index2));
									line = Integer.parseInt(blockName.substring(index2 + 2));
									Engine.logContext.debug("(JavelinTransaction) Cursor position retrieved from the field name \"" + blockName + "\"column: =" + column + " line=" + line);
								} catch(Exception e) {
									Engine.logContext.error("Unable to retrieve the cursor position from the field name \"" + blockName + "\"! Skipping it.", e);
									continue;
								}
    								
    							if (isFieldValueDifferent(blockName, value)) {
    								javelin.moveCursor(column, line);
    								Engine.logContext.debug("(JavelinTransaction) Cursor moved to column:" + column + ", line: " + line);
    	
    								// Delete the previous field value
    								if (javelin.getTerminalClass().equals(iJavelin.SNA) || javelin.getTerminalClass().equals(iJavelin.AS400)) {
    									javelin.doAction("KEY_ERASEEOF");
    								}
    								else if (javelin.getTerminalClass().equals(iJavelin.DKU)) {
    									javelin.doAction("ERAEOL");
    								}
    								
    								if (value.length() != 0) {
    									// Test if the field is numeric only
    									if ((block != null) && (block.attribute & iJavelin.AT_FIELD_NUMERIC) > 0) {
    										value = removeAllNonNumericChars(value);
    										Engine.logContext.warn("(JavelinTransaction) Numeric field. Non numeric chars may have been discarded ["+value+"]");
    									}

    									// Test if the field is autoenter and that its size equals to the field size
    									try {
    										if ((block != null) && (block.attribute & iJavelin.AT_AUTO_ENTER) > 0) {
    											if (value.length() == Integer.parseInt(block.getOptionalAttribute("size"))) {
    												fieldIsAutoEnter = true;										
    												Engine.logContext.debug("(JavelinTransaction) Field is auto enter, doAction will not be executed [" + value + "]");
    											}
    										}
    									} catch (Exception e) {
    										Engine.logContext.warn("(JavelinTransaction) Field is auto enter, but no size attribute present! [" + value + "]");
    									}
    	
    									javelin.send(value);
    									Engine.logContext.debug("(JavelinTransaction) Characters sent to the emulator on (" + column + ", " + line + "): '" + value + "'");
    								}
    								
    								// Update the block history if it is a non empty field
    								if ((block != null) && (value.length() != 0)) {
    									String historyBlock = block.getOptionalAttribute("history");
    									if ((block.type.equals("field")) && (historyBlock != null) && (historyBlock.equals("true")) && (value.length() > 0)) {
    										// TODO: paramétrer la liste des tagname à historiser
    										Vector<String> values = GenericUtils.cast(context.httpSession.getAttribute(block.tagName));
    										if (values == null) {
    											values = new Vector<String>(10);
    										}
    										if (!values.contains(value)) {
    											values.add(value);
    											context.httpSession.setAttribute(block.tagName, values);
    											Engine.logContext.debug("(JavelinTransaction) History: block '" + block.tagName + "' += '" + value + "'");
    										}
    									}
    								}

    								if (action.equalsIgnoreCase("KEY_FIELDPLUS")) {
    									Engine.logContext.debug("(JavelinTransaction) Action is KEY_FIELDPLUS while sending " + blockName + " field");
    									if (blockName.equalsIgnoreCase(currentField)) {
    										// The field we are handling is the current field.
    										// Do now the FIELD_PLUS action.
    										Engine.logContext.debug("(JavelinTransaction) We just sent the current field, and action is KEY_FIELDPLUS");
    										javelin.doAction("KEY_FIELDPLUS");
    										return;
    									}
    								}
    							}
                            }
                        }
                        
    					if (currentField != null) {
    						block = (Block) context.previousFields.get(currentField);
    						
                        	// we did not find the field in the previous fields : assume the current field is set on a static position. This is
                        	// legal as some applications rely on the cursor position event on static fields
							int index1 = currentField.indexOf("_c");
							int index2 = currentField.indexOf("_l");
							if ((index1 != -1) && (index2 != -1)) {
								int column = Integer.parseInt(currentField.substring(index1 + 2, index2));
								int line = Integer.parseInt(currentField.substring(index2 + 2));
								Engine.logContext.debug("(JavelinTransaction) Move cursor on Current field : Cursor position retrieved from the field name \"" + currentField + "\"column=" + column + " line=" + line);
								boolean moveDone = false;
								
								// if current field is numeric, put the cursor at the end of the field
								// searching field index
								int nbFields = javelin.getNumberOfFields();
								int i = 0;
								boolean found = false;
								while (i < nbFields && !found) {
									if (javelin.getFieldColumn(i) == column && javelin.getFieldLine(i) == line)
										found = true;
									else
										i++;
								}
								if (found) {
									// field index found
									if((javelin.getFieldAttribute(i) & iJavelin.AT_FIELD_NUMERIC) > 0) {
										// numeric field
										// put the cursor at the end of the field
										column = column + javelin.getFieldLength(i) -1;
										Engine.logContext.debug("(JavelinTransaction) Cursor is in a numeric field '" + currentField + "' ; moving cursor to the end of the field '" + currentField + "' at (" + column + ", " + line +")");
		                                javelin.moveCursor(column, line);
		                                Engine.logContext.debug("(JavelinTransaction) Moved cursor to (" + javelin.getCurrentColumn() + ", " + javelin.getCurrentLine() +")");
		                                moveDone = true;
									}
								}
								
								if (!moveDone) {
									// before moving the cursor to the current field, test if it has moved or not
									// and if the action is KEY_NPTUI or not
									if (javelin.getCurrentColumn() == column && javelin.getCurrentLine() == line && !action.equals("KEY_NPTUI")) {
										Engine.logContext.debug("(JavelinTransaction) Cursor has not moved from field '" + currentField + "' at (" + column + ", " + line +") and action is not \"KEY_NPTUI\" ; don't move cursor");
									} else {
										Engine.logContext.debug("(JavelinTransaction) Cursor has moved from field '" + currentField + "' or action is \"KEY_NPTUI\" ; moving cursor to field '" + currentField + "' at (" + column + ", " + line +")");
		                                javelin.moveCursor(column, line);
		                                Engine.logContext.debug("(JavelinTransaction) Moved cursor to (" + javelin.getCurrentColumn() + ", " + javelin.getCurrentLine() +")");
									}
								}
							}
                        }
                    }
        			else {
        				Engine.logContext.warn("(JavelinTransaction) Cancel applying fields because of document signature check");
        			}

        			// Storing last document signature received
        			context.documentSignatureReceived = documentSignature;

                    // Executing action
                    if (!fieldIsAutoEnter) {
                    	// Execute the action only if there were no AutoEnter fields...
                		if (action.equalsIgnoreCase("KEY_NPTUI")) {
                			// NPTUI action just wait as the moveCursor already triggered the HOST COMM. we only have
                			// to wait here
                			boolean bTimedOut = javelin.waitForDataStable(timeoutForDataStable, dataStableThreshold);
                			Engine.logContext.debug("(JavelinTransaction) Action performed on the Javelin object: '" + action + "'waitForDataStable returned :" + bTimedOut);
                		} else 	if (action.length() != 0) {
                        	javelin.doAction(action);
                        	Engine.logContext.debug("(JavelinTransaction) Action performed on the Javelin object: '" + action + "'");
                        	if (!action.equalsIgnoreCase("KEY_FIELDPLUS")) {
	                        	boolean bTimedOut = javelin.waitForDataStable(timeoutForDataStable, dataStableThreshold);
	                    		Engine.logContext.debug("(JavelinTransaction) WaitForDataStable() returned " + bTimedOut);
                        	} else 
	                    		Engine.logContext.debug("(JavelinTransaction) Action was KEY_FIELDPLUS, do not perform waitForDataStable" );
                    	} else {
                    		Engine.logContext.debug("(JavelinTransaction) Empty action string => action aborted");
                    	}
                    } else {
                		boolean bTimedOut = javelin.waitForDataStable(timeoutForDataStable, dataStableThreshold);
                		Engine.logContext.debug("(JavelinTransaction) WaitForDataStable() returned " + bTimedOut);
                    }
                }
            }
        }
        catch (Exception e) {
            Engine.logContext.error("Request handling aborted because of internal error.", e);
        }
        finally {
			context.statistics.stop(t);
        }
    }
    
    protected String removeAllNonNumericChars(String s) {
		String result = "";

		int len = s.length();
		if (len > 0) {
			// Append only leading '+' and '-' characters
			char c = s.charAt(0);
			if (Character.isDigit(c) || (c == '.') || (c == ',') || (c == '-') || (c == '+') || (c == ' ')) {
				result += c;
			}
			
			for (int k = 1 ; k < len ; k++) {
				c = s.charAt(k);
				if (Character.isDigit(c) || (c == '.') || (c == ',') || (c == ' ')) {
					result += c;
				}
			}
		}

		return result;
    }
    
    public void applyBlockFactory(JavelinScreenClass screenClass, BlockFactory blockFactory, iJavelin javelin, boolean bNotFirstLoop) {
    	String t = context.statistics.start(EngineStatistics.APPLY_BLOCK_FACTORY);
        
        try {
            blockFactory.make(javelin);
    		context.previousFields = blockFactory.fields; 
            
            // We fire engine events only in studio mode.
            if (Engine.isStudioMode()) {
                Engine.theApp.fireBlocksChanged(new EngineEvent(blockFactory));
                Engine.logContext.debug("(JavelinTransaction) Step reached after having applied the block factory.");
                Engine.theApp.fireStepReached(new EngineEvent(blockFactory));
            }
        }
        finally {
			context.statistics.stop(t, bNotFirstLoop);
        }
    }
    
    public void searchPanelsAndApplyExtractionRules(JavelinScreenClass screenClass, BlockFactory blockFactory, iJavelin javelin, boolean bNotFirstLoop) throws EngineException {
    	// if there are panel type blocks, apply extraction rules on the panel block children
        Block tmpBlock = null;
        try {
        	tmpBlock = blockFactory.getFirstBlock();
        } catch (NoSuchElementException e) {
        	Engine.logContext.trace("(JavelinTransaction) Can't apply extraction rules in panels : no block left in block factory.");
        	return;
        }
        
        PanelBlockFactory bf = null;
        
        while (tmpBlock != null) {
        	if (tmpBlock.type.equals("panel")
        		|| tmpBlock.type.equals("container") 
        		|| tmpBlock.type.equals("tabBox")) {
        		// creation of a BlockFactory containing the child blocks
        		try {
        			bf = PanelBlockFactory.cloneBlockFactory((DefaultBlockFactory)blockFactory);
        		} catch (CloneNotSupportedException e) {
        			throw new EngineException("(JavelinTransaction) Exception when cloning blockFactory", e);
        		}
        		
            	bf.setName("Panel_block_factory");
            	bf.make(tmpBlock);
            	
            	if (tmpBlock.type.equals("panel")) {
            		// if type = panel, apply extraction rules
            		Engine.logContext.trace("(JavelinTransaction) Applying extraction rules in a panel.");
            		applyExtractionRules(screenClass, bf, javelin, bNotFirstLoop);
            		Engine.logContext.trace("(JavelinTransaction) End applying extraction rules in a panel.");
            		
            		// replace panel content with blockFactory blocks
            		tmpBlock.clearOptionalChildren();
            		for(Block block : bf.list)
            			tmpBlock.addOptionalChildren(block);
            	} else if (tmpBlock.type.equals("container") || tmpBlock.type.equals("tabBox")) {
            		// if type = container or tabBox, recurse on this method to find inside panels
            		searchPanelsAndApplyExtractionRules(screenClass, bf, javelin, bNotFirstLoop);
            	}
        	}
        	// go on the next block
        	tmpBlock = blockFactory.getNextBlock(tmpBlock);
        }
    }
    
    public void applyExtractionRules(JavelinScreenClass screenClass, BlockFactory blockFactory, iJavelin javelin, boolean bNotFirstLoop) throws EngineException {
    	String t = context.statistics.start(EngineStatistics.APPLY_EXTRACTION_RULES);

        try {
            // We apply the extraction rules for this screen class to
            // the words list.
            Block block = null;
            JavelinExtractionRuleResult extractionRuleResult;
    		int extractionRuleInitReason;
            
            boolean panelExtractionRuleFound = false;

			List<ExtractionRule> vExtractionRules = screenClass.getExtractionRules();

			for (ExtractionRule extractionRule : vExtractionRules) {
				JavelinExtractionRule javelinExtractionRule = (JavelinExtractionRule) extractionRule;

				if (!runningThread.bContinue) break;
                /*
    			if ((Engine.objectsProvider == null) && !javelin.isConnected()) {
    				throw new ConnectionException("The emulator has been disconnected! See the emulator logs for more details...");
    			}
    			*/

                blockFactory.moveToFirstBlock();
                
                if (!extractionRule.isEnabled()) { 
                // if extraction rule is disabled
                    Engine.logContext.trace("(JavelinTransaction) Skipping the extraction rule \"" + extractionRule.getName() + "\" because it has been disabled.");
                    continue;
                }
                
                if (blockFactory instanceof PanelBlockFactory && !panelExtractionRuleFound) { 
                // if we apply the rules in a panel and the rule is before the rule which has created the panel
                	Engine.logContext.trace("(JavelinTransaction) Skipping the extraction rule \"" + extractionRule.getName() + "\" because it has been applied before the panel creation.");
                    if (extractionRule instanceof Nptui)
                    	panelExtractionRuleFound = true;
                	continue;
                }
                
                if(blockFactory instanceof PanelBlockFactory && extractionRule instanceof TabBox) {
                // if we try to apply tabbox rule in a panel
                	Block panel = ((PanelBlockFactory)blockFactory).getPanel();
                	XMLRectangle zone = ((TabBox)extractionRule).getSelectionScreenZone();
                	if (zone.contains(	panel.column, 
                						panel.line, 
                						Integer.parseInt(panel.getOptionalAttribute("width")), 
                						Integer.parseInt(panel.getOptionalAttribute("height"))	)
                	) {
                    // if the tabbox screen zone is larger than the panel
                		Engine.logContext.trace("(JavelinTransaction) Skipping the extraction rule \"" + extractionRule.getName() + "\" because the screen zone is larger than the panel.");
                		continue;
                	}
                }
                
                Engine.logContext.debug("(JavelinTransaction) Applying the extraction rule \"" + extractionRule.getName() + "\" on blocks");

                String extractionRuleQName = extractionRule.getQName();
    			if (vExtractionRulesInited.contains(extractionRuleQName)) {
    				extractionRuleInitReason = ExtractionRule.ACCUMULATING;
    			}
    			else {
    				extractionRuleInitReason = ExtractionRule.INITIALIZING;
    				vExtractionRulesInited.add(extractionRuleQName);
    			}
    			
    			Engine.logContext.trace("(JavelinTransaction) Initializing extraction rule (reason = " + extractionRuleInitReason + ")...");
                extractionRule.init(extractionRuleInitReason);
                
                // We fire engine events only in studio mode.
                if (Engine.isStudioMode()) {
                    Engine.theApp.fireObjectDetected(new EngineEvent(extractionRule));
                }
                
                // We try to apply the current extraction rule on each block.
                while (runningThread.bContinue && ((block = blockFactory.getNextBlock(block)) != null)) {
                    Engine.logContext.trace("(JavelinTransaction) Analyzing block \"" + block.getText() + "\"");
                    
                    // We skip final blocks.
                    if (block.bFinal) {
                        Engine.logContext.trace("(JavelinTransaction) The block has been marked as final; skipping it.");
                        continue;
                    }
                    
                    extractionRuleResult = javelinExtractionRule.apply(javelin, block, blockFactory, context.outputDocument);
                    
                    if (extractionRuleResult.hasMatched) {
                        
                        // If the extraction rule is final, we must skip the remaining
                        // extraction rules on this block and pass on the next block.
                        if (javelinExtractionRule.isFinal()) {
                            block.bFinal = true;
                            Engine.logContext.trace("(JavelinTransaction) Applying extraction rule '" + extractionRule.getName() + "': matching and final");
                        }
                        else {
                            Engine.logContext.trace("(JavelinTransaction) Applying extraction rule '" + extractionRule.getName() + "': matching");
                        }
                        
                        // We need to update the current block only if the
                        // rule has matched.
                        block = extractionRuleResult.newCurrentBlock;
                    }
                    else {
                        Engine.logContext.trace("(JavelinTransaction) Applying extraction rule '" + extractionRule.getName() + "': not matching");
                    }
                }
                
                // We fire engine events only in studio mode.
                if (Engine.isStudioMode()) {
                    Engine.theApp.fireBlocksChanged(new EngineEvent(blockFactory));
                    
                    Engine.logContext.debug("(JavelinTransaction) Step reached after having applied the extraction rule \"" + extractionRule.getName() + "\".");
                    Engine.theApp.fireStepReached(new EngineEvent(extractionRule));
                }
            }
            
            vExtractionRules = null;
            extractionRuleResult = null;
        }
        finally {
			context.statistics.stop(t, bNotFirstLoop);
        }
    }
    
    private transient String xsdType = null;
    
    private void renderBlocksToXml(Vector<Collection<Block>> vBlocks) throws EngineException {
    	String t = context.statistics.start(EngineStatistics.GENERATE_DOM);

        try {
            Enumeration<Collection<Block>> enumBlocks = vBlocks.elements();
			Hashtable<String, String> types = null;
            int nPage = 0;
            Element page = null;

			Element outputDocumentRootElement = context.outputDocument.getDocumentElement();
			outputDocumentRootElement.setAttribute("transaction", context.transactionName);
			outputDocumentRootElement.setAttribute("connector", context.connectorName);
			
            String prefix = getXsdTypePrefix();
			String transactionName = context.transactionName;

			boolean studioMode = Engine.isStudioMode();
			if (studioMode) {
				types = new Hashtable<String, String>(5);
				xsdType = "";
				xsdType += "<xsd:complexType name=\""+ prefix + transactionName +"Response\">\n";
				xsdType += "\t<xsd:sequence>\n";
				xsdType += "\t\t<xsd:element name=\"error\" minOccurs=\"0\" maxOccurs=\"1\" type=\"p_ns:ConvertigoError\"/>\n";
			}
			
			if (!isRemoveBlocksNode()) {
				xsdType += "\t\t<xsd:element name=\"blocks\" minOccurs=\"1\" maxOccurs=\"unbounded\" type=\"p_ns:"+ prefix + transactionName+"_blocksType\"/>\n";
				xsdType += "\t</xsd:sequence>\n";
				xsdType += "</xsd:complexType>\n";
				
				xsdType += "<xsd:complexType name=\""+ prefix + transactionName +"_blocksType\">\n";
				xsdType += "\t<xsd:sequence>\n";
				
			}
			
    		if (onlyOnePage) {
    			Engine.logContext.debug("(JavelinTransaction) Creating the unique page item into the XML document");
    			page = context.outputDocument.createElement("blocks");
    			outputDocumentRootElement.appendChild(page);
    		}
            
            while (enumBlocks.hasMoreElements()) {
    			if (!onlyOnePage) {
    				Engine.logContext.debug("(JavelinTransaction) Creating a new page item into the XML document");
                	page = context.outputDocument.createElement("blocks");
                	page.setAttribute("page-number", Integer.toString(nPage));
    				outputDocumentRootElement.appendChild(page);
    			}
                
    			Element xmlBlock, history, xmlValue;
    			Vector<String> values;
    			String value;
    			for (Block block : enumBlocks.nextElement()) {
    				Engine.logContext.trace("(JavelinTransaction) Block: " + block.toString());

    				// Skip rendering if needed
    				if (block.bRender) {
    					xmlBlock = block.toXML(context.outputDocument, getIncludedTagAttributes(), prefix + transactionName+"_blocks");
    					
    					// Add history
    					String historyBlock = block.getOptionalAttribute("history");

    					if ((context.httpSession != null) && (block != null) && (block.type.equals("field")) && (historyBlock != null) && (historyBlock.equals("true"))) {
    						values = GenericUtils.cast(context.httpSession.getAttribute(block.tagName));
    						if (values != null) {
    							history = context.outputDocument.createElement("history");

    							int len = values.size();
    							for (int i = 0 ; i < len ; i++) {
    								value = values.elementAt(i);
    								xmlValue = context.outputDocument.createElement("value");
    								xmlValue.appendChild(context.outputDocument.createTextNode(value));
    								history.appendChild(xmlValue);
    							}

    							xmlBlock.appendChild(history);
    						}
    					}
    					
    					page.appendChild(xmlBlock);
    					
    					if (studioMode) {
	    					if (types.get(block.tagName) == null) {
	    						xsdType += "\t\t\t" + block.xsdType + "\n";
	    						types.put(block.tagName, block.xsdTypes);
	    					}
    					}
    				}
    			}
                
                nPage++;
            }
            
            if (studioMode) {
            	if (!isRemoveBlocksNode()) {
	                xsdType += "\t</xsd:sequence>\n";
	    			xsdType += "\t<xsd:attribute name=\"page-number\" use=\"optional\"/>\n";
	                xsdType += "</xsd:complexType>\n";
            	}
            	else {
    				xsdType += "\t</xsd:sequence>\n";
    				xsdType += "</xsd:complexType>\n";
            	}
            	
                Enumeration<String> e = types.elements();
                while (e.hasMoreElements()) {
                	xsdType += e.nextElement();
                }

                //System.out.println(xsdType);
            }
        }
        finally {
			context.statistics.stop(t);
        }
        
        Engine.logContext.debug("(JavelinTransaction) DOM generated");
    }
    
    public String generateWsdlType(Document document) throws Exception {
    	if (xsdType == null) {
    		return super.generateWsdlType(document);
    	}
    	return xsdType;
    }
    
    /** Getter for property onlyOnePage.
     * @return Value of property onlyOnePage.
     */
    public boolean isOnlyOnePage() {
        return this.onlyOnePage;
    }
    
    /** Setter for property onlyOnePage.
     * @param onlyOnePage New value of property onlyOnePage.
     */
    public void setOnlyOnePage(boolean onlyOnePage) {
        this.onlyOnePage = onlyOnePage;
    }
    
    public void configure(Element element) throws Exception {
        super.configure(element);

		String version = element.getAttribute("version");
        
		if (version == null) {
			String s = XMLUtils.prettyPrintDOM(element);
			EngineException ee = new EngineException("Unable to find version number for the database object \"" + getName() + "\".\nXML data: " + s);
			throw ee;
		}

		if (VersionUtils.compare(version, "4.1.5") < 0) {
            NodeList properties = element.getElementsByTagName("property");
            Element propValue = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "executeExtractionRulesInPanels");

            if (propValue == null) {
                executeExtractionRulesInPanels = false;
            }

            hasChanged = true;
            Engine.logBeans.warn("[JavelinTransaction] The object \"" + getName() + "\" has been updated to version 4.1.5");
        }
    }    
}
