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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.engine.EngineException;

public class MobileApplicationComponentTreeObject extends MobileComponentTreeObject implements IEditableTreeObject {
	
	public MobileApplicationComponentTreeObject(Viewer viewer, ApplicationComponent object) {
		super(viewer, object);
	}

	public MobileApplicationComponentTreeObject(Viewer viewer, ApplicationComponent object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public void setParent(TreeParent parent) {
		super.setParent(parent);
	}

	@Override
	public ApplicationComponent getObject() {
		return (ApplicationComponent) super.getObject();
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		return super.testAttribute(target, name, value);
	}

	@Override
	public void hasBeenModified(boolean bModified) {
		super.hasBeenModified(bModified);
		if (bModified && !isInherited) {
			//markRouteAsDirty();
		}
	}
	
	protected void markRouteAsDirty() {
		ApplicationComponent ac = getObject();
		if (ac != null) {
			try {
				ac.doComputeRoute();
				ac.getProject().getMobileBuilder().routeChanged();
			} catch (EngineException e) {
				ConvertigoPlugin.logException(e,
						"Error while writing the component.ts file for app '" + ac.getName() + "'");	}
		}
	}

	protected void markTemplateAsDirty() {
		;// TODO
	}


	@Override
	public void launchEditor(String editorType) {
		ApplicationComponent application = (ApplicationComponent) getObject();
		synchronized (application) {
			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (activePage != null) {
				IEditorPart editorPart = null;
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i = 0; i < editorRefs.length; i++) {
					IEditorReference editorRef = (IEditorReference) editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if ((editorInput != null) && (editorInput instanceof ApplicationComponentEditorInput)) {
							if (((ApplicationComponentEditorInput) editorInput).is(application)) {
								editorPart = editorRef.getEditor(false);
								break;
							}
						}
					} catch(PartInitException e) {
					}
				}



				if (editorPart != null) {
					activePage.activate(editorPart);
				} else {
					try {
						editorPart = activePage.openEditor(new ApplicationComponentEditorInput(application),
								"com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditor");
					} catch (PartInitException e) {
						ConvertigoPlugin.logException(e,
								"Error while loading the page editor '"
										+ application.getName() + "'");
					}
				}
			}
		}
	}
	protected void markStyleAsDirty() {
		ApplicationComponent ac = getObject();
		try {
			ac.markStyleAsDirty();
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e,
					"Error while writing the app.scss for application '" + ac.getName() + "'");	}
	}
}
