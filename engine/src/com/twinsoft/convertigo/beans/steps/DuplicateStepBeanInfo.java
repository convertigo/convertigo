/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

public class DuplicateStepBeanInfo extends MySimpleBeanInfo{

	public DuplicateStepBeanInfo() {
		try {
			beanClass = DuplicateStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/duplicate_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/duplicate_32x32.png";

			resourceBundle = getResourceBundle("res/DuplicateStep");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[3];

			properties[0] = new PropertyDescriptor("sourcePath", beanClass, "getSourcePath", "setSourcePath");
			properties[0].setExpert(true);
			properties[0].setDisplayName(getExternalizedString("property.sourcePath.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.sourcePath.short_description"));
			properties[0].setValue(SCRIPTABLE, Boolean.TRUE);
			properties[0].setValue(MULTILINE, Boolean.TRUE);

			properties[1] = new PropertyDescriptor("copyName", beanClass, "getCopyName", "setCopyName");
			properties[1].setExpert(true);
			properties[1].setDisplayName(getExternalizedString("property.copyName.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.copyName.short_description"));
			properties[1].setValue(SCRIPTABLE, Boolean.TRUE);
			properties[1].setValue(MULTILINE, Boolean.TRUE);

			properties[2] = new PropertyDescriptor("overwrite", beanClass, "isOverwrite", "setOverwrite");
			properties[2].setExpert(true);
			properties[2].setDisplayName(getExternalizedString("property.overwrite.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.overwrite.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
