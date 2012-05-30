package com.twinsoft.convertigo.eclipse.wizards.install;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ChooseWorkspaceLocationPage extends WizardPage {

	private Text userWorkspaceLocation;
	private Composite container;

	public ChooseWorkspaceLocationPage() {
		super("User workspace location");
		setDescription("Choose the Convertigo user workspace location.");
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		
		GridData layoutData;
		
		Label label;
		
		label = new Label(container, SWT.NONE);
		label.setText("The Convertigo user workspace will contain all Convertigo " +
				"configuration files, log files, and all projects files.\n\n" +
				"You must choose a directory for which you have full read and write permissions.");
		layoutData = new GridData();
		layoutData.horizontalAlignment = SWT.FILL;
		label.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NULL);
		label.setText("User workspace location:");
		layoutData = new GridData();
		layoutData.horizontalAlignment = SWT.LEFT;
		label.setLayoutData(layoutData);

		userWorkspaceLocation = new Text(container, SWT.BORDER | SWT.SINGLE);
		userWorkspaceLocation.setText(System.getProperty("user.home") + "/convertigo");
		userWorkspaceLocation.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
			}
		});

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		userWorkspaceLocation.setLayoutData(gd);
		
		Button browseButton = new Button(container, SWT.NONE);
		browseButton.setText("Browse");
		browseButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog directoryChooserDialog = new DirectoryDialog(getShell());
		        directoryChooserDialog.setFilterPath(null);
		        directoryChooserDialog.setText("Select the Convertigo user workspace directory");

		        String dir = directoryChooserDialog.open();
		        if (dir != null) {
		        	userWorkspaceLocation.setText(dir);
		        }
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});

		// Required to avoid an error in the system
		setControl(container);
		setPageComplete(false);
	}

	public String getUserWorkspaceLocation() {
		return userWorkspaceLocation.getText();
	}

}
