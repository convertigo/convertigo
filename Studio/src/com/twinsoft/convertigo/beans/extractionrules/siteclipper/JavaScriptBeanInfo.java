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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.2.x/Studio/src/com/twinsoft/convertigo/beans/extractionrules/siteclipper/AddHeaderToRequestBeanInfo.java $
 * $Author: laetitiam $
 * $Revision: 31301 $
 * $Date: 2012-08-03 17:52:41 +0200 (ven., 03 ao√ªt 2012) $
 */

package com.twinsoft.convertigo.beans.extractionrules.siteclipper;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class JavaScriptBeanInfo extends MySimpleBeanInfo {

	public JavaScriptBeanInfo() {
		try {
			beanClass = JavaScript.class;
			additionalBeanClass = BaseRule.class;
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/extractionrules/siteclipper/res/JavaScript");

			properties = new PropertyDescriptor[1];
			
			properties[0] = new PropertyDescriptor("expression", beanClass, "getExpression", "setExpression" );
			properties[0].setDisplayName(getExternalizedString("property.expression.display_name") );
			properties[0].setShortDescription(getExternalizedString("property.expression.short_description") );
            properties[0].setPropertyEditorClass(getEditorClass("JavascriptTextEditor"));
            properties[0].setValue("scriptable", Boolean.TRUE);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
