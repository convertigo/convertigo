/*
 * Copyright (c) 2001-2015 Convertigo SA.
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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.eclipse.swt.widgets.Spinner;

public class LimitCharsLogsPreferenceDialog extends Dialog {

	private int nWidth = 250;
	private int nHeight = 100;
	
	private int MIN_LOG_CHARS = 1000;
	private int MAX_LOG_CHARS = Integer.MAX_VALUE;
	private int limitLogsChars;
	
	public LimitCharsLogsPreferenceDialog(Shell parentShell, int limitLogChars) {
		super(parentShell);
		this.limitLogsChars = limitLogChars;
	}
	
	@Override
	public void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Limit chars logs");
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
	
	@Override
	protected Control createDialogArea(Composite parent) {		
		
		Composite composite = new Composite(parent, SWT.NONE);
		
		Label labelDescription = new Label(composite, SWT.WRAP);
		labelDescription.setText("Limit chars logs");
		
		final Spinner spinnerBox = new Spinner(composite, SWT.WRAP);
		spinnerBox.setMaximum(MAX_LOG_CHARS);
		spinnerBox.setMinimum(MIN_LOG_CHARS);
		spinnerBox.setSelection(limitLogsChars);
		spinnerBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		
		spinnerBox.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				limitLogsChars = Integer.parseInt(spinnerBox.getText());
			}
		});
		
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		composite.setLayout(new GridLayout(2, false));
		
		return composite;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {	
		
		/* APPLY ACTION */
		Button buttonApply = createButton(parent, IDialogConstants.PROCEED_ID, "Apply", true);			
		buttonApply.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				applyProceed();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		buttonApply.setEnabled(true);
	}
	
	private void applyProceed() {	
		setReturnCode(SWT.OK);
		this.close();
	}
	
	public int getLimitLogsChars(){
		return limitLogsChars;
	}
}
