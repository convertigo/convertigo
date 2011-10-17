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

public class BrowserPropertyChangeStatementBeanInfo extends MySimpleBeanInfo {
    
	public BrowserPropertyChangeStatementBeanInfo() {
		try {
			beanClass = BrowserPropertyChangeStatement.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Statement.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/statements/images/browserpropertychange_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/statements/images/browserpropertychange_32x32.gif";
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/statements/res/BrowserPropertyChangeStatement");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[17];
			
			properties[0] = new PropertyDescriptor("bJavascriptChange", beanClass, "getBJavascriptChange", "setBJavascriptChange");
			properties[0].setDisplayName(getExternalizedString("property.bjavascriptchange.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.bjavascriptchange.short_description"));
			properties[0].setHidden(true);
			
			properties[1] = new PropertyDescriptor("bJavascriptStat", beanClass, "getBJavascriptStat", "setBJavascriptStat");
			properties[1].setDisplayName(getExternalizedString("property.bjavascriptstat.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.bjavascriptstat.short_description"));
			properties[1].setHidden(true);
			
			properties[2] = new PropertyDescriptor("bImageChange", beanClass, "getBImageChange", "setBImageChange");
			properties[2].setDisplayName(getExternalizedString("property.bimagechange.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.bimagechange.short_description"));
			properties[2].setHidden(true);
			
			properties[3] = new PropertyDescriptor("bImageStat", beanClass, "getBImageStat", "setBImageStat");
			properties[3].setDisplayName(getExternalizedString("property.bimagestat.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.bimagestat.short_description"));
			properties[3].setHidden(true);
			
			properties[4] = new PropertyDescriptor("bWindowOpenChange", beanClass, "getBWindowOpenChange", "setBWindowOpenChange");
			properties[4].setDisplayName(getExternalizedString("property.bwindowopenchange.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.bwindowopenchange.short_description"));
			properties[4].setHidden(true);
			
			properties[5] = new PropertyDescriptor("bWindowOpenStat", beanClass, "getBWindowOpenStat", "setBWindowOpenStat");
			properties[5].setDisplayName(getExternalizedString("property.bwindowopenstat.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.bwindowopenstat.short_description"));
			properties[5].setHidden(true);
			
			properties[6] = new PropertyDescriptor("bPluginChange", beanClass, "getBPluginChange", "setBPluginChange");
			properties[6].setDisplayName(getExternalizedString("property.bpluginchange.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.bpluginchange.short_description"));
			properties[6].setHidden(true);
			
			properties[7] = new PropertyDescriptor("bPluginStat", beanClass, "getBPluginStat", "setBPluginStat");
			properties[7].setDisplayName(getExternalizedString("property.bpluginstat.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.bpluginstat.short_description"));
			properties[7].setHidden(true);
			
			properties[8] = new PropertyDescriptor("bClearCookies", beanClass, "getBClearCookies", "setBClearCookies");
			properties[8].setDisplayName(getExternalizedString("property.bclearcookies.display_name"));
			properties[8].setShortDescription(getExternalizedString("property.bclearcookies.short_description"));
			
            properties[9] = new PropertyDescriptor("convertigoMode", beanClass, "getConvertigoMode", "setConvertigoMode");
            properties[9].setDisplayName(getExternalizedString("property.convertigomode.display_name"));
            properties[9].setShortDescription(getExternalizedString("property.convertigomode.short_description"));
            properties[9].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
            properties[9].setExpert(true);
            
			properties[10] = new PropertyDescriptor("bAttachmentChange", beanClass, "getBAttachmentChange", "setBAttachmentChange");
			properties[10].setDisplayName(getExternalizedString("property.battachmentchange.display_name"));
			properties[10].setShortDescription(getExternalizedString("property.battachmentchange.short_description"));
			properties[10].setHidden(true);
			
			properties[11] = new PropertyDescriptor("bAttachmentStat", beanClass, "getBAttachmentStat", "setBAttachmentStat");
			properties[11].setDisplayName(getExternalizedString("property.battachmentstat.display_name"));
			properties[11].setShortDescription(getExternalizedString("property.battachmentstat.short_description"));
			properties[11].setHidden(true);
			
			properties[12] = new PropertyDescriptor("javascriptMode", beanClass, "getJavascriptMode", "setJavascriptMode");
			properties[12].setDisplayName(getExternalizedString("property.javascriptmode.display_name"));
			properties[12].setShortDescription(getExternalizedString("property.javascriptmode.short_description"));
			properties[12].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
			
			properties[13] = new PropertyDescriptor("imageMode", beanClass, "getImageMode", "setImageMode");
			properties[13].setDisplayName(getExternalizedString("property.imagemode.display_name"));
			properties[13].setShortDescription(getExternalizedString("property.imagemode.short_description"));
			properties[13].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));

			properties[14] = new PropertyDescriptor("pluginMode", beanClass, "getPluginMode", "setPluginMode");
			properties[14].setDisplayName(getExternalizedString("property.pluginmode.display_name"));
			properties[14].setShortDescription(getExternalizedString("property.pluginmode.short_description"));
			properties[14].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));

			properties[15] = new PropertyDescriptor("attachmentMode", beanClass, "getAttachmentMode", "setAttachmentMode");
			properties[15].setDisplayName(getExternalizedString("property.attachmentmode.display_name"));
			properties[15].setShortDescription(getExternalizedString("property.attachmentmode.short_description"));
			properties[15].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));

			properties[16] = new PropertyDescriptor("windowOpenMode", beanClass, "getWindowOpenMode", "setWindowOpenMode");
			properties[16].setDisplayName(getExternalizedString("property.windowopenmode.display_name"));
			properties[16].setShortDescription(getExternalizedString("property.windowopenmode.short_description"));
			properties[16].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}