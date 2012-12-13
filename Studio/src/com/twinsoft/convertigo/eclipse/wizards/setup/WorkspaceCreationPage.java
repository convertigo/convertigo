package com.twinsoft.convertigo.eclipse.wizards.setup;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.twinsoft.convertigo.engine.Engine;

public class WorkspaceCreationPage extends WizardPage {

	private Composite container;

	public WorkspaceCreationPage() {
		super("WorkspaceCreationPage");
		setTitle("Convertigo Workspace");
		setDescription("First Convertigo launch in this workspace.");
		setPageComplete(true);
	}

	public void createControl(Composite parent) {		
		container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, true));
		
		Label label = new Label(container, SWT.WRAP);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		label.setText("We will create a new convertigo workspace in :\n\n" +
				Engine.PROJECTS_PATH + "\n\n" +
				"This action will be completed when this wizard finishes.");
		
		setControl(container);
	}
}