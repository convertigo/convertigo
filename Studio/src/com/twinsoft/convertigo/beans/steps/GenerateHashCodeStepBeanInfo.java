package com.twinsoft.convertigo.beans.steps;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class GenerateHashCodeStepBeanInfo extends MySimpleBeanInfo{
	
	public GenerateHashCodeStepBeanInfo() {
		try {
			beanClass = GenerateHashCodeStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/generatehashcode_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/generatehashcode_32x32.png";
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/steps/res/GenerateHashCodeStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");	          
		
			properties = new PropertyDescriptor[3];
			
			properties[0] = new PropertyDescriptor("sourcePath", beanClass, "getSourcePath", "setSourcePath");
			properties[0].setDisplayName(getExternalizedString("property.sourcePath.display_name"));
	        properties[0].setShortDescription(getExternalizedString("property.sourcePath.short_description"));            
	        properties[0].setValue("scriptable", Boolean.TRUE);
	        
	        properties[1] = new PropertyDescriptor("hashAlgorithm", beanClass, "getHashAlgorithm", "setHashAlgorithm");
			properties[1].setDisplayName(getExternalizedString("property.hashAlgorithm.display_name"));
	        properties[1].setShortDescription(getExternalizedString("property.hashAlgorithm.short_description"));
	        properties[1].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
	        
	        properties[2] = new PropertyDescriptor("nodeName", beanClass, "getNodeName", "setNodeName");
            properties[2].setDisplayName(getExternalizedString("property.nodeName.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.nodeName.short_description"));
            properties[2].setValue("normalizable", Boolean.TRUE);
            properties[2].setValue(BLACK_LIST_NAME, Boolean.TRUE); 
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
