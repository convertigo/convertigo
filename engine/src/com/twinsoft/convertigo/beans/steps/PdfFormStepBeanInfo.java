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
import com.twinsoft.convertigo.beans.steps.PdfFormStep.Action;

public class PdfFormStepBeanInfo extends MySimpleBeanInfo {
	
	public PdfFormStepBeanInfo() {
		try {	
			beanClass = PdfFormStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;
			
			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/pdfForm_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/pdfForm_32x32.png";
			
			resourceBundle = getResourceBundle("res/PdfFormStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[4];

            properties[0] = new PropertyDescriptor("filePath", beanClass, "getFilePath", "setFilePath");
            properties[0].setDisplayName(getExternalizedString("property.filePath.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.filePath.short_description"));
            properties[0].setPropertyEditorClass(getEditorClass("SmartTypeCellEditor"));
            
            properties[1] = new PropertyDescriptor("fieldsList", beanClass, "getFields", "setFields");
            properties[1].setDisplayName(getExternalizedString("property.fieldsList.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.fieldsList.short_description"));
            properties[1].setPropertyEditorClass(getEditorClass("SmartTypeCellEditor"));
            
            properties[2] = new PropertyDescriptor("targetFile", beanClass, "getTargetFile", "setTargetFile");
            properties[2].setDisplayName(getExternalizedString("property.targetFile.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.targetFile.short_description"));
            properties[2].setPropertyEditorClass(getEditorClass("SmartTypeCellEditor"));
            
            properties[3] = new PropertyDescriptor("action", beanClass, "getAction", "setAction");
            properties[3].setExpert(true);
            properties[3].setDisplayName(getExternalizedString("property.action.display_name"));
            properties[3].setShortDescription(getExternalizedString("property.action.short_description"));
            properties[3].setPropertyEditorClass(Action.class);
			properties[3].setValue(BLACK_LIST_NAME, Boolean.TRUE); 
            
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
		
	}

}
