/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.twinsoft.convertigo.beans.core.DatabaseObject.DboFolderType;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.engine.enums.FolderType;

@DboFolderType(type = FolderType.MENU)
public class UIDynamicMenu extends UIDynamicElement {

	private static final long serialVersionUID = 7671346079616209922L;

	public UIDynamicMenu() {
		super();
	}

	public UIDynamicMenu(String tagName) {
		super(tagName);
	}

	@Override
	public UIDynamicMenu clone() throws CloneNotSupportedException {
		UIDynamicMenu cloned = (UIDynamicMenu) super.clone();
		return cloned;
	}
	
	@Override
	protected StringBuilder initAttributes() {
		StringBuilder attributes = super.initAttributes();
		attributes.append(" menu-id=\""+ getId() + "\"");
		attributes.append(" content-id=\"main-content\"");
		return attributes;
	}
	
	protected String getId() {
		return getName();
	}
	
	protected String getSide() {
		IonBean ionBean = getIonBean();
		if (ionBean != null && ionBean.hasProperty("MenuSide")) {
			MobileSmartSourceType msst = (MobileSmartSourceType) ionBean.getPropertyValue("MenuSide");
			if (msst != null && !msst.getValue().equals("not set") && !msst.getValue().isEmpty()) {
				return msst.getValue();
			}
		}
		return "start";
	}
	
	protected Map<String, Set<String>> getInfoMap() {
		Set<UIComponent> done = new HashSet<>();
		Map<String, Set<String>> map = new HashMap<String, Set<String>>();
		for (UIComponent uiComponent : getUIComponentList()) {
			uiComponent.addInfos(done, map);
		}
		return map;
	}
}
