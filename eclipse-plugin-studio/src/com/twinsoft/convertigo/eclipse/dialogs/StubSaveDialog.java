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

package com.twinsoft.convertigo.eclipse.dialogs;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.eclipse.menus.StubDynamicMenu;

public class StubSaveDialog extends MyAbstractDialog {

	StubSaveDialogComposite composite = null;
	private DatabaseObject dbo = null;
	private String stubFileName = null;
	
	public StubSaveDialog(Shell parentShell, String dialogTitle, DatabaseObject dbo) {
		super(parentShell, StubSaveDialogComposite.class, dialogTitle);
		this.dbo = dbo;
	}
	
	public StubSaveDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle) {
		super(parentShell, dialogAreaClass, dialogTitle);
	}

	public StubSaveDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle, int width, int height) {
		super(parentShell, dialogAreaClass, dialogTitle, width, height);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);
		if (dialogComposite != null && dialogComposite instanceof StubSaveDialogComposite) {
			String stubsDirPath = dbo.getProject().getDirPath() + "/stubs";
			File stubsDir = new File(stubsDirPath);
			String defaultStubFileName = ((RequestableObject)dbo).getDefaultStubFileName();
			File defaultStubFile = new File(stubsDir, defaultStubFileName);
			
			Set<String> set = new HashSet<String>();
			try {
				set = StubDynamicMenu.listStubFiles(stubsDirPath, dbo);
			} catch (IOException e) {}
			
			((StubSaveDialogComposite)dialogComposite).fillCombo(set, "xml", defaultStubFile.getName());
		}
		return control;
	}
	
	protected Button getButtonOK(){
		return getButton(OK);
	}

	@Override
	protected void okPressed() {
		if (dialogComposite != null && dialogComposite instanceof StubSaveDialogComposite) {
			stubFileName = (String) ((StubSaveDialogComposite)dialogComposite).getValue(null);
		}
		super.okPressed();
	}

	public String getStubFileName() {
		return stubFileName;
	}
}
