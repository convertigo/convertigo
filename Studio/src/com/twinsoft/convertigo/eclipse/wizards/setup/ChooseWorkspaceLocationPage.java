package com.twinsoft.convertigo.eclipse.wizards.setup;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.Engine;

public class ChooseWorkspaceLocationPage extends WizardPage {

	private Text userWorkspaceLocation;
	private Composite container;

	public ChooseWorkspaceLocationPage() {
		super("Import previous workspace");
		setTitle("Workspace Importer");
		setDescription("Choose a previous Convertigo user workspace location.");
		setPageComplete(true);
	}

	public void createControl(Composite parent) {
		String userWorkspace = Engine.USER_WORKSPACE_PATH;
		String eclipseWorkspace = Engine.PROJECTS_PATH;
		
		container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginWidth = 30;
		container.setLayout(layout);
		
		Label label = new Label(container, SWT.WRAP);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		label.setText("The Convertigo projects are in the Eclipse Workspace.\n" +
				"The Convertigo User workspace contains all Convertigo configuration files, log files, and some technical folder.\n\n" +
				"Your Eclipse workspace is : " + eclipseWorkspace + "\n\n" +
				"Your Convertigo user workspace is: " + userWorkspace + "\n\n" +
				"You can import an old Convertigo user workspace into your current wokspaces, or you can ignore that step.\n\n\n\n");
		
		label = new Label(container, SWT.NONE);
		label.setText("User workspace location:");
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		userWorkspaceLocation = new Text(container, SWT.BORDER | SWT.SINGLE);
		
		userWorkspaceLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Button browseButton = new Button(container, SWT.NONE);
		browseButton.setText("Browse...");

		Button resetButton = new Button(container, SWT.NONE);
		resetButton.setText("Reset");
		
		final Label summary = new Label(container, SWT.WRAP);
		summary.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		final String[] importWorkspace = {""};
		
		for (String exWorkspace : Arrays.asList(eclipseWorkspace, new File(eclipseWorkspace).getParent(), System.getProperty("user.home") + "/convertigo")) {
			if (checkPre6_2_workspace(new File(exWorkspace))) {
				importWorkspace[0] = exWorkspace;
				break;
			}
		}
		
		browseButton.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog directoryChooserDialog = new DirectoryDialog(getShell());
		        directoryChooserDialog.setFilterPath(userWorkspaceLocation.getText());
		        directoryChooserDialog.setText("Select the Convertigo user workspace directory");

		        String dir = directoryChooserDialog.open();
		        if (dir != null) {
		        	userWorkspaceLocation.setText(dir);
		        }
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			
		});

		userWorkspaceLocation.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				if (userWorkspaceLocation.getText().length() != 0) {
					File directory = new File(userWorkspaceLocation.getText());
					if (directory.exists() && !directory.isDirectory()) {
						summary.setText("");
						setErrorMessage("This chosen location is not a directory!");
						setPageComplete(false);
					} else if (directory.exists() || directory.getParentFile().exists()) {
						if (checkPre6_2_workspace(directory)) {
							summary.setText("Import a pre CEMS 6.2 Convertigo user Workspace.");
							setErrorMessage(null);
							setPageComplete(true);
						} else {
							summary.setText("Import an another ... TODO");
							setErrorMessage("This case is not supported!");
							setPageComplete(false);
						}
					} else {
						summary.setText("");
						setErrorMessage("The parent directory of this current path doesn't exist!");
						setPageComplete(false);
					}
				} else {
					summary.setText("Nothing to import");
					
					setErrorMessage(null);
					setPageComplete(true);
				}
			}
			
		});
		
		resetButton.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
	        	userWorkspaceLocation.setText(importWorkspace[0]);
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			
		});

		userWorkspaceLocation.setText(importWorkspace[0]);
		
		// Required to avoid an error in the system
		setControl(container);
	}

	public String getUserWorkspaceLocation() {
		return userWorkspaceLocation.getText();
	}
	
	public boolean checkPre6_2_workspace(File directory) {
		boolean pre6_2 = false;

		for (String pathToCheck : Arrays.asList(
				"configuration/engine.properties",
				"minime/Java/login.txt",
				"cache",
				"projects",
				"logs"
				)) {
			pre6_2 = new File(directory, pathToCheck).exists();
			if (!pre6_2) {
				break;
			}
		}
		
		return pre6_2;
	}

	public void apply() {
		File userWorkspace = new File(Engine.USER_WORKSPACE_PATH);

		if (userWorkspace.list().length == 0) {
			ConvertigoPlugin.logInfo("First start, check for workspace migration …");

			File exUserWorkspace = new File(userWorkspaceLocation.getText());
			File eclipseWorkspace = new File(Engine.PROJECTS_PATH);
			
			boolean bMove = exUserWorkspace.equals(exUserWorkspace);

			if (checkPre6_2_workspace(exUserWorkspace)) {
				ConvertigoPlugin.logInfo("Your selected folder point to a pre-6.2.0 CEMS workspace. Migration starting …");

				boolean projectsMoveFailed = false;

				for (File file : exUserWorkspace.listFiles()) {
					if (!file.getName().equals(".metadata")) {
						try {
							ConvertigoPlugin.logInfo("Migration in progress : moving " + file.getName() + " …");
							if (bMove) {
								FileUtils.moveToDirectory(file, userWorkspace, false);
							} else {
								FileUtils.copyFileToDirectory(file, userWorkspace, true);
							}
						} catch (IOException e) {
							projectsMoveFailed = projectsMoveFailed || file.getName().equals("projects");
							ConvertigoPlugin.logInfo("Migration in progress : failed to " + (bMove ? "move " : "copy ") + file.getName() + " ! (" + e.getMessage() + ")");
						}
					}
				}

				if (!projectsMoveFailed) {
					ConvertigoPlugin.logInfo("Migration in progress : " + (bMove ? "move" : "copy") + " move back CEMS projects to the Eclipse workspace …");
					File exMetadata = new File(exUserWorkspace, "projects/.metadata");
					try {
						FileUtils.copyDirectoryToDirectory(exMetadata, eclipseWorkspace);
						if (bMove) {
							FileUtils.deleteQuietly(exMetadata);
						}
					} catch (IOException e1) {
						ConvertigoPlugin.logInfo("Migration in progress : failed to merge .metadata ! (" + e1.getMessage() + ")");
					}
					
					for (File file : new File(userWorkspace, "projects").listFiles()) {
						try {
							ConvertigoPlugin.logInfo("Migration in progress : " + (bMove ? "moving" : "copying") + " the file " + file.getName() + " into the Eclipse Workspace …");
							FileUtils.moveToDirectory(file, eclipseWorkspace, false);
						} catch (IOException e) {
							ConvertigoPlugin.logInfo("Migration in progress : failed to " + (bMove ? "move " : "copy ") + file.getName() + " ! (" + e.getMessage() + ")");
						}
					}
					
					ConvertigoPlugin.logInfo("Migration of workspace done !\n" +
							"Migration of the folder : " + exUserWorkspace.getAbsolutePath() + "\n" +
							"Eclipse Workspace with your CEMS projects : " + eclipseWorkspace.getAbsolutePath() + "\n" +
							"Convertigo Workspace with your CEMS configuration : " + userWorkspace.getAbsolutePath());
				} else {
					ConvertigoPlugin.logInfo("Migration incomplet : cannot move back CEMS projects to the Eclipse workspace !");
				}
			}
		}
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
