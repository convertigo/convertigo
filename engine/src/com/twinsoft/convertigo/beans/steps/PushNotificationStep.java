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

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.EngineException;

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
	private String useProductionAPNS = "false";
	private String notificationTitle = "\"TITLE\"";
	private ApnsNotificationType apnsNotificationType = ApnsNotificationType.Message;
	private String GCMApiKey = "\"<configure your api key here>\"";
	private int    AndroidTimeToLive = 3600;
	
	public PushNotificationStep() {
		super();
		setOutput(false);
		xml = true;
	}
	
	public int getAndroidTimeToLive() {
		return AndroidTimeToLive;
	}

	public void setAndroidTimeToLive(int androidTimeToLive) {
		AndroidTimeToLive = androidTimeToLive;
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
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			return true;
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


	public String getNotificationTitle() {
		return notificationTitle;
	}


	public void setNotificationTitle(String notificationTitle) {
		this.notificationTitle = notificationTitle;
	}
	
	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);		
		return element;
	}

	public String getUseProductionAPNS() {
		return useProductionAPNS;
	}

	public void setUseProductionAPNS(String useProductionAPNS) {
		this.useProductionAPNS = useProductionAPNS;
	}
}
