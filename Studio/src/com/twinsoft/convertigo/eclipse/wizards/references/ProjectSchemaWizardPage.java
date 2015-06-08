/*
 * Copyright (c) 2001-2011 Convertigo SA.
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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.wizards.references;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.eclipse.wizards.new_object.ObjectExplorerWizardPage;
import com.twinsoft.convertigo.engine.Engine;

public class ProjectSchemaWizardPage extends WizardPage {
	private Object parentObject = null;
	
	private Tree tree;
	private String projectName;
	
	public ProjectSchemaWizardPage(Object parentObject) {
		super("ProjectSchemaWizardPage");
		this.parentObject = parentObject;
		setTitle("Convertigo project");
		setDescription("Please choose the project to reference.");
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 2;
		layout.verticalSpacing = 9;

		Label label = new Label(container, SWT.NULL);
		label.setText("&Project:");
		
		tree = new Tree(container, SWT.SINGLE | SWT.BORDER);
		tree.setHeaderVisible(false);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.verticalSpan = 20;
		gridData.horizontalSpan = 2;
		tree.setLayoutData(gridData);		
		tree.addListener(SWT.Selection, new Listener() {
			public void handleEvent(final Event event) {
				TreeItem item = (TreeItem) event.item;
				projectName = item.getText();
				dialogChanged();
			}
		});
		tree.setVisible(false);
		
		initialize();
		setControl(container);
		dialogChanged();
	}

	@Override
	public void performHelp() {
		getPreviousPage().performHelp();
	}
	
	private void initialize() {
		if (parentObject instanceof Project) {
			Project project = (Project)parentObject;
			List<String> projectList = new ArrayList<String>();
			projectList.add(project.getName());
			List<Reference> references = project.getReferenceList();
			for (Reference reference: references) {
				if (reference instanceof ProjectSchemaReference) {
					projectList.add(((ProjectSchemaReference)reference).getProjectName());
				}
			}
			
			String[] projects = Engine.theApp.databaseObjectsManager.getAllProjectNamesArray();
			for (String name: projects) {
				if (!projectList.contains(name)) {
					TreeItem branch = new TreeItem(tree, SWT.NONE);
					branch.setText(name);
				}
			}
			tree.setVisible(true);
		}
		else
			tree.setVisible(false);
	}
	
	private void dialogChanged() {
		if (projectName == null) {
			updateStatus("Please select a project");
			return;
		}

		try {
			DatabaseObject dbo = ((ObjectExplorerWizardPage)getWizard().getPage("ObjectExplorerWizardPage")).getCreatedBean();
			if (dbo != null) {
				if (dbo instanceof ProjectSchemaReference) {
					((ProjectSchemaReference)dbo).setProjectName(projectName);
				}
			}
		} catch (NullPointerException e) {
			updateStatus("New Bean has not been instantiated");
			return;
		}
		
		updateStatus(null);
	}
	
	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	@Override
	public IWizardPage getNextPage() {
		return null;
	}
	
}
