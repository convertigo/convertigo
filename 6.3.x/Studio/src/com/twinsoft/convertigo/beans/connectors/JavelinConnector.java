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

package com.twinsoft.convertigo.beans.connectors;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.common.FindString;
import com.twinsoft.convertigo.beans.common.RegularExpression;
import com.twinsoft.convertigo.beans.common.XMLRectangle;
import com.twinsoft.convertigo.beans.core.ConnectorWithScreenClasses;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.screenclasses.JavelinScreenClass;
import com.twinsoft.convertigo.beans.transactions.JavelinTransaction;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.plugins.VicApi;
import com.twinsoft.convertigo.engine.util.LogWrapper;
import com.twinsoft.tas.Authentication;
import com.twinsoft.tas.Key;
import com.twinsoft.tas.KeyManager;
import com.twinsoft.tas.User;
import com.twinsoft.twinj.Javelin;
import com.twinsoft.util.DevicePool;
import com.twinsoft.util.TWSKey;

/**
 * This is the Javelin connector.
 */
public class JavelinConnector extends ConnectorWithScreenClasses {

	private static final long serialVersionUID = 872661252417553047L;

	public JavelinConnector() {
		super();
        
        javelin = null;
	}
	
	private String ibmTerminalType = "";
	
	public String getIbmTerminalType() {
		if (emulatorTechnology.startsWith("com.twinsoft.ibm.")) {
			// Default values: 'IBM-3279' for 3270 and 'IBM-3179' for 5250
			if ((ibmTerminalType == null) || ibmTerminalType.equals("")) {
				if (emulatorTechnology.equals("com.twinsoft.ibm.TerminalSNA")) return "IBM-3279";
				else if (emulatorTechnology.equals("com.twinsoft.ibm.Terminal400")) return "IBM-3179";
				else return "";
			}
		}

		return ibmTerminalType;
	}

	public void setIbmTerminalType(String ibmTerminalType) {
		this.ibmTerminalType = ibmTerminalType;
	}

	private boolean sslEnabled = false;
	
	public boolean isSslEnabled() {
		return sslEnabled;
	}

	public void setSslEnabled(boolean sslEnabled) {
		this.sslEnabled = sslEnabled;
	}

	private boolean sslTrustAllServerCertificates = true;
	
	public boolean isSslTrustAllServerCertificates() {
		return sslTrustAllServerCertificates;
	}

	public void setSslTrustAllServerCertificates(boolean sslTrustAllServerCertificates) {
		this.sslTrustAllServerCertificates = sslTrustAllServerCertificates;
	}

	@Override
	public JavelinConnector clone() throws CloneNotSupportedException {
		JavelinConnector clonedObject = (JavelinConnector) super.clone();
		clonedObject.emulatorID = emulatorID;
		clonedObject.javelin = null;
		return clonedObject;
	}

	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);
        emulatorID = findEmulatorId();
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.Connector#addTransaction(com.twinsoft.convertigo.beans.core.Transaction)
	 */
	@Override
	protected void addTransaction(Transaction transaction) throws EngineException {
		if (!(transaction instanceof JavelinTransaction))
			throw new EngineException("You cannot add to an Javelin connector a database object of type " + transaction.getClass().getName());
		super.addTransaction(transaction);
	}

	public long findEmulatorId() throws EngineException {
		try {
			Properties properties = new Properties();
			InputStream propsInputstream = getClass().getResourceAsStream("/emulators.properties");
			properties.load(propsInputstream);

			// Enumeration of the properties
			Enumeration<?> propsEnum = properties.propertyNames();
			String propertyName, propertyValue;
			
			while (propsEnum.hasMoreElements()) {
				propertyName = (String) propsEnum.nextElement();
				propertyValue = properties.getProperty(propertyName, "");
				if (propertyValue.indexOf(emulatorTechnology) != -1) {
					return Long.parseLong(propertyName);
				}
			}
			
			throw new EngineException("Unable to find the emulator ID with emulator technology \"" + emulatorTechnology + "\".");
		}
		catch(Exception e) {
			throw new EngineException("Unable to configure the emulator ID.", e);
		}
	}
    
	public byte[] getData() throws Exception {
		throw new IllegalArgumentException("The getData() method is not allowed within the Javelin connector!");
	}

	public String[] getAllScreenClassesNames() {
		List<JavelinScreenClass> vScreenClasses = getAllScreenClasses();
		String[] screenClassNames = new String[vScreenClasses.size()];
		int i = 0;
		for (ScreenClass screenClass : vScreenClasses) {
			screenClassNames[i] = screenClass.getName();
			i++;
		}
		return screenClassNames;
	}
	
	public transient Javelin javelin;
	
	public boolean checkKeys() {
		try {
			long emulatorID = findEmulatorId();
			TWSKey twsKey = new TWSKey();
			twsKey.CreateKey(52);
			
			boolean hasExpired = true;
			for (Object okey : KeyManager.keys.values()) {
				Key key = (Key) okey;
				if (key.emulatorID == emulatorID) {
					hasExpired &= (key.cv == 0) || twsKey.hasExpired(key.sKey);
				}
			}
			return !hasExpired;
		}
		catch (Exception e) {}
		return false;
	}

	@Override
	public void prepareForTransaction(Context context) throws EngineException {
		String t = context.statistics.start(EngineStatistics.GET_JAVELIN_OBJECT);
        
		try {
			// Quick and dirty workaround for ticket #1280
			if (!checkKeys()) throw new EngineException("No more key available; check your license keys!");
			
			// if something append during transaction execution
			com.twinsoft.api.Session session = Engine.theApp.sessionManager.getSession(context.contextID);
			if(session!=null) session.resetSomethingChange();
			
			if (Engine.isStudioMode()) {
				if (javelin != null) {
					Engine.logBeans.debug("(JavelinConnector) Using the studio Javelin object");
					return;
				}
				throw new EngineException("Studio mode: the Legacy connector must be open in order to execute transactions");
			}

			Engine.logBeans.debug("(JavelinConnector) Retrieving the Javelin object");
			
			JavelinTransaction javelinTransaction = null;
			try {
				javelinTransaction = (JavelinTransaction) context.requestedObject;
			}
			catch (ClassCastException e) {
				throw new EngineException("Requested object is not a transaction",e);
			}

			int timeout = javelinTransaction.getTimeoutForDataStable();
			int threshold = javelinTransaction.getDataStableThreshold();
		
			Authentication auth = null;

			if (context.isRequestFromVic) {
				// Check the VIC autorizations only if this is a non trusted
				// request, i.e. from a request not triggered from VIC (for
				// instance, from a web service call).
				if (!context.isTrustedRequest) {
					try {
						VicApi vicApi = new VicApi();
						if (!vicApi.isServiceAuthorized(context.tasUserName, context.tasVirtualServerName, context.tasServiceCode)) {
							throw new EngineException("The service '" + context.tasServiceCode + "' is not authorized for the user '" + context.tasUserName);
						}
					}
					catch(IOException e) {
						throw new EngineException("Unable to retrieve authorization from the VIC database.", e);
					}
				}
			}
			else {
				// Check the Carioca authorizations only if this is a non trusted request
				if (context.isTrustedRequest) {
					// Nothing to do: all the tas* variables from the context must have been set
					// by the caller.
					// Means we must not execute the TAS API.
				}
				else {
					// Getting Authentication object
					context.tasVirtualServerName = getVirtualServer();
	    
					try {
						String authName = (context.tasSessionKey == null ? context.tasUserName : context.tasSessionKey);
						auth = Engine.theApp.getAuthenticationObject(context.tasVirtualServerName, authName);
	        
						// Logging to Carioca only if needed
						User user = auth.getCurrentUser();
						if (user == null) {
							// Request from Carioca
							if (context.tasSessionKey != null) {
								auth.login(context.tasSessionKey);
								User currentUser = auth.getCurrentUser();
								context.tasUserName = currentUser.getName();
								context.tasUserPassword = currentUser.getPassword();
								context.tasVirtualServerName = "(SV #" + currentUser.getServerID() + ")";
								String message = "Authentication to Carioca with sesskey = '" + context.tasSessionKey + "' => user: \"" + context.tasUserName + "\"";
								Engine.logBeans.debug("(JavelinConnector) "+ message);
							}
							// Specific user
							else if ((context.tasUserName != null) && (context.tasUserPassword != null)) {
								auth.login(context.tasUserName, context.tasUserPassword);
								String message = "Authentication to Carioca with user = '" + context.tasUserName + "' and password = '" + context.tasUserPassword + "'";
								Engine.logBeans.debug("(JavelinConnector) "+ message);
							}
							else {
								context.tasUserName = EnginePropertiesManager.getProperty(PropertyName.CARIOCA_DEFAULT_USER_NAME);
								context.tasUserPassword = EnginePropertiesManager.getProperty(PropertyName.CARIOCA_DEFAULT_USER_PASSWORD);
								auth.login(context.tasUserName, context.tasUserPassword);
								String message = "Default authentication to Carioca";
								Engine.logBeans.debug("(JavelinConnector) "+ message);
							}
						}
						else {
							context.tasUserName = user.getName();
							context.tasUserPassword = user.getPassword();
							context.tasUserGroup = user.getMainGroupName();
							context.tasVirtualServerName = "(#" + Long.toString(user.getServerID()) + ")";
							String message = "Already authenticated to Carioca with user = '" + context.tasUserName + "' and password = '" + context.tasUserPassword + "'";
							Engine.logBeans.debug("(JavelinConnector) "+ message);
						}
					}
					catch(Exception e) {
						auth = null;
						String message =
							"Unable to authenticate to Carioca.\n" +
							"Carioca virtual server: " + context.tasVirtualServerName + "\n" +
							"SessKey: \"" + context.tasSessionKey + "\"\n" +
							"User: \"" + context.tasUserName + "\"\n" +
							"Password: \"" + context.tasUserPassword + "\"";
						EngineException ee = new EngineException(message, e);
						throw ee;
					}
				}
			}

			// We retrieve the current project
			String javelinServiceCode = context.tasServiceCode;
			if (javelinServiceCode == null) {
				javelinServiceCode = getServiceCode();
				Engine.logBeans.debug("(JavelinConnector)  No service code provided; getting the connector service code.");
			}

			// Analyzes/overrrides service code for device number pooling
			javelinServiceCode = analyzeServiceCode(javelinServiceCode);
			
			Engine.logBeans.debug("(JavelinConnector) Service code: " + javelinServiceCode);

			session = Engine.theApp.sessionManager.getSession(context.contextID);

			if (session == null) {
				Engine.logBeans.debug("(JavelinConnector) No session has been found; creation of a new one and ignoring the user request.");
				context.inputDocument = null;

				try {
					if (context.isRequestFromVic) {
						session = Engine.theApp.sessionManager.addVicSession(javelinServiceCode, context.tasUserName, context.tasUserGroup + "@" + context.tasVirtualServerName, context.tasDteAddress, context.tasCommDevice, context.contextID);
					}
					else if (context.isTrustedRequest) {
						String connectionParameters = getServiceCode();
						try {
							StringTokenizer st = new StringTokenizer(connectionParameters, ",");
							String appType = st.nextToken();
							if (appType.equals("vic")) {
								javelinServiceCode = st.nextToken();
								context.tasDteAddress = st.nextToken();
								context.tasCommDevice = st.nextToken();
								
								Engine.logBeans.debug("(JavelinConnector) Trusted request => the connector handles connection parameters : " + connectionParameters);
								Engine.logBeans.debug("(JavelinConnector) serviceCode: " + javelinServiceCode);
								Engine.logBeans.debug("(JavelinConnector) dteAddress: " + context.tasDteAddress);
								Engine.logBeans.debug("(JavelinConnector) commDevice: " + context.tasCommDevice);
								Engine.logBeans.debug("(JavelinConnector) user: " + context.tasUserName);
								String group = context.tasUserGroup + "@" + context.tasVirtualServerName;
								Engine.logBeans.debug("(JavelinConnector) group: " + group);
								session = Engine.theApp.sessionManager.addVicSession(javelinServiceCode, context.tasUserName, group, context.tasDteAddress, context.tasCommDevice, context.contextID);
							}
						}
						catch(NoSuchElementException e) {
							Engine.logBeans.error("(JavelinConnector) Invalid connector connection parameters: " + connectionParameters);
						}
					}
					else {
						session = Engine.theApp.sessionManager.addSession((int) emulatorID, auth,
								javelinServiceCode, context.contextID, getJavelinLanguage(), isSslEnabled(),
								isSslTrustAllServerCertificates(), getIbmTerminalType());
					}
				}
				catch(Exception e) {
					String message = "Unable to open the Javelin session: serviceCode= '" + javelinServiceCode + "', contextID = '" + context.contextID + "'";
					EngineException ee = new EngineException(message, e);
					throw ee;
				}

				if (session == null) {
					String message = "Unable to add a new session: the access to the service '" + javelinServiceCode + "' has been forbidden by the Carioca administrator.";
					EngineException ee = new EngineException(message);
					throw ee;
				}
    
				javelin = session.getJavelinObject();
				javelin.setLog(new LogWrapper(Engine.logEmulators));
				javelin.setDataStableOnCursorOn(false);

				javelin.connect(javelinTransaction.getTimeoutForConnect());
				boolean isConnected = javelin.isConnected();
				Engine.logBeans.debug("(JavelinConnector) isConnected=" + isConnected);
				if (!isConnected) {
					throw new ConnectionException("Unable to connect the session! See the emulator logs for more details...");
				}

				executeConnectionSyncCode(javelin, timeout, threshold);
			}
			else {
				Engine.logBeans.debug("(JavelinConnector) Using the existing session");				
				if (context.isNewSession) {
        
					Engine.logBeans.debug("(JavelinConnector) New session required and ignoring the user request");
					context.inputDocument = null;
				
					// First, we remove the previous session
					Engine.theApp.sessionManager.removeSession(context.contextID);
        
					try {
						if (context.isRequestFromVic) {
							session = Engine.theApp.sessionManager.addVicSession(javelinServiceCode, context.tasUserName, context.tasUserGroup, context.tasDteAddress, context.tasCommDevice, context.contextID);
						}
						else {
							session = Engine.theApp.sessionManager.addSession((int) emulatorID, auth,
									javelinServiceCode, context.contextID, getJavelinLanguage(),
									isSslEnabled(), isSslTrustAllServerCertificates(), getIbmTerminalType());
						}
					}
					catch(Exception e) {
						String message = "Unable to open the Javelin session: serviceCode= '" + javelinServiceCode + "', contextID = '" + context.contextID + "'";
						Engine.logBeans.warn("(JavelinConnector) " + message);
						EngineException ee = new EngineException(message, e);
						throw ee;
					}
        
					if (session == null) {
						String message = "Unable to add a new session: the access to the service '" + javelinServiceCode + "' has been forbidden by the Carioca administrator.";
						Engine.logBeans.warn("(JavelinConnector) " + message);
						EngineException ee = new EngineException(message);
						throw ee;
					}
        
					javelin = session.getJavelinObject();
					javelin.setLog(new LogWrapper(Engine.logEmulators));    
					javelin.setDataStableOnCursorOn(false);

					javelin.connect(javelinTransaction.getTimeoutForConnect());
					if (!javelin.isConnected()) {
						throw new ConnectionException("Unable to connect the session! See the emulator logs for more details...");
					}

					executeConnectionSyncCode(javelin, timeout, threshold);
				}
				else {
					javelin = session.getJavelinObject();
					
					// Reconnect only if the requested transaction is not the end transaction
					if ((!context.isDestroying) && (!session.isConnected())) {
						javelin.setDataStableOnCursorOn(false);

						javelin.connect(javelinTransaction.getTimeoutForConnect());
						if (!javelin.isConnected()) {
							throw new ConnectionException("Unable to connect the session! See the emulator logs for more details...");
						}

						executeConnectionSyncCode(javelin, timeout, threshold);
					}
				}
			}

			context.isNewSession = false;
		}
		finally {
			context.statistics.stop(t);
		}
	}
	
	public String analyzeServiceCode(String javelinServiceCode) {
		if (javelinServiceCode != null) {
			if (javelinServiceCode.indexOf("<POOL:") != 0) {
				int iStart = 0, iStop = 100, iIncr = 1, iDigits = 2;
				String connectorQName, sDevice;
				try {
					RE re = new RE("<POOL:([0-9]+)-([0-9]+)/([0-9]+)>");
					if (re.match(javelinServiceCode)) {
						if (re.getParenCount() == 4) {
							iStart = Integer.valueOf(re.getParen(1)).intValue();
							iStop = Integer.valueOf(re.getParen(2)).intValue();
							iDigits = Integer.valueOf(re.getParen(3)).intValue();
						}
					}
				} catch (Exception e) {
					// ignore
				}
				
				connectorQName = getQName();
				DevicePool devicePool = Engine.theApp.contextManager.getDevicePool(connectorQName,iStart,iStop,iIncr,iDigits);
				if (devicePool != null) {
					synchronized(devicePool) {
						Engine.logBeans.trace("(JavelinConnector) DevicePool for '"+ connectorQName +"':\n" + devicePool.toString());
						
						// retrieve device for the context, 
						// or available one if no device is associated with this context
						long contextNum = ((context == null) ? 1L:(long)context.contextNum);
						deviceID = devicePool.getDevice(contextNum);
						sDevice = devicePool.formatDevice(deviceID);
						
						// locks device
						Engine.logBeans.trace("(JavelinConnector) DevicePool for '"+ connectorQName +"' exist: locking deviceID "+ deviceID +" for context number " + contextNum);
						devicePool.lockContextDevice(deviceID, contextNum);
						
						// rewrite service code
						try {
							RE re = new RE("<POOL:([0-9]+)-([0-9]+)/([0-9]+)>");
							String sr = re.subst(javelinServiceCode,sDevice);
							javelinServiceCode = sr;
							Engine.logBeans.trace("(JavelinConnector) Overrides service code: " + javelinServiceCode);
						} catch (RESyntaxException e1) {
							// ignore
						}
					}
				}
			}
		}

		return javelinServiceCode;
	}
	
	private void executeConnectionSyncCode(Javelin javelin, int timeout, int threshold) throws EngineException {
		org.mozilla.javascript.Context javascriptContext = null;
		Scriptable scope = null;
		try {
			// Creating scripting context
			javascriptContext = org.mozilla.javascript.Context.enter();
			scope = javascriptContext.initStandardObjects(null);

			// Insert the Javelin object in the script scope
			Scriptable jsJavelin = org.mozilla.javascript.Context.toObject(javelin, scope);
			scope.put("javelin", scope, jsJavelin);
			
			// Insert the dataStableTimeout object in the script scope
			Scriptable jsDataStableTimeout = org.mozilla.javascript.Context.toObject(new Integer(timeout), scope);
			scope.put("timeout", scope, jsDataStableTimeout);
			
			// Insert the dataStableThreshold object in the script scope
			Scriptable jsDataStableThreshold = org.mozilla.javascript.Context.toObject(new Integer(threshold), scope);
			scope.put("threshold", scope, jsDataStableThreshold);

			javascriptContext.evaluateString(scope, connectionSyncCode, getName() + " - Connection synchronization code", 1, null);
		}
		catch(EcmaError e) {
			EngineException ee = new EngineException(
				"Unable to execute the connection synchronization code.\n" +
				"Transaction: \"" + getName() + "\"\n" +
				"A Javascript error has occured at line " + e.lineNumber() + ", column " + e.columnNumber() + ": " +
				e.getMessage() + "\n" +
				e.lineSource()
			);
			throw ee;
		}
		catch(EvaluatorException e) {
			EngineException ee = new EngineException(
				"Unable to execute the connection synchronization code.\n" +
				"Transaction: \"" + getName() + "\"\n" +
				"A Javascript evaluation error has occured: " + e.getMessage()
			);
			throw ee;
		}
		catch(JavaScriptException e) {
			throw new EngineException(
				"Unable to execute the connection synchronization code.\n" +
				"Transaction: \"" + getName() + "\"\n" +
				"Cause: " + e.getMessage()
			);
		}
		finally {
			if (javascriptContext != null) {
				org.mozilla.javascript.Context.exit();
			}
		}
	}

	/**
	 * The id of (Carioca) emulator on which Project technology is based.
	 */
	public transient long emulatorID = 0;

	/** Holds value of property connectionSyncCode. */
	private String connectionSyncCode = "javelin.waitForDataStable(timeout, threshold);";
    
	/** Getter for property connectionSyncCode.
	 * @return Value of property connectionSyncCode.
	 */
	public String getConnectionSyncCode() {
		return connectionSyncCode;
	}
    
	/** Setter for property connectionSyncCode.
	 * @param connectionSyncCode New value of property connectionSyncCode.
	 */
	public void setConnectionSyncCode(String connectionSyncCode) {
		this.connectionSyncCode = connectionSyncCode;
	}

	/** Holds value of property serviceCode. */
	private String serviceCode = "none";
    
	/** Holds value of property virtualServer. */
	private String virtualServer = "";

	/** Holds value of property emulatorTechnology. */
	private String emulatorTechnology = "";
    
	/** Getter for property emulatorTechnology.
	 * @return Value of property emulatorTechnology.
	 */
	public String getEmulatorTechnology() {
		return emulatorTechnology;
	}
    
	/** Setter for property emulatorTechnology.
	 * @param emulatorTechnology New value of property emulatorTechnology.
	 */
	public void setEmulatorTechnology(String emulatorTechnology) {
		this.emulatorTechnology = emulatorTechnology;
	}

	/** Getter for property serviceCode.
	 * @return Value of property serviceCode.
	 */
	public String getServiceCode() {
		return serviceCode;
	}
    
	/** Setter for property serviceCode.
	 * @param serviceCode New value of property serviceCode.
	 */
	public void setServiceCode(String serviceCode) {
		this.serviceCode = serviceCode;
	}
    
	/** Getter for property virtualServer.
	 * @return Value of property virtualServer.
	 */
	public String getVirtualServer() {
		return this.virtualServer;
	}
    
	/** Setter for property virtualServer.
	 * @param virtualServer New value of property virtualServer.
	 */
	public void setVirtualServer(String virtualServer) {
		this.virtualServer = virtualServer;
	}
	
	/**
	 * Waits for a new page for the same screen class or a new screen class.
	 * 
 	 * The method wait for one of the screens described by the screen classes in the
 	 * project to arrive. We wait for all the screen classes except the current one. In the
 	 * case of a next page on the same screen class, waitNextPage() will monitor the cursor
 	 * position. the method will return when the cursor position returns to the same position
 	 * it was before calling waitNextPage().  
	 *  
	 * You can use waitNextPage() method to synchronize your handler before returning
	 * "redetect", "accumulate" or "skip".
	 * 
	 * @param context 		the Convertigo context.
	 * @param timeout		the time (in ms) we have to wait for the screen class.
	 * @param hardDelay		a delay (in ms) added after the nextpage has arrived.
	 * 
	 * @return				true, if we the page did arrive, false otherwise.
	 */
	public boolean waitNextPage(Context context, String action, int timeout, int hardDelay) throws EngineException {
		if (javelin != null) {
			try {
				List<JavelinScreenClass> vScreenClasses = getAllScreenClasses();
				List<Criteria> vCriterias;
				List<Criteria> vAllCriterias = new Vector<Criteria>(128);
				RegularExpression regularExpression;
				FindString findString;

				javelin.deleteWaitAts();
		
				ScreenClass lastDetectedScreenClass = (ScreenClass)context.lastDetectedObject;
				
				// Set all screen class criterias observations
				int j = 1;
				for (ScreenClass screenClass : vScreenClasses) {
					Engine.logBeans.debug("(JavelinConnector) Analyzing screen class \"" + screenClass.getName() + "\"");

					// Do not detect the current screen class
					if ((lastDetectedScreenClass != null) && (screenClass.getQName().equals(lastDetectedScreenClass.getQName()))) {
						Engine.logBeans.debug("(JavelinConnector) Ignoring screen class \"" + screenClass.getName() + "\"");
						continue;
					}
				
					vCriterias = screenClass.getLocalCriterias();
					for (Criteria criteria : vCriterias) {
						if (criteria instanceof RegularExpression) {
							regularExpression = (RegularExpression) criteria;
					
							if ((regularExpression.getX() == -1) || (regularExpression.getX() == -1)) {
								Engine.logBeans.debug("(JavelinConnector) Ignoring criteria \"" + regularExpression.getRegularExpression() + "\" at (" + regularExpression.getX() + ", " + regularExpression.getY() + ")");
							}
							else {
								Engine.logBeans.debug("(JavelinConnector) Adding a wait at ID: " + j +" \"" + regularExpression.getRegularExpression() + "\" at (" + regularExpression.getX() + ", " + regularExpression.getY() + ")");
								javelin.waitAtId(j, regularExpression.getRegularExpression(), regularExpression.getX(), regularExpression.getY());
								vAllCriterias.add(criteria);
								j++;
							}
						}
					}
				}
		
				// Wait for the cursor to move out from the current location
				int line = javelin.getCurrentLine();
				int col  = javelin.getCurrentColumn();
				Engine.logBeans.debug("(JavelinConnector) WaitNextPage(): cursor is now in col: " + col + ", line: " + line);
		
				javelin.waitCursorAtId(1, col, line, false);
				javelin.doAction(action);
				int ret = javelin.waitTrigger(timeout);
				if (ret == -1) {
					Engine.logBeans.debug("(JavelinConnector) WaitNextPage(): cursor did not move out in time");
					return false;
				}
			
				// Set one more observation ID on the cursor position
				Engine.logBeans.debug("(JavelinConnector) Adding a waitCursorAt at ID: " + 0 + " in col: " + col + ", line: " + line);
				javelin.waitCursorAtId(0, col, line, true);
		
				j = javelin.waitTrigger(timeout);
				javelin.deleteWaitAts();
		
				Criteria criteria;
				if (j != -1) {
					if (j < vAllCriterias.size()) {
						criteria = (Criteria) vAllCriterias.get(j);
						String string = "?";
						int x = -1;
						int y = -1;
						if (criteria instanceof FindString) {
							findString = (FindString) criteria;
							string = findString.getString();
							x = findString.getX();
							y = findString.getY();
						}
						else if (criteria instanceof RegularExpression) {
							regularExpression = (RegularExpression) criteria;
							string = regularExpression.getRegularExpression();
							x = regularExpression.getX();
							y = regularExpression.getY();
						}
						Engine.logBeans.debug("(JavelinConnector) WaitNextPage(): found criteria ID:" + j + " \"" + string + "\" at (" + x + ", " + y + ")");
					}
					else { 
						Engine.logBeans.debug("(JavelinConnector) WaitNextPage(): cursor back in col: " + col + ", line: " + line);
					}
				
					if (hardDelay > 0) {			
						javelin.waitSync(hardDelay);
					}
				
					return true;
				}

				Engine.logBeans.debug("(JavelinConnector) WaitNextPage(): automatic data stable event detection aborted because of timeout!");
				return false;
			}
			catch(Exception e) {
				Engine.logBeans.error("Exception while trying to wait next page", e);
				throw new EngineException("Unable to wait for the next page! See the emulator log for more details...", e);
			}
		}
		else {
			throw new EngineException("Unable to call the waitNextPage() function without a Javelin emulator!");
		}
	}
	
	/**
	 * Waits for one of the screens described by the screen classes
	 * in the project to arrive. The method waits for all the screen classes
	 * except the current one. You can use waitAtScreenClass() method to synchronize
	 * your handler before returning "redetect", "accumulate" or "skip".
	 * 
	 * @param context		the Convertigo context.
	 * @param timeout		the time (in ms) we have to wait for the screen class.
	 * @param hardDelay		a delay (in ms) added after the screen class has arrived.
	 * 
	 * @return				true, if we the screen did arrive, false otherwise.
	 */
	public boolean waitAtScreenClass(Context context, int timeout, int hardDelay) throws EngineException {
		if (javelin != null) {
			try {
				List<JavelinScreenClass> vScreenClasses = getAllScreenClasses();
				List<Criteria> vCriterias;
				List<Criteria> vAllCriterias = new Vector<Criteria>(128);
				RegularExpression regularExpression;
				FindString findString;
				int j = 0;
			
				ScreenClass lastDetectedScreenClass = (ScreenClass)context.lastDetectedObject;
				
				for (ScreenClass screenClass : vScreenClasses) {
					// Do not detect the current screen class
					if ((lastDetectedScreenClass != null)&& (screenClass.getName().equals(lastDetectedScreenClass.getName()))) {
						Engine.logBeans.debug("(JavelinConnector) WaitAtScreenClass(): last detected screen class is " + lastDetectedScreenClass.getName());
						continue;
					}
					
					vCriterias = screenClass.getLocalCriterias();
					for (Criteria criteria : vCriterias) {
						if (criteria instanceof FindString) {
							findString = (FindString) criteria;
						
							if ((findString.getX() == -1) || (findString.getX() == -1)) {
								Engine.logBeans.debug("(JavelinConnector) Ignoring criteria \"" + findString.getString() + "\" at (" + findString.getX() + ", " + findString.getY() + ")");
							}
							else {
								Engine.logBeans.debug("(JavelinConnector) Adding a wait at ID:" + j +" \"" + findString.getString() + "\" at (" + findString.getX() + ", " + findString.getY() + ")");
								javelin.waitAtId(j, findString.getString(), findString.getX(), findString.getY());
								vAllCriterias.add(criteria);
								j++;
							}
						}
						else if (criteria instanceof RegularExpression) {
							regularExpression = (RegularExpression) criteria;
						
							if ((regularExpression.getX() == -1) || (regularExpression.getX() == -1)) {
								Engine.logBeans.debug("(JavelinConnector) Ignoring criteria \"" + regularExpression.getRegularExpression() + "\" at (" + regularExpression.getX() + ", " + regularExpression.getY() + ")");
							}
							else {
								Engine.logBeans.debug("(JavelinConnector) Adding a wait at ID:" + j +" \"" + regularExpression.getRegularExpression() + "\" at (" + regularExpression.getX() + ", " + regularExpression.getY() + ")");
								javelin.waitAtId(j, regularExpression.getRegularExpression(), regularExpression.getX(), regularExpression.getY());
								vAllCriterias.add(criteria);
								j++;
							}
						}
					}
				}
			
				j = javelin.waitTrigger(timeout);
				javelin.deleteWaitAts();
			
				Criteria criteria;
				if (j != -1) {
					criteria = (Criteria) vAllCriterias.get(j);
					String string = "?";
					int x = -1;
					int y = -1;
					if (criteria instanceof FindString) {
						findString = (FindString) criteria;
						string = findString.getString();
						x = findString.getX();
						y = findString.getY();
					}
					else if (criteria instanceof RegularExpression) {
						regularExpression = (RegularExpression) criteria;
						string = regularExpression.getRegularExpression();
						x = regularExpression.getX();
						y = regularExpression.getY();
					}
					Engine.logBeans.debug("(JavelinConnector) WaitAtScreenClass(): found criteria ID:" + j + " \"" + string + "\" at (" + x + ", " + y + ")");
	
					if (hardDelay > 0) {			
						javelin.waitSync(hardDelay);
					}
					
					return true;
				}
	
				Engine.logBeans.debug("(JavelinConnector) Automatic data stable event detection aborted because of timeout!");
				return false;
			}
			catch(Exception e) {
				Engine.logBeans.error("Exception while trying to wait at screen class", e);
				throw new EngineException("Unable to wait at screen class! See the emulator log for more details...", e);
			}
		}
		else {
			throw new EngineException("Unable to call the waitAtScreenClass() function without a Javelin emulator!");
		}
	}
	
	private int javelinLanguage = 0;
	
	public int getJavelinLanguage() {
		return javelinLanguage;
	}

	public void setJavelinLanguage(int javelinLanguage) {
		this.javelinLanguage = javelinLanguage;
	}
	
	private transient int deviceID = 0;
	
	public int getDeviceID() {
		return deviceID;
	}
	
	/**
	 * Returns the selected zone from the connector
	 * @return XMLRectangle
	 */
	@Override
	public XMLRectangle getSelectionZone() {
		Rectangle rTmp = javelin.getSelectionZone();
		return (new XMLRectangle(rTmp.x, rTmp.y, rTmp.width, rTmp.height));
	}

	@Override
	public JavelinTransaction newTransaction() {
		return new JavelinTransaction();
	}
	
	public JavelinScreenClass newScreenClass() {
		return new JavelinScreenClass();
	}
}