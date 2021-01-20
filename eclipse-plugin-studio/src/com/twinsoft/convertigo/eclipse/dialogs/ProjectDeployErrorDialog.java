/*
 * Copyright (c) 2001-2021 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ProjectDeployErrorDialog extends Dialog {
	
	private String errorMessage = null;
	private String stackTrace = null;
	private CLabel clabel = null;
	private Image image = null;
	private Text text = null;

	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public ProjectDeployErrorDialog(Shell parentShell,String errorMessage, String stackTrace) {
		super(parentShell);
		this.errorMessage = errorMessage;
		this.stackTrace = stackTrace;
		this.setShellStyle(SWT.TITLE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		
		newShell.setText("Convertigo");
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		
		container.setLayout(gridLayout);
		
		GridData gridData = new GridData();
		gridData.verticalSpan = 2;
		
		
		image = new Image(parent.getDisplay(), parent.getDisplay().getSystemImage(SWT.ICON_ERROR), 0);
		Label labelImage = new Label(container, SWT.NONE);
		labelImage.setImage(image);
		labelImage.setLayoutData(gridData);
		
		gridData = new GridData();
		gridData.verticalSpan = 2;
		gridData.grabExcessVerticalSpace = true;
		gridData.grabExcessHorizontalSpace = true;

		clabel = new CLabel(container, SWT.WRAP);
		clabel.setText(errorMessage);
		clabel.setLayoutData(gridData);
		
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		
		Label labelTrace = new Label(container, SWT.NONE);
		labelTrace.setText("Stacktrace: ");
		labelTrace.setLayoutData(gridData);
		
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		
		text = new Text(container, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		text.setText(stackTrace);
		text.setLayoutData(gridData);

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
		return new Point(650, 325);
	}
}