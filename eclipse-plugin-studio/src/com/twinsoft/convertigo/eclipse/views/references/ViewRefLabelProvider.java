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

package com.twinsoft.convertigo.eclipse.views.references;

import java.beans.BeanInfo;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewImageProvider;
import com.twinsoft.convertigo.eclipse.views.references.model.AbstractNode;
import com.twinsoft.convertigo.eclipse.views.references.model.DboNode;
import com.twinsoft.convertigo.eclipse.views.references.model.InformationNode;
import com.twinsoft.convertigo.eclipse.views.references.model.IsUsedByNode;
import com.twinsoft.convertigo.eclipse.views.references.model.RequiresNode;

class ViewRefLabelProvider implements IStyledLabelProvider, ILabelProvider {
	private boolean isTarget = false;

	public ViewRefLabelProvider() {
	}

	public ViewRefLabelProvider(boolean isTarget) {
		this.isTarget = isTarget;
	}

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

		if (element instanceof DboNode node) {
			if (node.getSource() == null && isTarget) {
				return null;
			}
			DatabaseObject databaseObject = isTarget ? node.getTarget() : node.getSource();

			/**
			 * treat case Entry handlers, Exit handlers etc... where dbo == null
			 * could be cleaner if having its own icon etc...
			 */
			if (databaseObject == null) {
				if (node.getName().contains("entry"))
					iconName = "/com/twinsoft/convertigo/beans/core/images/handler_entry_16x16.png";
				else 
					if (node.getName().contains("exit"))
						iconName = "/com/twinsoft/convertigo/beans/core/images/handler_exit_16x16.png";
					else
						iconName = null;
			} else {
				iconName = MySimpleBeanInfo.getIconName(databaseObject, BeanInfo.ICON_COLOR_16x16);
				element = databaseObject;
			}
		}
		else if (isTarget) {
			return null;
		} else {
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
		if (element instanceof DboNode node && node.getSource() != null) {
			return isTarget ? node.getName() : node.getSource().getName();
		}
		return isTarget ? "" : ((AbstractNode) element).getName();
	}

	@Override
	public StyledString getStyledText(Object element) {
		return new StyledString(getText(element));
	}
}

