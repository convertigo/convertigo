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

package com.twinsoft.convertigo.eclipse.editors.xsl;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.wst.xml.ui.internal.tabletree.XMLMultiPageEditorPart;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;

@SuppressWarnings("restriction")
public class XslRuleEditor extends EditorPart implements IPropertyListener {
	Composite plane;

	Composite editor;

	XMLMultiPageEditorPart xmlEditor;

	private IFile file;

	protected Document xslDom;

	private IEditorSite eSite;

	private IEditorInput eInput;

	private SashForm sashForm = null;

	private ListenerList listenerList;

	private Sheet parentStyleSheet;
	
	private String parentStyleSheetUrl;
	
	public XslRuleEditor() {
		super();
	}
	
	@Override
	public void dispose() {
		xmlEditor.removePropertyListener(this);
		xmlEditor.dispose();
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {
		xmlEditor.doSave(monitor);

		parentStyleSheet.hasChanged = true;
		// Refresh tree
		ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (projectExplorerView != null)
			projectExplorerView.updateDatabaseObject(parentStyleSheet);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	public void doSaveAs() {
		xmlEditor.doSaveAs();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 */
	public boolean isDirty() {
		return (xmlEditor.isDirty());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 */
	public void createPartControl(Composite parent) {
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.marginWidth = 2;
		gridLayout.marginHeight = 2;

		editor = parent;
		editor.setLayout(gridLayout);

		xmlEditor = new XMLMultiPageEditorPart();
		xmlEditor.addPropertyListener(this);

		createSashForm();
		createXslSourceEditor();
		/* see #2299 */
		//createBrowser();
		//sashForm.setWeights(new int[] { 50, 50 });
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		xmlEditor.setFocus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite,
	 *      org.eclipse.ui.IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		file = ((FileEditorInput) input).getFile();
		xslDom = parseXslFile(file);
		setSite(site);
		setInput(input);
		eSite = site;
		eInput = input;
		parentStyleSheet = ((XslFileEditorInput) input).getParentStyleSheet();
		parentStyleSheetUrl = ((XslFileEditorInput) input).getParentStyleSheetUrl();
		
		setPartName(file.getName());
	}

	/**
	 * This method initializes the Xsl Source Editor
	 * 
	 */
	private void createXslSourceEditor() {
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalSpan = 2;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		
		try {
			xmlEditor.init(eSite, eInput);
			xmlEditor.setInitializationData(getConfigurationElement(), null, null);
		} catch (PartInitException e) {
			ConvertigoPlugin.logException(e, "Error inialiazing XSL editor '" + eInput.getName() + "'");
		}
		
		xmlEditor.createPartControl(sashForm);
		
		/* see #2299 */
		//Control childs[] = sashForm.getChildren();
		//childs[1].setLayoutData(gridData);
		
	}

	/**
	 * Parses as a DOM the IFile passed in argument ..
	 * 
	 * @param file
	 *            to parse
	 * @return parsed Document
	 */
	private Document parseXslFile(IFile file) {
		Document doc;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder documentBuilder = factory.newDocumentBuilder();
			doc = documentBuilder.parse(new InputSource(file.getContents()));
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error parsing xsl file '"
					+ file.getName() + "'");
			doc = null;
		}
		return doc;
	}

	/**
	 * This method initializes sashForm
	 * 
	 */
	private void createSashForm() {
		GridData gridData1 = new org.eclipse.swt.layout.GridData();
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData1.grabExcessHorizontalSpace = true;
		gridData1.grabExcessVerticalSpace = true;
		gridData1.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		sashForm = new SashForm(editor, SWT.NONE);
		sashForm.setOrientation(org.eclipse.swt.SWT.VERTICAL);
		sashForm.setLayoutData(gridData1);
	}

	@Override
	public void addPropertyListener(IPropertyListener l) {
		if (listenerList == null)
			listenerList = new ListenerList();
		listenerList.add(l);
	}

	@Override
	public void removePropertyListener(IPropertyListener l) {
		if (listenerList != null) {
			listenerList.remove(l);
		}
	}

	public void propertyChanged(Object source, int propId) {
		// When a property from the xslEditor Changes, walk the list all the listeners and notify them.
		Object listeners[] = listenerList.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			IPropertyListener listener = (IPropertyListener) listeners[i];
			listener.propertyChanged(this, propId);
		}

		if (propId == IEditorPart.PROP_DIRTY) {
			if (!xmlEditor.isDirty()) {
				// We changed from Dirty to non dirty ==> User has saved so,
				// launch Convertigo engine

				// "touch" the parent style sheet ==> Convertigo engine will
				// recompile it
				
				IPath path;
				path = file.getRawLocation();
				path = path.append("../../" + parentStyleSheetUrl);
				File parentFile = path.toFile();
				parentFile.setLastModified(System.currentTimeMillis());
			}
		}
	}
}
