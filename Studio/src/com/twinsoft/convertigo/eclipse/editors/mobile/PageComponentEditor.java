/*
 * Copyright (c) 2001-2016 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.editors.mobile;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobilePageComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class PageComponentEditor extends EditorPart implements ISelectionChangedListener {

	private ProjectExplorerView projectExplorerView = null;
	private PageComponentEditorInput pageEditorInput;
	private Browser browser;
	private String initialUrl;
	
	public PageComponentEditor() {
		projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (projectExplorerView != null) {
			projectExplorerView.addSelectionChangedListener(this);
		}
	}

	
	@Override
	public void dispose() {
		if (projectExplorerView != null) {
			projectExplorerView.removeSelectionChangedListener(this);
		}
		if (browser != null) {
			browser.dispose();
		}
		super.dispose();
	}


	@Override
	public void doSave(IProgressMonitor monitor) {
		
	}

	@Override
	public void doSaveAs() {
		
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		
		pageEditorInput = (PageComponentEditorInput)input;
		setPartName(pageEditorInput.page.getProject().getName() + " [P: " + pageEditorInput.page.getName()+"]");
		
		initialUrl = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
		initialUrl += "/projects/" + pageEditorInput.page.getProject().getName() + "/DisplayObjects/mobile/index.html";
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		browser = createBrowser(parent);
		browser.setUrl(initialUrl);
	}

	private Browser createBrowser(Composite parent) {
		GridLayout gridLayout = new GridLayout();
		//gridLayout.numColumns = 2;
		parent.setLayout(gridLayout);
		
		browser = new Browser(parent, SWT.NONE);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		//data.horizontalSpan = 2;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		browser.setLayoutData(data);
		
		return browser;
	}

	@Override
	public void setFocus() {
		if (browser != null && !browser.isDisposed()) {
			browser.setFocus();
		}
	}

	public void refreshBrowser() {
		if (browser != null && !browser.isDisposed()) {
			browser.refresh();
		}
	}
	
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if (event.getSource() instanceof ISelectionProvider) {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			TreeObject treeObject = (TreeObject) selection.getFirstElement();
			if (treeObject != null) {
				if (treeObject instanceof MobilePageComponentTreeObject) {
					if (pageEditorInput.is(((MobilePageComponentTreeObject)treeObject).getObject())) {
						//System.out.println(pageEditorInput.getQName() +" has been selected");
						//getEditorSite().getPage().activate(this);
					}
				}
			}
		}
	}

}
