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

package com.twinsoft.convertigo.eclipse.views.mobile;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

public class MobilePickerView extends ViewPart implements ISelectionListener {
	private MobilePickerComposite mpc;
	
	public MobilePickerView() {
		
	}

	@Override
	public void createPartControl(Composite parent) {
		mpc = new MobilePickerComposite(parent, false);
		getSite().getPage().addSelectionListener(this);
	}
	
	@Override
	public void dispose() {
		try {
			getSite().getPage().removeSelectionListener(this);
		}
		catch (Exception e) {};
		mpc.dispose();
		super.dispose();
	}
	
	@Override
	public void setFocus() {
		mpc.setFocus();
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection && part instanceof ProjectExplorerView) {
			Object selected = ((IStructuredSelection)selection).getFirstElement();
			mpc.setCurrentInput(selected, null);
		}
	}
}
