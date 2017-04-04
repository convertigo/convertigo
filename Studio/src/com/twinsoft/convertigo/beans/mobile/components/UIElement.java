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

import java.util.Iterator;

import com.twinsoft.convertigo.engine.EngineException;

public class UIElement extends UIComponent implements IStyleGenerator {
	
	private static final long serialVersionUID = -8671694717057158581L;

	public UIElement() {
		super();
	}

	protected UIElement(String tagName) {
		this();
		this.tagName = tagName;
	}
	
	@Override
	public UIElement clone() throws CloneNotSupportedException {
		UIElement cloned = (UIElement) super.clone();
		return cloned;
	}

	/*
	 * The tagname
	 */
	private String tagName = "tag";
	
	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	/*
	 * the self-closing state
	 */
	private boolean selfClose = false;
	
	public boolean isSelfClose() {
		return selfClose;
	}

	public void setSelfClose(boolean selfClose) {
		this.selfClose = selfClose;
	}
	
	
	@Override
	public void addUIComponent(UIComponent uiComponent) throws EngineException {
		if (isSelfClose() && !(uiComponent instanceof UIAttribute)) {
			throw new EngineException("You cannot add component to this self-closing tag");
		}
		else {
			super.addUIComponent(uiComponent);
		}
	}

	@Override
	public String toString() {
		String label = getTagName();
		return label.isEmpty() ? super.toString() : label;
	}

	public String getId() {
		return "tag"+priority;
	}
	
	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			StringBuilder attributes = new StringBuilder();
			StringBuilder children = new StringBuilder();
			
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
			
			String attrId = " id=\""+ getId() +"\"";
			
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
		StringBuilder styles = new StringBuilder();
		StringBuilder others = new StringBuilder();
		
		Iterator<UIComponent> it = getUIComponentList().iterator();
		while (it.hasNext()) {
			UIComponent component = (UIComponent)it.next();
			if (component instanceof UIStyle) {
				String tpl = component.computeTemplate();
				if (!tpl.isEmpty()) {
					styles.append(tpl).append(";").append(System.getProperty("line.separator"));
				}
			}
			else if (component instanceof UIElement) {
				String tpl = ((UIElement)component).computeStyle();
				if (!tpl.isEmpty()) {
					others.append(tpl);
				}
			}
		}
		
		StringBuilder sb = new StringBuilder();
		if (styles.length()>0) {
			sb.append("#"+ getId()).append(" {").append(System.getProperty("line.separator"));
			sb.append(styles);
			sb.append("}").append(System.getProperty("line.separator"));
		}
		if (others.length()>0) {
			sb.append(others);
		}
		return sb.toString();
	}


}
