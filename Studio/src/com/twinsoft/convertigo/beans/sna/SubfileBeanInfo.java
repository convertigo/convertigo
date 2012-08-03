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

package com.twinsoft.convertigo.beans.sna;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.common.Table;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class SubfileBeanInfo extends MySimpleBeanInfo {    

	public SubfileBeanInfo() {
		try {
			beanClass = Subfile.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

		    iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/table_color_16x16.png";
		    iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/table_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/sna/res/Subfile");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[10];
			
            properties[PROPERTY_actionLineAttribute] = new PropertyDescriptor ( "actionLineAttribute", Subfile.class, "getActionLineAttribute", "setActionLineAttribute" );
            properties[PROPERTY_actionLineAttribute].setDisplayName ( getExternalizedString("property.actionLineAttribute.display_name") );
            properties[PROPERTY_actionLineAttribute].setShortDescription ( getExternalizedString("property.actionLineAttribute.short_description") );
            properties[PROPERTY_actionLineAttribute].setPropertyEditorClass (getEditorClass("JavelinAttributeEditor"));
            
            properties[PROPERTY_endStringAttribute] = new PropertyDescriptor ( "endStringAttribute", Subfile.class, "getEndStringAttribute", "setEndStringAttribute" );
            properties[PROPERTY_endStringAttribute].setDisplayName ( getExternalizedString("property.endStringAttribute.display_name") );
            properties[PROPERTY_endStringAttribute].setShortDescription ( getExternalizedString("property.endStringAttribute.short_description") );
            properties[PROPERTY_endStringAttribute].setPropertyEditorClass (getEditorClass("JavelinAttributeEditor"));
            
            properties[PROPERTY_titleRowAttribute] = new PropertyDescriptor ( "titleRowAttribute", Subfile.class, "getTitleRowAttribute", "setTitleRowAttribute" );
            properties[PROPERTY_titleRowAttribute].setDisplayName ( getExternalizedString("property.titleRowAttribute.display_name") );
            properties[PROPERTY_titleRowAttribute].setShortDescription ( getExternalizedString("property.titleRowAttribute.short_description") );
            properties[PROPERTY_titleRowAttribute].setPropertyEditorClass (getEditorClass("JavelinAttributeEditor"));
            
            properties[PROPERTY_endString] = new PropertyDescriptor ( "endString", Subfile.class, "getEndString", "setEndString" );
            properties[PROPERTY_endString].setDisplayName ( getExternalizedString("property.endString.display_name") );
            properties[PROPERTY_endString].setShortDescription ( getExternalizedString("property.endString.short_description") );
			
            properties[PROPERTY_autoValidate] = new PropertyDescriptor ( "autoValidate", Subfile.class, "isAutoValidate", "setAutoValidate" );
			properties[PROPERTY_autoValidate].setDisplayName ( getExternalizedString("property.autoValidate.display_name") );
			properties[PROPERTY_autoValidate].setShortDescription ( getExternalizedString("property.autoValidate.short_description") );
			
			properties[PROPERTY_removeActionLines] = new PropertyDescriptor ( "removeActionLines", Subfile.class, "isRemoveActionLines", "setRemoveActionLines" );
			properties[PROPERTY_removeActionLines].setDisplayName ( getExternalizedString("property.removeActionLines.display_name") );
			properties[PROPERTY_removeActionLines].setShortDescription ( getExternalizedString("property.removeActionLines.short_description") );
			
			properties[PROPERTY_doNotIncludeTitles] = new PropertyDescriptor ( "doNotIncludeTitles", Table.class, "isDoNotIncludeTitles", "setDoNotIncludeTitles" );
			properties[PROPERTY_doNotIncludeTitles].setDisplayName ( java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/Table").getString("property.doNotIncludeTitles.display_name") );
			properties[PROPERTY_doNotIncludeTitles].setShortDescription ( java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/Table").getString("property.doNotIncludeTitles.short_description") );
			
			properties[PROPERTY_keepEmptyLines] = new PropertyDescriptor ( "keepEmptyLines", Table.class, "isKeepEmptyLines", "setKeepEmptyLines" );
			properties[PROPERTY_keepEmptyLines].setDisplayName ( getExternalizedString("property.keepEmptyLines.display_name") );
			properties[PROPERTY_keepEmptyLines].setShortDescription ( getExternalizedString("property.keepEmptyLines.short_description") );
			
			properties[PROPERTY_subFileDetectionStartLine] = new PropertyDescriptor ( "subFileDetectionStartLine", Subfile.class, "getSubFileDetectionStartLine", "setSubFileDetectionStartLine" );
			properties[PROPERTY_subFileDetectionStartLine].setDisplayName ( getExternalizedString("property.subFileDetectionStartLine.display_name") );
			properties[PROPERTY_subFileDetectionStartLine].setShortDescription ( getExternalizedString("property.subFileDetectionStartLine.short_description") );
			
			properties[PROPERTY_separatorCharsForTokens] = new PropertyDescriptor ( "separatorCharsForTokens", Table.class, "getSeparatorCharsForTokens", "setSeparatorCharsForTokens" );
            properties[PROPERTY_separatorCharsForTokens].setDisplayName ( java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/Table").getString("property.separatorCharsForTokens.display_name") );
            properties[PROPERTY_separatorCharsForTokens].setShortDescription ( java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/Table").getString("property.separatorCharsForTokens.short_description") );
           
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

    private static final int PROPERTY_actionLineAttribute = 0;
    private static final int PROPERTY_endStringAttribute = 1;
    private static final int PROPERTY_titleRowAttribute = 2;
	private static final int PROPERTY_endString = 3;
	private static final int PROPERTY_autoValidate = 4;
	private static final int PROPERTY_removeActionLines = 5;
    private static final int PROPERTY_doNotIncludeTitles = 6;
    private static final int PROPERTY_keepEmptyLines = 7;
    private static final int PROPERTY_subFileDetectionStartLine = 8;
    private static final int PROPERTY_separatorCharsForTokens = 9;
    
}

