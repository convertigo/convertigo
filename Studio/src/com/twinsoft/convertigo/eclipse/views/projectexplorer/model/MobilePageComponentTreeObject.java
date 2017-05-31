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
 * $URL: svn://devus.twinsoft.fr/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/views/projectexplorer/model/ReferenceTreeObject.java $
 * $Author: nathalieh $
 * $Revision: 39934 $
 * $Date: 2015-06-11 19:30:12 +0200 (jeu., 11 juin 2015) $
 */

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIStyle;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.engine.EngineException;

public class MobilePageComponentTreeObject extends MobileComponentTreeObject implements IEditableTreeObject {
	
	public MobilePageComponentTreeObject(Viewer viewer, PageComponent object) {
		super(viewer, object);
		isDefault = getObject().isRoot;
	}

	public MobilePageComponentTreeObject(Viewer viewer, PageComponent object, boolean inherited) {
		super(viewer, object, inherited);
		isDefault = getObject().isRoot;
	}

	@Override
	public PageComponent getObject() {
		return (PageComponent) super.getObject();
	}

	@Override
	public void setParent(TreeParent parent) {
		super.setParent(parent);
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		if (getObject().testAttribute(name, value)) {
			return true;
		}
		return super.testAttribute(target, name, value);
	}

	@Override
	public void launchEditor(String editorType) {
		ApplicationComponentEditor editor = ((MobileApplicationComponentTreeObject) getParentDatabaseObjectTreeObject()).activeEditor();
		editor.selectPage(getObject().getName());
	}
	
	@Override
	public boolean rename(String newName, boolean bDialog) {
		PageComponent page = getObject();
		String oldName = page.getName();
		boolean renamed = super.rename(newName, bDialog);
		if (renamed) {
			try {
				page.getProject().getMobileBuilder().pageRenamed(page, oldName);
			} catch (EngineException e) {
				ConvertigoPlugin.logException(e,
						"Error while writting source files for page '" + page.getName() + "'");
			}
		}
		return renamed;
	}

	@Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		super.treeObjectPropertyChanged(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject doto = (DatabaseObjectTreeObject)treeObject;
			DatabaseObject dbo = doto.getObject();
			try {
				if (dbo instanceof UIComponent) {
					UIComponent uic = (UIComponent)dbo;
					if (getObject().equals(uic.getPage())) {
						if (dbo instanceof UIStyle) {
							markStyleAsDirty();
						}
						else {
							markTemplateAsDirty();
						}
					}
				}
				else if (this.equals(dbo)) {
					markTemplateAsDirty();
				}
			} catch (Exception e) {}
		}
	}

	@Override
	public void hasBeenModified(boolean bModified) {
		super.hasBeenModified(bModified);
	}
		
	protected void markTemplateAsDirty() {
		PageComponent page = getObject();
		try {
			page.markTemplateAsDirty();
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e,
					"Error while writing the html file for page '" + page.getName() + "'");	}
	}

	protected void markStyleAsDirty() {
		PageComponent page = getObject();
		try {
			page.markStyleAsDirty();
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e,
					"Error while writing the style file for page '" + page.getName() + "'");	}
	}
}
