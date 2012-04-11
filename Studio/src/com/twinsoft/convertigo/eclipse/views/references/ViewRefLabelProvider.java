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

import java.io.InputStream;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.references.model.AbstractNode;
import com.twinsoft.convertigo.eclipse.views.references.model.EntryHandlerFolder;
import com.twinsoft.convertigo.eclipse.views.references.model.ExitHandlerFolder;
import com.twinsoft.convertigo.eclipse.views.references.model.Folder;
import com.twinsoft.convertigo.eclipse.views.references.model.ProjectNode;
import com.twinsoft.convertigo.eclipse.views.references.model.ScreenClassNode;
import com.twinsoft.convertigo.eclipse.views.references.model.SequenceNode;
import com.twinsoft.convertigo.eclipse.views.references.model.TransactionNode;


class ViewRefLabelProvider implements ILabelProvider {

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public Image getImage(Object element) {

		String iconName = null;
		Image image = null;

		if (element instanceof TransactionNode) {
			iconName = "/com/twinsoft/convertigo/eclipse/views/references/images/transaction_color_16x16.gif";
		} else if (element instanceof ScreenClassNode) {
			iconName = "/com/twinsoft/convertigo/eclipse/views/references/images/screenclass_color_16x16.gif";
		} else if (element instanceof Folder) {
			iconName = "/com/twinsoft/convertigo/eclipse/views/references/images/variable_color_16x16.gif";
		} else if (element instanceof ProjectNode) {
			iconName = "/com/twinsoft/convertigo/eclipse/views/references/images/project_color_16x16.gif";
		} else if (element instanceof EntryHandlerFolder) {
			iconName = "/com/twinsoft/convertigo/eclipse/views/references/images/handlers_sc_exit.gif";
		} else if (element instanceof ExitHandlerFolder) {
			iconName = "/com/twinsoft/convertigo/eclipse/views/references/images/handlers_sc_entry.gif";
		} else if (element instanceof SequenceNode) {
			iconName = "/com/twinsoft/convertigo/eclipse/views/references/images/sequence_color_16x16.gif";
		}
		
		image = getImageFromCache(iconName, (Object) element);
		return image;
	    
	}
	
	public static Image getImageFromCache(String iconName, Object object) {
		Image image = null;
		Device device = Display.getCurrent();
		InputStream inputStream = ConvertigoPlugin.class.getResourceAsStream(iconName);
		image = new Image(device, inputStream);
		
		ImageData imageData = image.getImageData();
		image = new Image(device, imageData);
		
		return image;
	}
	
	@Override
	public String getText(Object element) {
		return ((AbstractNode) element).getName();
	}
}

