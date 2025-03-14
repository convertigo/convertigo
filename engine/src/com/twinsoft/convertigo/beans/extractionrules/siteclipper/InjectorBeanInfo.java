/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.beans.extractionrules.siteclipper;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.engine.enums.HtmlLocation;

public class InjectorBeanInfo extends MySimpleBeanInfo {

	public InjectorBeanInfo() {
		try {
			beanClass = Injector.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.siteclipper.BaseRule.class;

			resourceBundle = getResourceBundle("res/Injector");

			properties = new PropertyDescriptor[2];

			properties[0] = new PropertyDescriptor("location", beanClass, "getLocation", "setLocation");
	        properties[0].setDisplayName(getExternalizedString("property.location.display_name"));
	        properties[0].setShortDescription(getExternalizedString("property.location.short_description"));
	    	properties[0].setPropertyEditorClass(HtmlLocation.class);
	    	
			properties[1] = new PropertyDescriptor("customRegexp", beanClass, "getCustomRegexp", "setCustomRegexp");
	        properties[1].setDisplayName(getExternalizedString("property.customRegexp.display_name"));
	        properties[1].setShortDescription(getExternalizedString("property.customRegexp.short_description"));
	    	properties[1].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
