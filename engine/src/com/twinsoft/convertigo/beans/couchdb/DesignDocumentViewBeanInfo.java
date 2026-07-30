/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software under the GNU Affero General Public License.
 */

package com.twinsoft.convertigo.beans.couchdb;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class DesignDocumentViewBeanInfo extends MySimpleBeanInfo {

	public DesignDocumentViewBeanInfo() {
		beanClass = DesignDocumentView.class;
		additionalBeanClass = com.twinsoft.convertigo.beans.core.DatabaseObject.class;
		displayName = "FullSync view";
		shortDescription = "Map and optional reduce functions exposed by a FullSync design document.";
		iconNameC16 = "/com/twinsoft/convertigo/beans/couchdb/images/designdocument_color_16x16.png";
		iconNameC32 = "/com/twinsoft/convertigo/beans/couchdb/images/designdocument_color_32x32.png";
		properties = new PropertyDescriptor[0];
	}
}
