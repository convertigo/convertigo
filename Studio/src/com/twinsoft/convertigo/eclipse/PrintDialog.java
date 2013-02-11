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
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse;

import java.io.IOException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.print.ConvertigoPrint;
import com.twinsoft.convertigo.engine.print.PrintHTML;
import com.twinsoft.convertigo.engine.print.PrintPDF;


public class PrintDialog extends Dialog {
	private PrintDialogArea printDialogArea;

	private Project project = null;
	
	private String outputFileName=null;

	public PrintDialog(Shell parentShell) {
		super(parentShell);
		try {
			StructuredSelection selection = (StructuredSelection) PlatformUI
					.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.getSelection();

			Object element = selection.getFirstElement();
			if (element == null) {
				MessageDialog.openInformation(parentShell, "Error",
						"Please select a convertigo object before printing");
			} else if (element instanceof UnloadedProjectTreeObject) {
				MessageDialog
						.openInformation(parentShell, "Error",
								"Please select an opened project or an opened project item before printing");
			} else if (element instanceof ProjectTreeObject) {
				ProjectTreeObject dboto = (ProjectTreeObject) element;
				project = dboto.getObject();				 
			} else {
				DatabaseObjectTreeObject dboto = (DatabaseObjectTreeObject) element;
				DatabaseObject dbo = dboto.getObject();
				project = dbo.getProject();
			}
		} catch (Exception e) {
			MessageDialog.openInformation(parentShell, "Error",
					"Wrong object selectid, please select a Convertigo object before printing");
		}
	}

	public PrintDialog(IShellProvider parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		printDialogArea = new PrintDialogArea(parent, 0);		
		String projectName = project.getName();
		String outputFileName = Engine.PROJECTS_PATH + "\\" + projectName;
		printDialogArea.setFileLocation(outputFileName);
		return printDialogArea;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Print project");
		
		
	}

	@Override
	protected void okPressed() {
		String openFileText="Open file";
		if (this.getButton(IDialogConstants.OK_ID).getText().equals(openFileText)) {
			try {
				Runtime.getRuntime().exec("cmd.exe /c \""+outputFileName+"\"");
				this.close();
			} catch (IOException e) {				
				ConvertigoPlugin.logException(e, "Unable to open youre file");
			}
		}
		else {
			ConvertigoPrint convertigoPrint;
			String projectName=project.getName();
			SwtStatus progress=new SwtStatus(printDialogArea.getProgressBar(), printDialogArea.getProgressLabel());
			switch (printDialogArea.getCombo().getSelectionIndex()) {				
				case 1:
					convertigoPrint=new PrintHTML(projectName,progress);					
					break;
				default:convertigoPrint=new PrintPDF(projectName,progress);
			}
			printDialogArea.getProgressBar().setMaximum(100);		
			
			try {
				convertigoPrint.setProduct(com.twinsoft.convertigo.eclipse.Version.productVersion);				
				outputFileName=convertigoPrint.print(printDialogArea.getFileLocation());
				this.getButton(IDialogConstants.OK_ID).setText(openFileText);
				ConvertigoPlugin
				.logInfo("Votre rapport de documentation est prêt et se trouve dans le fichier :"
						+ outputFileName);
			} catch (Exception e) {
				ConvertigoPlugin.logException(e, "Unable to print");
			}
		}
			
	}

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}
}
