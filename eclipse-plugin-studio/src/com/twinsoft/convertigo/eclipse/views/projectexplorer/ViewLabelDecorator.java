/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

public class ViewLabelDecorator extends LabelProvider implements ILightweightLabelDecorator {
	
	protected static final ImageDescriptor DEFAULT_IMAGE;
	
	static {
		DEFAULT_IMAGE = ImageDescriptor.createFromFile(ViewImageProvider.class, "bean_default.gif"); 
	}
	
	public ViewLabelDecorator() {
	}

	@Override
	public void decorate(Object element, IDecoration decoration) {
		// in a tree grouped by tags, each row is an occurrence of a tree object and shows its marks
		Object target = element instanceof TagTreeObject row && row.target != null ? row.target : element;
		if (target instanceof DatabaseObjectTreeObject databaseObjectTreeObject) {
			if (!databaseObjectTreeObject.isInherited && isDefault(databaseObjectTreeObject)) {
				decoration.addOverlay(DEFAULT_IMAGE);
			}
		}
	}

	/**
	 * @return whether the object is the default connector of its project or the default transaction of its
	 *         connector, as the object tells it now (the web Studio of the same engine can change it), else
	 *         whether its tree object was created for a default object, as the root page of an application
	 */
	static boolean isDefault(DatabaseObjectTreeObject treeObject) {
		Object object = treeObject.getObject();
		if (object instanceof Connector connector) {
			return connector.isDefault;
		}
		if (object instanceof Transaction transaction) {
			return transaction.isDefault;
		}
		return treeObject.isDefault;
	}
	
}
