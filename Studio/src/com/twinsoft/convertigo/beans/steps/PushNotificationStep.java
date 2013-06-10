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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/beans/steps/SmtpStep.java $
 * $Author: nicolasa $
 * $Revision: 32814 $
 * $Date: 2012-12-03 17:03:22 +0100 (lun., 03 déc. 2012) $
 */

package com.twinsoft.convertigo.beans.steps;

import java.util.Iterator;

import javapns.Push;
import javapns.notification.PushedNotification;
import javapns.notification.PushedNotifications;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Visibility;

public class PushNotificationStep extends Step implements IStepSourceContainer, ITagsProperty {

	
	private static final long serialVersionUID = 3915732415195665643L;
	
	
	private XMLVector<String> sourceDefinition = new XMLVector<String>();
	private XMLVector<String> tokens =  new XMLVector<String>();
	
	private String pushType = "APNS";
	private String clientCertificate = "\".//<client certificate>.p12\"";
	private String certificatePassword = "<your .p12 certificate password>";
	private String apnsNotificationType = "Message";
	
	private transient StepSource source = null;
	private transient StepSource tokenSource = null;
	private transient String     sClientCertificate;
	private transient String     sCertificatePassword;
	private transient String     sPayload;
	
	public PushNotificationStep() {
		super();
	}

	protected StepSource getSource() {
		if (source == null) {
			source = new StepSource(this, sourceDefinition);
		}
		return source;
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
		source = new StepSource(this, sourceDefinition);
	}	
	
	
	public XMLVector<String> getTokens() {
		return tokens;
	}
	
	public void setTokens(XMLVector<String> nTokens) {
		this.tokens = nTokens;
	}

	public String getPushType() {
		return this.pushType;
	}
	
	public void setPushType(String nPushType) {
		this.pushType = nPushType;
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
	
	public String getApnsNotificationType() {
		return apnsNotificationType;
	}

	public void setApnsNotificationType(String apnsNotificationType) {
		this.apnsNotificationType = apnsNotificationType;
	}

	@Override
    public PushNotificationStep clone() throws CloneNotSupportedException {
    	PushNotificationStep clonedObject = (PushNotificationStep) super.clone();
    	clonedObject.source = null;
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

	protected boolean workOnSource() {
		return true;
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		String[] result = new String[0];
		if(propertyName.equals("pushType")){
			String[] pushTypes = {"APNS","GCM"};
			result = pushTypes;
		}
		
		if(propertyName.equals("apnsNotificationType")){
			String[] pushTypes = {"Message","Badge", "Sound"};
			result = pushTypes;
		}

		return result;
	}

	protected String getAbsoluteFilePath(String entry) throws EngineException {
		if (entry.equals(""))
			throw new EngineException("Please fill the Source property field.");

		return Engine.theApp.filePropertyManager.getFilepathFromProperty(entry, getProject().getName());
	}

	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.stepExecute(javascriptContext, scope)) {
				try {
					if (pushType.equalsIgnoreCase("APNS")) {
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
							String[] devices = new String[list.getLength()];
							for (int i=0; i< list.getLength(); i++) {
								devices[i] = getNodeValue(list.item(i));
							}


							// get Source data as a string to payload
							StepSource stepSource = getSource();
							list = stepSource.inError() ? null : stepSource.getContextOutputNodes();
							if (list != null)
								sPayload = getNodeValue(list.item(0));
							
							// Submit the push to JavaPN libarary...
							PushedNotifications pn;
							if (apnsNotificationType.equalsIgnoreCase("Message")) {
								pn = Push.alert(sPayload,
												sClientCertificate,
												sCertificatePassword,
												true,
												devices);
								
							} else if (apnsNotificationType.equalsIgnoreCase("Badge")) {
								pn = Push.badge(Integer.parseInt(sPayload, 10),
												sClientCertificate,
												sCertificatePassword,
												true,
												devices);
								
							} else { 
								pn = Push.sound(sPayload,
												sClientCertificate,
												sCertificatePassword,
												true,
												devices);
							}
							
							// Analyse the responses..
							Iterator<PushedNotification>  iPn  = pn.iterator();
							while (iPn.hasNext()) {
								PushedNotification notif = iPn.next();
								Engine.logBeans.debug("Push notification :" + notif + " Sucessfull : " + notif.isSuccessful());
							}
						}
						
					} else if (pushType.equalsIgnoreCase("GCM")) {
						// Todo : Implement Google cloud messening
					}
				} catch (EngineException e) {
					throw e;
				} catch (Exception e) {
					throw new EngineException(e.getClass().getSimpleName() + " during push notification", e);
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
}
