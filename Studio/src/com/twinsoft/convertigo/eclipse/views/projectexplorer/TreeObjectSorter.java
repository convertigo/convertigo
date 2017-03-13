/*
 * Copyright (c) 2001-2011 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ObjectsFolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableColumnTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableRowTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.engine.EngineException;

class TreeObjectSorter extends ViewerSorter {
	@Override
	public int category(Object element) {
		if (element instanceof UnloadedProjectTreeObject) return 10;
		if (element instanceof DatabaseObjectTreeObject) return 10;
		if (element instanceof ObjectsFolderTreeObject) {
			if (((ObjectsFolderTreeObject)element).folderType == ObjectsFolderTreeObject.FOLDER_TYPE_ACTIONS) {
				return 21;
			}
			return 20;
		}
		return 0;
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if ((e1 instanceof DatabaseObjectTreeObject) && (e2 instanceof DatabaseObjectTreeObject)) {
			DatabaseObject d1 = ((DatabaseObjectTreeObject) e1).getObject();
			DatabaseObject d2 = ((DatabaseObjectTreeObject) e2).getObject();
			
			DatabaseObjectTreeObject p1 = ((DatabaseObjectTreeObject)e1).getOwnerDatabaseObjectTreeObject();
			DatabaseObjectTreeObject p2 = ((DatabaseObjectTreeObject)e2).getOwnerDatabaseObjectTreeObject();
			if (p1.equals(p2)) {
				DatabaseObject p = (DatabaseObject)p1.getObject();
				try {
					Object o1 = p.getOrder(d1);
					Object o2 = p.getOrder(d2);
					if ((o1 instanceof Long) && (o2 instanceof Long)) {
						long l1 = ((Long)o1).longValue();
						long l2 = ((Long)o2).longValue();
						int dp = (int) (l1 - l2);
						if (dp != 0) return dp;
					}

				} catch (EngineException e) {
					ConvertigoPlugin.logException(e, "Error while sorting objects \""+ d1.getQName()+"\" and \""+ d2.getQName() +"\" in Tree.");
				}
			}
		}
		else if ((e1 instanceof PropertyTableRowTreeObject) && (e2 instanceof PropertyTableRowTreeObject)) {
			PropertyTableRowTreeObject r1 = (PropertyTableRowTreeObject)e1;
			PropertyTableRowTreeObject r2 = (PropertyTableRowTreeObject)e2;
			
			int i1 = r1.getParent().indexOf(r1);
			int i2 = r2.getParent().indexOf(r2);
			return i1 - i2;
		}
		else if ((e1 instanceof PropertyTableColumnTreeObject) && (e2 instanceof PropertyTableColumnTreeObject)) {
			PropertyTableColumnTreeObject c1 = (PropertyTableColumnTreeObject)e1;
			PropertyTableColumnTreeObject c2 = (PropertyTableColumnTreeObject)e2;
			
			int i1 = c1.getParent().indexOf(c1);
			int i2 = c2.getParent().indexOf(c2);
			return i1 - i2;
		}
		
		return super.compare(viewer, e1, e2);
	}
}
