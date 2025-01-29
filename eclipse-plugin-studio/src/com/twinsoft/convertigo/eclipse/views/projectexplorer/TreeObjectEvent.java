/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

import java.util.Set;

import com.twinsoft.convertigo.engine.events.BaseEvent;

public class TreeObjectEvent implements BaseEvent {
	public static final int UPDATE_NONE = 0;
	public static final int UPDATE_ALL = 1;
	public static final int UPDATE_LOCAL = 2;
	public static final int TYPE_ADDED = 3;
	public static final int TYPE_PROPERTY_CHANGED = 4;
	public static final int TYPE_REMOVED = 5;
	
	public int type = 0;
	public Object source = null;
	public String propertyName = null;
	public Object oldValue = null;
	public Object newValue = null;
	public int update = 0;
	public Set<Object> done = null;
	public Set<Object> reset = null;
	
	/** Creates new TreeObjectEvent */
	public TreeObjectEvent(Object source) {
		this(source, null, null, null);
	}

	public TreeObjectEvent(Object source, String propertyName, Object oldValue, Object newValue) {
		this(source, propertyName, oldValue, newValue, 0);
	}
	
	public TreeObjectEvent(Object source, String propertyName, Object oldValue, Object newValue, int update) {
		this(source, propertyName, oldValue, newValue, update, null, null);
	}
	
	public TreeObjectEvent(Object source, String propertyName, Object oldValue, Object newValue, int update, Set<Object> done) {
		this(source, propertyName, oldValue, newValue, update, done, null);
	}
	
	private TreeObjectEvent(Object source, String propertyName, Object oldValue, Object newValue, int update, Set<Object> done, Set<Object> reset) {
		this.source = source;
		this.propertyName = propertyName;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.update = update;
		this.done = done;
		this.reset = reset;
	}

	public Object getSource() {
		return source;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + 
				"@"+ hashCode()+"[source=" + source + 
				", propertyName=" + propertyName +
				", oldValue=" + oldValue +
				", newValue=" + newValue +
				", update=" + update +
				", done="+ done +"]";
	}
	
	
}
