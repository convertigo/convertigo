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

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import java.awt.EventQueue;
import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.AbstractConnectorComposite;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditorInput;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditorPart;
import com.twinsoft.convertigo.eclipse.editors.connector.JavelinConnectorComposite;
import com.twinsoft.convertigo.eclipse.editors.text.TraceFileEditorInput;
import com.twinsoft.convertigo.eclipse.trace.TracePlayerThread;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;

public class TraceTreeObject extends TreeObject {

	public boolean hasChanged = false;
	
	public TraceTreeObject(Viewer viewer, File object) {
		super(viewer, object);
	}
	
	@Override
	public File getObject(){
		return (File) super.getObject();
	}
	
	@Override
	public String getName() {
		return getObject().getName();
	}
	
	public void rename(String newName) {
		File file = getObject();
		if (file.exists()) {
			String path = file.getAbsolutePath();
			path = path.substring(0,path.lastIndexOf("\\")+1);
			path += newName + ((newName.indexOf(".etr") == -1)? ".etr":"");
			File dest = new File(path);
			if (file.renameTo(dest)) {
				setObject(dest);
				hasChanged = true;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}

	public void launchEditor(String editorType) {
		// Retrieve the project name
		String projectName = ConvertigoPlugin.projectManager.currentProjectName;
		try {
			// Refresh project resource
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);

			// Open editor
			openTextEditor(project);
			
		} catch (CoreException e) {
			ConvertigoPlugin.logException(e, "Unable to open project named '" + projectName + "'!");
		}
	}
	
	public void openTextEditor(IProject project) {
		
		ConnectorTreeObject connectorTreeObject = (ConnectorTreeObject)this.parent.parent;
		
		IFile file = project.getFile("Traces/"+ connectorTreeObject.getName() + "/" + this.getName());
		
		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			try {
				activePage.openEditor(new TraceFileEditorInput(connectorTreeObject.getObject(), file),"org.eclipse.ui.DefaultTextEditor");
			} catch(PartInitException e) {
				ConvertigoPlugin.logException(e, "Error while loading the text editor.");
			}
		}
	}
	
	public void play() {
		play(true);
	}
	
	public void play(boolean bReplace) {
		ProjectExplorerView explorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();		
		if (explorerView == null)
			return;
		
		JavelinConnector javelinConnector = (JavelinConnector)getParent().getParent().getObject();
		
		// Launch TracePlayer
		if (explorerView.tracePlayerThread != null) {
			if (!bReplace) return;
			explorerView.tracePlayerThread.stopPlayer();
		}
		
		File file = (File) getObject();
		String traceFile = file.toString();
		explorerView.tracePlayerThread = new TracePlayerThread("IbmTracePlayerThread", javelinConnector.getName(), traceFile);
		
		// Connect javelin
		IEditorPart wpart = getConnectorEditor(javelinConnector);
		if (wpart != null) {
			ConnectorEditor connectorEditor = (ConnectorEditor) wpart;
			ConnectorEditorPart connectorEditorPart = connectorEditor.getConnectorEditorPart();
			AbstractConnectorComposite connectorComposite = connectorEditorPart.getConnectorComposite();
			if ((connectorComposite != null) && (connectorComposite instanceof JavelinConnectorComposite)) {
				// Asynchronize javelin connection
				final JavelinConnectorComposite javelinConnectorComposite = ((JavelinConnectorComposite)connectorComposite);
				EventQueue.invokeLater(new Runnable(){
					public void run() {
						javelinConnectorComposite.renew(true);
						javelinConnectorComposite.connect();	
					}
				});
			}
		}
	}
	
	public IEditorPart getConnectorEditor(Connector connector) {
		IEditorPart editorPart = null;
		IWorkbenchPage activePage = getActivePage();

		if (activePage != null) {
			if (connector != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i=0;i<editorRefs.length;i++) {
					IEditorReference editorRef = (IEditorReference)editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if ((editorInput != null) && (editorInput instanceof ConnectorEditorInput)) {
							if (((ConnectorEditorInput)editorInput).is(connector)) {
								editorPart = editorRef.getEditor(false);
								break;
							}
						}
					}
					catch(PartInitException e) {
						//ConvertigoPlugin.logException(e, "Error while retrieving the connector editor '" + editorRef.getName() + "'");
					}
				}
			}
		}
		return editorPart;
	}
	
	private IWorkbenchPage getActivePage() {
		return PlatformUI
				.getWorkbench()
				.getActiveWorkbenchWindow()
				.getActivePage();
	}
	
}
