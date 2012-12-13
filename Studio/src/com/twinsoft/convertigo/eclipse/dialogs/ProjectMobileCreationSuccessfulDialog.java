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

package com.twinsoft.convertigo.eclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ProjectMobileCreationSuccessfulDialog extends Dialog {
	
	private String projectURL;
	private String projectName;

	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public ProjectMobileCreationSuccessfulDialog(Shell parentShell, String projectURL, String projectName) {
		super(parentShell);
		this.projectURL = projectURL;
		this.projectName = projectName;
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		
		newShell.setText("Application creation successful");
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		
		Label label = new Label(container, SWT.NONE);
		
		String txt = "The project "+ projectName +" has been created successfully.\r\n" +
				"This project is based on a fully functional application template you can edit and modify as needed.\r\n" +
				"For your convenience, we created a default HTML connector and two sample sequences serving some sample data to this application.\r\n" +
				"You are free to delete any of these objects to suit your needs.\r\n" +
				"You can edit the application sources by clicking on the Project View and opening the DisplayObject/mobile/sources directory.\r\n";
		
		label.setText(txt);
		
		Link link = new Link(container, SWT.NONE);
		link.setText("You can try or build this application by <a href=\""+ projectURL + "\">accessing the following address :</a>");
		link.addListener (SWT.Selection, new Listener () {
			public void handleEvent(Event event) {
				org.eclipse.swt.program.Program.launch(event.text);
			}
		});
		
		link.setSize(330, 150);
		
		Text text = new Text(container, SWT.READ_ONLY);
		text.setEditable(false);
		text.setText(projectURL + "\n");
		
		GridData gridData = new GridData();
		gridData.verticalIndent = 3;
		gridData.horizontalIndent = 50;
		gridData.horizontalSpan = 2;
		text.setLayoutData(gridData);

		Label label1 = new Label(container, SWT.NONE);
		label1.setText("Be sure to use a WebKit based browser.");
	
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
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(750, 240);
	}

}
