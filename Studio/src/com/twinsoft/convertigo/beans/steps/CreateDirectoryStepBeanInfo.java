package com.twinsoft.convertigo.beans.steps;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class CreateDirectoryStepBeanInfo extends MySimpleBeanInfo{
	
	public CreateDirectoryStepBeanInfo() {
		try {
			beanClass = CreateDirectoryStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/createDirectory_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/createDirectory_32x32.png";
			
			resourceBundle = getResourceBundle("res/CreateDirectoryStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");	          
		
			properties = new PropertyDescriptor[2];
			
	        properties[0] = new PropertyDescriptor("destinationPath", beanClass, "getDestinationPath", "setDestinationPath");
			properties[0].setExpert(true);
			properties[0].setDisplayName(getExternalizedString("property.destinationPath.display_name"));
	        properties[0].setShortDescription(getExternalizedString("property.destinationPath.short_description"));    
	        properties[0].setValue("scriptable", Boolean.TRUE);
	        
			properties[1] = new PropertyDescriptor("createNonExistentParentDirectories", beanClass, "isCreateNonExistentParentDirectories", "setCreateNonExistentParentDirectories");
			properties[1].setDisplayName(getExternalizedString("property.createNonExistentParentDirectories.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.createNonExistentParentDirectories.short_description"));
			properties[1].setExpert(true);
	     
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
