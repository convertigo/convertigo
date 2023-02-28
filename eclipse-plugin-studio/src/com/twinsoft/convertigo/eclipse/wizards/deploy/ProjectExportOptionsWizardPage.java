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

package com.twinsoft.convertigo.eclipse.wizards.deploy;

import java.io.File;
import java.util.Set;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.enums.ArchiveExportOption;
import com.twinsoft.convertigo.engine.util.FileUtils;

public class ProjectExportOptionsWizardPage extends WizardPage {

	Project project;
	String version;
	Set<ArchiveExportOption> archiveExportOptions;
	
	private Text versionSWT;
	private Button[] archiveExportOptionsSWT;
	
	public ProjectExportOptionsWizardPage(Project project) {
		super("ProjectExportOptionsWizardPage", "Archive options", null);
		this.project = project;
		this.version = project.getVersion();
		this.archiveExportOptions = ArchiveExportOption.load(project.getDirFile());
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		
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
		for (ArchiveExportOption option: ArchiveExportOption.values()) {
			Button check = new Button(group, SWT.CHECK);
			check.setData(option);
			archiveExportOptionsSWT[i++] = check;
		}
		fillOptions();
		
		setControl(composite);
	}

	private void fillOptions() {
		File projectDir = project.getDirFile();
		for (Button check: archiveExportOptionsSWT) {
			ArchiveExportOption option = (ArchiveExportOption) check.getData();
			long size = option.size(projectDir);
			if (option == ArchiveExportOption.includeTestCase) {
				check.setText(option.display());
			} else {
				check.setText(option.display() + " [" + FileUtils.byteCountToDisplaySize(size) +"]");
			}
			check.setSelection(archiveExportOptions.contains(option));
		}
	}
	
	protected void doUpdate() {
		version = versionSWT.getText();
		archiveExportOptions.clear();
		archiveExportOptions.addAll(ArchiveExportOption.all);
		for (Button check: archiveExportOptionsSWT) {
			if (check != null && !check.getSelection()) {
				archiveExportOptions.remove((ArchiveExportOption) check.getData());
			}
		}
		ArchiveExportOption.save(project.getDirFile(), archiveExportOptions);
		
		if (!version.equals(project.getVersion())) {
			ProjectExplorerView explorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
			TreeObject to = explorerView.findTreeObjectByUserObject(project);
			if (to != null && to instanceof ProjectTreeObject) {
				ProjectTreeObject projectTreeObject = (ProjectTreeObject)to;
				project.setVersion(version);
				project.hasChanged = true;
				projectTreeObject.save(false);
				
				explorerView.refreshTreeObject(projectTreeObject);
			}
		}
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible && isControlCreated()) {
			fillOptions();
		}
		if (!visible && isControlCreated() && !isCurrentPage()) {
			try {
				if (getContainer().getCurrentPage().equals(getWizard().getNextPage(this))) {
					doUpdate();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean isPageComplete() {
		return super.isPageComplete();
	}

}
