/*
 * Copyright (c) 2009-2014 Convertigo. All Rights Reserved.
 *
 * The copyright to the computer  program(s) herein  is the property
 * of Convertigo.
 * The program(s) may  be used  and/or copied  only with the written
 * permission  of  Convertigo  or in accordance  with  the terms and
 * conditions  stipulated  in the agreement/contract under which the
 * program(s) have been supplied.
 *
 * Convertigo makes  no  representations  or  warranties  about  the
 * suitability of the software, either express or implied, including
 * but  not  limited  to  the implied warranties of merchantability,
 * fitness for a particular purpose, or non-infringement. Convertigo
 * shall  not  be  liable for  any damage  suffered by licensee as a
 * result of using,  modifying or  distributing this software or its
 * derivatives.
 */

/*
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/dialogs/WsReferenceImportLoginPasswordDialog.java $
 * $Author: julienda $
 * $Revision: 36524 $
 * $Date: 2014-02-27 16:30:53 +0100 (jeu., 27 févr. 2014) $
 */

package com.twinsoft.convertigo.eclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class WsReferenceImportLoginPasswordDialog extends Dialog {
	private Label informLabel;
	private Text loginText, passwordText;
	private int nWidth = 300;
	private int nHeight = 170;
	public static String[] basicAuthenticatedValues = new String[2];

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public WsReferenceImportLoginPasswordDialog(Shell parentShell) {
		super(parentShell);
		
		this.setShellStyle(SWT.APPLICATION_MODAL | SWT.YES);
	}

	@Override
	public void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Basic authentification ");
		newShell.setSize(nWidth, nHeight);

		int nLeft = 0;
		int nTop = 0;

		Display display = newShell.getDisplay();

		Point pt = display.getCursorLocation();
		Monitor[] monitors = display.getMonitors();

		for (int i = 0; i < monitors.length; i++) {
			if (monitors[i].getBounds().contains(pt)) {
				Rectangle rect = monitors[i].getClientArea();

				if (rect.x < 0)
					nLeft = ((rect.width - nWidth) / 2) + rect.x;
				else
					nLeft = (rect.width - nWidth) / 2;

				if (rect.y < 0)
					nTop = ((rect.height - nHeight) / 2) + rect.y;
				else
					nTop = (rect.height - nHeight) / 2;

				break;
			}
		}
		newShell.setBounds(nLeft, nTop, nWidth, nHeight);
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = new GridLayout();
		gridLayout.makeColumnsEqualWidth = false;
		
		container.setLayout(gridLayout);
		
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		informLabel = new Label(container, SWT.NONE);
		informLabel.setText("This WSDL URL need an authenfication.\nPlease insert your login/password.");
		informLabel.setLayoutData(data);
		
		data = new GridData(GridData.FILL_HORIZONTAL);
		loginText = new Text(container, SWT.NONE | SWT.BORDER);
		loginText.setMessage("Login");
		loginText.setLayoutData(data);
		
		passwordText = new Text(container, SWT.SINGLE | SWT.PASSWORD | SWT.BORDER);
		passwordText.setMessage("Password");
		passwordText.setLayoutData(data);
		
		return container;
	}
	@Override
	protected void okPressed() {
		basicAuthenticatedValues[0] = loginText.getText();
		basicAuthenticatedValues[1] = passwordText.getText();
		this.close();
	}
	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button buttonOk = createButton(parent, IDialogConstants.OK_ID, "OK",
				true);
		buttonOk.setEnabled(true);
	}
}