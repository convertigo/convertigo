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
 * $URL: http://sourceus/svn/CEMS/trunk/Studio/src/com/twinsoft/convertigo/beans/steps/IfThenElseStepBeanInfo.java $
 * $Author: fabienb $
 * $Revision: 28379 $
 * $Date: 2011-09-27 11:38:59 +0200 (mar., 27 sept. 2011) $
 */

package com.twinsoft.convertigo.beans.steps;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class IfFileExistThenElseStepBeanInfo extends MySimpleBeanInfo {
    
	public IfFileExistThenElseStepBeanInfo() {
		try {
			beanClass = IfFileExistThenElseStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.steps.IfFileExistStep.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/if_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/if_32x32.png";
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/steps/res/IfFileExistThenElseStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
