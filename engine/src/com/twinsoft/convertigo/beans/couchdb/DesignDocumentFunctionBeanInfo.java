/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software under the GNU Affero General Public License.
 */

package com.twinsoft.convertigo.beans.couchdb;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class DesignDocumentFunctionBeanInfo extends MySimpleBeanInfo {

	public DesignDocumentFunctionBeanInfo() {
		beanClass = DesignDocumentFunction.class;
		additionalBeanClass = com.twinsoft.convertigo.beans.core.DatabaseObject.class;
		displayName = "FullSync function";
		shortDescription = "Filter, update or validate function of a FullSync design document.";
		iconNameC16 = "/com/twinsoft/convertigo/beans/couchdb/images/function_color_16x16.png";
		iconNameC32 = "/com/twinsoft/convertigo/beans/couchdb/images/function_color_32x32.png";
		properties = new PropertyDescriptor[0];
	}
}
