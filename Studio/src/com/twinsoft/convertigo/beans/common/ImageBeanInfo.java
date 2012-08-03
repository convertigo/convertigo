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

import java.beans.*;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class ImageBeanInfo extends MySimpleBeanInfo {

	public ImageBeanInfo() {
		try {
			beanClass = Image.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinMashupEventExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/picture_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/picture_32x32.png";

 			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/Image");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[7];
			
            properties[PROPERTY_Label] = new PropertyDescriptor ( "label", Image.class, "getLabel", "setLabel" );
            properties[PROPERTY_Label].setDisplayName ( getExternalizedString("property.label.display_name") );
            properties[PROPERTY_Label].setShortDescription ( getExternalizedString("property.label.short_description") );
			
            properties[PROPERTY_Action] = new PropertyDescriptor ( "action", Image.class, "getAction", "setAction" );
			properties[PROPERTY_Action].setDisplayName ( getExternalizedString("property.action.display_name") );
			properties[PROPERTY_Action].setShortDescription ( getExternalizedString("property.action.short_description") );
			
			properties[PROPERTY_DoTransaction] = new PropertyDescriptor ( "doTransaction", Image.class, "isDoTransaction", "setDoTransaction" );
			properties[PROPERTY_DoTransaction].setDisplayName ( getExternalizedString("property.dotransaction.display_name") );
			properties[PROPERTY_DoTransaction].setShortDescription ( getExternalizedString("property.dotransaction.short_description") );
			
			properties[PROPERTY_ImageDesc] = new PropertyDescriptor ( "imageDesc", Image.class, "getImageDesc", "setImageDesc" );
			properties[PROPERTY_ImageDesc].setDisplayName ( getExternalizedString("property.imageDesc.display_name") );
			properties[PROPERTY_ImageDesc].setShortDescription ( getExternalizedString("property.imageDesc.short_description") );
			properties[PROPERTY_ImageDesc].setPropertyEditorClass(getEditorClass("ZoneEditor"));
			
			properties[PROPERTY_Url] = new PropertyDescriptor ( "url", Image.class, "getUrl", "setUrl" );
            properties[PROPERTY_Url].setDisplayName ( getExternalizedString("property.url.display_name") );
            properties[PROPERTY_Url].setShortDescription ( getExternalizedString("property.url.short_description") );
            
            properties[PROPERTY_KeepSize] = new PropertyDescriptor ( "keepSize", Image.class, "isKeepSize", "setKeepSize" );
			properties[PROPERTY_KeepSize].setDisplayName ( getExternalizedString("property.keepSize.display_name") );
			properties[PROPERTY_KeepSize].setShortDescription ( getExternalizedString("property.keepSize.short_description") );
			
			properties[PROPERTY_ZOrder] = new PropertyDescriptor ( "zOrder", Image.class, "getZOrder", "setZOrder" );
			properties[PROPERTY_ZOrder].setDisplayName ( getExternalizedString("property.zOrder.display_name") );
			properties[PROPERTY_ZOrder].setShortDescription ( getExternalizedString("property.zOrder.short_description") );
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
	
    private static final int PROPERTY_Label 		= 0;
	private static final int PROPERTY_Action 		= 1;
	private static final int PROPERTY_DoTransaction = 2;
	private static final int PROPERTY_ImageDesc 	= 3;
	private static final int PROPERTY_Url 			= 4;
	private static final int PROPERTY_KeepSize 		= 5;
	private static final int PROPERTY_ZOrder 		= 6;
}

