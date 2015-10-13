/*
* Copyright (c) 2001-2014 Convertigo. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javapns.Push;
import javapns.notification.Payload;
import javapns.notification.PushNotificationPayload;
import javapns.notification.PushedNotification;
import javapns.notification.PushedNotifications;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
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
	private ApnsNotificationType apnsNotificationType = ApnsNotificationType.Message;
	private String GCMApiKey = "\"<configure your api key here>\"";
	private int    AndroidTimeToLive = 3600;
	
	private transient StepSource tokenSource = null;
	private transient String     sClientCertificate;
	private transient String     sCertificatePassword;
	private transient String	 sNotificationTitle;
	private transient String     sPayload;
	private transient String 	 sGCMApiKey;

	
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

	protected void PushToGCM(Context javascriptContext, Scriptable scope, Map<String, String> dictionary) throws EngineException, Exception
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
				
				if (devicesList.isEmpty())
					return;

				evaluate(javascriptContext, scope, this.notificationTitle, "notificationTitle", false);
				sNotificationTitle = evaluated instanceof Undefined ? "" : evaluated.toString();
				
				// use this line to send message with payload data
				Message.Builder builder = new Message.Builder()										
											.collapseKey("1") 
											.timeToLive(AndroidTimeToLive)
											.delayWhileIdle(true);
				
				// add all dictionary entries in turn
				for(Map.Entry<String, String> e : dictionary.entrySet())
				    builder.addData(e.getKey(), e.getValue());
				
				Message message = builder.build(); 
		
				// Use this for multicast messages 
				MulticastResult result = sender.send(message, devicesList, 1); 
				sender.send(message, devicesList, 1); 
				Engine.logBeans.debug("Push notification, Android devices notified: " + result.toString());
	
				if (result.getResults() != null) { 
					int canonicalRegId = result.getCanonicalIds(); 
					if (canonicalRegId != 0) { 
					} 
				} else { 
					int error = result.getFailure(); 
					Engine.logBeans.error("Push notification, Android device error: " + error);
				}
			}
		} catch (Exception e) { 
			Engine.logBeans.error("Push notification, Android device exception: " + e);
		} 
	}
		
	//
	// seems like total size of Payload cannot exceed 256 bytes.
	//
	protected void PushToAPNS(Context javascriptContext, Scriptable scope, Map<String, String> dictionary) throws EngineException, Exception
	{
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
			
			// Submit the push to JavaPN library...
			PushedNotifications pn;
			
			if (dictionary.size() > 1) {
				String str = null;
				/* Build a blank payload to customize */ 
		        PushNotificationPayload payload = PushNotificationPayload.complex();
		        if ((str = dictionary.get("alert")) != null)
		        	payload.addAlert(str);

		        if ((str = dictionary.get("badge")) != null)
		        	payload.addBadge(Integer.parseInt(str, 10));

		        if ((str = dictionary.get("sound")) != null)
		        	payload.addSound(str);
		        
				// add all dictionary entries in turn
				for(Map.Entry<String, String> e : dictionary.entrySet()) {
					if (e.getKey().equalsIgnoreCase("alert"))
						continue;
					if (e.getKey().equalsIgnoreCase("badge"))
						continue;
					if (e.getKey().equalsIgnoreCase("sound"))
						continue;
					payload.addCustomDictionary(e.getKey(), e.getValue());
				}

				pn = Push.payload(payload, 
							sClientCertificate,
							sCertificatePassword,
							true,
							devicesList);
			}
			else {
				if (apnsNotificationType == ApnsNotificationType.Message) {				
					pn = Push.alert(dictionary.get("alert"),
									sClientCertificate,
									sCertificatePassword,
									true,
									devicesList);
					
				} else if (apnsNotificationType == ApnsNotificationType.Badge) {	// mod jmc 07/10/2015
					pn = Push.badge(Integer.parseInt(dictionary.get("badge"), 10),
									sClientCertificate,
									sCertificatePassword,
									true,
									devicesList);
					
				} else { 
					pn = Push.sound(dictionary.get("sound"),
									sClientCertificate,
									sCertificatePassword,
									true,
									devicesList);
				}
			}
			
			// Analyze the responses..
			Iterator<PushedNotification>  iPn  = pn.iterator();
			while (iPn.hasNext()) {
				PushedNotification notif = iPn.next();
				Engine.logBeans.debug("Push notification :" + notif + " Successful : " + notif.isSuccessful());
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
	public static void enumAllStrings(Node node, Map<String, String> dictionary) {
		if (node == null)
			return;

		String value = node.getFirstChild().getNodeValue();
		if (value != null)
			dictionary.put(node.getNodeName(), node.getFirstChild().getNodeValue());

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
			if (super.stepExecute(javascriptContext, scope)) {
				// get Source data as a string to payload
				StepSource stepSource = getSource();
				NodeList list;
				
				list = stepSource.inError() ? null : stepSource.getContextOutputNodes();
				
				if (list != null) {
					Map<String, String> dictionary = new HashMap<String, String>();
					NodeList childNodes = list.item(0).getChildNodes();
					
					if (childNodes.getLength() > 0)
						enumAllStrings(list.item(0), dictionary);
					else
						dictionary.put(list.item(0).getParentNode().getNodeName(), list.item(0).getNodeValue());
					
					try {
						PushToAPNS(javascriptContext, scope, dictionary);
						PushToGCM(javascriptContext, scope, dictionary);
					} catch (EngineException e) {
						throw e;
					} catch (Exception e) {
						throw new EngineException(e.getClass().getSimpleName() + " during push notification", e);
					}
				}
			}
		}
		return false;
	}
	
	public String toJsString() {
		return "";
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
