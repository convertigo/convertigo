/*
 * Copyright (c) 2001-2016 Convertigo SA.
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

package com.twinsoft.convertigo.beans.mobile.components;

import java.beans.BeanInfo;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.IDynamicBean;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonEvent;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonProperty;
import com.twinsoft.convertigo.engine.EngineException;

public class UIDynamicElement extends UIElement implements IDynamicBean {

	private static final long serialVersionUID = -614143305710906263L;
	
	public UIDynamicElement() {
		super();
	}
	
	public UIDynamicElement(String tagName) {
		super(tagName);
	}

	@Override
	public UIDynamicElement clone() throws CloneNotSupportedException {
		UIDynamicElement cloned = (UIDynamicElement) super.clone();
		cloned.ionBean = null;
		return cloned;
	}

	private String beanData = null;
	
	public String getBeanData() {
		return beanData;
	}

	public void setBeanData(String beanData) {
		this.beanData = beanData;
	}
	
	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);

		// load bean data
		loadBean();
	}
	
	@Override
	public Element toXml(org.w3c.dom.Document document) throws EngineException {
    	// store bean data 
		saveBean();
		
		return super.toXml(document);
	}

	transient protected IonBean ionBean = null;
	
	protected void loadBean() throws Exception {
		if (ionBean == null && beanData != null) {
			ionBean = ComponentManager.loadBean(beanData);
		}
	}
	
	protected void saveBean() {
		if (ionBean != null) {
			beanData = ionBean.toBeanData();
    	}
	}
	
	public IonBean getIonBean() {
		try {
			loadBean();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ionBean;
	}

	@Override
	public String getDynamicIconName(int iconType) {
    	if (ionBean != null) {
			if (iconType == BeanInfo.ICON_COLOR_16x16) {
				return ionBean.getIconColor16Path();
			}
			else if (iconType == BeanInfo.ICON_COLOR_32x32) {
				return ionBean.getIconColor32Path();
			}
    	}
		return null;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	protected StringBuilder initAttributes() {
		StringBuilder attributes = super.initAttributes();

		IonBean ionBean = getIonBean();
		
    	if (ionBean != null) {
    		String formControlVarName = getFormControlName();
    		
			for (IonProperty property : ionBean.getProperties().values()) {
				String name = property.getName();
				String attr = property.getAttr();
				Object value = property.getValue();
				
				// case value is set
				if (!value.equals(false)) {
					if (name.equals("AutoDisable")) {
						UIForm form = getUIForm();
						if (form != null) {
							String formGroupName = form.getFormGroupName();
							attributes.append(" [disabled]=\"!").append(formGroupName).append(".valid\"");
						}
					} else {
						String smartValue = property.getSmartValue();
						if (name.equals("FormControlName") && formControlVarName.isEmpty()) {
							continue;
						}
						if (attr.equals("[ngModel]")) {
							String tagName = getTagName();
							if (tagName.equals("ion-checkbox") || tagName.equals("ion-toggle")) {
								attr = formControlVarName.isEmpty() ? "[checked]":"[ngModel]";
							} else if (tagName.equals("ion-datetime")) {
								attr = formControlVarName.isEmpty() ? "value":"[ngModel]";
							}
						}
						
						attributes.append(" ");
						if (attr.isEmpty()) {
							attributes.append(smartValue);
						}
						else if (attr.indexOf("%%") != -1){
							attributes.append(attr.replaceFirst("%%", smartValue));
						}
						else {
							attributes.append(attr).append("=");
							attributes.append("\"").append(smartValue).append("\"");
						}
					}
				}
				// case value is not set
				else {
					if (attr.equals("[ngModel]")) {
						String tagName = getTagName();
						if (formControlVarName.isEmpty() 
								|| tagName.equals("ion-checkbox") || tagName.equals("ion-toggle")) {
							continue;
						} else {
							attributes.append(" [ngModel]=\"null\"");
						}
					}
				}
			}
    	}
		return attributes;
	}

	@Override
	protected String getFormControlName() {
		IonBean ionBean = getIonBean();
		if (ionBean != null && ionBean.hasProperty("FormControlName")) {
			MobileSmartSourceType msst = (MobileSmartSourceType) ionBean.getPropertyValue("FormControlName");
			if (msst != null && !msst.getValue().equals("not set") && !msst.getValue().isEmpty()) {
				return msst.getValue();
			}
		}
		return super.getFormControlName();
	}
	
	@Override
	public boolean updateSmartSource(String oldString, String newString) {
		if (beanData != null) {
			if (beanData.indexOf(oldString) != -1 || Pattern.compile(oldString).matcher(beanData).find()) {
				beanData = beanData.replaceAll(oldString, newString);
				ionBean = null;
				this.hasChanged = true;
			}
			
		}
		boolean updated = super.updateSmartSource(oldString, newString);
		return updated || this.hasChanged;
	}
	
	protected String getEventAttr(String eventName) {
		IonBean ionBean = getIonBean();
		if (ionBean != null) {
			IonEvent ionEvent = ionBean.getEvent(eventName);
			if (ionEvent != null) {
				return ionEvent.getAttr();
			}
		}
		return "";
	}
	
	protected String[] getEventNames() {
		IonBean ionBean = getIonBean();
		
		String[] eventNames = new String[0];
    	if (ionBean != null) {
    		eventNames = ionBean.getEvents().keySet().toArray(new String[0]);
    		Arrays.sort(eventNames);
    	}
    	return eventNames;
	}
}
