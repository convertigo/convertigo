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
		setDescription("This is the first time Convertigo is launched this workspace...");
		setPageComplete(true);
	}

	public void createControl(Composite parent) {		
		container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, true));
		
		Label label = new Label(container, SWT.WRAP);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		label.setText("A new Convertigo workspace will be created in:\n\n" +
				"\t'" + Engine.PROJECTS_PATH + "'\n\n" +
				"This action will be completed when this wizard finishes.");
		
		setControl(container);
	}
}