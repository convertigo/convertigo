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

public class RouteFullsyncEvent extends RouteEventComponent implements ITagsProperty {

	private static final long serialVersionUID = 5883194866752109784L;

	public enum Verb {
		all,
		create,
		get,
		delete,
		delete_attachment,
		destroy,
		post,
		put_attachment,
		replicate_pull,
		replicate_push,
		reset,
		sync,
		view,
		
	}
	
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
	private String verb = Verb.get.name();
	
	public String getVerb() {
		return verb;
	}

	public void setVerb(String verb) {
		this.verb = verb;
	}
	
	
	@Override
	protected String getRequestableSource() {
		String fullsyncDb = getSource();
		int index = fullsyncDb.indexOf('.');
		if (index != -1) {
			try {
				fullsyncDb = fullsyncDb.substring(index+1);
			} catch (Exception e) {}
		}
		
		return "fs://" + fullsyncDb + "." + getVerb();
	}

	@Override
	public String toString() {
		String label = getSource();
		return "on_" + (label.equals("") ? "?" : label + "." + getVerb());
	}
	
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("verb")) {
			return EnumUtils.toNames(Verb.class);
		}
		return new String[0];
	}
}
