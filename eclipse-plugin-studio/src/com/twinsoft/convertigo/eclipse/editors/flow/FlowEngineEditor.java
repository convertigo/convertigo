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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.editors.flow;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.mobile.MobileDebugView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewImageProvider;
import com.twinsoft.convertigo.engine.Engine;

public class FlowEngineEditor extends EditorPart {

	public static final String ID = "com.twinsoft.convertigo.eclipse.editors.flow.FlowEngineEditor";

	private FlowEngineEditorInput input;
	private C8oBrowser browser;

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof FlowEngineEditorInput flowInput)) {
			throw new PartInitException("Invalid Flow browser editor input.");
		}
		this.input = flowInput;
		setSite(site);
		setInput(input);
		setPartName(flowInput.getName());
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
	public void dispose() {
		if (browser != null && !browser.isDisposed()) {
			browser.dispose();
		}
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		SwtUtils.refreshTheme();
		parent.setLayout(new GridLayout(1, false));

		var toolbar = new ToolBar(parent, SWT.FLAT | SWT.WRAP | SWT.RIGHT);
		toolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		var project = getProject();
		browser = project == null ? new C8oBrowser(parent, SWT.NONE) : new C8oBrowser(parent, SWT.NONE, project);
		browser.setLayoutData(new GridData(GridData.FILL_BOTH));
		browser.setUseExternalBrowser(false);

		browser.addToolItemNavigation(toolbar);
		browser.addToolItemOpenExternal(toolbar);
		addToolItemFrontendDebug(toolbar);
		browser.setUrl(input.getUrl());
	}

	private Project getProject() {
		try {
			var projectName = input == null ? "" : input.getProjectName();
			if (projectName != null && !projectName.isBlank()) {
				return Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
			}
		} catch (Exception e) {
		}
		return null;
	}

	private void addToolItemFrontendDebug(ToolBar toolbar) {
		new ToolItem(toolbar, SWT.SEPARATOR);
		var item = new ToolItem(toolbar, SWT.NONE);
		item.setImage(ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/debug.png"));
		item.setToolTipText("Open FrontEnd Debug");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MobileDebugView view = ConvertigoPlugin.getDefault().getMobileDebugView(true);
				if (view != null) {
					view.onActivated(FlowEngineEditor.this);
				}
			}
		});
	}

	public String getDebugUrl() {
		return browser == null || browser.isDisposed() ? "" : browser.getDebugUrl();
	}

	public String getProjectName() {
		return input == null ? "" : input.getProjectName();
	}

	public void updateInput(FlowEngineEditorInput input) {
		this.input = input;
		setInput(input);
		setPartName(input.getName());
		if (browser != null && !browser.isDisposed()) {
			browser.setUrl(input.getUrl());
		}
	}

	@Override
	public void setFocus() {
		if (browser != null && !browser.isDisposed()) {
			browser.setFocus();
		}
	}
}
