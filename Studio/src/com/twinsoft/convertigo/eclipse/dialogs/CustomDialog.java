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