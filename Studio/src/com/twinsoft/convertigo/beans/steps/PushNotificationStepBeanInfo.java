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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/beans/steps/SmtpStepBeanInfo.java $
 * $Author: nicolasa $
 * $Revision: 31403 $
 * $Date: 2012-08-17 16:56:26 +0200 (ven., 17 août 2012) $
 */

package com.twinsoft.convertigo.beans.steps;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class PushNotificationStepBeanInfo extends MySimpleBeanInfo {
    
	public PushNotificationStepBeanInfo() {
		try {
			beanClass = PushNotificationStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/pushnotificationstep_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/pushnotificationstep_32x32.png";
			
			resourceBundle = getResourceBundle("res/PushNotificationStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[7];
			
			properties[0] = new PropertyDescriptor("token", beanClass, "getTokens", "setTokens");
            properties[0].setDisplayName(getExternalizedString("property.token.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.token.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("StepSourceEditor"));


			properties[1] = new PropertyDescriptor("GCMApiKey", beanClass, "getGCMApiKey", "setGCMApiKey");
            properties[1].setDisplayName(getExternalizedString("property.gcmapikey.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.gcmapikey.short_description"));
            properties[1].setValue("scriptable", Boolean.TRUE);

			
			properties[2] = new PropertyDescriptor("clientCertificate", beanClass, "getClientCertificate", "setClientCertificate");
            properties[2].setDisplayName(getExternalizedString("property.clientcertificate.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.clientcertificate.short_description"));
            properties[2].setValue("scriptable", Boolean.TRUE);

			properties[3] = new PropertyDescriptor("certificatePassword", beanClass, "getCertificatePassword", "setCertificatePassword");
            properties[3].setDisplayName(getExternalizedString("property.certificatepassword.display_name"));
            properties[3].setShortDescription(getExternalizedString("property.certificatepassword.short_description"));
            properties[3].setValue("scriptable", Boolean.TRUE);

			properties[4] = new PropertyDescriptor("apnsNotificationType", beanClass, "getApnsNotificationType", "setApnsNotificationType");
            properties[4].setDisplayName(getExternalizedString("property.apnsnotificationtype.display_name"));
            properties[4].setShortDescription(getExternalizedString("property.apnsnotificationtype.short_description"));
            properties[4].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
            
			properties[5] = new PropertyDescriptor("sourceDefinition", beanClass, "getSourceDefinition", "setSourceDefinition");
			properties[5].setExpert(false);
			properties[5].setDisplayName(getExternalizedString("property.sourceDefinition.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.sourceDefinition.short_description"));
			properties[5].setPropertyEditorClass(getEditorClass("StepSourceEditor"));

			properties[6] = new PropertyDescriptor("androidTimeToLive", beanClass, "getAndroidTimeToLive", "setAndroidTimeToLive");
			properties[6].setExpert(false);
			properties[6].setDisplayName(getExternalizedString("property.androidTimeToLive.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.androidTimeToLive.short_description"));
            properties[6].setValue("scriptable", Boolean.TRUE);
			
			
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
