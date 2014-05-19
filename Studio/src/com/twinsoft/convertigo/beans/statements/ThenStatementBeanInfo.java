
package com.twinsoft.convertigo.beans.statements;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class ThenStatementBeanInfo extends MySimpleBeanInfo {
    
	public ThenStatementBeanInfo() {
		try {
			beanClass = ThenStatement.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.StatementWithExpressions.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/statements/images/if_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/statements/images/if_32x32.png";
			
			resourceBundle = getResourceBundle("res/ThenStatement");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
