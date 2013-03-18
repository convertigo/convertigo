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

package com.twinsoft.convertigo.beans.common;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class TagNameBeanInfo extends MySimpleBeanInfo {
    
    private static final int PROPERTY_labelPolicy = 0;
    private static final int PROPERTY_tagName = 1;
    private static final int PROPERTY_bSaveHistory = 2;

    public TagNameBeanInfo() {
		try {
			beanClass =  TagName.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinMashupEventExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/tagname_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/tagname_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/TagName");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[3];
			
            properties[PROPERTY_labelPolicy] = new PropertyDescriptor ( "labelPolicy", TagName.class, "getLabelPolicy", "setLabelPolicy" );
            properties[PROPERTY_labelPolicy].setDisplayName ( getExternalizedString("property.labelPolicy.display_name") );
            properties[PROPERTY_labelPolicy].setShortDescription ( getExternalizedString("property.labelPolicy.short_description") );
            properties[PROPERTY_labelPolicy].setPropertyEditorClass (getEditorClass("TagNameLabelPolicyEditor"));

            properties[PROPERTY_tagName] = new PropertyDescriptor ( "tagName", TagName.class, "getTagName", "setTagName" );
            properties[PROPERTY_tagName].setDisplayName ( getExternalizedString("property.tagName.display_name") );
            properties[PROPERTY_tagName].setShortDescription ( getExternalizedString("property.tagName.short_description") );
            properties[PROPERTY_tagName].setValue(DatabaseObject.PROPERTY_XMLNAME, Boolean.TRUE);
            
            properties[PROPERTY_bSaveHistory] = new PropertyDescriptor ( "bSaveHistory", TagName.class, "isSaveHistory", "setSaveHistory" );
            properties[PROPERTY_bSaveHistory].setDisplayName ( getExternalizedString("property.bSaveHistory.display_name") );
            properties[PROPERTY_bSaveHistory].setShortDescription ( getExternalizedString("property.bSaveHistory.short_description") );
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
