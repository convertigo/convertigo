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

public class ChoiceBeanInfo extends MySimpleBeanInfo {
    
    private static final int PROPERTY_actions = 0;
    private static final int PROPERTY_separatorChars = 1;
    private static final int PROPERTY_endPattern = 2;
    private static final int PROPERTY_separatorCharsForTokens = 3;
    private static final int PROPERTY_choiceCharacterPolicy = 4;
    private static final int PROPERTY_actionsFromScreen = 5;
    private static final int PROPERTY_startPattern = 6;
    private static final int PROPERTY_tagName = 7;
    private static final int PROPERTY_radio = 8;

    public ChoiceBeanInfo() {
		try {
			beanClass =  Choice.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/choice_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/choice_color_32x32.png";

			resourceBundle = getResourceBundle("res/Choice");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[9];
			
            properties[PROPERTY_actions] = new PropertyDescriptor ( "actions", Choice.class, "getActions", "setActions" );
            properties[PROPERTY_actions].setDisplayName ( getExternalizedString("property.actions.display_name") );
            properties[PROPERTY_actions].setShortDescription ( getExternalizedString("property.actions.short_description") );
            properties[PROPERTY_actions].setPropertyEditorClass (getEditorClass("ActionsEditor"));

            properties[PROPERTY_separatorChars] = new PropertyDescriptor ( "separatorChars", Choice.class, "getSeparatorChars", "setSeparatorChars" );
            properties[PROPERTY_separatorChars].setDisplayName ( getExternalizedString("property.separatorChars.display_name") );
            properties[PROPERTY_separatorChars].setShortDescription ( getExternalizedString("property.separatorChars.short_description") );
            
            properties[PROPERTY_endPattern] = new PropertyDescriptor ( "endPattern", Choice.class, "getEndPattern", "setEndPattern" );
            properties[PROPERTY_endPattern].setDisplayName ( getExternalizedString("property.endPattern.display_name") );
            properties[PROPERTY_endPattern].setShortDescription ( getExternalizedString("property.endPattern.short_description") );
            
            properties[PROPERTY_separatorCharsForTokens] = new PropertyDescriptor ( "separatorCharsForTokens", Choice.class, "getSeparatorCharsForTokens", "setSeparatorCharsForTokens" );
            properties[PROPERTY_separatorCharsForTokens].setDisplayName ( getExternalizedString("property.separatorCharsForTokens.display_name") );
            properties[PROPERTY_separatorCharsForTokens].setShortDescription ( getExternalizedString("property.separatorCharsForTokens.short_description") );
            
            properties[PROPERTY_choiceCharacterPolicy] = new PropertyDescriptor ( "choiceCharacterPolicy", Choice.class, "getChoiceCharacterPolicy", "setChoiceCharacterPolicy" );
            properties[PROPERTY_choiceCharacterPolicy].setDisplayName ( getExternalizedString("property.choiceCharacterPolicy.display_name") );
            properties[PROPERTY_choiceCharacterPolicy].setShortDescription ( getExternalizedString("property.choiceCharacterPolicy.short_description") );
            properties[PROPERTY_choiceCharacterPolicy].setPropertyEditorClass (getEditorClass("ChoiceCharacterPolicyEditor"));
            
            properties[PROPERTY_actionsFromScreen] = new PropertyDescriptor ( "actionsFromScreen", Choice.class, "isActionsFromScreen", "setActionsFromScreen" );
            properties[PROPERTY_actionsFromScreen].setDisplayName ( getExternalizedString("property.actionsFromScreen.display_name") );
            properties[PROPERTY_actionsFromScreen].setShortDescription ( getExternalizedString("property.actionsFromScreen.short_description") );
            
            properties[PROPERTY_startPattern] = new PropertyDescriptor ( "startPattern", Choice.class, "getStartPattern", "setStartPattern" );
            properties[PROPERTY_startPattern].setDisplayName ( getExternalizedString("property.startPattern.display_name") );
            properties[PROPERTY_startPattern].setShortDescription ( getExternalizedString("property.startPattern.short_description") );
            
            properties[PROPERTY_tagName] = new PropertyDescriptor ( "tagName", Choice.class, "getTagName", "setTagName" );
            properties[PROPERTY_tagName].setDisplayName ( getExternalizedString("property.tagName.display_name") );
            properties[PROPERTY_tagName].setShortDescription ( getExternalizedString("property.tagName.short_description") );
            properties[PROPERTY_tagName].setValue(DatabaseObject.PROPERTY_XMLNAME, Boolean.TRUE);
            
            properties[PROPERTY_radio] = new PropertyDescriptor ( "radio", Choice.class, "isRadio", "setRadio" );
            properties[PROPERTY_radio].setDisplayName ( getExternalizedString("property.radio.display_name") );
            properties[PROPERTY_radio].setShortDescription ( getExternalizedString("property.radio.short_description") );
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
