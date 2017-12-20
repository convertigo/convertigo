package com.twinsoft.convertigo.eclipse.editors.mobile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;

public class InstallNpmComposite extends Composite {

	public InstallNpmComposite(Composite parent, int style) {
		super(parent, style);
		
//		setBackground(getDisplay().getSystemColor(SWT.COLOR_RED));
		setLayout(new GridLayout(1, true));
		
		Link link = new Link(this, SWT.WRAP);
		
		link.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		link.setText(
"NodeJS and NPM (≥ 5) are mandatory to use the Mobile Builder.\n" +
"To get both, please use the LTS NodeJS installer from <a href=\"https://nodejs.org\">https://nodejs.org</a>.\n" +
"Install, and then close and re-open this editor."
				);
		
		link.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				org.eclipse.swt.program.Program.launch(e.text);
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {	
			}
			
		});
		
		FontData[] fd = link.getFont().getFontData();
		fd[0].setHeight(18);
		link.setFont(new Font(getDisplay(), fd));
	}

}
