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

public class InputHtmlSetSelectedStatementBeanInfo extends MySimpleBeanInfo {
    
	public InputHtmlSetSelectedStatementBeanInfo() {
		try {
			beanClass = InputHtmlSetSelectedStatement.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.statements.AbstractComplexeEventStatement.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/statements/images/inputhtmlsetselected_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/statements/images/inputhtmlsetselected_32x32.gif";
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/statements/res/InputHtmlSetSelectedStatement");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[2];
            
            properties[0] = new PropertyDescriptor("expression", beanClass, "getExpression", "setExpression");
            properties[0].setDisplayName(getExternalizedString("property.expression.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.expression.short_description"));
            //properties[0].setPropertyEditorClass(getEditorClass("JavascriptTextEditor"));
            properties[0].setValue("scriptable", Boolean.TRUE);
            
            properties[1] = new PropertyDescriptor("mode", beanClass, "getMode", "setMode");
            properties[1].setDisplayName(getExternalizedString("property.mode.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.mode.short_description"));
            properties[1].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
