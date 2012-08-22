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

public class TranslateTextBeanInfo extends MySimpleBeanInfo {
    
	public TranslateTextBeanInfo() {
		try {
			beanClass = TranslateText.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/translatetext_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/translatetext_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/TranslateText");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[3];

		    properties[0] = new PropertyDescriptor("dictionary", beanClass, "getDictionary", "setDictionary");
			properties[0].setDisplayName(getExternalizedString("property.dictionary.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.dictionary.short_description"));

		    properties[1] = new PropertyDescriptor("generateOrphans", beanClass, "getGenerateOrphans", "setGenerateOrphans");
			properties[1].setDisplayName(getExternalizedString("property.generateOrphans.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.generateOrphans.short_description"));

		    properties[2] = new PropertyDescriptor("encoding", beanClass, "getEncoding", "setEncoding");
			properties[2].setDisplayName(getExternalizedString("property.encoding.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.encoding.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
