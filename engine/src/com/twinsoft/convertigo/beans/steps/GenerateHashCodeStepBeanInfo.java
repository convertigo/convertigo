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

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.steps.GenerateHashCodeStep.HashAlgorithm;

public class GenerateHashCodeStepBeanInfo extends MySimpleBeanInfo{

	public GenerateHashCodeStepBeanInfo() {
		try {
			beanClass = GenerateHashCodeStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/generatehashcode_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/generatehashcode_32x32.png";

			resourceBundle = getResourceBundle("res/GenerateHashCodeStep");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[4];

			properties[0] = new PropertyDescriptor("sourcePath", beanClass, "getSourcePath", "setSourcePath");
			properties[0].setDisplayName(getExternalizedString("property.sourcePath.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.sourcePath.short_description"));
			properties[0].setValue(SCRIPTABLE, Boolean.TRUE);
			properties[0].setValue(MULTILINE, Boolean.TRUE);

			properties[1] = new PropertyDescriptor("hashAlgorithm", beanClass, "getHashAlgorithm", "setHashAlgorithm");
			properties[1].setDisplayName(getExternalizedString("property.hashAlgorithm.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.hashAlgorithm.short_description"));
			properties[1].setPropertyEditorClass(HashAlgorithm.class);

			properties[2] = new PropertyDescriptor("nodeName", beanClass, "getNodeName", "setNodeName");
			properties[2].setDisplayName(getExternalizedString("property.nodeName.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.nodeName.short_description"));
			properties[2].setValue(DatabaseObject.PROPERTY_XMLNAME, Boolean.TRUE);
			properties[2].setValue(BLACK_LIST_NAME, Boolean.TRUE); 

			properties[3] = new PropertyDescriptor("offset", beanClass, "getOffset", "setOffset");
			properties[3].setDisplayName(getExternalizedString("property.offset.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.offset.short_description"));
			properties[3].setValue(SCRIPTABLE, Boolean.TRUE);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
