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

package com.twinsoft.convertigo.eclipse.views.picker;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewImageProvider;

public class DatabaseObjectPickerLabelProvider extends LabelProvider {

	@Override
	public Image getImage(Object element) {
		if (element instanceof DatabaseObjectPickerNode node) {
			Object object = node.getObject();
			if (object instanceof DatabaseObject) {
				return ViewImageProvider.getImageFromCache(null, object);
			}
			String iconPath = node.getIconPath();
			if (iconPath != null && !iconPath.isEmpty()) {
				try {
					if (iconPath.startsWith("icons/")) {
						return ConvertigoPlugin.getDefault().getStudioIcon(iconPath);
					}
					return ViewImageProvider.getImageFromCache(iconPath);
				} catch (Exception e) {
					return null;
				}
			}
			try {
				return ConvertigoPlugin.getDefault().getStudioIcon(DatabaseObjectPickerNode.FOLDER_ICON);
			} catch (Exception e) {
				return null;
			}
		}
		return super.getImage(element);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof DatabaseObjectPickerNode node) {
			return node.getLabel();
		}
		return super.getText(element);
	}
}
