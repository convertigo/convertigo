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

package com.twinsoft.convertigo.beans.couchdb;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class AbstractFullSyncFilterListenerBeanInfo extends MySimpleBeanInfo {

	public AbstractFullSyncFilterListenerBeanInfo() {
		try {
			beanClass =  AbstractFullSyncFilterListener.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.couchdb.AbstractFullSyncListener.class;

		    iconNameC16 = "/com/twinsoft/convertigo/beans/couchdb/images/fullsynclistener_color_16x16.png";
		    iconNameC32 = "/com/twinsoft/convertigo/beans/couchdb/images/fullsynclistener_color_32x32.png";
			
			resourceBundle = getResourceBundle("res/AbstractFullSyncFilterListener");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
			
			properties = new PropertyDescriptor[1];
			
			properties[0] = new PropertyDescriptor("targetFilter", beanClass, "getTargetFilter", "setTargetFilter");
			properties[0].setDisplayName(getExternalizedString("property.targetFilter.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.targetFilter.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("NamedSourceSelectorEditor"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
