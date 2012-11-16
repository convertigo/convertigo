package com.twinsoft.convertigo.eclipse.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class CustomDialogComposite extends MyAbstractDialogComposite {

	protected int response;
	private String question = "";
	private Label labelQuestion;

	public CustomDialogComposite(Composite parent, int style) {
		super(parent, style);
	}

	@Override
	public void initialize() {
		GridLayout gridLayout = new GridLayout();
		GridData data = new GridData();
		labelQuestion = new Label(this, SWT.NONE);

		gridLayout.numColumns = 1;
		setLayout(gridLayout);
		data.horizontalAlignment = SWT.FILL;
		data.verticalAlignment = SWT.TOP;
		labelQuestion.setText(question);
		labelQuestion.setLayoutData(data);
	}

	@Override
	public Object getValue(String name) {
		return response;
	}
	
	public void setQuestion(String question) {
		this.question = question;
		labelQuestion.setText(question);
	}
} // @jve:decl-index=0:visual-constraint="10,10"