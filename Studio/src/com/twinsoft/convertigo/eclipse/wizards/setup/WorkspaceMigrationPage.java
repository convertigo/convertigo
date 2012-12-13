package com.twinsoft.convertigo.eclipse.wizards.setup;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.twinsoft.convertigo.engine.Engine;

public class WorkspaceMigrationPage extends WizardPage {

	private Composite container;

	public WorkspaceMigrationPage() {
		super("WorkspaceMigrationPage");
		setTitle("Convertigo Workspace migration");
		setDescription("Older workspace detected.");
		setPageComplete(true);
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, true));
		
		Label label = new Label(container, SWT.WRAP);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		label.setText("We detected that The workspace chosen is based on a previous version of Convertigo.\n" +
				"This workspace will be migrated to the new workspace format when this wizard finishes.\n\n" +
				"If you do not want to migrate your workspace, end the wizard and re-launch the studio to choose a different workspace.\n\n\n" +
				"We will migration the new convertigo workspace in :\n\n" +
				Engine.PROJECTS_PATH);
		
		setControl(container);
	}

//	public void apply() {
//		File userWorkspace = new File(Engine.USER_WORKSPACE_PATH);
//
//		if (userWorkspace.list().length == 0) {
//			ConvertigoPlugin.logInfo("First start, check for workspace migration …");
//
//			File exUserWorkspace = new File(userWorkspaceLocation.getText());
//			File eclipseWorkspace = new File(Engine.PROJECTS_PATH);
//			
//			boolean bMove = exUserWorkspace.equals(exUserWorkspace);
//
//			if (checkPre6_2_workspace(exUserWorkspace)) {
//				ConvertigoPlugin.logInfo("Your selected folder point to a pre-6.2.0 CEMS workspace. Migration starting …");
//
//				boolean projectsMoveFailed = false;
//
//				for (File file : exUserWorkspace.listFiles()) {
//					if (!file.getName().equals(".metadata")) {
//						try {
//							ConvertigoPlugin.logInfo("Migration in progress : moving " + file.getName() + " …");
//							if (bMove) {
//								FileUtils.moveToDirectory(file, userWorkspace, false);
//							} else {
//								FileUtils.copyFileToDirectory(file, userWorkspace, true);
//							}
//						} catch (IOException e) {
//							projectsMoveFailed = projectsMoveFailed || file.getName().equals("projects");
//							ConvertigoPlugin.logInfo("Migration in progress : failed to " + (bMove ? "move " : "copy ") + file.getName() + " ! (" + e.getMessage() + ")");
//						}
//					}
//				}
//
//				if (!projectsMoveFailed) {
//					ConvertigoPlugin.logInfo("Migration in progress : " + (bMove ? "move" : "copy") + " move back CEMS projects to the Eclipse workspace …");
//					File exMetadata = new File(exUserWorkspace, "projects/.metadata");
//					try {
//						FileUtils.copyDirectoryToDirectory(exMetadata, eclipseWorkspace);
//						if (bMove) {
//							FileUtils.deleteQuietly(exMetadata);
//						}
//					} catch (IOException e1) {
//						ConvertigoPlugin.logInfo("Migration in progress : failed to merge .metadata ! (" + e1.getMessage() + ")");
//					}
//					
//					for (File file : new File(userWorkspace, "projects").listFiles()) {
//						try {
//							ConvertigoPlugin.logInfo("Migration in progress : " + (bMove ? "moving" : "copying") + " the file " + file.getName() + " into the Eclipse Workspace …");
//							FileUtils.moveToDirectory(file, eclipseWorkspace, false);
//						} catch (IOException e) {
//							ConvertigoPlugin.logInfo("Migration in progress : failed to " + (bMove ? "move " : "copy ") + file.getName() + " ! (" + e.getMessage() + ")");
//						}
//					}
//					
//					ConvertigoPlugin.logInfo("Migration of workspace done !\n" +
//							"Migration of the folder : " + exUserWorkspace.getAbsolutePath() + "\n" +
//							"Eclipse Workspace with your CEMS projects : " + eclipseWorkspace.getAbsolutePath() + "\n" +
//							"Convertigo Workspace with your CEMS configuration : " + userWorkspace.getAbsolutePath());
//				} else {
//					ConvertigoPlugin.logInfo("Migration incomplet : cannot move back CEMS projects to the Eclipse workspace !");
//				}
//			}
//		}
//	}
}