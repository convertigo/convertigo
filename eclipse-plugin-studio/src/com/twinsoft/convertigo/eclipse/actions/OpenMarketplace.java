/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.eclipse.editors.MarketplaceEditor;

public class OpenMarketplace implements IWorkbenchWindowActionDelegate {
	
	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}

	public void run(IAction action) {
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			activePage.openEditor(new IEditorInput() {
				
				@Override
				public <T> T getAdapter(Class<T> adapter) {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public String getToolTipText() {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public IPersistableElement getPersistable() {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public String getName() {
					// TODO Auto-generated method stub
					return "Convertigo Marketplace";
				}
				
				@Override
				public ImageDescriptor getImageDescriptor() {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public boolean exists() {
					// TODO Auto-generated method stub
					return false;
				}
			} , MarketplaceEditor.ID);
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
