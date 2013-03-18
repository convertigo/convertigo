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

public class TableBeanInfo extends MySimpleBeanInfo {
    
    private static final int PROPERTY_actions = 0;
    private static final int PROPERTY_doNotIncludeTitles = 1;
    private static final int PROPERTY_separatorChars = 2;
    private static final int PROPERTY_endPattern = 3;
    private static final int PROPERTY_separatorCharsForTokens = 4;
    private static final int PROPERTY_lineActions = 5;
    private static final int PROPERTY_columns = 6;
    private static final int PROPERTY_columnSelection = 7;
    private static final int PROPERTY_offset = 8;
    private static final int PROPERTY_startPattern = 9;
    private static final int PROPERTY_tagName = 10;
	private static final int PROPERTY_doNotAccumulate = 11;
	private static final int PROPERTY_autoValidate = 12;
	private static final int PROPERTY_removeActionLines = 13;
	private static final int PROPERTY_keepEmptyLines = 14;
	private static final int PROPERTY_resize = 15;

    public TableBeanInfo() {
		try {
			beanClass =  Table.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinMashupEventExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/table_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/table_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/Table");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[16];
			
            properties[PROPERTY_actions] = new PropertyDescriptor ( "actions", Table.class, "getActions", "setActions" );
            properties[PROPERTY_actions].setDisplayName ( getExternalizedString("property.actions.display_name") );
            properties[PROPERTY_actions].setShortDescription ( getExternalizedString("property.actions.short_description") );
            properties[PROPERTY_actions].setPropertyEditorClass (getEditorClass("ActionsForSelectionColumnEditor"));
			
            properties[PROPERTY_doNotIncludeTitles] = new PropertyDescriptor ( "doNotIncludeTitles", Table.class, "isDoNotIncludeTitles", "setDoNotIncludeTitles" );
			properties[PROPERTY_doNotIncludeTitles].setDisplayName ( getExternalizedString("property.doNotIncludeTitles.display_name") );
			properties[PROPERTY_doNotIncludeTitles].setShortDescription ( getExternalizedString("property.doNotIncludeTitles.short_description") );
            
			properties[PROPERTY_separatorChars] = new PropertyDescriptor ( "separatorChars", Table.class, "getSeparatorChars", "setSeparatorChars" );
            properties[PROPERTY_separatorChars].setDisplayName ( getExternalizedString("property.separatorChars.display_name") );
            properties[PROPERTY_separatorChars].setShortDescription ( getExternalizedString("property.separatorChars.short_description") );
            
            properties[PROPERTY_endPattern] = new PropertyDescriptor ( "endPattern", Table.class, "getEndPattern", "setEndPattern" );
            properties[PROPERTY_endPattern].setDisplayName ( getExternalizedString("property.endPattern.display_name") );
            properties[PROPERTY_endPattern].setShortDescription ( getExternalizedString("property.endPattern.short_description") );
            
            properties[PROPERTY_separatorCharsForTokens] = new PropertyDescriptor ( "separatorCharsForTokens", Table.class, "getSeparatorCharsForTokens", "setSeparatorCharsForTokens" );
            properties[PROPERTY_separatorCharsForTokens].setDisplayName ( getExternalizedString("property.separatorCharsForTokens.display_name") );
            properties[PROPERTY_separatorCharsForTokens].setShortDescription ( getExternalizedString("property.separatorCharsForTokens.short_description") );
            
            properties[PROPERTY_lineActions] = new PropertyDescriptor ( "lineActions", Table.class, "getLineActions", "setLineActions" );
            properties[PROPERTY_lineActions].setDisplayName ( getExternalizedString("property.lineActions.display_name") );
            properties[PROPERTY_lineActions].setShortDescription ( getExternalizedString("property.lineActions.short_description") );
            
            properties[PROPERTY_columns] = new PropertyDescriptor ( "columns", Table.class, "getColumns", "setColumns" );
            properties[PROPERTY_columns].setDisplayName ( getExternalizedString("property.columns.display_name") );
            properties[PROPERTY_columns].setShortDescription ( getExternalizedString("property.columns.short_description") );
            properties[PROPERTY_columns].setPropertyEditorClass (getEditorClass("ColumnEditor"));
            
            properties[PROPERTY_columnSelection] = new PropertyDescriptor ( "columnSelection", Table.class, "getColumnSelection", "setColumnSelection" );
            properties[PROPERTY_columnSelection].setDisplayName ( getExternalizedString("property.columnSelection.display_name") );
            properties[PROPERTY_columnSelection].setShortDescription ( getExternalizedString("property.columnSelection.short_description") );
            
            properties[PROPERTY_offset] = new PropertyDescriptor ( "offset", Table.class, "getOffset", "setOffset" );
            properties[PROPERTY_offset].setDisplayName ( getExternalizedString("property.offset.display_name") );
            properties[PROPERTY_offset].setShortDescription ( getExternalizedString("property.offset.short_description") );
            
            properties[PROPERTY_startPattern] = new PropertyDescriptor ( "startPattern", Table.class, "getStartPattern", "setStartPattern" );
            properties[PROPERTY_startPattern].setDisplayName ( getExternalizedString("property.startPattern.display_name") );
            properties[PROPERTY_startPattern].setShortDescription ( getExternalizedString("property.startPattern.short_description") );
            
            properties[PROPERTY_tagName] = new PropertyDescriptor ( "tagName", Table.class, "getTagName", "setTagName" );
            properties[PROPERTY_tagName].setDisplayName ( getExternalizedString("property.tagName.display_name") );
            properties[PROPERTY_tagName].setShortDescription ( getExternalizedString("property.tagName.short_description") );
            properties[PROPERTY_tagName].setValue(DatabaseObject.PROPERTY_XMLNAME, Boolean.TRUE);
            
            properties[PROPERTY_doNotAccumulate] = new PropertyDescriptor ( "doNotAccumulate", Table.class, "isDoNotAccumulate", "setDoNotAccumulate" );
			properties[PROPERTY_doNotAccumulate].setDisplayName ( getExternalizedString("property.doNotAccumulate.display_name") );
			properties[PROPERTY_doNotAccumulate].setShortDescription ( getExternalizedString("property.doNotAccumulate.short_description") );
			
			properties[PROPERTY_autoValidate] = new PropertyDescriptor ( "autoValidate", Table.class, "isAutoValidate", "setAutoValidate" );
			properties[PROPERTY_autoValidate].setDisplayName ( getExternalizedString("property.autoValidate.display_name") );
			properties[PROPERTY_autoValidate].setShortDescription ( getExternalizedString("property.autoValidate.short_description") );
			
			properties[PROPERTY_removeActionLines] = new PropertyDescriptor ( "removeActionLines", Table.class, "isRemoveActionLines", "setRemoveActionLines" );
			properties[PROPERTY_removeActionLines].setDisplayName ( java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/sna/res/Subfile").getString("property.removeActionLines.display_name") );
			properties[PROPERTY_removeActionLines].setShortDescription ( java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/sna/res/Subfile").getString("property.removeActionLines.short_description") );
			
			properties[PROPERTY_keepEmptyLines] = new PropertyDescriptor ( "keepEmptyLines", Table.class, "isKeepEmptyLines", "setKeepEmptyLines" );
			properties[PROPERTY_keepEmptyLines].setDisplayName ( java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/sna/res/Subfile").getString("property.keepEmptyLines.display_name") );
			properties[PROPERTY_keepEmptyLines].setShortDescription ( java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/sna/res/Subfile").getString("property.keepEmptyLines.short_description") );
			
			properties[PROPERTY_resize] = new PropertyDescriptor ( "resize", Table.class, "getResize", "setResize" );
            properties[PROPERTY_resize].setDisplayName ( getExternalizedString("property.resize.display_name") );
            properties[PROPERTY_resize].setShortDescription ( getExternalizedString("property.resize.short_description") );
            
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
