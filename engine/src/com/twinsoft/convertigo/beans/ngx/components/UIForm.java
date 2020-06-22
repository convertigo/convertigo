/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

public class UIForm extends UIElement {

	private static final long serialVersionUID = 4115423575037640826L;

	public UIForm() {
		super("form");
	}

	@Override
	public UIForm clone() throws CloneNotSupportedException {
		UIForm cloned = (UIForm) super.clone();
		return cloned;
	}

	@Override
	protected StringBuilder initAttributes() {
		StringBuilder sb = new StringBuilder();
		if (!identifier.isEmpty()) {
			sb.append(" #"+ identifier + "=\"ngForm\"");
		}
		return sb;
	}	
	
	@Override
	public String computeJsonModel() {
		if (isEnabled()) {
			StringBuilder models = new StringBuilder();
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof UIElement) {
					String model = ((UIElement)component).computeJsonModel();
					models.append(models.length() > 0 && !model.isEmpty() ? ",":"").append(model);
				}
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			if (models.length() > 0) {
				  sb.append("\"controls\":{").append(models).append("}").append(",");
			}
			sb.append("\"errors\":\"\"").append(",")
			  .append("\"status\":\"\"").append(",")
			  .append("\"valid\":\"\"").append(",")
			  .append("\"value\":\"\"")
			  .append("}");
			return sb.toString();
		}
		return "";
	}
	
}
