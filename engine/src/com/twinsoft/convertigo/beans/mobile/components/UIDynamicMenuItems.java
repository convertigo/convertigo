/*
 * Copyright (c) 2001-2022 Convertigo SA.
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
						if (uiAttribute.isEnabled()) {
							attrclasses.append(attrclasses.length()>0 ? " ":"").append(uiAttribute.getAttrValue());
						}
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
			if (compareToTplVersion("7.5.2.1") < 0) {
				sb.append("<button"+attrclass+ " ion-item menuClose=\""+menuId+"\" *ngFor=\"let p of getPagesIncludedInAutoMenu()\" (click)=\"openPage(p)\" >")
				.append(System.lineSeparator())
				.append("<ion-icon name=\"{{p.icon}}\" ></ion-icon>")
				.append(System.lineSeparator())
				.append("{{p.title}}")
				.append(System.lineSeparator())
				.append("</button>")
				.append(System.lineSeparator());
				
			} else {
				sb.append("<button"+attrclass+ " ion-item menuClose=\""+menuId+"\" *ngFor=\"let p of getPagesIncludedInAutoMenu()\" (click)=\"openPage(p)\" [ngSwitch]=\"p.iconPos\">")
					.append(System.lineSeparator())
					.append("<ion-icon name=\"{{p.icon}}\" *ngSwitchCase=\"'item-start'\" item-start></ion-icon>")
					.append(System.lineSeparator())
					.append("<ion-icon name=\"{{p.icon}}\" *ngSwitchCase=\"'item-end'\" item-end></ion-icon>")
					.append(System.lineSeparator())
					.append("<ion-icon name=\"{{p.icon}}\" *ngSwitchCase=\"'item-right'\" item-right></ion-icon>")
					.append(System.lineSeparator())
					.append("<ion-icon name=\"{{p.icon}}\" *ngSwitchDefault item-left></ion-icon>")
					.append(System.lineSeparator())
					.append(compareToTplVersion("7.7.0.6") < 0 ? "{{p.title}}":"{{p.titleKey | translate}}")
					.append(System.lineSeparator())
					.append("</button>")
					.append(System.lineSeparator());
			}
			return sb.toString();
		}
		return "";
	}

	@Override
	public String toString() {
		return getName();
	}
}
