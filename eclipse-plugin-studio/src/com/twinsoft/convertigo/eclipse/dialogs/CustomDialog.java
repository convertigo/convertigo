/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class CustomDialog extends MyAbstractDialog {

	private String question;
	private List<ButtonSpec> buttonSpecs;

	public CustomDialog(Shell parentShell, String title, String question, int width, int height,
			ButtonSpec... buttonSpecs) {
		this(parentShell, CustomDialogComposite.class, title, width, height);
		this.question = question;
		this.buttonSpecs = Arrays.asList(buttonSpecs);
	}

	public CustomDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle,
			int width, int height) {
		super(parentShell, dialogAreaClass, dialogTitle, width, height);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);
		((CustomDialogComposite) dialogComposite).setQuestion(question);
		return control;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		for (int i = 0; i < buttonSpecs.size(); i++) {
			createButton(parent, i, buttonSpecs.get(i).label, buttonSpecs.get(i).defaultButton);
		}
	}

	@Override
	protected void buttonPressed(int buttonId) {
		setReturnCode(buttonId);
		close();
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() & (~SWT.RESIZE);
	}
}