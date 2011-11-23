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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.beans.steps;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class ProcessExecStepBeanInfo extends MySimpleBeanInfo {
	public ProcessExecStepBeanInfo() {
		try {
			beanClass = ProcessExecStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;
	
			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/processexec_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/processexec_32x32.gif";
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/steps/res/ProcessExecStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[4];
	
	        properties[0] = new PropertyDescriptor("commandLine", beanClass, "getCommandLine", "setCommandLine");
	        properties[0].setDisplayName(getExternalizedString("property.commandLine.display_name"));
	        properties[0].setShortDescription(getExternalizedString("property.commandLine.short_description"));
	        properties[0].setValue("scriptable", Boolean.TRUE);
	        
	        properties[1] = new PropertyDescriptor("executionDirectory", beanClass, "getExecutionDirectory", "setExecutionDirectory");
	        properties[1].setDisplayName(getExternalizedString("property.executionDirectory.display_name"));
	        properties[1].setShortDescription(getExternalizedString("property.executionDirectory.short_description"));
	
	        properties[2] = new PropertyDescriptor("envParameters", beanClass, "getEnvParameters", "setEnvParameters");
			properties[2].setDisplayName(getExternalizedString("property.envParameters.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.envParameters.short_description"));
			properties[2].setPropertyEditorClass(getEditorClass("EnvParametersEditor"));
			
	        properties[3] = new PropertyDescriptor("waitForProcessEnd", beanClass, "isWaitForProcessEnd", "setWaitForProcessEnd");
	        properties[3].setDisplayName(getExternalizedString("property.waitForProcessEnd.display_name"));
	        properties[3].setShortDescription(getExternalizedString("property.waitForProcessEnd.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}
