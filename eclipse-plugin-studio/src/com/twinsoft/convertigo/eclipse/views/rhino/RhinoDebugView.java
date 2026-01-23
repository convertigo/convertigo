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

package com.twinsoft.convertigo.eclipse.views.rhino;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class RhinoDebugView extends ViewPart implements IPartListener2 {
	private RhinoDebug rhinoDebug;
	
	public RhinoDebugView() {
		
	}
	
	@Override
	public void dispose() {
		getSite().getPage().removePartListener(this);
		if (rhinoDebug != null) {
			rhinoDebug.dispose();
		}
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		rhinoDebug = new RhinoDebug(parent, SWT.NONE);
		getSite().getPage().addPartListener(this);
	}

	@Override
	public void setFocus() {
		rhinoDebug.setFocus();
	}
	
	public boolean onActivated(IWorkbenchPart part) {
		return true;
	}
	
	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		IWorkbenchPart part = partRef.getPart(false);
		onActivated(part);
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

}
