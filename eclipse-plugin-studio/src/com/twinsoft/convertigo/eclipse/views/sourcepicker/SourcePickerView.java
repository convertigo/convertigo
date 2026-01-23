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
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.views.sourcepicker;

import java.util.Arrays;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.variables.StepVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.mobile.MobilePickerComposite;
import com.twinsoft.convertigo.eclipse.views.mobile.NgxPickerComposite;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.StepSourceEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.StepSourceListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class SourcePickerView extends ViewPart implements StepSourceListener, ISelectionListener, IPartListener2 {

	private StackLayout stack;
	private SourcePickerComposite spc;
	private NgxPickerComposite npc;
	private MobilePickerComposite mpc;
	private ISelection lastSelection;
	private boolean isVisible = true;

	public SourcePickerView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(stack = new StackLayout());
		spc = new SourcePickerComposite(parent, SWT.NONE);
		npc = new NgxPickerComposite(parent, false);
		mpc = new MobilePickerComposite(parent, false);

		SelectionAdapter selectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String val = ((ToolItem) e.widget).getSelection() ? "on" : "off";
				ConvertigoPlugin.setProperty("sourcepicker.link", val);
				if ("on".equals(val) && lastSelection != null) {
					selectionChanged((ProjectExplorerView) null, lastSelection);
				}
			}
		};

		for (ToolItem tiLink: Arrays.asList(spc.getTiLink(), npc.getTiLink(), mpc.getTiLink())) {
			tiLink.setToolTipText("Link with the 'Projects tree' selection");
			try {
				tiLink.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/resize_connector.gif"));
			} catch (Exception e3) {
				tiLink.setText("Link");
			}
			tiLink.setSelection(!"off".equals(ConvertigoPlugin.getProperty("sourcepicker.link")));
			tiLink.addSelectionListener(selectionListener);
			ConvertigoPlugin.asyncExec(() -> tiLink.setBackground(null));
		}

		stack.topControl = spc;
		stack.topControl.getParent().layout(true);
		getSite().getPage().addSelectionListener(this);
		getSite().getPage().addPartListener(this);
		isVisible = getSite().getPage().isPartVisible(this);

		ProjectExplorerView pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (pev != null) {
			ITreeSelection selection = pev.viewer.getStructuredSelection();
			if (selection != null) {
				selectionChanged(pev, selection);
			}
		}
	}



	@Override
	public void setFocus() {
		stack.topControl.setFocus();
	}



	public void close() {
		spc.close();
	}

	@Override
	public void sourceSelected(StepSourceEvent stepSourceEvent) {
		spc.sourceSelected(stepSourceEvent);
	}

	public Object getObject() {
		return spc.getObject();
	}

	@Override
	public void dispose() {
		try {
			getSite().getPage().removeSelectionListener(this);
			getSite().getPage().removePartListener(this);
		}
		catch (Exception e) {};
		if (spc == null) {
			spc.dispose();
		}
		if (npc == null) {
			npc.dispose();
		}
		if (mpc == null) {
			mpc.dispose();
		}
		super.dispose();
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (isVisible && selection instanceof IStructuredSelection && (part == null || part instanceof ProjectExplorerView)) {
			if("off".equals(ConvertigoPlugin.getProperty("sourcepicker.link"))) {
				lastSelection = selection;
				return;
			}

			TreeObject selected = (TreeObject) ((IStructuredSelection) selection).getFirstElement();
			if (selected == stack.topControl.getParent().getData("LastSelected")) {
				return;
			}
			stack.topControl.getParent().setData("LastSelected", selected);
			if (selected == null) {
				return;
			}
			DatabaseObjectTreeObject dbot = selected.getDatabaseObjectTreeObject();
			if (dbot == null) {
				return;
			}
			DatabaseObject dbo = dbot.getObject();
			if (dbo instanceof Step || dbo instanceof StepVariable) {
				if (stack.topControl != spc) {
					stack.topControl = spc;
					stack.topControl.getParent().layout(true);
				}
				spc.getTiLink().setSelection(true);
				spc.sourceSelected(new StepSourceEvent(dbo));
			} else if (dbo.getClass().getName().contains(".ngx.")) {
				if (stack.topControl != npc) {
					stack.topControl = npc;
					stack.topControl.getParent().layout(true);
				}
				npc.getTiLink().setSelection(true);
				npc.setCurrentInput(selected, null);
			} else if (dbo.getClass().getName().contains(".mobile.")) {
				if (stack.topControl != mpc) {
					stack.topControl = mpc;
					stack.topControl.getParent().layout(true);
				}
				mpc.getTiLink().setSelection(true);
				mpc.setCurrentInput(selected, null);
			}
		}
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
		if (partRef.getId().equals(getViewSite().getId()) && !isVisible) {
			isVisible = true;
			ProjectExplorerView pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
			if (pev != null) {
				selectionChanged(pev, pev.viewer.getStructuredSelection());
			}
		}
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		if (partRef.getId().equals(getViewSite().getId())) {
			isVisible = false;
		}
	}
}
