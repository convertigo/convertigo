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

public class UIDynamicMenuItem extends UIDynamicElement {

	private static final long serialVersionUID = 3562736859348770057L;

	public UIDynamicMenuItem() {
		super();
	}

	public UIDynamicMenuItem(String tagName) {
		super(tagName);
	}

	@Override
	public UIDynamicMenuItem clone() throws CloneNotSupportedException {
		UIDynamicMenuItem cloned = (UIDynamicMenuItem) super.clone();
		return cloned;
	}
	
	@Override
	protected void addUIComponent(UIComponent uiComponent, Long after) throws EngineException {
        if (!(uiComponent instanceof UIAttribute)) {
            throw new EngineException("You cannot add component to this menu item");
        }
        super.addUIComponent(uiComponent, after);
	}
	
	/*
	 * The page associated with item
	 */
	private String itempage = "";
	
	public String getItemPage() {
		return itempage;
	}

	public void setItemPage(String itempage) {
		this.itempage = itempage;
	}
	
	private String getPageName() {
		if (!itempage.isEmpty()) {
			try {
				return itempage.substring(itempage.lastIndexOf('.')+1);
			} catch (Exception e) {}
		}
		return "";
	}
	
	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			StringBuilder attributes = initAttributes();
			StringBuilder attrclasses = new StringBuilder();
			
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof UIAttribute) {
					UIAttribute uiAttribute = (UIAttribute)component;
					if (uiAttribute.getAttrName().equals("class")) {
						attrclasses.append(attrclasses.length()>0 ? " ":"").append(uiAttribute.getAttrValue());
					} else {
						attributes.append(component.computeTemplate());
					}
				}
			}
			
			String tagClass = getTagClass();
			if (attrclasses.indexOf(tagClass) == -1) {
				attrclasses.append(attrclasses.length()>0 ? " ":"").append(tagClass);
			}
			String attrclass = attrclasses.length()>0 ? " class=\""+ attrclasses +"\"":"";
			
			boolean pageIsEnabled = false;
			String pageName = getPageName();
			String pageTitle = "Please specify a page for item";
			if (!pageName.isEmpty()) {
				try {
					pageIsEnabled = getApplication().getPageComponentByName(pageName).isEnabled();
					pageTitle = getApplication().getPageComponentByName(pageName).getTitle();
				} catch (Exception e) {}
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append("<").append(getTagName()).append(attrclass)
				.append(" ion-item menuClose").append(pageIsEnabled ? "":" disabled")
				.append(pageName.isEmpty() ? "":" (click)=\"openPageWithName('"+ pageName +"')\"")
			  	.append(attributes.length()>0 ? attributes:"").append(">").append(pageTitle)
				.append("</").append(getTagName()).append(">").append(System.getProperty("line.separator"));
			
			return sb.toString();
		}
		return "";
	}

	@Override
	public String toString() {
		String pageName = getPageName();
		return super.toString() + ": " + (pageName.isEmpty() ? "?":pageName);
	}
}
