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
 * $Date: 2012-08-03 17:52:41 +0200 (ven., 03 août 2012) $
 */

package com.twinsoft.convertigo.beans.extractionrules.siteclipper;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class RequestJavaScriptBeanInfo extends MySimpleBeanInfo {

	public RequestJavaScriptBeanInfo() {
		try {
			beanClass = RequestJavaScript.class;
			additionalBeanClass = JavaScript.class;
	
		    iconNameC16 = "/com/twinsoft/convertigo/beans/extractionrules/siteclipper/images/requestjavascript_color_16x16.png";
		    iconNameC32 = "/com/twinsoft/convertigo/beans/extractionrules/siteclipper/images/requestjavascript_color_32x32.png";
	
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/extractionrules/siteclipper/res/RequestJavaScript");
	
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
