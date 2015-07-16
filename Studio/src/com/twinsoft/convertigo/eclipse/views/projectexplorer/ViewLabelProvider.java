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

import java.beans.BeanInfo;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentFunctionTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentViewTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.FolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.HandlersDeclarationTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableColumnTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableRowTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ResourceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TemplateTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TraceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.VariableTreeObject2;
import com.twinsoft.convertigo.engine.enums.Accessibility;

class ViewLabelProvider extends LabelProvider implements IFontProvider, IColorProvider {

	private Font fontSystem;
	private Font fontModifiedDatabaseObject;
	private Font fontDetectedDatabaseObject;

	private Color colorUnloadedProject;
	private Color colorDisabledDatabaseObject;
	private Color colorInheritedDatabaseObject;
	private Color colorUnreachableDatabaseObject;
	private Color colorDetectedDatabaseObject;
	
	public ViewLabelProvider() {
		Device device = Display.getCurrent();

		fontSystem = device.getSystemFont();

		FontData fontData = fontSystem.getFontData()[0];
		
		fontDetectedDatabaseObject = new Font(device, fontData);
		
		FontData fontDataModified = fontSystem.getFontData()[0];
		fontDataModified.setStyle(SWT.BOLD);
		fontModifiedDatabaseObject = new Font(device, fontDataModified);

		colorUnloadedProject = new Color(device, 12, 116, 176);
		colorDisabledDatabaseObject = new Color(device, 255, 0, 0);
		colorInheritedDatabaseObject = new Color(device, 150, 150, 150);
		colorUnreachableDatabaseObject = new Color(device, 255, 140, 0);
		colorDetectedDatabaseObject = new Color(device, 192, 219, 207);
	}
	
	@Override
	public void dispose() {
		fontModifiedDatabaseObject.dispose();
		fontDetectedDatabaseObject.dispose();

		colorUnloadedProject.dispose();
		colorDisabledDatabaseObject.dispose();
		colorInheritedDatabaseObject.dispose();
		colorDetectedDatabaseObject.dispose();
		colorUnreachableDatabaseObject.dispose();

		super.dispose();
	}

	@Override
	public String getText(Object obj) {
		if (obj instanceof DatabaseObjectTreeObject) {
			DatabaseObject dbo = ((DatabaseObjectTreeObject) obj).getObject();
			if (dbo.isSymbolError() || (dbo instanceof Project && ((Project) dbo).undefinedGlobalSymbols)) {
				return obj.toString() + " (! undefined symbol !)"; 
			}
			String osname = System.getProperty ( "os.name" );
			String version = System.getProperty ( "os.version" );
			
			boolean notShownSpecialChar = osname.toLowerCase().startsWith("windows") && Double.parseDouble(version) < 6.2;
			boolean isMac = osname.toLowerCase().startsWith("mac");
			
			if (dbo instanceof RequestableObject && !notShownSpecialChar ) {
				return ( ((RequestableObject) dbo).getAccessibility() == Accessibility.Private ? "🔒 " : ( 
						((RequestableObject) dbo).getAccessibility() == Accessibility.Hidden ? "👓 " : (isMac ? "🚪 " : " 🚪  " ) ) ) + obj.toString();
			}
		}
		return obj.toString();
	}
	
	@Override
	public Image getImage(Object obj) {
		try {
			String iconName = "/com/twinsoft/convertigo/beans/core/images/default_color_16x16.png";
			Image image = null;
			
			if (obj instanceof UnloadedProjectTreeObject) {
				String imageKey = org.eclipse.ui.ide.IDE.SharedImages.IMG_OBJ_PROJECT_CLOSED;
				image = PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
			}
			else if (obj instanceof ResourceTreeObject) {
				ILabelProvider workbenchLabelProvider = WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider();
				image = workbenchLabelProvider.getImage(obj);
			}
			else if (obj instanceof FolderTreeObject) {
				String imageKey = ISharedImages.IMG_OBJ_FOLDER;
				image = PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
				FolderTreeObject t_folder = (FolderTreeObject)obj;
				if(t_folder.getParent() instanceof DatabaseObjectTreeObject){
					DatabaseObjectTreeObject parent = (DatabaseObjectTreeObject) t_folder.getParent();
					if(!parent.isEnabled())
						image = ViewImageProvider.getImageFromCache(imageKey+"_disable", image, obj);
					else if(parent.hasAncestorDisabled())
						image = ViewImageProvider.getImageFromCache(imageKey+"_unreachable", image, obj);
				}	
			}
			else if (obj instanceof TemplateTreeObject) {
				String imageKey = ISharedImages.IMG_OBJ_FILE;
				image = PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
			}
			else if (obj instanceof HandlersDeclarationTreeObject) {
				String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
				image = PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
			}
			else if (obj instanceof TraceTreeObject) {
				String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
				image = PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
			}
			else if (obj instanceof VariableTreeObject2) {
				VariableTreeObject2 variableTreeObject = (VariableTreeObject2) obj;
				
				iconName = MySimpleBeanInfo.getIconName(variableTreeObject.databaseObjectBeanInfo, BeanInfo.ICON_COLOR_16x16);
				if (iconName == null) {
					iconName = "/com/twinsoft/convertigo/beans/core/images/variable_color_16x16.png";
				}
				
				image = ViewImageProvider.getImageFromCache(iconName, variableTreeObject);
			}
			else if (obj instanceof DatabaseObjectTreeObject) {
				DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject) obj;
				
				iconName = MySimpleBeanInfo.getIconName(databaseObjectTreeObject.getObject(), BeanInfo.ICON_COLOR_16x16);
				
				image = ViewImageProvider.getImageFromCache(iconName, databaseObjectTreeObject);
			}
			else if (obj instanceof PropertyTableTreeObject) {
				iconName = "/com/twinsoft/convertigo/beans/core/images/bean_property_16x16.png";
				image = ViewImageProvider.getImageFromCache(iconName, (PropertyTableTreeObject)obj);
			}
			else if (obj instanceof PropertyTableRowTreeObject) {
				iconName = "/com/twinsoft/convertigo/beans/core/images/bean_property_16x16.png";
				image = ViewImageProvider.getImageFromCache(iconName, (PropertyTableRowTreeObject)obj);
			}
			else if (obj instanceof PropertyTableColumnTreeObject) {
				iconName = "/com/twinsoft/convertigo/beans/core/images/bean_property_16x16.png";
				image = ViewImageProvider.getImageFromCache(iconName, (PropertyTableColumnTreeObject)obj);
			}
			else if (obj instanceof DesignDocumentViewTreeObject) {
				iconName = "/com/twinsoft/convertigo/beans/couchdb/images/view_color_16x16.png";
				image = ViewImageProvider.getImageFromCache(iconName, (DesignDocumentViewTreeObject)obj);
			}
			else if (obj instanceof DesignDocumentFunctionTreeObject) {
				iconName = "/com/twinsoft/convertigo/beans/couchdb/images/function_color_16x16.png";
				image = ViewImageProvider.getImageFromCache(iconName, (DesignDocumentFunctionTreeObject)obj);
			}
			else {
				throw new IllegalArgumentException("Unexpected tree item object");
			}

			return image;
		}
		catch(Exception e) {
			String message = "Error while getting tree view item image for object [" + obj.getClass().getName() + "] " + obj.toString();
			ConvertigoPlugin.logException(e, message);
			String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
			if (obj instanceof TreeParent) {
			   imageKey = ISharedImages.IMG_OBJ_FOLDER;
			}
			return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
		}
	}

	public Font getFont(Object element) {
		if (element instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject) element;
			DatabaseObject databaseObject = databaseObjectTreeObject.getObject();
			if (databaseObject.hasChanged) return fontModifiedDatabaseObject;
			if (databaseObjectTreeObject.isDetectedObject) return fontDetectedDatabaseObject;
		}
		return fontSystem;
	}

	public Color getForeground(Object element) {
		if (element instanceof UnloadedProjectTreeObject) {
			return colorUnloadedProject;
		}

		if (element instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject) element;
			
			if (!databaseObjectTreeObject.isEnabled()) return colorDisabledDatabaseObject;
			if (databaseObjectTreeObject.isInherited) return colorInheritedDatabaseObject;
			if (databaseObjectTreeObject.hasAncestorDisabled()) return colorUnreachableDatabaseObject;
		}
		if (element instanceof PropertyTableTreeObject) {
			PropertyTableTreeObject table = (PropertyTableTreeObject)element;
			if (table != null) {
				DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject)table.getTreeObjectOwner();
				if (databaseObjectTreeObject.isInherited) return colorInheritedDatabaseObject;
			}
		}
		if (element instanceof PropertyTableRowTreeObject) {
			PropertyTableTreeObject table = ((PropertyTableRowTreeObject)element).getParentTable();
			if (table != null) {
				DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject)table.getTreeObjectOwner();
				if (databaseObjectTreeObject.isInherited) return colorInheritedDatabaseObject;
			}
		}
		if (element instanceof PropertyTableColumnTreeObject) {
			PropertyTableTreeObject table = ((PropertyTableColumnTreeObject)element).getParentTable();
			if (table != null) {
				DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject)table.getTreeObjectOwner();
				if (databaseObjectTreeObject.isInherited) return colorInheritedDatabaseObject;
			}
		}
		if (element instanceof FolderTreeObject) {
			FolderTreeObject t_folder = (FolderTreeObject)element;
			return getForeground(t_folder.getParent());
		}
		return null;
	}

	public Color getBackground(Object element) {
		if (element instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject) element;
			if (databaseObjectTreeObject.isDetectedObject) return colorDetectedDatabaseObject;
		}
		return null;
	}
}
