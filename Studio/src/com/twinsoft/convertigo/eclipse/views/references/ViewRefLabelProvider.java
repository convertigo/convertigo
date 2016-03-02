package com.twinsoft.convertigo.eclipse.views.references;
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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/views/projectexplorer/ViewLabelProvider.java $
 * $Author: nathalieh $
 * $Revision: 29152 $
 * $Date: 2011-11-30 18:38:10 +0100 (Wed, 30 Nov 2011) $
 */

import java.beans.BeanInfo;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewImageProvider;
import com.twinsoft.convertigo.eclipse.views.references.model.AbstractNode;
import com.twinsoft.convertigo.eclipse.views.references.model.AbstractNodeWithDatabaseObjectReference;
import com.twinsoft.convertigo.eclipse.views.references.model.InformationNode;
import com.twinsoft.convertigo.eclipse.views.references.model.IsUsedByNode;
import com.twinsoft.convertigo.eclipse.views.references.model.RequiresNode;


public class ViewRefLabelProvider implements ILabelProvider {

	public void addListener(ILabelProviderListener listener) {
	}

	public void dispose() {
	}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
	}

	public Image getImage(Object element) {

		String iconName = null;
		Image image = null;

		if (element instanceof AbstractNodeWithDatabaseObjectReference) {
			AbstractNodeWithDatabaseObjectReference node = (AbstractNodeWithDatabaseObjectReference) element;
			DatabaseObject databaseObject = node.getRefDatabaseObject();

			/**
			 * treat case Entry handlers, Exit handlers etc... where dbo == null
			 * could be cleaner if having its own icon etc...
			 */
			if (databaseObject == null) {
				if (node.getName().contains("entry")) {
					iconName = "/com/twinsoft/convertigo/beans/statements/images/handler_entry_16x16.png";
				} else { 
					if (node.getName().contains("exit")) {
						iconName = "/com/twinsoft/convertigo/beans/statements/images/handler_exit_16x16.png";
					} else {
						iconName = null;
					}
				}
			} else {
				iconName = MySimpleBeanInfo.getIconName(databaseObject, BeanInfo.ICON_COLOR_16x16);
			}
		}
		else {
			if (element instanceof InformationNode) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/references/images/information_color_16x16.png";
			} else if (element instanceof IsUsedByNode) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/references/images/isusedby_16x16.png";
			} else if (element instanceof RequiresNode) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/references/images/requires_16x16.png";
			} else {
				return null;
			}
		}
		
		image = ViewImageProvider.getImageFromCache(iconName, element);
		return image;
	}
	
	public String getText(Object element) {
		return ((AbstractNode) element).getName();
	}
}

