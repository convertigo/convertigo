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

import com.twinsoft.convertigo.beans.core.DatabaseObject.DboFolderType;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.enums.FolderType;

@DboFolderType(type = FolderType.CONTROL)
public abstract class UIControlAttr extends UIAttribute implements ITagsProperty {
	
	private static final long serialVersionUID = -1131663200389122563L;

	public UIControlAttr() {
		super();
	}

	@Override
	public UIControlAttr clone() throws CloneNotSupportedException {
		UIControlAttr cloned = (UIControlAttr) super.clone();
		return cloned;
	}

	@Override
	public String getAttrValue() {
		if (isEnabled()) {
			StringBuilder children = new StringBuilder();
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				//if (component instanceof UIControlAction) {
				if (component instanceof IAction) {
					children.append(children.length() > 0 ? ";":"");
					children.append(component.computeTemplate());
				}
			}
			return children.toString();
		}
		return "";
	}

	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			String attr = getAttrName();
			String val = getAttrValue();
			
	        if (attr.isEmpty() || val.isEmpty()) { 
	        	return "";
	        }
	        else {
	        	if (isThrottleEvent(attr)) {
	        		return (" throttleEvent [throttleTime]=\""+ getThrottleTime(attr) +"\"" + 
	        				" throttleType=\""+ getThrottleType(attr) +"\"" +
	        				" #refThrottle"+ getParent().priority + "=\"throttleEvent\"" +
	        				" (throttleEvent)=\""+ val +"\"");
	        	} else {
	        		return (" "+attr+"=\""+ val +"\"");
	        	}
	        }
		}
		return "";
	}
	
}
