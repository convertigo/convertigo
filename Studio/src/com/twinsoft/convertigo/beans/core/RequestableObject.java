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

package com.twinsoft.convertigo.beans.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.mozilla.javascript.Scriptable;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.jacob.com.ComThread;
import com.twinsoft.convertigo.beans.steps.StepException;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.util.LogWrapper;
import com.twinsoft.convertigo.engine.util.ThreadUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.util.StringEx;

public abstract class RequestableObject extends DatabaseObject implements ISheetContainer {

	private static final long serialVersionUID = -8343815173166853025L;

	public static int nbCurrentWorkerThreads = 0;
	
    public static final String EVENT_REQUESTABLE_STARTED = "RequestableStarted";
    public static final String EVENT_REQUESTABLE_FINISHED = "RequestableFinished";
	public static final String EVENT_REQUESTABLE_XML_GENERATED = "RequestableXmlGenerated";
	
    public static final int SHEET_LOCATION_NONE = 0;
    public static final int SHEET_LOCATION_FROM_REQUESTABLE = 1;
    public static final int SHEET_LOCATION_FROM_LAST_DETECTED_OBJECT_OF_REQUESTABLE = 2;

    public static final int ACCESSIBILITY_PUBLIC = 0;
    public static final int ACCESSIBILITY_HIDDEN = 1;
    public static final int ACCESSIBILITY_PRIVATE = 2;
    
    protected final static String fake_root = "document";
    
    private boolean addStatistics = false;

	/**
     * The requester. This object is responsible for realizing
     * the interface between the Convertigo engine and the transaction.
     */
    transient protected Requester requester = null;
    
    /**
     * The context associated to the XML producer. The XML producer is
     * responsible for updating relevantly this context.
     */
    transient public Context context = null;
    
    transient private String workerThreadCreationStatistic = null;
    
    transient public Scriptable scope = null;
    
    transient public RequestableThread runningThread;
	
    transient protected long score = 0;
    
	public RequestableObject() {
        super();
	}

	@Override
    public RequestableObject clone() throws CloneNotSupportedException {
    	RequestableObject clonedObject = (RequestableObject) super.clone();
        clonedObject.vSheets = new Vector<Sheet>();
        return clonedObject;
    }
    
	public long getScore() {
		return score;
	}
	
    public String generateXsdTypes(Document document, boolean extract) throws Exception {
    	String xsdTypes = null;
    	xsdTypes = generateXsdRequestData() + generateXsdResponseData(document, extract);
    	return xsdTypes;
    }
    
    protected String getBackupWsdlTypes() throws Exception {
    	String backupWsdlTypes = null;
    	String wsdlBackupDir = getWsdlBackupDir();
        File dir = new File(wsdlBackupDir);
		if (dir.exists()) {
			File file = new File(wsdlBackupDir + "/" + getName() + ".xml");
			if (file.exists()) {
                DocumentBuilder documentBuilder = XMLUtils.getDefaultDocumentBuilder();
                Document document = documentBuilder.parse(file);
				Element fake = document.getDocumentElement();
				StringEx sx = new StringEx(XMLUtils.prettyPrintElement(fake, true, true));
				sx.replace("<"+fake_root+">","");
				sx.replace("</"+fake_root+">","");
				backupWsdlTypes = sx.toString();
			}
		}
		return backupWsdlTypes;
    }
    
    public String migrateToXsdTypes() {
    	String xsdTypes = null;
    	try {
			// Retrieve backup wsdlTypes
			String backupWsdlTypes = getBackupWsdlTypes();
			if (backupWsdlTypes != null) {
		    	StringEx sx = new StringEx(backupWsdlTypes);
		    	
		    	// Replace ccc_xxxResponse by ccc__xxxResponse (Fix ticket #252)
		    	sx.replace("_"+ getName() + "Response\"", "__" + getName() + "Response\"");

		    	// Fix missing type for sql_output element (SqlTransaction)
		    	sx.replace("<xsd:element maxOccurs=\"1\" minOccurs=\"0\" name=\"sql_output\"/>", "<xsd:element maxOccurs=\"1\" minOccurs=\"0\" name=\"sql_output\" type=\"xsd:string\"/>");
		    	
		    	// Replace xxxResponse by yyy__xxxResponseData
		    	sx.replace("\""+ getName() + "Response\"", "\"" + getXsdTypePrefix() + getName() + "ResponseData\"");
		    	sx.replace(":"+ getName() + "Response\"", ":" + getXsdTypePrefix() + getName() + "ResponseData\"");
		    	sx.replace("__"+ getName() + "Response\"", "__" + getName() + "ResponseData\"");
		    	sx.replaceAll("tns:", getProject().getName() + "_ns:");
		    	xsdTypes = generateXsdRequestData() + " " + sx.toString();
			}
    	}catch (Exception e) {
    		Engine.logBeans.error("Unable to migrate to XSD types for requestable \""+ getName() +"\"", e);
    	}
    	return xsdTypes;
    }
    
    protected String getWsdlBackupDir() throws Exception {
    	return Engine.PROJECTS_PATH + "/"+ getProject().getName() + "/backup-wsdl";
    }
    
    protected String getWsdlBackupDir(Element element) throws Exception {
    	Element rootElement = element.getOwnerDocument().getDocumentElement();
    	Element projectNode = (Element) XMLUtils.findChildNode(rootElement, Node.ELEMENT_NODE);
    	NodeList properties = projectNode.getElementsByTagName("property");
		Element pName = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "name");
		String projectName = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(pName, Node.ELEMENT_NODE));
    	return Engine.PROJECTS_PATH + "/"+ projectName + "/backup-wsdl";
    }
    
    protected void backupWsdlTypes(Element element) throws TransformerFactoryConfigurationError, Exception {
    	if (wsdlType.equals(""))
    		return;
    	
		StringEx sx = new StringEx(wsdlType);
		sx.replaceAll("<cdata>","<![CDATA[");
		sx.replaceAll("</cdata>","]]>");
		String sDom = "<"+fake_root+">\n" + sx.toString() + "</"+fake_root+">";
		DocumentBuilder documentBuilder = XMLUtils.getDefaultDocumentBuilder();
		Document document = documentBuilder.parse(new InputSource(new StringReader(sDom)));
		
		String wsdlBackupDir = getWsdlBackupDir(element);
        Source source = new DOMSource(document);
        File dir = new File(wsdlBackupDir);
		if (!dir.exists())
			dir.mkdirs();

		File file = new File(wsdlBackupDir + "/" + getName() + ".xml");
        Result result = new StreamResult(new FileOutputStream(file));
        Transformer xformer = TransformerFactory.newInstance().newTransformer();
        xformer.transform(source, result);
    }
    
    public abstract String generateXsdArrayOfData() throws Exception;
    
    public abstract String generateXsdRequestData() throws Exception;
    
    protected abstract String generateXsdResponseData(Document document, boolean extract) throws Exception;
    
    public abstract String generateWsdlType(Document document) throws Exception;
    
    protected abstract String extractXsdType(Document document) throws Exception;
    
	public abstract void setStatisticsOfRequestFromCache();
	
	public abstract String getRequestString(Context context);
    
	public abstract String getXsdTypePrefix();
	
	public abstract String getXsdTypePrefix(DatabaseObject parentObject);
	
	public abstract String getXsdExtractPrefix();
	
	public abstract void abort();
	
	public void parseInputDocument(Context context){
		this.context = context;
	}

	public abstract boolean hasToRunCore();
	
    public abstract void runCore() throws EngineException;
    
	public abstract void prepareForRequestable(Context context, org.mozilla.javascript.Context javascriptContext, Scriptable scope) throws EngineException;
    
	public abstract void handleRequestableEvent(String eventType, org.mozilla.javascript.Context javascriptContext) throws EngineException;
	
	public abstract void fireRequestableEvent(String eventType);
	
    /**
     * Initializes all common things for transaction and launch
     * the execution of the core transaction.
     *
     * @param requester the calling requester.
     * @param context the associated context.
     *
     * @return the resulting DOM of the transaction.
     */
    public Document run(Requester requester, Context context) throws EngineException {
        
    	fireRequestableEvent(RequestableObject.EVENT_REQUESTABLE_STARTED);
        
        try {
            this.requester = requester;
            this.context = context;
            
            this.score = 0;
            
            context.cacheControl = this.isClientCachable() ? "true":"false";
            
            context.outputDocument = requester.createDOM(getEncodingCharSet());
            
			Element outputDocumentRootElement = context.outputDocument.createElement("document");
			context.outputDocument.appendChild(outputDocumentRootElement);
            
			outputDocumentRootElement.setAttribute("project", context.projectName);
			outputDocumentRootElement.setAttribute("sequence", context.sequenceName);
			outputDocumentRootElement.setAttribute("connector", context.connectorName);
			outputDocumentRootElement.setAttribute("transaction", context.transactionName);
			if (context.lang != null && context.lang.length() != 0) {
				outputDocumentRootElement.setAttribute("lang", context.lang);
			}
			
            int maxNbCurrentWorkerThreads = Integer.parseInt(EnginePropertiesManager.getProperty(PropertyName.DOCUMENT_THREADING_MAX_WORKER_THREADS));
            
            if (nbCurrentWorkerThreads >= maxNbCurrentWorkerThreads)
            	throw new EngineException("No more available worker thread (" + maxNbCurrentWorkerThreads + ")");
            
            Engine.logContext.debug("(RequestableObject) Start of the thread for the requested object '" + getName() + "' ("+context.contextID+")");

            workerThreadCreationStatistic = context.statistics.start(EngineStatistics.WORKER_THREAD_START);
            
            runningThread = new RequestableThread();
            String currentThreadName = Thread.currentThread().getName();
            runningThread.setName(currentThreadName + "/RequestableThread");
            runningThread.setDaemon(true);
            runningThread.start();
            
            try {
            	synchronized(Engine.theApp) {
            		// Check the requestable's running thread engine ID to prevent wrong
            		// modification of the worker threads counter
                	if (runningThread.engineId == Engine.startStopDate)
                		nbCurrentWorkerThreads++;
            	}

                boolean hasBeenInterrupted = false;

                do {
    	            try {
    	            	synchronized(runningThread) {
    	            		long lTime = System.currentTimeMillis();
    	            		long haveToWait;
    	            		
    	            		while (runningThread.bContinue) {
    			                if (getResponseTimeout() <= 0) {
    			                	Engine.logContext.trace("(RequestableObject) Waiting for requested object response during 60s...");
    			                	haveToWait = 60000;
    			                	runningThread.wait(60000);
    			                } else if (Engine.isStudioMode()) {
    			                	// Studio context => ~infinite responseTimeout
    			                	Engine.logContext.trace("(RequestableObject) Waiting for requested object response during infinite timeout (24 hours) because of Studio context execution...");
    			                	haveToWait = 1000*60*60*24;
    			                	runningThread.wait(1000*60*60*24);
    			                } else {
    			                	Engine.logContext.trace("(RequestableObject) Waiting for requested object response during " + getResponseTimeout() + "s");
    			                	haveToWait = getResponseTimeout() * 1000;
    			                	runningThread.wait(getResponseTimeout() * 1000);
    			                }
    	
    			                Engine.logContext.trace("(RequestableObject) End of wait(), runningThread.bContinue=" + runningThread.bContinue);
    			                
    			                if (runningThread.bContinue) {
    			                	if (System.currentTimeMillis() - lTime > haveToWait) {
    			                		// Cleans up before stopping
    			                		cleanup();
    			                		
    			                		// Stops thread
    				                	if (EnginePropertiesManager.getProperty(PropertyName.DOCUMENT_THREADING_USE_STOP_METHOD).equalsIgnoreCase("true")) {
    				                		ThreadUtils.stopThread(runningThread);
    					                    Engine.logContext.error("(RequestableObject) Stopping the thread for the requested object '" + getName() + "' because of timeout expiration");
    				                	} else {
    				                		runningThread.bContinue = false;
    					                    Engine.logContext.error("(RequestableObject) Request for stopping the thread for the requested object '" + getName() + "' because of timeout expiration");
    				                	}
    		
    				                    TransactionTimeoutException e = new TransactionTimeoutException("The requested object '" + getName() + "' has been interrupted because it did not terminate quickly enough.");
    				                    throw e;
    			                	} else Engine.logContext.trace("(RequestableObject) Spurious wakup , keep waiting" );
    			                }
    	            		}
    	            	}
    	            } catch(InterruptedException e) {
    	                Engine.logContext.warn("(RequestableObject) InterruptedException while waiting for requested object response; retrying wait()");
    	                hasBeenInterrupted = true;
    	            }
                } while(hasBeenInterrupted && runningThread.bContinue);
            }
            finally {
            	synchronized(Engine.theApp) {
            		// Check the requestable's running thread engine ID to prevent wrong
            		// modification of the worker threads counter
                	if (runningThread.engineId == Engine.startStopDate)
                		nbCurrentWorkerThreads--;
            	}
            }

            // An exception has been thrown ?
            if (runningThread.exception != null)
                if (runningThread.exception instanceof EngineException)
                    throw (EngineException) runningThread.exception;
                else throw new EngineException("An unexpected error has occured while the execution of the requested object '" + getName() + "'.", runningThread.exception);

			outputDocumentRootElement.setAttribute("generated", Calendar.getInstance(Locale.getDefault()).getTime().toString());
			
            Engine.logContext.debug("(RequestableObject) End of the thread for the requested object '" + getName() + "'");
            
            /* 
             * If we have a status node , this means that we are in async mode.
             * As the outputDocument has been used to return the status, the outputdocument will be appended to the status.
             * 
             *  In this case, We have to : 
             *  1) delete JOB tags in status
             *  2) add as child node to outputDocumentRootElement the follwing sibling (the real data)
             *  3) replace status with outputDocumentRootElement.
             *
            if (context.isAsync) {
	            NodeList nl = context.outputDocument.getElementsByTagName("status");
	            if (nl.getLength() != 0) {
	            	nl.item(0).removeChild(nl.item(0).getFirstChild()); // remove "job" tag, now firstChild is the real data
	            	Node node = outputDocumentRootElement.getOwnerDocument().importNode(nl.item(0).getFirstChild(), true); 
	            	outputDocumentRootElement.appendChild(node);
	            	node = context.outputDocument.importNode(outputDocumentRootElement, true);
	            	context.outputDocument.removeChild(nl.item(0));
	            	context.outputDocument.appendChild(node);
	            }
            }
            */
        } finally {
        	fireRequestableEvent(RequestableObject.EVENT_REQUESTABLE_FINISHED);
        }
        
        return context.outputDocument;
    }
	
    /*
     * Cleans up
     */
    protected void cleanup() {
    	// does nothing
    }
    
    protected void insertObjectsInScope() throws EngineException {
		// Insert the DOM into the scripting context
		Scriptable jsContext = org.mozilla.javascript.Context.toObject(context, scope);
		scope.put("context", scope, jsContext);

		// Insert the log object into the scripting context
		Scriptable jsLog = org.mozilla.javascript.Context.toObject(new LogWrapper(Engine.logUser), scope);
		scope.put("log", scope, jsLog);

		// Insert the steps vector into the scripting context
		Scriptable jsSteps = org.mozilla.javascript.Context.toObject(context.steps, scope);
		scope.put("steps", scope, jsSteps);

		// Insert the DOM into the scripting context
		Scriptable jsDOM = org.mozilla.javascript.Context.toObject(context.outputDocument, scope);
		scope.put("dom", scope, jsDOM);
    }
    
    protected void removeObjectsFromScope() {
    	for(Object id : scope.getIds())
    		scope.delete((String)id);
    }
    
    public Requester getRequester() {
    	return requester;
    }
    
	/** Holds value of property sheetLocation. */
	private int sheetLocation = 0;
    
    /** Getter for property sheetLocation.
     * @return Value of property sheetLocation.
     */
    public int getSheetLocation() {
        return sheetLocation;
    }
    
    /** Setter for property sheetLocation.
     * @param sheetLocation New value of property sheetLocation.
     */
    public void setSheetLocation(int sheetLocation) {
        this.sheetLocation = sheetLocation;
    }
	
	/** Holds value of property billable. */
    private boolean billable = false;
    
    /** Getter for property billable.
     * @return Value of property billable.
     */
    public boolean isBillable() {
        return this.billable;
    }
    
    /** Setter for property billable.
     * @param billable New value of property billable.
     */
    public void setBillable(boolean billable) {
        this.billable = billable;
    }
    
	/** Holds value of property clientCachable. */
    private boolean clientCachable = false;

    /** Getter for property clientCachable.
     * @return Value of property clientCachable.
     */
    public boolean isClientCachable() {
        return this.clientCachable;
    }
    
    /** Setter for property clientCachable.
     * @param billable New value of property clientCachable.
     */
    public void setClientCachable(boolean clientCachable) {
        this.clientCachable = clientCachable;
    }
	
	/** Holds value of property encodingCharSet. */
	private String encodingCharSet = "UTF-8";
    
    /** Getter for property encodingCharSet.
     * @return Value of property encodingCharSet.
     */
    public String getEncodingCharSet() {
        return encodingCharSet;
    }
    
    /** Setter for property encodingCharSet.
     * @param encodingCharSet New value of property encodingCharSet.
     */
    public void setEncodingCharSet(String encodingCharSet) {
        this.encodingCharSet = encodingCharSet;
    }
	
	/** Holds value of property accessibility. */
	private int accessibility = 0;

    /** Getter for property accessibility.
     * @return Value of property accessibility.
     */
    public int getAccessibility() {
        return this.accessibility;
    }
    
    /** Setter for property accessibility.
     * @param accessibility New value of property accessibility.
     */
    public void setAccessibility(int accessibility) {
        this.accessibility = accessibility;
    }
    
    public boolean isPublicAccessibility() {
    	return accessibility == ACCESSIBILITY_PUBLIC;
    }
	
    public boolean isPrivateAccessibility() {
    	return accessibility == ACCESSIBILITY_PRIVATE;
    }
	
    public boolean isHiddenAccessibility() {
    	return accessibility == ACCESSIBILITY_HIDDEN;
    }
	
    private boolean secureConnectionRequired = false;

	public boolean isSecureConnectionRequired() {
		return secureConnectionRequired;
	}

	public void setSecureConnectionRequired(boolean secureConnectionRequired) {
		this.secureConnectionRequired = secureConnectionRequired;
	}

	/** Holds value of property responseTimeout. */
	private long responseTimeout = 60;
    
	/** Getter for property responseTimeout.
	 * @return Value of property responseTimeout.
	 */
	public long getResponseTimeout() {
		return this.responseTimeout;
	}
    
	/** Setter for property responseTimeout.
	 * @param responseTimeout New value of property responseTimeout.
	 */
	public void setResponseTimeout(long responseTimeout) {
		this.responseTimeout = responseTimeout;
	}
    
	/** Holds value of property responseExpiryDate. */
	private String responseExpiryDate = "";
    
	/** Getter for property responseExpiryDate.
	 * @return Value of property responseExpiryDate.
	 */
	public String getResponseExpiryDate() {
		return this.responseExpiryDate;
	}
    
	/** Setter for property responseExpiryDate.
	 * @param responseExpiryDate New value of property responseExpiryDate.
	 */
	public void setResponseExpiryDate(String responseExpiryDate) {
		this.responseExpiryDate = responseExpiryDate;
	}
	
	/**
	 * Retrieves the transaction response date in milliseconds.
	 */
	public long getResponseExpiryDateInMillis() throws EngineException {
		if (responseExpiryDate.length() == 0) {
			return 0;
		}
		
		Calendar nowCalendar = Calendar.getInstance();
		long now = nowCalendar.getTime().getTime();
		int cY = nowCalendar.get(Calendar.YEAR);
		int cM = nowCalendar.get(Calendar.MONTH);
		int cD = nowCalendar.get(Calendar.DATE);
		int cDoW = nowCalendar.get(Calendar.DAY_OF_WEEK);

		try {		
			StringTokenizer st = new StringTokenizer(responseExpiryDate, "," , false);
			String param;
			param = st.nextToken();
			
			if (param.equals("absolute")) {
				param = st.nextToken();
				return now + Long.parseLong(param) * 1000;
			}else if (param.equals("daily")) {
				param = st.nextToken();
				StringTokenizer st2 = new StringTokenizer(param, ":" , false);
				int h = Integer.parseInt(st2.nextToken());
				int m = Integer.parseInt(st2.nextToken());
				int s = Integer.parseInt(st2.nextToken());
				
				Calendar responseCalendar = Calendar.getInstance();
				Calendar refCalendarOfDay = Calendar.getInstance();
				refCalendarOfDay.setTimeInMillis(now);
				refCalendarOfDay.set(Calendar.HOUR_OF_DAY, h);
				refCalendarOfDay.set(Calendar.MINUTE, m);
				refCalendarOfDay.set(Calendar.SECOND, s);
				if(responseCalendar.getTime().after(refCalendarOfDay.getTime())) cD=cD+1;
				responseCalendar.set(cY, cM, cD, h, m, s);
				Engine.logCacheManager.info("Date of response calendar" + responseCalendar.getTime());
				return responseCalendar.getTime().getTime();
			}else if (param.equals("weekly")) {
				param = st.nextToken();
				StringTokenizer st2 = new StringTokenizer(param, ":" , false);
				int h = Integer.parseInt(st2.nextToken());
				int m = Integer.parseInt(st2.nextToken());
				int s = Integer.parseInt(st2.nextToken());

				param = st.nextToken();
				int dayOfWeek = Integer.parseInt(param);

				Calendar responseCalendar = Calendar.getInstance();
				responseCalendar.set(cY, cM, cD, h, m, s);

				if (cDoW < dayOfWeek) {
					responseCalendar.set(cY, cM, cD + (dayOfWeek - cDoW), h, m, s);
				}else if (cDoW == dayOfWeek) {
					if (nowCalendar.after(responseCalendar))
						responseCalendar.set(cY, cM, cD + 7, h, m, s);
				} else responseCalendar.set(cY, cM, cD + (dayOfWeek + 7 - cDoW), h, m, s);
				
				return responseCalendar.getTime().getTime();
			} else if (param.equals("monthly")) {
				param = st.nextToken();
				StringTokenizer st2 = new StringTokenizer(param, ":" , false);
				int h = Integer.parseInt(st2.nextToken());
				int m = Integer.parseInt(st2.nextToken());
				int s = Integer.parseInt(st2.nextToken());

				param = st.nextToken();
				int dayOfMonth = Integer.parseInt(param);
				
				Calendar responseCalendar = Calendar.getInstance();
				responseCalendar.set(cY, cM, dayOfMonth, h, m, s);

				if (nowCalendar.after(responseCalendar))
					responseCalendar.add(Calendar.MONTH, 1);
				
				return responseCalendar.getTime().getTime();
			} else if (param.equals("always")) {
				// Returns arbitraty 1 day after
				return now + 24 * 60 * 60 * 1000;
			} else throw new EngineException("Unknown type: " + param);
		} catch(NumberFormatException e) {
			throw new EngineException("Number format error.", e);
		} catch(NoSuchElementException e) {
			throw new EngineException("Missing parameter in the expiry date string.", e);
		}
	}

	/**
	 * The String containing the WSDL type definition for this requested object.
	 */
	transient public String wsdlType = "";
	
	transient protected List<Sheet> vSheets = new LinkedList<Sheet>();
    
	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
        if (databaseObject instanceof Sheet)
            addSheet((Sheet) databaseObject);
        else throw new EngineException("You cannot add to a requestable object a database object of type " + databaseObject.getClass().getName());
    }

	@Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
        if (databaseObject instanceof Sheet)
            removeSheet((Sheet) databaseObject);
        else throw new EngineException("You cannot remove from a requestable object a database object of type " + databaseObject.getClass().getName());
		super.remove(databaseObject);
    }

    public void addSheet(Sheet sheet) throws EngineException {
    	checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vSheets, sheet.getName(), sheet.bNew);
		sheet.setName(newDatabaseObjectName);
        // Check for sheet with the same browser
        String requestedBrowser = sheet.getBrowser();
        for(Sheet sh : vSheets)
            if (sh.getBrowser().equals(requestedBrowser))
                throw new EngineException("Cannot add the sheet because a sheet is already defined for the browser \"" + requestedBrowser + "\" in the requestable object \"" + getName() + "\".");
        vSheets.add(sheet);
        super.add(sheet);
    }
    
    @Deprecated
    public Vector<Sheet> getSheets(){
    	return new Vector<Sheet>(vSheets);
    }
    
    public List<Sheet> getSheetsList() {
    	checkSubLoaded();
        return vSheets;
    }
    
    public Sheet getSheet(String browser) {
    	checkSubLoaded();
    	for(Sheet sheet : vSheets)
    		if (sheet.getBrowser().equals(browser)) return sheet;
        return null;
    }
    
    public void removeSheet(Sheet sheet) {
    	checkSubLoaded();
        vSheets.remove(sheet);
    }
	
    public void createRequestableThread(){
    	runningThread = new RequestableThread();
    }
    
    public class RequestableThread extends Thread {
        protected Throwable exception = null;
        protected Thread callingThread;
        public boolean bContinue = true;
        
        private long engineId;
        
        public org.mozilla.javascript.Context javascriptContext = null;
        
        public RequestableThread() {
        	super();
            callingThread = Thread.currentThread();
            engineId = Engine.startStopDate;
        }
        
        @Override
        public void run() {
        	context.statistics.stop(workerThreadCreationStatistic);
        	
			context.steps = new Vector<String>(8, 8);

            String xmlEngine = EnginePropertiesManager.getProperty(PropertyName.DOCUMENT_XML_ENGINE);
            String xsltEngine = EnginePropertiesManager.getProperty(PropertyName.DOCUMENT_XSLT_ENGINE);
            boolean isMsXml = (xmlEngine.equals("msxml")) && (xsltEngine.equals("msxml"));
        	
            try {
                if (isMsXml) {
                	ComThread.InitMTA();
                }

				// Creating scripting context
    			javascriptContext = org.mozilla.javascript.Context.enter();
    			scope = javascriptContext.initStandardObjects();
    			
            	prepareForRequestable(context, javascriptContext, scope);
            	
        		if (!runningThread.bContinue)
        			return;

				handleRequestableEvent(RequestableObject.EVENT_REQUESTABLE_STARTED, javascriptContext);

            	if (hasToRunCore())
            		runCore();

                if (!runningThread.bContinue)
                	return;

                handleRequestableEvent(RequestableObject.EVENT_REQUESTABLE_XML_GENERATED, javascriptContext);
            } catch(Throwable e) {
            	// Try to find an exception cause thrown by a jException step
				Throwable jExceptionStepCause = e;
				while ((jExceptionStepCause = jExceptionStepCause.getCause()) != null)  {
					if (jExceptionStepCause instanceof StepException) {
						break;
					}
				}	
            	if (jExceptionStepCause != null) {
            		Engine.logContext.info("An exception was thrown by a jException step: " + jExceptionStepCause.getMessage());
            	} else {
            		Engine.logContext.error("Exception thrown during requested object execution", e);
            	}
                exception = e;
            } finally {
				Engine.logContext.debug("(RequestableObject) Final stage for requested object thread");

				if (isMsXml)
                	ComThread.Release();

				if (javascriptContext != null) {
					removeObjectsFromScope();
					org.mozilla.javascript.Context.exit();
					javascriptContext = null;
					scope = null;
				}
				
				Engine.logContext.debug("(RequestableObject) End of requested object thread");
				
                synchronized(this) {
	            	if (runningThread.bContinue) {
	    				Engine.logContext.debug("(RequestableObject) Notifying the calling thread '" + callingThread.getName() + "' because of normal request termination");
	    				runningThread.bContinue = false;
    					notify();
	    				Engine.logContext.debug("(RequestableObject) The calling thread has been notified!");
	            	} else {
	        			Engine.logContext.warn("(RequestableObject) Requested object \"" + RequestableObject.this.getName() + "\" aborted because of session or requestable timeout on context " + context.contextID);
	    				Engine.logContext.debug("(RequestableObject) Requested object thread: nothing to notify");
	            	}
                }
            }
        }
    }

    @Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
		String version = element.getAttribute("version");
        
		if (version == null) {
			String s = XMLUtils.prettyPrintDOM(element);
			EngineException ee = new EngineException("Unable to find version number for the database object \"" + getName() + "\".\nXML data: " + s);
			throw ee;
		}

        try {
            NodeList childNodes = element.getElementsByTagName("wsdltype");
            int len = childNodes.getLength();
            if (len > 0) {
                Node childNode = childNodes.item(0);
                Node cdata = XMLUtils.findChildNode(childNode, Node.CDATA_SECTION_NODE);
                if (cdata != null) {
                    wsdlType = cdata.getNodeValue();
                    Engine.logBeans.trace("(Requestable) Requestable.configure() : wsdltype has been successfully set");
                } else Engine.logBeans.trace("(Requestable) Requestable.configure() : wsdltype is empty");
            }
        } catch(Exception e) {
            throw new EngineException("Unable to configure the WSDL types of the requestable \"" + getName() + "\".", e);
        }
        
        try {
        	// Convert the publicMethod property to new semantic (accessibility)
            if (VersionUtils.compare(version, "6.1.2") < 0) {
				boolean publicMethod = (Boolean) XMLUtils.findPropertyValue(element, "publicMethod");
				if (publicMethod) setAccessibility(ACCESSIBILITY_PUBLIC);
				else setAccessibility(ACCESSIBILITY_HIDDEN);
				
                hasChanged = true;
                Engine.logBeans.warn("[RequestableObject] The object \"" + getName() + "\" has been updated to version 6.1.2; publicMethod=" + publicMethod + "; accessibility=" + accessibility);
            }
        } catch(Exception e) {
            throw new EngineException("Unable to migrate the accessibility for requestable \"" + getName() + "\".", e);
        }
        if (VersionUtils.compare(version, "4.6.0") < 0) {
			// Backup wsdlTypes to file
			try {
				backupWsdlTypes(element);
				if (!wsdlType.equals("")) {
					wsdlType = "";
					hasChanged = true;
					Engine.logBeans.warn("[RequestableObject] Successfully backup wsdlTypes for requestable \""+ getName() +"\" (v 4.6.0)");
				} else {
					Engine.logBeans.warn("[RequestableObject] Empty wsdlTypes for requestable \""+ getName() +"\", none backup done (v 4.6.0)");
				}
	    	} catch (Exception e) {
	    		Engine.logBeans.error("[RequestableObject] Could not backup wsdlTypes for requestable \""+ getName() +"\" (v 4.6.0)", e);
	    	}
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
        } catch(NullPointerException e) {
            // Silently ignore
        }  
        return element;
	}  
	
	
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep=super.getAllChildren();
		List<Sheet> sheets=getSheetsList();		
		for(Sheet sheet:sheets){
			rep.add(sheet);
		}
		
		return rep;
	}
	
    public boolean getAddStatistics() {
		return addStatistics;
	}

	public void setAddStatistics(boolean addStatistics) {
		this.addStatistics = addStatistics;
	}
	
	private boolean authenticatedContextRequired = false;

	public boolean getAuthenticatedContextRequired() {
		return authenticatedContextRequired;
	}

	public void setAuthenticatedContextRequired(boolean authenticatedContextRequired) {
		this.authenticatedContextRequired = authenticatedContextRequired;
	}
}