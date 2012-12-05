package com.twinsoft.convertigo.eclipse.wizards.setup;

import java.io.File;

import org.eclipse.jface.wizard.IWizardPage;
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
		setTitle("Workspace Launcher");
		setDescription("Choose the Convertigo user workspace location.");
		setPageComplete(true);
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.marginWidth = 30;
		
		GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
		layoutData.horizontalSpan = 3;
		
		final String userWorkspace = System.getProperty("convertigo.cems.user_workspace_path",
				System.getProperty("user.home") + "/convertigo");

		Label label = new Label(container, SWT.WRAP);
		label.setText("The Convertigo user workspace will contain all Convertigo " +
				"configuration files, log files, and all projects files.\n\n" +
				"You must choose a directory for which you have full read and write permissions.\n" +
				"If the chosen location does not exist, it will be automatically created.\n\n" +
				"Convertigo studio is currently installed in: " + System.getProperty("user.dir") + "\n\n" +
				"Default Convertigo user workspace is: " + userWorkspace + "\n\n" +
				"Changing the Convertigo user workspace will require a studio restart.\n\n\n\n");		
		label.setLayoutData(layoutData);
		
		layoutData = new GridData(GridData.FILL_HORIZONTAL);
		layoutData.horizontalSpan = 3;
		
		label = new Label(container, SWT.NONE);
		label.setText("User workspace location:");
		label.setLayoutData(layoutData);
		
		layoutData = new GridData();
		layoutData.horizontalAlignment = SWT.FILL;
		layoutData.grabExcessHorizontalSpace = true;
		
		userWorkspaceLocation = new Text(container, SWT.BORDER | SWT.SINGLE);
		userWorkspaceLocation.setText(userWorkspace);
		userWorkspaceLocation.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				if (userWorkspaceLocation.getText().length() != 0) {
					File directory = new File(userWorkspaceLocation.getText());
					if (directory.exists() && !directory.isDirectory()) {
						setErrorMessage("This chosen location is not a directory!");
						setPageComplete(false);
					} else {
						setErrorMessage(null);
						setPageComplete(true);
					}
				}
			}
		});
		userWorkspaceLocation.setLayoutData(layoutData);
		
		Button browseButton = new Button(container, SWT.NONE);
		browseButton.setText("Browse...");
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
			}
		});

		Button resetButton = new Button(container, SWT.NONE);
		resetButton.setText("Reset");
		resetButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
	        	userWorkspaceLocation.setText(userWorkspace);
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		// Required to avoid an error in the system
		setControl(container);
	}

	public String getUserWorkspaceLocation() {
		return userWorkspaceLocation.getText();
	}

	@Override
	public IWizardPage getNextPage() {
		SetupWizard setupWizard = (SetupWizard) getWizard();
		((SummaryPage) setupWizard.getPage("SummaryPage")).updateSummary();
		return super.getNextPage();
	}
	
	@Override
	public IWizardPage getPreviousPage() {
		SetupWizard setupWizard = (SetupWizard) getWizard();
		((SummaryPage) setupWizard.getPage("SummaryPage")).updateSummary();
		return super.getPreviousPage();
	}
}
