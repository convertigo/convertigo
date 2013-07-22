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

package com.twinsoft.convertigo.beans.extractionrules;

import java.beans.*;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class HtmlExtractionRuleBeanInfo extends MySimpleBeanInfo {
    
	public HtmlExtractionRuleBeanInfo() {
		try {
			beanClass = HtmlExtractionRule.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.ExtractionRule.class;

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/extractionrules/res/HtmlExtractionRule");

			properties = new PropertyDescriptor[1];
			
			properties[0] = new PropertyDescriptor("xpath", HtmlExtractionRule.class, "getXpath", "setXpath");
			properties[0].setExpert(true);
			properties[0].setDisplayName(getExternalizedString("property.xpath.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.xpath.short_description"));			
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
