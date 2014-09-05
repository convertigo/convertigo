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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/dialogs/ProjectDeploySuccessfulDialog.java $
 * $Author: nicolasa $
 * $Revision: 32996 $
 * $Date: 2012-12-13 17:34:57 +0100 (jeu., 13 déc. 2012) $
 */

package com.twinsoft.convertigo.eclipse.dialogs;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.engine.Engine;

public class BuildLocalSuccessfulDialog extends Dialog {
	
	private String applicationBuildedPath;
	private String applicationName;
	private String cordovaPlatform;
	
	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public BuildLocalSuccessfulDialog(Shell parentShell, String applicationBuildedPath, 
			String applicationName, String cordovaPlatform) {
		super(parentShell);
		this.applicationBuildedPath = applicationBuildedPath;
		this.applicationName = applicationName;
		this.cordovaPlatform = cordovaPlatform;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Build local successful");
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, true));
		
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		
		Label label = new Label(container, SWT.NONE);
		label.setText("Application \"" + applicationName
				+ "\" has been successfully builded locally."
				+ "\nThe builded file for \""+cordovaPlatform+"\" platform is located here :");
		label.setLayoutData(data);
		
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		
		Text absolutePath = new Text(container, SWT.NONE);
		absolutePath.setText(applicationBuildedPath);
		absolutePath.setEditable(false);
		absolutePath.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		absolutePath.setLayoutData(data);

		if (cordovaPlatform.equals("ios")){
			Label iosNotify = new Label(container, SWT.NONE);
			data = new GridData(GridData.FILL_HORIZONTAL);
			data.grabExcessHorizontalSpace = true;
			
			iosNotify.setText("\nTo generate your \"ipa\" file you need to open the \".xcodeproj\" with Xcode \napplication and go to the menu \"Product>Archive\".");
			iosNotify.setLayoutData(data);
		}
		
		return container;
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button button = createButton(parent, IDialogConstants.OK_ID, "OK", true);
		button.setEnabled(true);
		
		if (!cordovaPlatform.equals(null) && cordovaPlatform.equals("ios")) {
			Button openXcode = createButton(parent, IDialogConstants.OPEN_ID, "OPEN XCODE", true);
			openXcode.setEnabled(true);
			openXcode.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					//TODO
					try {
						Runtime runtime = Runtime.getRuntime();
						File buildedFile = new File(applicationBuildedPath);
						if (buildedFile.exists()) {
							runtime.exec( "open " + applicationBuildedPath );
						}
						
					} catch (IOException e1) {
						Engine.logEngine.error("Error when trying to open the xcode project:\n" + 
								e1.getMessage(), e1);
					} finally {
						close();
					}
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {}
			});
		}
	}

}
