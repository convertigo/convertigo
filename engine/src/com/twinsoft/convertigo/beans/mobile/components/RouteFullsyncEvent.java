/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.util.EnumUtils;

public class RouteFullsyncEvent extends RouteEventComponent implements ITagsProperty {

	private static final long serialVersionUID = 5883194866752109784L;
	
	public RouteFullsyncEvent() {
		super();
		source = "";
	}

	@Override
	public RouteFullsyncEvent clone() throws CloneNotSupportedException {
		RouteFullsyncEvent cloned = (RouteFullsyncEvent)super.clone();
		return cloned;
	}
	
	/*
	 * The fullsync verb
	 */
	private String verb = FullSyncVerb.get.name();
	
	public String getVerb() {
		return verb;
	}

	public void setVerb(String verb) {
		this.verb = verb;
	}
	
	
	@Override
	protected String getRequestableSource() {
		String requestableSource = super.getRequestableSource();
		if (!requestableSource.isEmpty()) {
			int index = requestableSource.indexOf('.');
			if (index != -1) {
				try {
					requestableSource = requestableSource.substring(index+1);
				} catch (Exception e) {}
			}
			requestableSource = "fs://" + requestableSource + "." + getVerb();
		}
		return requestableSource;
	}

	@Override
	public String toString() {
		String label = getSourceName();
		return "on " + (label.equals("") ? "?" : label + "." + getVerb());
	}
	
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("verb")) {
			return EnumUtils.toNames(FullSyncVerb.class);
		}
		return new String[0];
	}
}
