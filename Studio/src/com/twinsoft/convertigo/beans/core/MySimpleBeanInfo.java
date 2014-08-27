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

package com.twinsoft.convertigo.beans.core;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.io.IOException;
import java.util.Collections;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.apache.commons.lang.ArrayUtils;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;

public class MySimpleBeanInfo extends SimpleBeanInfo {
	public static final String BLACK_LIST_NAME = "blackListedFromAdmin";
	
	protected Class<? extends DatabaseObject> beanClass = null;
	protected Class<? extends DatabaseObject> additionalBeanClass = null;
	
	protected ResourceBundle resourceBundle;
	
	protected String displayName = "?";
	protected String shortDescription = "?";
	
	protected PropertyDescriptor[] properties = new PropertyDescriptor[0];
	protected PropertyDescriptor[] localProperties = null;

	protected String iconNameC16 = null;
	protected String iconNameC32 = null;
	protected String iconNameM16 = null;
	protected String iconNameM32 = null;
	
	private java.awt.Image iconColor16 = null;
	private java.awt.Image iconColor32 = null;
	private java.awt.Image iconMono16 = null;
	private java.awt.Image iconMono32 = null;
	
	private boolean additionalPropertiesLoaded = false;
	
	public String getExternalizedString(String key){
		try {
			return resourceBundle.getString(key);
		}
		catch (java.util.MissingResourceException e) {
			return key;
		}
	}
	
	@Override
	public BeanDescriptor getBeanDescriptor() {
		BeanDescriptor beanDescriptor = new BeanDescriptor(beanClass, null);
		beanDescriptor.setDisplayName(displayName);
		beanDescriptor.setShortDescription(shortDescription);
		if (iconNameC16 != null) {
			beanDescriptor.setValue("icon" + BeanInfo.ICON_COLOR_16x16, iconNameC16);
		}
		if (iconNameC32 != null) {
			beanDescriptor.setValue("icon" + BeanInfo.ICON_COLOR_32x32, iconNameC32);
		}
		if (iconNameM16 != null) {
			beanDescriptor.setValue("icon" + BeanInfo.ICON_MONO_16x16, iconNameM16);
		}
		if (iconNameM32 != null) {
			beanDescriptor.setValue("icon" + BeanInfo.ICON_MONO_32x32, iconNameM32);
		}
		return beanDescriptor;
	}
    
	@Override
	public PropertyDescriptor[] getPropertyDescriptors() {
		checkAdditionalProperties();
		return properties;
	}

	public PropertyDescriptor[] getLocalPropertyDescriptors() {
		checkLocalProperties();
		return localProperties;
	}

	private void checkLocalProperties() {
    	if (localProperties == null) {
    		localProperties = new PropertyDescriptor[0];
    		ArrayUtils.addAll(localProperties, properties);
    	}
	}

	@Override
    public EventSetDescriptor[] getEventSetDescriptors() {
		EventSetDescriptor[] eventSets = new EventSetDescriptor[0];
		return eventSets;
    }
    
	@Override
    public MethodDescriptor[] getMethodDescriptors() {
		MethodDescriptor[] methods = new MethodDescriptor[0];
		return methods;
    }

    /**
     * gets a reference to a editor, if the class is found or null if the class is not in the class path. This is used
     * to be able to have Convertigo Engine having no references to the Convertigo Plugin.
     */
    public Class<?> getEditorClass(String className) {
   		try {
   			//Engine.logBeans.trace("Try to get the property editor");
			Class<?> c = Class.forName("com.twinsoft.convertigo.eclipse.property_editors." + className);
			return c;
		} catch (ClassNotFoundException e) {
			// Server mode => no eclipse.jar, so no editor classes as expected
			// If studio mode: error!
			if (!Engine.isEngineMode())
				Engine.logBeans.error("Property editor '" + className + "' cannot be found", e);
			
			return null;
		} catch (Throwable e) {
			// Any Throwable will result in a return null (as NoDefClassFound) 
   			Engine.logBeans.error("Property editor '" + className + "' cannot be found", e);
			return null;
		}
    }
    
    /**
     * This method returns an image object that can be used to
     * represent the bean in toolboxes, toolbars, etc.   Icon images
     * will typically be GIFs, but may in future include other formats.
     * <p>
     * Beans aren't required to provide icons and may return null from
     * this method.
     * <p>
     * There are four possible flavors of icons (16x16 color,
     * 32x32 color, 16x16 mono, 32x32 mono).  If a bean choses to only
     * support a single icon we recommend supporting 16x16 color.
     * <p>
     * We recommend that icons have a "transparent" background
     * so they can be rendered onto an existing background.
     *
     * @param  iconKind  The kind of icon requested.  This should be
     *    one of the constant values ICON_COLOR_16x16, ICON_COLOR_32x32,
     *    ICON_MONO_16x16, or ICON_MONO_32x32.
     * @return  An image object representing the requested icon.  May
     *    return null if no suitable icon is available.
     */
    @Override
    public java.awt.Image getIcon(int iconKind) {
        switch (iconKind) {
            case ICON_COLOR_16x16:
                if (iconNameC16 == null)
                    return null;
                else {
                    if (iconColor16 == null)
                        iconColor16 = loadImage(iconNameC16);
                    return iconColor16;
                }
            case ICON_COLOR_32x32:
                if (iconNameC32 == null)
                    return null;
                else {
                    if (iconColor32 == null)
                        iconColor32 = loadImage(iconNameC32);
                    return iconColor32;
                }
            case ICON_MONO_16x16:
                if (iconNameM16 == null)
                    return null;
                else {
                    if (iconMono16 == null)
                        iconMono16 = loadImage(iconNameM16);
                    return iconMono16;
                }
            case ICON_MONO_32x32:
                if (iconNameM32 == null)
                    return null;
                else {
                    if (iconMono32 == null)
                        iconMono32 = loadImage(iconNameM32);
                    return iconMono32;
                }
            default: return null;
        }
    }
    
    public static String getIconName(BeanInfo bean, int iconType) {
    	return bean != null ? (String) bean.getBeanDescriptor().getValue("icon" + iconType) : null;
    }
    
    protected PropertyDescriptor getPropertyDescriptor(String name) throws IntrospectionException {
    	checkAdditionalProperties();
    	for (int i = 0 ; i < properties.length ; i++) {
    		PropertyDescriptor property = properties[i];
    		if (name.equals(property.getName())) {
    			PropertyDescriptor clone = new PropertyDescriptor(name, property.getReadMethod(), property.getWriteMethod());
    			clone.setDisplayName(property.getDisplayName());
    			clone.setShortDescription(property.getShortDescription());
    			clone.setPropertyEditorClass(property.getPropertyEditorClass());
    			clone.setBound(property.isBound());
    			clone.setConstrained(property.isConstrained());
    			clone.setExpert(property.isExpert());
    			clone.setHidden(property.isHidden());
    			clone.setPreferred(property.isPreferred());
    			for (String attributeName : Collections.list(property.attributeNames())) {
    				clone.setValue(attributeName, property.getValue(attributeName));
    			}
    			return properties[i] = clone;
    		}
    	}
    	return null;
    }
    
    private void checkAdditionalProperties() {
    	if (!additionalPropertiesLoaded) {
    		checkLocalProperties();
    		if (additionalBeanClass != null) {
				try {
					properties = (PropertyDescriptor[]) ArrayUtils.addAll(properties, CachedIntrospector
							.getBeanInfo(additionalBeanClass).getPropertyDescriptors());
				} catch (IntrospectionException e) {
				}
    		}
    		additionalPropertiesLoaded = true;
    	}
    }
    
    protected ResourceBundle getResourceBundle(String path) throws IOException {
    	return new PropertyResourceBundle(beanClass.getResourceAsStream(path + ".properties"));
    }
}