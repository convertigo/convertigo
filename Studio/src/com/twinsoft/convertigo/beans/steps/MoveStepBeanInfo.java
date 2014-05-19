package com.twinsoft.convertigo.beans.steps;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class MoveStepBeanInfo extends MySimpleBeanInfo{
	
	public MoveStepBeanInfo() {
		try {
			beanClass = MoveStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.steps.CopyStep.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/move_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/move_32x32.png";
			
			resourceBundle = getResourceBundle("res/MoveStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");	          
		
			PropertyDescriptor property = getPropertyDescriptor("sourcePath");
			property.setDisplayName(getExternalizedString("property.sourcePath.display_name"));
			property.setShortDescription(getExternalizedString("property.sourcePath.short_description"));

			property = getPropertyDescriptor("destinationPath");
			property.setDisplayName(getExternalizedString("property.destinationPath.display_name"));
			property.setShortDescription(getExternalizedString("property.destinationPath.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
