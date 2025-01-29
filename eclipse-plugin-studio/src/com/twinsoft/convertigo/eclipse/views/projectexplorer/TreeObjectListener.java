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

import com.twinsoft.convertigo.engine.events.BaseEventListener;

public interface TreeObjectListener extends BaseEventListener {
	default public void onEvent(TreeObjectEvent treeObjectEvent) {
		switch (treeObjectEvent.type) {
		case TreeObjectEvent.TYPE_ADDED:
			treeObjectAdded(treeObjectEvent);
			break;
		case TreeObjectEvent.TYPE_PROPERTY_CHANGED:
			treeObjectPropertyChanged(treeObjectEvent);
			break;
		case TreeObjectEvent.TYPE_REMOVED:
			treeObjectRemoved(treeObjectEvent);
			break;
		}
	}
	
	
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent);
	
	public void treeObjectAdded(TreeObjectEvent treeObjectEvent);
	
	public void treeObjectRemoved(TreeObjectEvent treeObjectEvent);
	
}
