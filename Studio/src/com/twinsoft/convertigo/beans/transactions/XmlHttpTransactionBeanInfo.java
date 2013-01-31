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

package com.twinsoft.convertigo.beans.transactions;

import java.beans.PropertyDescriptor;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class XmlHttpTransactionBeanInfo extends MySimpleBeanInfo {
    
	public XmlHttpTransactionBeanInfo() {
		try {
			beanClass = XmlHttpTransaction.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.transactions.AbstractHttpTransaction.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/images/xmlhttptransaction_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/images/xmlhttptransaction_color_32x32.png";

			properties = new PropertyDescriptor[4];
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/transactions/res/XmlHttpTransaction");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
			
			properties[0] = new PropertyDescriptor("xmlEncoding", XmlHttpTransaction.class, "getXmlEncoding", "setXmlEncoding");
			properties[0].setDisplayName(getExternalizedString("property.xmlEncoding.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.xmlEncoding.short_description"));
			properties[0].setExpert(true);
			
			properties[1] = new PropertyDescriptor("responseElementQName", XmlHttpTransaction.class, "getResponseElementQName", "setResponseElementQName");
			properties[1].setDisplayName(getExternalizedString("property.responseElementQName.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.responseElementQName.short_description"));
			properties[1].setExpert(true);
			//TODO : properties[1].setHidden(false);
			
			properties[2] = new PropertyDescriptor("ignoreSoapEnveloppe", XmlHttpTransaction.class, "isIgnoreSoapEnveloppe", "setIgnoreSoapEnveloppe");
			properties[2].setDisplayName(getExternalizedString("property.ignoreSoapEnveloppe.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.ignoreSoapEnveloppe.short_description"));
			properties[2].setExpert(true);
			
            properties[3] = new PropertyDescriptor("xmlElementRefAffectation", beanClass, "getXmlElementRefAffectation", "setXmlElementRefAffectation");
			properties[3].setDisplayName(getExternalizedString("property.xmlElementRefAffectation.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.xmlElementRefAffectation.short_description"));
			properties[3].setExpert(true);
			properties[3].setHidden(false);
			properties[3].setPropertyEditorClass(getEditorClass("XmlQNameEditor"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
