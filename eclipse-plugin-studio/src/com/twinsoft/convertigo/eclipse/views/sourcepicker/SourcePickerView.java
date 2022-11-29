/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.eclipse.views.mobile.MobilePickerComposite;
import com.twinsoft.convertigo.eclipse.views.mobile.NgxPickerComposite;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.StepSourceEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.StepSourceListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class SourcePickerView extends ViewPart implements StepSourceListener, ISelectionListener {
	
	private StackLayout stack;
	private SourcePickerComposite spc;
	private NgxPickerComposite npc;
	private MobilePickerComposite mpc;
	
	public SourcePickerView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(stack = new StackLayout());
		spc = new SourcePickerComposite(parent, SWT.NONE);
		npc = new NgxPickerComposite(parent, false);
		mpc = new MobilePickerComposite(parent, false);
		stack.topControl = spc;
		stack.topControl.getParent().layout(true);
		getSite().getPage().addSelectionListener(this);
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
		}
		catch (Exception e) {};
		spc.dispose();
		npc.dispose();
		mpc.dispose();
		super.dispose();
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection && part instanceof ProjectExplorerView) {
			TreeObject selected = (TreeObject) ((IStructuredSelection) selection).getFirstElement();
			if (selected == stack.topControl.getParent().getData("LastSelected")) {
				return;
			}
			stack.topControl.getParent().setData("LastSelected", selected);
			DatabaseObjectTreeObject dbot = (DatabaseObjectTreeObject) (selected instanceof DatabaseObjectTreeObject ? selected : selected.getParent());
			DatabaseObject dbo = dbot.getObject();
			if (dbo instanceof Step) {
				if (stack.topControl != spc) {
					stack.topControl = spc;
					stack.topControl.getParent().layout(true);
				}
				spc.sourceSelected(new StepSourceEvent(dbo));
			} else if (dbo.getClass().getName().contains(".ngx.")) {
				if (stack.topControl != npc) {
					stack.topControl = npc;
					stack.topControl.getParent().layout(true);
				}
				npc.setCurrentInput(selected, null);
			} else if (dbo.getClass().getName().contains(".mobile.")) {
				if (stack.topControl != mpc) {
					stack.topControl = mpc;
					stack.topControl.getParent().layout(true);
				}
				mpc.setCurrentInput(selected, null);
			}
		}
	}
}
