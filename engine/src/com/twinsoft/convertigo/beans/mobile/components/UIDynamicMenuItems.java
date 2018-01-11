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

public class UIDynamicMenuItems extends UIDynamicMenuItem {

	private static final long serialVersionUID = -1878766101499902856L;

	public UIDynamicMenuItems() {
		super();
	}

	public UIDynamicMenuItems(String tagName) {
		super(tagName);
	}

	@Override
	public UIDynamicMenuItems clone() throws CloneNotSupportedException {
		UIDynamicMenuItems cloned = (UIDynamicMenuItems) super.clone();
		return cloned;
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
			
			String menuId = getMenuId();
			
			StringBuilder sb = new StringBuilder();
			sb.append("<button"+attrclass+ " ion-item menuClose=\""+menuId+"\" *ngFor=\"let p of getPagesIncludedInAutoMenu()\" (click)=\"openPage(p)\">")
				.append("<ion-icon name=\"{{p.icon}}\"></ion-icon>&nbsp;&nbsp;&nbsp;&nbsp;")
				.append("{{p.title}}")
				.append("</button>")
				.append(System.lineSeparator());
			return sb.toString();
		}
		return "";
	}

	@Override
	public String toString() {
		return getName();
	}
}
