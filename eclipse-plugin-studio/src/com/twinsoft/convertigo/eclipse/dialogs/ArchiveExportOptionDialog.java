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

package com.twinsoft.convertigo.eclipse.dialogs;

import java.io.File;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.core.IApplicationComponent;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.engine.enums.ArchiveExportOption;
import com.twinsoft.convertigo.engine.util.FileUtils;

public class ArchiveExportOptionDialog extends Dialog {
	private boolean bDeploy;
	private Project project;
	private String version;
	private Set<ArchiveExportOption> archiveExportOptions;
	
	private Text versionSWT;
	private Button[] archiveExportOptionsSWT;
	
	public ArchiveExportOptionDialog(Shell parent, Project project, boolean bDeploy) {
		super(parent);
		this.project = project;
		this.bDeploy = bDeploy;
		this.version = project.getVersion();
		this.archiveExportOptions = ArchiveExportOption.load(project.getDirFile());
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		GridLayout gl = new GridLayout(1, false);
		gl.verticalSpacing = 10;
		composite.setLayout(gl);
		
		Label label = new Label(composite, SWT.NONE);
		label.setText("You can update the version of your project before export or deployment.");
		Group group = new Group(composite, SWT.NONE);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText("If you wish to, please change the value below: ");
		group.setLayout(new FillLayout(SWT.VERTICAL));
		versionSWT = new Text(group, SWT.NONE);
		versionSWT.setText(version);
		
		group = new Group(composite, SWT.NONE);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText("Check to include: ");
		group.setLayout(new FillLayout(SWT.VERTICAL));
		archiveExportOptionsSWT = new Button[ArchiveExportOption.values().length];
		int i = 0;
		File projectDir = project.getDirFile();
		for (ArchiveExportOption option: ArchiveExportOption.values()) {
			long size = option.size(projectDir);
			if (size > 0) {
				Button check = new Button(group, SWT.CHECK);
				check.setData(option);
				if (option == ArchiveExportOption.includeTestCase) {
					check.setText(option.display());
				} else {
					check.setText(option.display() + " [" + FileUtils.byteCountToDisplaySize(size) +"]");
				}
				check.setSelection(archiveExportOptions.contains(option));
				archiveExportOptionsSWT[i++] = check;
			} else {
				archiveExportOptionsSWT[i++] = null;
			}
		}
		
		IApplicationComponent app = project.getMobileApplication() != null ? project.getMobileApplication().getApplicationComponent() : null;
		String msg = app != null ? app.getUnbuiltMessage() : null;
		
		if (msg != null) {
			label = new Label(composite, SWT.NONE);
			label.setText(msg + "\nOnce the build finished, you can " + (bDeploy ? "deploy" : "export") + " again.");
			SwtUtils.applyStyle(label, "{ color: red }");
		}
		
		composite.pack(true);
		return composite;
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control buttonBar = super.createButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setText("Continue");
		return buttonBar;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Version update");
	}
	
	@Override
	protected int getShellStyle() {
		return SWT.TITLE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL;
	}
	
	@Override
	protected void okPressed() {
		version = versionSWT.getText();
		archiveExportOptions.clear();
		archiveExportOptions.addAll(ArchiveExportOption.all);
		for (Button check: archiveExportOptionsSWT) {
			if (check != null && !check.getSelection()) {
				archiveExportOptions.remove((ArchiveExportOption) check.getData());
			}
		}
		ArchiveExportOption.save(project.getDirFile(), archiveExportOptions);
		super.okPressed();
	}
	
	public String getVersion() {
		return version;
	}

	public Set<ArchiveExportOption> getArchiveExportOptions() {
		return archiveExportOptions;
	}

}
