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

package com.twinsoft.convertigo.beans.connectors;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class SiteClipperConnectorBeanInfo extends MySimpleBeanInfo {
	public SiteClipperConnectorBeanInfo() {
		try {
			beanClass = SiteClipperConnector.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Connector.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/connectors/images/siteclipperconnector_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/connectors/images/siteclipperconnector_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/connectors/res/SiteClipperConnector");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[3];
			
			properties[0] = new PropertyDescriptor("defaultResponseCharset", beanClass, "getDefaultResponseCharset", "setDefaultResponseCharset");
			properties[0].setDisplayName(getExternalizedString("property.defaultResponseCharset.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.defaultResponseCharset.short_description"));
			
			properties[1] = new PropertyDescriptor("domainsListing", beanClass, "getDomainsListing", "setDomainsListing");
			properties[1].setDisplayName(getExternalizedString("property.domainsListing.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.domainsListing.short_description"));
			properties[1].setPropertyEditorClass(getEditorClass("DomainsListingEditor"));
			
			properties[2] = new PropertyDescriptor("trustAllServerCertificates", beanClass, "isTrustAllServerCertificates", "setTrustAllServerCertificates");
			properties[2].setDisplayName(getExternalizedString("property.trustAllServerCertificates.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.trustAllServerCertificates.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}
