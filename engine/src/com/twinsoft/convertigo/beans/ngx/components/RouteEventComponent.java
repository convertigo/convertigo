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

import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.beans.core.IEnableAble;

@DboCategoryInfo(
		getCategoryId = "RouteEventComponent",
		getCategoryName = "Event",
		getIconClassCSS = "convertigo-action-newRouteEventComponent"
	)
public class RouteEventComponent extends MobileComponent implements IRouteGenerator, IEnableAble {

	private static final long serialVersionUID = -5879576200562937068L;
	
	public RouteEventComponent() {
		super();
		
		this.priority = getNewOrderValue();
	}
	
	@Override
	public RouteEventComponent clone() throws CloneNotSupportedException {
		RouteEventComponent cloned = (RouteEventComponent) super.clone();
		return cloned;
	}
	
    @Override
    public Object getOrderedValue() {
    	return priority;
    }
	
	@Override
	public RouteComponent getParent() {
		return (RouteComponent) super.getParent();
	}	

	/*
	 * The event source
	 */
	protected String source = "*";

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
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
	 * The enabled state
	 */
	private boolean isEnabled = true;
	
	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	@Override
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
	
	/*
	 * The 'requestable' used by mobile client application
	 */
	protected String getRequestableSource() {
		return getSource();
	}
	
	public String getRequestableString() {
		String requestableSource = getRequestableSource();
		if (!requestableSource.isEmpty()) {
			requestableSource =  requestableSource + (marker.isEmpty() ? "" : "#" + marker);
		}
		return requestableSource;
	}

	protected String getSourceName() {
		String sourceName = getSource();
		if (!sourceName.isEmpty() && sourceName.startsWith(getProject().getName())) {
			try {
				sourceName = sourceName.substring(sourceName.lastIndexOf('.')+1);
			} catch (IndexOutOfBoundsException e) {}
		}
		return sourceName;
	}
	
	@Override
	public String toString() {
		String label = getSourceName();
		return "on " + (label.equals("") ? "?" : label);
	}
	
	@Override
	public String computeRoute() {
		if (isEnabled()) {
			return getRequestableString();
		}
		return "";
	}

	@Override
	public boolean testAttribute(String name, String value) {
		if (name.equals("isEnabled")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isEnabled()));
		}
		return super.testAttribute(name, value);
	}
	
	@Override
	public FolderType getFolderType() {
		return FolderType.EVENT;
	}
}
