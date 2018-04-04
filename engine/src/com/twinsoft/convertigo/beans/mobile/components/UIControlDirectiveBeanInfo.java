/*
 * Copyright (c) 2001-2018 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.mobile.components;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class UIControlDirectiveBeanInfo extends MySimpleBeanInfo {
	
	public UIControlDirectiveBeanInfo() {
		try {
			beanClass = UIControlDirective.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.mobile.components.UIElement.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/mobile/components/images/uicontroldirective_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/mobile/components/images/uicontroldirective_color_32x32.png";

			resourceBundle = getResourceBundle("res/UIControlDirective");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[3];
			
			properties[0] = new PropertyDescriptor("directiveName", beanClass, "getDirectiveName", "setDirectiveName");
			properties[0].setDisplayName(getExternalizedString("property.directiveName.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.directiveName.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
			
			properties[1] = new PropertyDescriptor("directiveExpression", beanClass, "getDirectiveExpression", "setDirectiveExpression");
			properties[1].setDisplayName(getExternalizedString("property.directiveExpression.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.directiveExpression.short_description"));
			properties[1].setValue("scriptable", Boolean.TRUE);
			properties[1].setExpert(true);
			
            properties[2] = new PropertyDescriptor("directiveSource", beanClass, "getSourceSmartType", "setSourceSmartType");
            properties[2].setDisplayName(getExternalizedString("property.directiveSource.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.directiveSource.short_description"));
            properties[2].setPropertyEditorClass(getEditorClass("MobileSmartSourcePropertyDescriptor"));
            properties[2].setExpert(true);
			
			getPropertyDescriptor("tagName").setValue("disable", Boolean.TRUE);
			getPropertyDescriptor("selfClose").setHidden(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
