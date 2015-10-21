/*
* Copyright (c) 2001-2015 Convertigo. All Rights Reserved.
*
* The copyright to the computer  program(s) herein  is the property
* of Convertigo.
* The program(s) may  be used  and/or copied  only with the written
* permission  of  Convertigo  or in accordance  with  the terms and
* conditions  stipulated  in the agreement/contract under which the
* program(s) have been supplied.
*
* Convertigo makes  no  representations  or  warranties  about  the
* suitability of the software, either express or implied, including
* but  not  limited  to  the implied warranties of merchantability,
* fitness for a particular purpose, or non-infringement. Convertigo
* shall  not  be  liable for  any damage  suffered by licensee as a
* result of using,  modifying or  distributing this software or its
* derivatives.
*/

/*
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.steps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javapns.Push;
import javapns.communication.exceptions.CommunicationException;
import javapns.communication.exceptions.KeystoreException;
import javapns.notification.PushNotificationPayload;
import javapns.notification.PushedNotification;
import javapns.notification.PushedNotifications;
import javapns.notification.ResponsePacket;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Visibility;

public class PushNotificationStep extends Step implements IStepSourceContainer {

	
	private static final long serialVersionUID = 3915732415195665643L;
	
	public static int NB_RETRIES=1;
	
	public enum ApnsNotificationType {
		Message,
		Badge,
		Sound
	}
	
	private XMLVector<String> sourceDefinition = new XMLVector<String>();
	private XMLVector<String> tokens =  new XMLVector<String>();
	
	private String clientCertificate = "\".//<client certificate>.p12\"";
	private String certificatePassword = "\"<your .p12 certificate password>\"";
	private String notificationTitle = "\"TITLE\"";
	private String errorMessage = "";
	private ApnsNotificationType apnsNotificationType = ApnsNotificationType.Message;
	private String GCMApiKey = "\"<configure your api key here>\"";
	private int    AndroidTimeToLive = 3600;
	
	private transient StepSource tokenSource = null;
	private transient String     sClientCertificate;
	private transient String     sCertificatePassword;
	private transient String 	 sGCMApiKey;
	private transient JSONArray  errorList; 

	// child class
	private class Parameters {
		String name;
		String plug;
		String type;
		String value;
		
		public Parameters(String name, String plug, String type, String value) {
			this.name = (name == null) ? "":name;
			this.plug = (plug == null) ? "":plug;
			this.type = (type == null) ? "":type;
			this.value = (value == null) ? "":value;
		}
    };
	
	public PushNotificationStep() {
		super();
	}
	
	public int getAndroidTimeToLive() {
		return AndroidTimeToLive;
	}

	public void setAndroidTimeToLive(int androidTimeToLive) {
		AndroidTimeToLive = androidTimeToLive;
	}
	
	protected StepSource getTokenSource() {
		if (tokenSource == null) {
			tokenSource = new StepSource(this, tokens);
		}
		return tokenSource;
	}

	public XMLVector<String> getSourceDefinition() {
		return sourceDefinition;
	}

	public void setSourceDefinition(XMLVector<String> sourceDefinition) {
		this.sourceDefinition = sourceDefinition;
	}	
	
	
	public XMLVector<String> getTokens() {
		return tokens;
	}
	
	public void setTokens(XMLVector<String> nTokens) {
		this.tokens = nTokens;
	}


	public String getGCMApiKey() {
		return GCMApiKey;
	}

	public void setGCMApiKey(String gCMApiKey) {
		GCMApiKey = gCMApiKey;
	}

	public String getClientCertificate() {
		return this.clientCertificate;
	}
	
	public void setClientCertificate(String nClientCertificate) {
		this.clientCertificate = nClientCertificate;
	}

	public String getCertificatePassword() {
		return this.certificatePassword;
	}
	
	public void setCertificatePassword(String nCertificatePassword) {
		this.certificatePassword = nCertificatePassword;
	}
	
	public ApnsNotificationType getApnsNotificationType() {
		return apnsNotificationType;
	}

	public void setApnsNotificationType(ApnsNotificationType apnsNotificationType) {
		this.apnsNotificationType = apnsNotificationType;
	}

	@Override
    public PushNotificationStep clone() throws CloneNotSupportedException {
    	PushNotificationStep clonedObject = (PushNotificationStep) super.clone();
        return clonedObject;
    }

	@Override
	public PushNotificationStep copy() throws CloneNotSupportedException {
		PushNotificationStep copiedObject = (PushNotificationStep) super.copy();
		return copiedObject;
	}

	@Override
	public String toString() {
		return "Push Notification";
	}

	protected String getAbsoluteFilePath(String entry) throws EngineException {
		if (entry.equals(""))
			throw new EngineException("Please fill the Source property field.");

		return Engine.theApp.filePropertyManager.getFilepathFromProperty(entry, getProject().getName());
	}

	private void saveErrorForOutput(String plugin, String regId, String messageId, String canonicalRegId, String errorType) {
		try {
			JSONObject jso = new JSONObject();
			jso.put("plugIn", plugin);
			jso.put("regId", regId);
			jso.put("messageId", messageId);
			jso.put("canonicalRegId", canonicalRegId);
			jso.put("errorType", "" + errorType);
			errorList.put(jso);
		}
		catch(JSONException e) {			
		}
	}
	
	protected void PushToGCM(Context javascriptContext, Scriptable scope, List<Parameters> dictionary) throws EngineException, Exception
	{
		Engine.logBeans.debug("Push notification, Notifying Android devices");
		try {
			if (dictionary == null) {
				Engine.logBeans.debug("Push notification, dictionary empty");
				return;
			}
			
			evaluate(javascriptContext, scope, this.GCMApiKey, "gcmapikey", false);
			sGCMApiKey = evaluated instanceof Undefined ? "" : evaluated.toString();
			
			Sender sender = new Sender(sGCMApiKey); 

			// get Token List
			StepSource tokens = getTokenSource();
			NodeList list;
			list = tokens.inError() ? null : tokens.getContextOutputNodes();
			if (list != null) {
				ArrayList<String> devicesList = new ArrayList<String>(); 
				for (int i=0; i< list.getLength(); i++) {
					String token = getNodeValue(list.item(i));
					if (token.startsWith("gcm:")) {
						devicesList.add(token.substring(4));
						Engine.logBeans.trace("Push notification, Android device " + token.substring(4) + " will be notified");
					}
				}
				
				if (devicesList.isEmpty()) {
					Engine.logBeans.debug("Push notification, device list empty");
					return;
				}

				// use this line to send message with payload data
				Message.Builder builder = new Message.Builder()										
											.collapseKey("1") 
											.timeToLive(AndroidTimeToLive)
											.delayWhileIdle(true);
				
				// add all dictionary entries in turn				
				for(int i=0; i<dictionary.size(); i++) {
					// if plugin specified, check it and skip accordingly
					if ((dictionary.get(i).plug.length() != 0) && !(dictionary.get(i).plug.equalsIgnoreCase("gcm") || dictionary.get(i).plug.equalsIgnoreCase("all"))) 
						continue;
					
					// for compatibility with former Convertigo versions
					// if only one dictionary entry, hardcode key as "message"
					if (dictionary.size() == 1) {
						builder.addData("message", dictionary.get(i).value);
						break;
					}

					builder.addData(dictionary.get(i).name, dictionary.get(i).value);
				}
				
				Message message = builder.build(); 
		
				// Use this for multicast messages
				MulticastResult multicastResult;
				
				try {
					multicastResult = sender.send(message, devicesList, NB_RETRIES);
				} catch(IOException e) {
					errorMessage = "Push notification, error posting Android messages " + e.toString();
					Engine.logBeans.debug(errorMessage);
					return;
				}
 
				if (multicastResult.getResults() != null) {
					Engine.logBeans.debug("Push notification, Android devices notified: " + multicastResult.toString());
					
					List<Result> results = multicastResult.getResults();
					// analyze the result for each device

					for (int i=0; i<devicesList.size(); i++) {
						Result result = results.get(i);
						String regId = devicesList.get(i);
						String messageId = result.getMessageId();
						String canonicalRegId = result.getCanonicalRegistrationId();
						if (messageId != null) {
							Engine.logBeans.info("Push notification, succesfully sent message to device: " + regId + "; messageId = " + messageId);
							canonicalRegId = result.getCanonicalRegistrationId();
				            if (canonicalRegId != null) {
				            	saveErrorForOutput("gcm", regId, messageId, canonicalRegId, "Device registered more than once");				            	
				              // same device has more than on registration id: update it
				            	Engine.logBeans.info("Push notification, warning, same device has more than on registration canonicalRegId " + canonicalRegId);
				            }
						}
						else {
							String error = result.getErrorCodeName();
							
				            if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
				            	saveErrorForOutput("gcm", regId, messageId, canonicalRegId, "Application removed from device");
				            	
				              // application has been removed from device - unregister it
				            	Engine.logBeans.info("Push notification, unregistered device: " + regId);
				            } else {
				            	saveErrorForOutput("gcm", regId, "", canonicalRegId, error);
				            	Engine.logBeans.debug("Push notification, error sending message to '" + regId + "': " + error);
				            }
						}
					}
				} else { 
					errorMessage = "Push notification, Android device error: " + multicastResult.getFailure(); 
					Engine.logBeans.error(errorMessage);
				}
			}
		} catch (Exception e) { 
			errorMessage = "Push notification, Android device exception: " + e;
			Engine.logBeans.error(errorMessage);
		} 
	}
		
	//
	// seems like total size of Payload cannot exceed 256 bytes.
	//
	protected void PushToAPNS(Context javascriptContext, Scriptable scope, List<Parameters> dictionary) throws EngineException, Exception
	{
		Engine.logBeans.debug("Push notification, Notifying IOS devices");
		
		evaluate(javascriptContext, scope, this.clientCertificate, "clientCertificate", false);
		sClientCertificate = evaluated instanceof Undefined ? "" : evaluated.toString();
		sClientCertificate = getAbsoluteFilePath(sClientCertificate);

		evaluate(javascriptContext, scope, this.certificatePassword, "certificatePassword", false);
		sCertificatePassword = evaluated instanceof Undefined ? "" : evaluated.toString();

		// get Token List
		StepSource tokens = getTokenSource();
		NodeList list;
		list = tokens.inError() ? null : tokens.getContextOutputNodes();
		if (list != null) {
			ArrayList<String> devicesList = new ArrayList<String>(); 
			
			for (int i=0; i<list.getLength(); i++) {
				String token = getNodeValue(list.item(i));
				if (token.startsWith("apns:")) {
					devicesList.add(token.substring(5));
					Engine.logBeans.trace("Push notification, iOS device " + token.substring(5) + " will be notified");
				}
			}

			if (devicesList.isEmpty())
				return;
			
			try {
				// Submit the push to JavaPN library...
				PushedNotifications pn;
				
				if (dictionary.size() > 1) {
					/* Build a blank payload to customize */ 
			        PushNotificationPayload payload = PushNotificationPayload.complex();
			        
			        for(int i=0; i<dictionary.size(); i++) {
						// if plugin specified, check it and skip accordingly
			        	if ((dictionary.get(i).plug.length() != 0) && !(dictionary.get(i).plug.equalsIgnoreCase("aps") || dictionary.get(i).plug.equalsIgnoreCase("all"))) 
							continue;
			        	
			        	String value = dictionary.get(i).value;
			        	
				        if (dictionary.get(i).name.equalsIgnoreCase("alert"))
				        	payload.addAlert(value);
				        else
			        	if (dictionary.get(i).name.equalsIgnoreCase("badge"))
				        	payload.addBadge(Integer.parseInt(value, 10));
				        else
			        	if (dictionary.get(i).name.equalsIgnoreCase("sound"))
				        	payload.addSound(value);
			        	else {
							if (dictionary.get(i).type.equalsIgnoreCase("int"))
								payload.addCustomDictionary(dictionary.get(i).name, Integer.parseInt(value, 10));
							else
								payload.addCustomDictionary(dictionary.get(i).name, value);
			        	}
			        }
	
					pn = Push.payload(payload, 
								sClientCertificate,
								sCertificatePassword,
								true,
								devicesList);
				}
				else {
					if (apnsNotificationType == ApnsNotificationType.Message) {				
						pn = Push.alert(dictionary.get(0).value,
										sClientCertificate,
										sCertificatePassword,
										true,
										devicesList);
						
					} else if (apnsNotificationType == ApnsNotificationType.Badge) {	// mod jmc 07/10/2015
						pn = Push.badge(Integer.parseInt(dictionary.get(0).value, 10),
										sClientCertificate,
										sCertificatePassword,
										true,
										devicesList);
						
					} else { 
						pn = Push.sound(dictionary.get(0).value,
										sClientCertificate,
										sCertificatePassword,
										true,
										devicesList);
					}
				}
				
				// Analyze the responses..
				for (PushedNotification notification : pn) {
	                if (!notification.isSuccessful()) {
                        String invalidToken = notification.getDevice().getToken();

                        /* Find out more about what the problem was */  
                        Exception theProblem = notification.getException();

                        /* If the problem was an error-response packet returned by Apple, get it */  
                        ResponsePacket theErrorResponse = notification.getResponse();
                        
                        if (theErrorResponse != null)
                        	saveErrorForOutput("aps", invalidToken, ""+theErrorResponse.getIdentifier(), "", theErrorResponse.getMessage());
                        else
                        	saveErrorForOutput("aps", invalidToken, "", "", theProblem.getMessage());
	                }
				}
			}
			catch (KeystoreException e) {
				errorMessage = e.toString();
				Engine.logBeans.error("Push notification, IOS keystore exception : " + errorMessage);
			} catch (CommunicationException e) {
				/* A critical communication error occurred while trying to contact Apple servers */  
				errorMessage = e.toString();
				Engine.logBeans.error("Push notification, IOS communication exception : " + errorMessage);
			} catch (Exception e) { 
				errorMessage = "Push notification, IOS device exception: " + e.toString();
				Engine.logBeans.error(errorMessage);
			} 
		}
	}

	/**
	 *  recurse in nodelist and put in dictionnary couples 'variableName, variableValue'
	 * 	variables with no values are not used
	 * 
	 * @param node
	 * @param dictionary
	 */
	public void enumAllStrings(Node node, List<Parameters> dictionary) {
		if (node == null)
			return;

		String value = node.getFirstChild().getNodeValue();

		if (value != null) {
			PushNotificationStep.Parameters entry = this.new Parameters(node.getNodeName(), ((Element)node).getAttribute("plugin"), ((Element)node).getAttribute("type"), node.getFirstChild().getNodeValue()); 
			dictionary.add(entry);
			// dictionary.put(node.getNodeName(), node.getFirstChild().getNodeValue());
		}

	    NodeList nodeList = node.getChildNodes();
	    for (int i = 0; i < nodeList.getLength(); i++) {
	        Node currentNode = nodeList.item(i);
	        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
	            //calls this method for all the children which is Element
	        	enumAllStrings(currentNode, dictionary);
	        }
	    }
	}
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			 errorList = new JSONArray();
				
			// get Source data as a string to payload
			StepSource stepSource = getSource();
			NodeList list = stepSource.inError() ? null : stepSource.getContextOutputNodes();
			
			if (list != null) {
				List<Parameters> dictionary = new ArrayList<PushNotificationStep.Parameters>();
				NodeList childNodes = list.item(0).getChildNodes();
				
				if (childNodes.getLength() > 0)
					enumAllStrings(list.item(0), dictionary);
				else
					dictionary.add(new PushNotificationStep.Parameters(list.item(0).getParentNode().getNodeName(), ((Element)list.item(0).getParentNode()).getAttribute("plugin"), "string", list.item(0).getNodeValue()));
				
				try {	
					PushToAPNS(javascriptContext, scope, dictionary);
					PushToGCM(javascriptContext, scope, dictionary);
				} catch (EngineException e) {
					errorMessage = e.toString();
					throw e;
				} catch (Exception e) {
					errorMessage = e.getClass().getSimpleName() + " during push notification";
					throw new EngineException(e.getClass().getSimpleName() + " during push notification", e);
				}
			}
			
			if (super.stepExecute(javascriptContext, scope)) {
				return true;
			}
		}
		
		return false;
	}
	
	public String toJsString() {
		return "";
	}

	@Override
	public String getStepNodeName() {
		return "pushnotification";
	}	

@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		String plugIn;
		String regId;		
		String messageId;
		String canonicalRegId;
		String errorType;

		try {	
			Element device, devices, element;

			if (errorMessage.length() != 0) {
            	element = doc.createElement("errorMessage");
            	element.setTextContent(errorMessage);
            	stepNode.appendChild(element);
			}
			
			if (errorList.length() != 0) {
	        	devices = doc.createElement("devices");
	        	stepNode.appendChild(devices);
	
				for(int i=0; i<errorList.length(); i++) {
					JSONObject jso = (JSONObject)errorList.get(i);

					try {
						plugIn = jso.getString("plugIn");
					} catch(JSONException j1) {
						plugIn = "";
					}
					try {
						regId = jso.getString("regId");
					} catch(JSONException j1) {
						regId = "";
					}
					try {
						messageId = jso.getString("messageId");
					} catch(JSONException j2) {
						messageId = "";
					}
					try {
						canonicalRegId = jso.getString("canonicalRegId");
					} catch(JSONException j3) {
						canonicalRegId = "";
					}
					try {
						errorType = jso.getString("errorType");
					} catch(JSONException j4) {
						errorType = "";
					}
					
	            	device = doc.createElement("device");
	            	device.setAttribute("plugIn", plugIn);
	            	device.setAttribute("regId", regId);
	            	device.setAttribute("messageId", messageId);
	            	device.setAttribute("canonicalRegId", (canonicalRegId == null) ? "":canonicalRegId);
	            	device.setAttribute("errorType", errorType);
	            	devices.appendChild(device);
				}
			}
		}
		catch(JSONException e) {
		}
	}
	
    @Override
	public void configure(Element element) throws Exception {
		super.configure(element);
    }
    
	@Override
	public boolean isMaskedProperty(Visibility target, String propertyName) {
		if ("certificatePassword".equals(propertyName)) {
			return true;
		}
		return super.isMaskedProperty(target, propertyName);
	}

	@Override
	public boolean isCipheredProperty(String propertyName) {
		if ("certificatePassword".equals(propertyName)) {
			return true;
		}
		return super.isCipheredProperty(propertyName);
	}


	public String getNotificationTitle() {
		return notificationTitle;
	}


	public void setNotificationTitle(String notificationTitle) {
		this.notificationTitle = notificationTitle;
	}
}
