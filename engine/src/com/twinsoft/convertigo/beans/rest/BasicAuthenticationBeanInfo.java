package com.twinsoft.convertigo.beans.rest;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class BasicAuthenticationBeanInfo extends MySimpleBeanInfo {

	public BasicAuthenticationBeanInfo() {
		try {
			beanClass = BasicAuthentication.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.UrlAuthentication.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/rest/images/basicauthentication_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/rest/images/basicauthentication_color_32x32.png";

			resourceBundle = getResourceBundle("res/BasicAuthentication");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
