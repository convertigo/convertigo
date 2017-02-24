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

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;

public class MobileBuilderAction extends MyAbstractAction {

	public MobileBuilderAction() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		
		MobileBuilder mb = getMobileBuilder();
		String actionID = action.getId();
		/*if (actionID.equals("convertigo.action.mobilebuilder.install")) {
			
		}
		if (actionID.equals("convertigo.action.mobilebuilder.startWatch")) {
			action.setEnabled(mb != null && !mb.isWatching());
		}
		if (actionID.equals("convertigo.action.mobilebuilder.stopWatch")) {
			action.setEnabled(mb != null && mb.isWatching());
		}*/
		action.setEnabled(false);
	}

	@Override
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
			MobileBuilder mb = getMobileBuilder();
			if (mb != null) {
				String actionID = action.getId();
				if (actionID.equals("convertigo.action.mobilebuilder.startWatch")) {
					mb.cmdStartWatch();
				}
				if (actionID.equals("convertigo.action.mobilebuilder.stopWatch")) {
					mb.cmdStopWatch();
				}
			}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to start watch!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}

	private MobileBuilder getMobileBuilder() {
		ProjectExplorerView explorerView = getProjectExplorerView();
		if (explorerView != null) {
			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
			Object dbo = treeObject.getObject();
			if (dbo != null && dbo instanceof ApplicationComponent) {
				return ((ApplicationComponent)dbo).getProject().getMobileBuilder();
			}
		}
		return null;
	}
}
