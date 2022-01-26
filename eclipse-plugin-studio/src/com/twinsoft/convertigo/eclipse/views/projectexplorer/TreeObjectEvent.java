/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.util.EventObject;
import java.util.Set;

public class TreeObjectEvent extends EventObject {

	private static final long serialVersionUID = 8653860381743808101L;

	public static final int UPDATE_NONE = 0;
	public static final int UPDATE_ALL = 1;
	public static final int UPDATE_LOCAL = 2;
	
	public String propertyName = null;
	public Object oldValue = null;
	public Object newValue = null;
	public int update = 0;
	public Set<Object> done = null;
	
	/** Creates new TreeObjectEvent */
	public TreeObjectEvent(Object source) {
		this(source, null, null, null);
	}

	public TreeObjectEvent(Object source, String propertyName, Object oldValue, Object newValue) {
		this(source, propertyName, oldValue, newValue, 0);
	}
	
	public TreeObjectEvent(Object source, String propertyName, Object oldValue, Object newValue, int update) {
		this(source, propertyName, oldValue, newValue, update, null);
	}
	
	public TreeObjectEvent(Object source, String propertyName, Object oldValue, Object newValue, int update, Set<Object> done) {
		super(source);
		this.propertyName = propertyName;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.update = update;
		this.done = done;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + 
				"[source=" + source + 
				", propertyName=" + propertyName +
				", oldValue=" + oldValue +
				", newValue=" + newValue +
				", done="+ done +"]";
	}
	
	
}
