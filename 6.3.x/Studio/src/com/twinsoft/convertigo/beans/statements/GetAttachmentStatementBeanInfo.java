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

package com.twinsoft.convertigo.beans.statements;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class GetAttachmentStatementBeanInfo extends MySimpleBeanInfo {
    
	public GetAttachmentStatementBeanInfo() {
		try {
			beanClass = GetAttachmentStatement.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Statement.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/statements/images/getattachment_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/statements/images/getattachment_32x32.png";
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/statements/res/GetAttachmentStatement");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[4];

            properties[0] = new PropertyDescriptor("timeout", beanClass, "getTimeout", "setTimeout");
            properties[0].setDisplayName(getExternalizedString("property.timeout.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.timeout.short_description"));
            
            properties[1] = new PropertyDescriptor("policy", beanClass, "getPolicy", "setPolicy");
            properties[1].setDisplayName(getExternalizedString("property.policy.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.policy.short_description"));
			properties[1].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
            
            properties[2] = new PropertyDescriptor("filename", beanClass, "getFilename", "setFilename");
            properties[2].setDisplayName(getExternalizedString("property.filename.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.filename.short_description"));
            properties[2].setValue("scriptable", Boolean.TRUE);

            properties[3] = new PropertyDescriptor("threshold", beanClass, "getThreshold", "setThreshold");
            properties[3].setDisplayName(getExternalizedString("property.threshold.display_name"));
            properties[3].setShortDescription(getExternalizedString("property.threshold.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}