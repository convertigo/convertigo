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

package com.twinsoft.convertigo.eclipse.views.mobile;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.eclipse.wizards.new_ngx.ComponentExplorerComposite;

public class NgxPaletteView extends ViewPart implements ISelectionListener, TreeObjectListener {

	protected ComponentExplorerComposite explorerComposite = null;
	
	public NgxPaletteView() {
		
	}

	@Override
	public void createPartControl(Composite parent) {
		explorerComposite = new ComponentExplorerComposite(parent, SWT.NONE);
		
		getSite().getPage().addSelectionListener(this);
		ConvertigoPlugin.projectManager.getProjectExplorerView().addTreeObjectListener(this);
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		ConvertigoPlugin.projectManager.getProjectExplorerView().removeTreeObjectListener(this);
		
		super.dispose();
	}

	@Override
	public void setFocus() {
		if (explorerComposite != null) {
			explorerComposite.setFocus();
		}
	}

	public synchronized void refresh() {
		if (explorerComposite != null) {
			explorerComposite.reloadComponents();
		}
	}
	
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		
	}

	@Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		
	}

	@Override
	public void treeObjectAdded(TreeObjectEvent treeObjectEvent) {
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (treeObject instanceof ProjectTreeObject || treeObject instanceof UnloadedProjectTreeObject) {
			refresh();
		}
	}

	@Override
	public void treeObjectRemoved(TreeObjectEvent treeObjectEvent) {
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (treeObject instanceof ProjectTreeObject || treeObject instanceof UnloadedProjectTreeObject) {
			refresh();
		}
	}
}
