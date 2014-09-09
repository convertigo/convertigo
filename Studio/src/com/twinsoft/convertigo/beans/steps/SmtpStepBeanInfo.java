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

package com.twinsoft.convertigo.beans.steps;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.steps.SmtpStep.SmtpAuthType;

public class SmtpStepBeanInfo extends MySimpleBeanInfo {
    
	public SmtpStepBeanInfo() {
		try {
			beanClass = SmtpStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/smtpstep_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/smtpstep_32x32.png";
			
			resourceBundle = getResourceBundle("res/SmtpStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[12];

            properties[0] = new PropertyDescriptor("smtpServer", beanClass, "getSmtpServer", "setSmtpServer");
            properties[0].setDisplayName(getExternalizedString("property.smtpServer.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.smtpServer.short_description"));
            
            properties[1] = new PropertyDescriptor("smtpPort", beanClass, "getSmtpPort", "setSmtpPort");
            properties[1].setDisplayName(getExternalizedString("property.smtpPort.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.smtpPort.short_description"));
            properties[1].setExpert(true);
            
            properties[2] = new PropertyDescriptor("smtpUsername", beanClass, "getSmtpUsername", "setSmtpUsername");
            properties[2].setDisplayName(getExternalizedString("property.smtpUsername.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.smtpUsername.short_description"));
            properties[2].setExpert(true);
            
            properties[3] = new PropertyDescriptor("smtpPassword", beanClass, "getSmtpPassword", "setSmtpPassword");
            properties[3].setDisplayName(getExternalizedString("property.smtpPassword.display_name"));
            properties[3].setShortDescription(getExternalizedString("property.smtpPassword.short_description"));
            properties[3].setExpert(true);
            
            properties[4] = new PropertyDescriptor("smtpRecipients", beanClass, "getSmtpRecipients", "setSmtpRecipients");
            properties[4].setDisplayName(getExternalizedString("property.smtpRecipients.display_name"));
            properties[4].setShortDescription(getExternalizedString("property.smtpRecipients.short_description"));
	        properties[4].setValue("scriptable", Boolean.TRUE);
            
            properties[5] = new PropertyDescriptor("smtpSubject", beanClass, "getSmtpSubject", "setSmtpSubject");
            properties[5].setDisplayName(getExternalizedString("property.smtpSubject.display_name"));
            properties[5].setShortDescription(getExternalizedString("property.smtpSubject.short_description"));
	        properties[5].setValue("scriptable", Boolean.TRUE);
            
            properties[6] = new PropertyDescriptor("smtpAuthType", beanClass, "getSmtpAuthType", "setSmtpAuthType");
            properties[6].setDisplayName(getExternalizedString("property.smtpAuthType.display_name"));
            properties[6].setShortDescription(getExternalizedString("property.smtpAuthType.short_description"));
            properties[6].setPropertyEditorClass(SmtpAuthType.class);
            properties[6].setExpert(true);
            
			properties[7] = new PropertyDescriptor("sourceDefinition", beanClass, "getSourceDefinition", "setSourceDefinition");
			properties[7].setExpert(true);
			properties[7].setDisplayName(getExternalizedString("property.sourceDefinition.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.sourceDefinition.short_description"));
			properties[7].setPropertyEditorClass(getEditorClass("StepSourceEditor"));
			
			properties[8] = new PropertyDescriptor("smtpSender", beanClass, "getSmtpSender", "setSmtpSender");
            properties[8].setDisplayName(getExternalizedString("property.smtpSender.display_name"));
            properties[8].setShortDescription(getExternalizedString("property.smtpSender.short_description"));
            properties[8].setExpert(true);
			
			properties[9] = new PropertyDescriptor("xslFilepath", beanClass, "getXslFilepath", "setXslFilepath");
            properties[9].setDisplayName(getExternalizedString("property.xslFilepath.display_name"));
            properties[9].setShortDescription(getExternalizedString("property.xslFilepath.short_description"));
	        properties[9].setValue("scriptable", Boolean.TRUE);
	        
	        properties[10] = new PropertyDescriptor("contentType", beanClass, "getContentType", "setContentType");
            properties[10].setDisplayName(getExternalizedString("property.contentType.display_name"));
            properties[10].setShortDescription(getExternalizedString("property.contentType.short_description"));
            properties[10].setExpert(true);
	        properties[10].setValue("scriptable", Boolean.TRUE);

	        properties[11] = new PropertyDescriptor("attachments", beanClass, "getAttachments", "setAttachments");
            properties[11].setDisplayName(getExternalizedString("property.attachments.display_name"));
            properties[11].setShortDescription(getExternalizedString("property.attachments.short_description"));
	        properties[11].setPropertyEditorClass(getEditorClass("SmtpAttachmentEditor"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
