
package com.twinsoft.convertigo.beans.statements;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class IfXpathExistsThenElseStatementBeanInfo extends MySimpleBeanInfo {
    
	public IfXpathExistsThenElseStatementBeanInfo() {
		try {
			beanClass = IfXpathExistsThenElseStatement.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.statements.IfXpathExistsStatement.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/statements/images/ifxpathexists_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/statements/images/ifxpathexists_32x32.gif";
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/statements/res/IfXpathExistsThenElseStatement");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
