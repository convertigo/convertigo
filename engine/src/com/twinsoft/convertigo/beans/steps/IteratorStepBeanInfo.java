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

public class IteratorStepBeanInfo extends MySimpleBeanInfo {

	public IteratorStepBeanInfo() {
		try {
			beanClass = IteratorStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.steps.LoopStep.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/iterator_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/iterator_32x32.png";

			resourceBundle = getResourceBundle("res/IteratorStep");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[2];

			properties[0] = new PropertyDescriptor("sourceDefinition", beanClass, "getSourceDefinition", "setSourceDefinition");
			properties[0].setExpert(true);
			properties[0].setDisplayName(getExternalizedString("property.sourceDefinition.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.sourceDefinition.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("StepSourceEditor"));

			properties[1] = new PropertyDescriptor("startIndex", beanClass, "getStartIndex", "setStartIndex");
			properties[1].setDisplayName(getExternalizedString("property.startIndex.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.startIndex.short_description"));
			properties[1].setValue(SCRIPTABLE, Boolean.TRUE);
			properties[1].setValue(MULTILINE, Boolean.TRUE);

			PropertyDescriptor property = getPropertyDescriptor("condition");
			property.setDisplayName(getExternalizedString("property.condition.display_name"));
			property.setShortDescription(getExternalizedString("property.condition.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
