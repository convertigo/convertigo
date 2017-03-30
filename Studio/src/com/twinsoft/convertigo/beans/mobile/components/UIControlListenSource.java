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

public abstract class UIControlListenSource extends UIControlSource {

	private static final long serialVersionUID = 4176597213482241515L;

	public UIControlListenSource() {
		super();
	}

	@Override
	public UIControlListenSource clone() throws CloneNotSupportedException {
		UIControlListenSource cloned = (UIControlListenSource) super.clone();
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
	
	/*
	 * The 'requestable' used 
	 */
	protected String getRequestableTarget() {
		String requestableTarget = getTarget();
		int index = requestableTarget.indexOf('.');
		if (index != -1) {
			try {
				requestableTarget = requestableTarget.substring(index+1);
			} catch (Exception e) {}
		}
		return requestableTarget;
	}
	
	public String getRequestableString() {
		String requestableTarget = getRequestableTarget();
		if (!requestableTarget.isEmpty()) {
			requestableTarget = (requestableTarget.startsWith("fs://")? "":".") 
									+ requestableTarget + (marker.isEmpty() ? "" : "#" + marker);
		}
		return requestableTarget;
	}
	
	protected String getTargetName() {
		String targetName = getTarget();
		if (!targetName.isEmpty() && targetName.startsWith(getProject().getName())) {
			try {
				targetName = targetName.substring(targetName.lastIndexOf('.')+1);
			} catch (IndexOutOfBoundsException e) {}
		}
		return targetName;
	}

	@Override
	public String toString() {
		String label = getTargetName();
		return (label.isEmpty() ? "?":label);
	}
	
	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			return "'"+ getRequestableString() + "'";// TODO: add parameters
		}
		return "";
	}	
	
}
