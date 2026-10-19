/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.jsonpicker;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.Step;

/** The JSON picker on a step: its JSON and the JSON of the steps before it, to copy an expression or a JSON path. */
public class JsonPickerDialog extends Dialog {
	private static final int COPY = IDialogConstants.CLIENT_ID + 1;
	private final Step step;
	private JsonPickerComposite picker;

	public JsonPickerDialog(Shell parent, Step step) {
		super(parent);
		this.step = step;
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("JSON picker - " + step.getName());
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		picker = new JsonPickerComposite(area, step, null, true);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 1000;
		gd.heightHint = 560;
		picker.setLayoutData(gd);
		picker.selectStep(step);
		return area;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, COPY, "Copy expression", false);
		createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == COPY) {
			Clipboard clipboard = new Clipboard(getShell().getDisplay());
			clipboard.setContents(new Object[] { picker.getExpression() }, new Transfer[] { TextTransfer.getInstance() });
			clipboard.dispose();
		} else {
			close();
		}
	}

	public JsonPickerComposite getPicker() {
		return picker;
	}
}
