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

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class MergeBlocksBeanInfo extends MySimpleBeanInfo {
    
	public MergeBlocksBeanInfo() {
		try {
			beanClass = MergeBlocks.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

		    iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/mergeblocks_color_16x16.png";
		    iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/mergeblocks_color_32x32.png";

			resourceBundle = getResourceBundle("res/MergeBlocks");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[3];
			
			properties[0] = new PropertyDescriptor("separationPattern", MergeBlocks.class, "getSeparationPattern", "setSeparationPattern");
			properties[0].setDisplayName(getExternalizedString("property.separationPattern.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.separationPattern.short_description"));
			
			properties[1] = new PropertyDescriptor("bMultilineMerge", MergeBlocks.class, "isMultilineMerge", "setMultilineMerge");
			properties[1].setDisplayName(getExternalizedString("property.bMultiline.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.bMultiline.short_description"));
			
			properties[2] = new PropertyDescriptor("multilineSeparatorChar", MergeBlocks.class, "getMultilineSeparatorChar", "setMultilineSeparatorChar");
			properties[2].setDisplayName(getExternalizedString("property.multilineSeparatorChar.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.multilineSeparatorChar.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
