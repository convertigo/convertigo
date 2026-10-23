/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.util;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean;

/**
 * The switch of the new experimental features (JSON native sequences and their native steps), for everything that
 * lists the objects and their properties: the reference documentation, the palettes and property sheets of the Studios,
 * the catalog of the Convertigo MCP library. Off by default, so a server never shows them; the Eclipse Studio turns it
 * on with its preference "Show new experimental features" (the system property {@value #SYSTEM_PROPERTY}).
 * <p>
 * The marker: a bean of database_objects.xml with experimental="true" ({@link DboBean#isExperimental()}), a property
 * descriptor with the value {@link MySimpleBeanInfo#EXPERIMENTAL} true.
 * <p>
 * The rule while the switch is off: an experimental bean is not offered nor documented, an experimental property is
 * not documented and an object shows it only when it is on (true, not empty). What a project already holds stays
 * shown and editable: an existing native step, a sequence already native.
 */
public final class ExperimentalFeatures {
	public static final String SYSTEM_PROPERTY = "convertigo.experimental.features";

	private ExperimentalFeatures() {
	}

	/** the experimental features are shown (false by default) */
	public static boolean isShown() {
		return Boolean.parseBoolean(System.getProperty(SYSTEM_PROPERTY));
	}

	/** shows or hides the experimental features (the preference of the Eclipse Studio) */
	public static void setShown(boolean shown) {
		System.setProperty(SYSTEM_PROPERTY, Boolean.toString(shown));
	}

	public static boolean isExperimental(DboBean bean) {
		return bean != null && bean.isExperimental();
	}

	public static boolean isExperimental(PropertyDescriptor propertyDescriptor) {
		return propertyDescriptor != null && Boolean.TRUE.equals(propertyDescriptor.getValue(MySimpleBeanInfo.EXPERIMENTAL));
	}

	/** an experimental bean, not offered nor documented while the switch is off */
	public static boolean isHiddenBean(DboBean bean) {
		return isHiddenBean(bean, isShown());
	}

	public static boolean isHiddenBean(DboBean bean, boolean shown) {
		return !shown && isExperimental(bean);
	}

	/** an experimental property, not documented nor described while the switch is off (no object to look at) */
	public static boolean isHiddenProperty(PropertyDescriptor propertyDescriptor) {
		return !isShown() && isExperimental(propertyDescriptor);
	}

	/** an experimental property of an object, hidden while the switch is off and the property is off */
	public static boolean isHiddenProperty(Object object, PropertyDescriptor propertyDescriptor) {
		return isHiddenProperty(object, propertyDescriptor, isShown());
	}

	public static boolean isHiddenProperty(Object object, PropertyDescriptor propertyDescriptor, boolean shown) {
		if (shown || !isExperimental(propertyDescriptor)) {
			return false;
		}
		if (object == null) {
			return true;
		}
		Method getter = propertyDescriptor.getReadMethod();
		if (getter == null) {
			return true;
		}
		try {
			return isOff(getter.invoke(object));
		} catch (Exception e) {
			// unreadable: shown, an existing value must stay editable
			return false;
		}
	}

	/** the experimental property propertyName of a database object, hidden while the switch is off and it is off */
	public static boolean isHiddenProperty(DatabaseObject databaseObject, String propertyName) {
		if (isShown() || databaseObject == null || propertyName == null) {
			return false;
		}
		try {
			BeanInfo beanInfo = CachedIntrospector.getBeanInfo(databaseObject);
			for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
				if (propertyName.equals(propertyDescriptor.getName())) {
					return isHiddenProperty(databaseObject, propertyDescriptor, false);
				}
			}
		} catch (Exception e) {
			// no bean info: nothing hidden
		}
		return false;
	}

	/** the value of a property left to its default: null, false, an empty text */
	static boolean isOff(Object value) {
		return value == null || Boolean.FALSE.equals(value) || (value instanceof CharSequence && ((CharSequence) value).length() == 0);
	}
}
