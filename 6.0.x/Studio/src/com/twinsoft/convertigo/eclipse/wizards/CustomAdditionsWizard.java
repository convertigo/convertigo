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

package com.twinsoft.convertigo.eclipse.wizards;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.Engine;

public class CustomAdditionsWizard extends Wizard {
	private String projectName;
	private CustomAdditionsWizardPage cawp;
	
	public CustomAdditionsWizard(String projectName) {
		super();
		this.projectName = projectName;
		setWindowTitle("Add files to project");
	}
	
	@Override
	public void addPages() {
		cawp = new CustomAdditionsWizardPage("CustomAdditionsWizardPage");
		cawp.setTitle("Project additions");
		cawp.setMessage("Please select a file to add to your project if you'd like to customize it");
		this.addPage(cawp);
	}

	@Override
	public boolean canFinish() {
		return getContainer().getCurrentPage().getNextPage() == null;
	}

	@Override
	public boolean performFinish() {
		String fileName = null;
		InputStream is = null;
		try {
			// Retrieve file name
			fileName = cawp.getSelectedFileName();
			
			// Retrieve file content
			is = getInputStream(fileName);
			
			// Create or Update file into project
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			final IFile file = project.getFile(fileName);
			if (file.exists()) file.setContents(is, true, false, null);
			else file.create(is, true, null);
			
			// Display file
			getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
					IWorkbenchPage page =
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					try {
						IDE.openEditor(page, file, true);
					} catch (PartInitException e) {}
				}
			});
			
		}
		catch (Exception e) {
            String message = "Unable to add file '"+fileName+"' to project.";
            ConvertigoPlugin.logException(e, message);
		}
		finally {
			if (is != null) {
				try {is.close();}
				catch (IOException e) {}
			}
		}
		return true;
	}

	private InputStream getInputStream(String fileName) {
		InputStream is;
		if (fileName.endsWith(".xsl")) {
			File file = new File(Engine.XSL_PATH + "/"+ fileName);
			try {
				is = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				is = new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes());
			}
		}
		else {
			is = new ByteArrayInputStream("".getBytes());
		}
		return is;
	}
}
