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

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.util.EnumUtils;

public class UIControlCallFullSync extends UIControlCall implements ITagsProperty {

	private static final long serialVersionUID = -5796622953379062045L;

	public UIControlCallFullSync() {
		super();
	}

	@Override
	public UIControlCallFullSync clone() throws CloneNotSupportedException {
		UIControlCallFullSync cloned = (UIControlCallFullSync) super.clone();
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
	protected String getRequestableTarget() {
		String requestableTarget = super.getRequestableTarget();
		if (!requestableTarget.isEmpty()) {
			requestableTarget = "fs://" + requestableTarget + "." + getVerb();
		}
		return requestableTarget;
	}

	@Override
	public String toString() {
		String label = getTargetName();
		return "call " + (label.equals("") ? "?" : label + "." + getVerb());
	}
	
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("verb")) {
			return EnumUtils.toNames(FullSyncVerb.class);
		}
		return new String[0];
	}
	
}
