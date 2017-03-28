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

public class UIControlListen extends UIControlDirectiveValue implements IIteratable {

	private static final long serialVersionUID = 7510058495102155462L;

	public UIControlListen() {
		super();
	}

	@Override
	public UIControlListen clone() throws CloneNotSupportedException {
		UIControlListen cloned = (UIControlListen)super.clone();
		return cloned;
	}

	@Override
	public String getDirectiveValue() {
		if (isEnabled()) {
			StringBuilder children = new StringBuilder();
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof UIControlListenSource) {
					String srcTpl = component.computeTemplate();
					if (!srcTpl.isEmpty()) {
						children.append(children.length() > 0 ? ",":"");
						children.append(srcTpl);
					}
				}
			}
			
			StringBuilder sb = new StringBuilder();
			if (children.length() > 0) {
				String item = getItemName();
				String path = getItemPath();
				
				sb.append("let "+ (item.isEmpty() ? "item":item))
						.append(" of listen([").append(children).append("])")
						.append(path);
				return sb.toString();
			}
		}
		return "";
	}

	/*
	 * The iteratable item name
	 */
	private String itemName = "item";
	
	public String getItemName() {
		return itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}
	
	
	/*
	 * The iteratable item path
	 */
	private String itemPath = "";
	
	public String getItemPath() {
		return itemPath;
	}

	public void setItemPath(String itemPath) {
		this.itemPath = itemPath;
	}

	
	@Override
	public String toString() {
		String label = getDirectiveValue();
		return label.isEmpty() ? "?":label;
	}
	
}
