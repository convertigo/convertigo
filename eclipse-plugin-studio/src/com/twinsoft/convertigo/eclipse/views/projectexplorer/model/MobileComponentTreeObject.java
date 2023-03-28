/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.texteditor.ITextEditor;

import com.twinsoft.convertigo.beans.mobile.components.MobileComponent;
import com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditor;
import com.twinsoft.convertigo.eclipse.editors.mobile.ComponentFileEditorInput;
import com.twinsoft.convertigo.engine.Engine;

public class MobileComponentTreeObject extends DatabaseObjectTreeObject implements IEditableTreeObject {
	
	final private Pattern pMarker = Pattern.compile("/\\*Begin_c8o_(.*?)\\*/\\s+(.*?)\\s*/\\*End_c8o_", Pattern.DOTALL);
	
	public MobileComponentTreeObject(Viewer viewer, MobileComponent object) {
		super(viewer, object);
	}

	public MobileComponentTreeObject(Viewer viewer, MobileComponent object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public MobileComponent getObject() {
		return (MobileComponent) super.getObject();
	}

	@Override
	public boolean acceptSymbols() {
		return false;
	}
	
	@Override
    protected List<PropertyDescriptor> getDynamicPropertyDescriptors() {
		List<PropertyDescriptor> l = super.getDynamicPropertyDescriptors();
		return l;
	}
	
	@Override
	public Object getPropertyValue(Object id) {
		if (id == null) return null;
		
		return super.getPropertyValue(id);
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
		super.setPropertyValue(id, value);
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		return super.testAttribute(target, name, value);
	}
	
	@Override
	public void launchEditor(String editorType) {
		DatabaseObjectTreeObject parent = getParentDatabaseObjectTreeObject();
		while (!(parent == null || parent instanceof MobileApplicationComponentTreeObject)) {
			parent = parent.getParentDatabaseObjectTreeObject();
		}
		
		if (parent != null) {
			ApplicationComponentEditor editor = ((MobileApplicationComponentTreeObject) parent).activeEditor();
			editor.highlightComponent(getObject(), true);
		}
		
	}

	public void closeAllEditors(boolean save) {
		MobileComponent mc = getObject();
		
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (activePage != null) {
			IEditorReference[] editorRefs = activePage.getEditorReferences();
			for (int i = 0; i < editorRefs.length; i++) {
				IEditorReference editorRef = (IEditorReference) editorRefs[i];
				try {
					IEditorInput editorInput = editorRef.getEditorInput();
					if (editorInput != null && editorInput instanceof ComponentFileEditorInput) {
						if (((ComponentFileEditorInput)editorInput).is(mc) ||
							((ComponentFileEditorInput)editorInput).isChildOf(mc)) {
								activePage.closeEditor(editorRef.getEditor(false),save);
						}
					}
				} catch(Exception e) {
					
				}
			}
		}
	}
	
	protected void addMarkers(IFile file, IEditorPart editorPart) {
		ITextEditor textEditor = editorPart.getAdapter(ITextEditor.class);
		String content = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput()).get();
		boolean first = true;
		Matcher m = pMarker.matcher(content);
		while (m.find()) {
			int offset = m.start(2);
			
			try {
				IMarker marker = file.createMarker(IMarker.TASK);
				int count = StringUtils.countMatches(content.substring(0, offset), '\n');
				marker.setAttribute(IMarker.LINE_NUMBER, count + 1);
				marker.setAttribute(IMarker.MESSAGE, m.group(1));
				if (first) {
					int len = m.end(2) - offset;
					textEditor.setHighlightRange(offset, len, true);
					first = false;
				}
			} catch (Exception e) {
				Engine.logStudio.debug("Failed to create marker", e);
			}
		}
	}
	
	protected void closeComponentFileEditor(final IFile file) {
		try {
			IWorkbenchPage activePage = PlatformUI
					.getWorkbench()
					.getActiveWorkbenchWindow()
					.getActivePage();
			
			IContainer parent = file.getParent();
			String extension = file.getFileExtension();
			
			for (IEditorReference editorReference : activePage.getEditorReferences()) {
				IEditorInput editorInput = editorReference.getEditorInput();
				if (editorInput instanceof ComponentFileEditorInput) {
					ComponentFileEditorInput cfei = (ComponentFileEditorInput) editorInput;
					IFile oldFile = cfei.getFile();
					if (parent.equals(oldFile.getParent()) && extension.equals(oldFile.getFileExtension())) {
						activePage.closeEditor(editorReference.getEditor(false), true);
						return;
					}
				}
			}
		} catch (Exception e) {
			
		}
	}
}
