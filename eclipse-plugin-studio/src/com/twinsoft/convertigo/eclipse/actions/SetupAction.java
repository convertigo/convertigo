/*
 * Copyright (c) 2001-2023 Convertigo SA.
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.twinsoft.convertigo.eclipse.wizards.setup.SetupWizard;

public class SetupAction implements IWorkbenchWindowActionDelegate {
	
	public void run(IAction action) {
		SetupAction.runSetup();
	}

	public void selectionChanged(IAction action, ISelection selection) {

	}

	public static boolean runSetup() {
		Display display = Display.getDefault();
		WizardDialog wizardDialog = new WizardDialog(display.getActiveShell(), new SetupWizard());
		int ret = wizardDialog.open();
		
		return ret == WizardDialog.OK;
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}
	
}
