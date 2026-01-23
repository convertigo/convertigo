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

package com.twinsoft.convertigo.eclipse.wizards.deploy;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import com.twinsoft.convertigo.beans.core.IApplicationComponent;
import com.twinsoft.convertigo.beans.core.Project;

class ProjectBuildOptionsWizardPage extends WizardPage {

	private boolean doBuild = true;
	private Project project;
	
	ProjectBuildOptionsWizardPage(Project project) {
		super("ProjectBuildOptionsWizardPage", "Build options", null);
		this.project = project;
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		
		GridLayout gl = new GridLayout(1, false);
		gl.verticalSpacing = 10;
		composite.setLayout(gl);
		
		Label label = new Label(composite, SWT.NONE);
		label.setText("Your application is not yet built to be deployed, or has been modifed since last deploy.");
		
		Group group = new Group(composite, SWT.NONE);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText("Select one option below:");
		group.setLayout(new FillLayout(SWT.VERTICAL));
		
		Button checkB = new Button(group, SWT.RADIO);
		checkB.setText("Build in production mode (recommended)");
		checkB.setSelection(true);
		checkB.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doBuild = true;
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				doBuild = true;
			}
		});

		Button checkI = new Button(group, SWT.RADIO);
		checkI.setText("Do not build");
		checkI.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doBuild = false;
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				doBuild = false;
			}
		});
		
		setControl(composite);
	}

	private String getUnbuiltMessage() {
		if (project != null) {
			IApplicationComponent app = project.getMobileApplication() != null ? project.getMobileApplication().getApplicationComponent() : null;
			return app != null ? app.getUnbuiltMessage() : null;
		}
		return null;
	}
	
	@Override
	public IWizardPage getPreviousPage() {
		if (getUnbuiltMessage() == null) {
			return null;
		}
		return super.getPreviousPage();
	}

	@Override
	public IWizardPage getNextPage() {
		IWizardPage nextPage = super.getNextPage();
		if (doBuild) {
			return nextPage;
		} else {
			return nextPage.getNextPage();
		}
	}
}
