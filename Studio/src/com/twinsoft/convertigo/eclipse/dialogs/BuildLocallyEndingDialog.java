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
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.beans.mobileplatforms.IOs;
import com.twinsoft.convertigo.engine.Engine;

public class BuildLocallyEndingDialog extends Dialog {
	
	private File applicationBuilt;
	private MobilePlatform mobilePlatform;
	private int exitValue;
	private String errorLines;
	
	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public BuildLocallyEndingDialog(Shell parentShell, File applicationBuilt, int exitValue, String errorLines, MobilePlatform mobilePlatform) {
		super(parentShell);
		this.applicationBuilt = applicationBuilt;
		this.mobilePlatform = mobilePlatform;
		this.exitValue = exitValue;
		this.errorLines = errorLines;
		setBlockOnOpen(false);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		if (exitValue == 0){
			newShell.setText("Build local successful");
		} else {
			newShell.setText("An error occurred!");
		}
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		String applicationName = mobilePlatform.getParent().getApplicationName();
		
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, true));
		
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		
		String message = null;
		Label label = new Label(container, SWT.NONE);
		
		//Normal ending
		if (exitValue == 0){
			if (!applicationName.isEmpty()) {
				message = "Application \"" + applicationName
					+ "\" has been successfully built locally."
					+ "\nThe built file for \"" + mobilePlatform.getCordovaPlatform() + "\" platform is located here:";
			} else {
				message = "Application from the Convertigo project \"" + mobilePlatform.getProject().getName()
						+ "\" has been successfully built locally."
						+ "\nThe built file for \"" + mobilePlatform.getCordovaPlatform() + "\" platform is located here:";
			}
		//Error ending
		} else {
			if (!applicationName.isEmpty()) {
				message = "An error occurred on the \"" + applicationName + "\" application during the \"Local build\"!";
			} else {
				message = "An error occurred on the application from the Convertigo project \"" + mobilePlatform.getProject().getName() 
						+ "\" during the \"Local build\"!";
			}
		}
		label.setText(message);
		label.setLayoutData(data);
		
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		
		if (exitValue == 0) {
			String href = applicationBuilt.getParentFile().getAbsolutePath();
			String text = applicationBuilt.getAbsolutePath();
			
			try {
				href = applicationBuilt.getParentFile().getCanonicalPath();
				text = applicationBuilt.getCanonicalPath();
			} catch (IOException e1) {}
			
			Text absolutePath = new Text(container, SWT.NONE);
			absolutePath.setText(text);
			absolutePath.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
			absolutePath.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
			absolutePath.setEditable(false);
			absolutePath.setLayoutData(data);
			
			data = new GridData(GridData.FILL_HORIZONTAL);
			data.grabExcessHorizontalSpace = true;
			
			Link link = new Link(container, SWT.WRAP);
			
			link.setText("<a href=\"" + href + "\">Click here to open the parent folder.</a>");
			link.setLayoutData(data);
			link.addSelectionListener(new SelectionListener() {
				
				public void widgetSelected(SelectionEvent e) {
					org.eclipse.swt.program.Program.launch(e.text);
				}
				
				public void widgetDefaultSelected(SelectionEvent e) {	
				}
				
			});
			
			if (mobilePlatform instanceof IOs) {
				Label iosNotify = new Label(container, SWT.NONE);
				data = new GridData(GridData.FILL_HORIZONTAL);
				data.grabExcessHorizontalSpace = true;
				
				iosNotify.setText("\nTo generate your \"ipa\" file you need to open the \".xcodeproj\" with Xcode \napplication and go to the menu \"Product>Archive\".");
				iosNotify.setLayoutData(data);
			}
		
		} else if (errorLines != null && !errorLines.equals("")){
			Text absolutePath = new Text(container, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
			absolutePath.setText(errorLines);
			absolutePath.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			absolutePath.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
			absolutePath.setEditable(false);
			absolutePath.setLayoutData(data);
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
		
		if (mobilePlatform instanceof IOs && exitValue == 0) {
			Button openXcode = createButton(parent, IDialogConstants.OPEN_ID, "Open Xcode", true);
			openXcode.setEnabled(true);
			openXcode.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {

					try {
						if (applicationBuilt.exists()) {
							new ProcessBuilder("open", applicationBuilt.getCanonicalPath()).start();
						}
						
					} catch (IOException e1) {
						Engine.logEngine.error("Error when trying to open the xcode project:\n" + e1.getMessage(), e1);
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
