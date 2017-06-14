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

public abstract class UIControlCallAction extends UIControlAction {
	
	private static final long serialVersionUID = 952743867765987510L;

	public UIControlCallAction() {
		super();
	}

	@Override
	public UIControlCallAction clone() throws CloneNotSupportedException {
		UIControlCallAction cloned = (UIControlCallAction)super.clone();
		return cloned;
	}
	
	/*
	 * The requestable target
	 */
	protected String target = "";

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}
	
	/*
	 * The marker for mobile client application
	 */
	private String marker = "";
	
	public String getMarker() {
		return marker;
	}

	public void setMarker(String marker) {
		this.marker = marker;
	}
	
	protected String getRequestableTarget() {
		return getTarget();
	}
	
	protected String getRequestableString() {
		String requestableTarget = getRequestableTarget();
		if (!requestableTarget.isEmpty()) {
			requestableTarget = requestableTarget + (marker.isEmpty() ? "" : "#" + marker);
		}
		return requestableTarget;
	}
	
	protected String getTargetName() {
		String targetName = getTarget();
		if (!targetName.isEmpty()) {
			int index = targetName.indexOf(".");
			targetName = index != -1 ? targetName.substring(index+1):targetName;
		}
		return targetName;
	}
	
	@Override
	public String toString() {
		String label = getTargetName();
		return "call " + (label.isEmpty() ? "?":label);
	}
	
	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			StringBuilder parameters = new StringBuilder();
			
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof UIControlVariable) {
					String parameter = component.computeTemplate();
					if (!parameter.isEmpty()) {
						parameters.append(parameters.length()> 0 ? ", ":"").append(parameter);
					}
				}
			}
			
			String requestableString = getRequestableString();
			if (!requestableString.isEmpty()) {
				if (parameters.length()> 0) {
					return "call('"+ requestableString + "', {"+ parameters +"})";
				} else {
					return "call('"+ requestableString + "')";
				}
			}
		}
		return "";
	}	
	
}
