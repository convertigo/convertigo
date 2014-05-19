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

package com.twinsoft.convertigo.beans.steps;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class WriteFileStepBeanInfo extends MySimpleBeanInfo{
	
	public WriteFileStepBeanInfo() {
		try {
			beanClass = WriteFileStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;

			resourceBundle = getResourceBundle("res/WriteFileStep");
			
			properties = new PropertyDescriptor[5];
			
			properties[0] = new PropertyDescriptor("dataFile", beanClass, "getDataFile", "setDataFile");
			properties[0].setDisplayName(getExternalizedString("property.dataFile.display_name"));
	        properties[0].setShortDescription(getExternalizedString("property.dataFile.short_description"));            
	        properties[0].setValue("scriptable", Boolean.TRUE);
	        
	        properties[1] = new PropertyDescriptor("appendTimestamp", beanClass, "isAppendTimestamp", "setAppendTimestamp");
			properties[1].setDisplayName(getExternalizedString("property.appendTimestamp.display_name"));
	        properties[1].setShortDescription(getExternalizedString("property.appendTimestamp.short_description"));
		
			properties[2] = new PropertyDescriptor("sourceDefinition", beanClass, "getSourceDefinition", "setSourceDefinition");
			properties[2].setExpert(true);
			properties[2].setDisplayName(getExternalizedString("property.sourceDefinition.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.sourceDefinition.short_description"));
			properties[2].setPropertyEditorClass(getEditorClass("StepSourceEditor"));
	        
			properties[3] = new PropertyDescriptor("encoding", beanClass, "getEncoding", "setEncoding");
			properties[3].setDisplayName(getExternalizedString("property.encoding.display_name"));
		    properties[3].setShortDescription(getExternalizedString("property.encoding.short_description"));
		    
		    properties[4] = new PropertyDescriptor("appendResult", beanClass, "isAppendResult", "setAppendResult");
			properties[4].setExpert(true);
			properties[4].setDisplayName(getExternalizedString("property.appendResult.display_name"));
	        properties[4].setShortDescription(getExternalizedString("property.appendResult.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
