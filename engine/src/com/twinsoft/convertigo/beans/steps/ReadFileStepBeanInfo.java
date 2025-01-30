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

package com.twinsoft.convertigo.beans.steps;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class ReadFileStepBeanInfo extends MySimpleBeanInfo{

	public ReadFileStepBeanInfo() {
		try {
			beanClass = ReadFileStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;

			resourceBundle = getResourceBundle("res/ReadFileStep");

			properties = new PropertyDescriptor[2];

			properties[0] = new PropertyDescriptor("dataFile", beanClass, "getDataFile", "setDataFile");
			properties[0].setExpert(false);
			properties[0].setDisplayName(getExternalizedString("property.dataFile.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.dataFile.short_description"));
			properties[0].setValue(SCRIPTABLE, Boolean.TRUE);
			properties[0].setValue(MULTILINE, Boolean.TRUE);

			properties[1] = new PropertyDescriptor("replaceStepElement", beanClass, "isReplaceStepElement", "setReplaceStepElement");
			properties[1].setExpert(true);
			properties[1].setDisplayName(getExternalizedString("property.replaceStepElement.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.replaceStepElement.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
