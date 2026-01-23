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

package com.twinsoft.convertigo.beans.ngx.components;

import java.util.Iterator;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

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
	public void configure(Element element) throws Exception {
		super.configure(element);

		String version = element.getAttribute("version");

		if (version == null) {
			String s = XMLUtils.prettyPrintDOM(element);
			EngineException ee = new EngineException(
					"Unable to find version number for the database object \"" + getName() + "\".\nXML data: " + s);
			throw ee;
		}

		try {
			if (VersionUtils.compare(version, "8.4.0") < 0) {
				if (!isI18n()) {
					this.setI18n(true); // for compatibility
					this.hasChanged = true;
				}
			}
		} catch (Exception e) {
			throw new EngineException("Unable to migrate the UICustomAction \"" + getName() + "\".", e);
		}
	}
	
	private boolean i18n = false;

	public boolean isI18n() {
		return i18n;
	}

	public void setI18n(boolean i18n) {
		this.i18n = i18n;
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
			
//			String menuId = getMenuId();
			
			StringBuilder sb = new StringBuilder();
			if (compareToTplVersion("7.9.0.2") >= 0) {
				if (compareToTplVersion("8.4.0.6") >= 0) { // #983
					sb.append("<ion-item "+ attrclass +" *ngFor=\"let p of appPages;\" [hidden]=\"!p.includedInAutoMenu\" routerDirection=\"root\" [routerLink]=\"[p.url]\" lines=\"none\" detail=\"false\" [class.selected]=\"(selectedPath$ | async)?.startsWith('/' + p.url)\">");
				} else {
					sb.append("<ion-item "+ attrclass +" *ngFor=\"let p of appPages;\" [hidden]=\"!p.includedInAutoMenu\" routerDirection=\"root\" [routerLink]=\"[p.url]\" lines=\"none\" detail=\"false\" [class.selected]=\"selectedPath.startsWith('/' + p.url)\">");
				}
				sb.append(System.lineSeparator())
				.append("<ion-icon [slot]=\"(p.icon != '' && p.iconPos == '') ? 'start':p.iconPos\" [ios]=\"p.icon + (p.icon == '' ? '':'-outline')\" [md]=\"p.icon + (p.icon == '' ? '':'-sharp')\"></ion-icon>")
				.append(System.lineSeparator())
				.append("<ion-label>"+ (isI18n() ? "{{ p.titleKey | translate }}" : "{{p.title}}") +"</ion-label>")
				.append(System.lineSeparator())
				.append("</ion-item>")
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
