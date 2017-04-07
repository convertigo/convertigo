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
import java.util.Iterator;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.IDynamicBean;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonProperty;
import com.twinsoft.convertigo.engine.Engine;
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
			if (Engine.isStudioMode()) {
				ionBean = ComponentManager.loadBean(beanData);
			}
			else {
				ionBean = new IonBean(beanData);
			}
		}
	}
	
	protected void saveBean() {
		if (ionBean != null) {
			beanData = ionBean.toString();
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
	public String computeTemplate() {
		if (isEnabled()) {
			StringBuilder attributes = new StringBuilder();
			StringBuilder children = new StringBuilder();
			
	    	if (ionBean != null) {
				for (IonProperty property : ionBean.getProperties().values()) {
					String attr = property.getAttr();
					Object value = property.getValue();
					if (!value.equals(false)) {
						attributes.append(" ");
						if (attr.isEmpty()){
							attributes.append(value);
						}
						else if (attr.indexOf("%%") != -1){
							attributes.append(attr.replaceFirst("%%", value.toString()));
						}
						else {
							attributes.append(attr).append("=");
							attributes.append("\"").append(value).append("\"");
						}
					}
				}
	    	}
	    	
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof UIStyle) {
					;// ignore
				} else if (component instanceof UIAttribute) {
					attributes.append(component.computeTemplate());
				} else {
					children.append(component.computeTemplate());
				}
			}
			
			String attrId = " class=\""+ getTagClass() +"\"";
			
			StringBuilder sb = new StringBuilder();
			sb.append("<").append(getTagName())
				.append(attrId)
				.append(attributes.length()>0 ? attributes:"");
			
			if (isSelfClose()) {
				sb.append("/>").append(System.getProperty("line.separator"));
			}
			else {
				sb.append(">").append(System.getProperty("line.separator"))
					.append(children.length()>0 ? children:"")
				  .append("</").append(getTagName())
				  	.append(">").append(System.getProperty("line.separator"));
			}
			
			return sb.toString();
		}
		return "";
	}

	@Override
	public String computeStyle() {
		return super.computeStyle();
	}
}
