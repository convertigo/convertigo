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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.twinsoft.convertigo.beans.core.MobileComponent;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.html.MobileComponentEditorInput;

public class MobileComponentTreeObject extends DatabaseObjectTreeObject implements IEditableTreeObject {

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
	public boolean testAttribute(Object target, String name, String value) {
		return super.testAttribute(target, name, value);
	}

	@Override
	public void launchEditor(String editorType) {
		final MobileComponent mc = (MobileComponent)getObject();
		String filePath = "/_private/" + mc.getQName() + " " + mc.getName()+".html";
		try {
			// Refresh project resource
			String projectName = mc.getProject().getName();
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			
			// Create temporary file if needed
			IFile file = project.getFile(filePath);
			if (!file.exists()) {
				try {
					InputStream is = new ByteArrayInputStream(mc.getHtmlTemplate().getBytes("ISO-8859-1"));
					file.create(is, true, null);
					file.setCharset("ISO-8859-1", null);
				} catch (UnsupportedEncodingException e) {
				}
			}
			
			// Open file in editor
			if (file.exists()) {
				IEditorInput input = new MobileComponentEditorInput(file,mc);
				if (input != null) {
					//IPath path = file.getProjectRelativePath();
					//String fileName = path.removeFirstSegments(path.segmentCount() - 1).toString();
					//String editorId = getEditorId(fileName);
					String editorId = "org.eclipse.wst.html.core.htmlsource.source";
					
					IWorkbenchPage activePage = PlatformUI
							.getWorkbench()
							.getActiveWorkbenchWindow()
							.getActivePage();
	
					IEditorPart editorPart = activePage.openEditor(input, editorId);
					editorPart.addPropertyListener(new IPropertyListener() {
						boolean isFirstChange = false;
						
						@Override
						public void propertyChanged(Object source, int propId) {
							if (source instanceof ITextEditor) { //org.eclipse.wst.sse.ui.StructuredTextEditor
								if (propId == IEditorPart.PROP_DIRTY) {
									if (!isFirstChange) {
										isFirstChange = true;
										return;
									}
									
									isFirstChange = false;
									ITextEditor editor = (ITextEditor)source;
									IDocumentProvider dp = editor.getDocumentProvider();
									IDocument doc = dp.getDocument(editor.getEditorInput());
									String htmlTemplate = doc.get();
									MobileComponentTreeObject.this.setPropertyValue("htmlTemplate", htmlTemplate);
								}
							}
						}
					});
				}			
			}
		} catch (CoreException e) {
			ConvertigoPlugin.logException(e, "Unable to open file '" + filePath + "'!");
		}
	}

	@SuppressWarnings("unused")
	private String getEditorId(String fileName) { 
		IWorkbench workbench = PlatformUI
				  					.getWorkbench() 
				  					.getActiveWorkbenchWindow()
				  					.getWorkbench(); 
		  
		IEditorRegistry editorRegistry = workbench.getEditorRegistry();
		  
		IEditorDescriptor descriptor = editorRegistry.getDefaultEditor(fileName, getContentType(fileName)); 
		 
		if (descriptor == null && editorRegistry.isSystemInPlaceEditorAvailable(fileName)) { 
			descriptor = editorRegistry.findEditor(IEditorRegistry.SYSTEM_INPLACE_EDITOR_ID); 
		} 
		 
		if (descriptor == null && editorRegistry.isSystemExternalEditorAvailable(fileName)) { 
		   descriptor = editorRegistry.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID); 
		} 
		 
		return (descriptor == null) ? "" : descriptor.getId();
	} 
	
	private IContentType getContentType(String fileName) { 
		if (fileName == null) { 
		   return null; 
		} 
		return Platform.getContentTypeManager().findContentTypeFor(fileName); 
	}	
}
