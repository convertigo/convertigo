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
		//GridData data = new GridData();
		GridData data = new GridData(GridData.FILL_BOTH);
		//GridData data = new GridData(GridData.FILL_HORIZONTAL);
		labelQuestion = new Label(this, SWT.NONE);

		gridLayout.numColumns = 1;
		setLayout(gridLayout);
		data.grabExcessVerticalSpace = true;
	    data.verticalAlignment = SWT.FILL;
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