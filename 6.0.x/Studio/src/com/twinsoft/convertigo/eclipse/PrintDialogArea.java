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

package com.twinsoft.convertigo.eclipse;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class PrintDialogArea extends Composite {

	private Text fileLocation = null;
	private Label label = null;
	private Combo combo = null;
	private ProgressBar progressBar = null;
	private Label label3 = null;
	private Label progressLabel = null;
	Shell shell;
	

	public PrintDialogArea(Composite parent, int style) {
		super(parent, style);
		shell = this.getShell();
		initialize();
	}

	private void initialize() {	
				
		//table of 3 columns
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		setLayout(gridLayout);
		
		//******************line 1**********************/
		//layout
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		gridData.horizontalSpan = 2;
		
		//content
		label = new Label(this, SWT.NONE);
		label.setText("Output type : ");
				
		combo = new Combo(this, SWT.NONE);	
		combo.add("PDF");	
		combo.add("HTML");
		combo.select(0);	
		combo.setLayoutData(gridData);
		
		//******************line 2**********************/
		//content
		Label labelLocation = new Label(this, SWT.NONE);
		labelLocation.setText("Output location :");

		fileLocation = new Text(this, SWT.BORDER);
		
		Button button = new Button(this, SWT.PUSH);
		button.setText("Browse");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(shell, SWT.NULL);
				dialog.setFilterPath(fileLocation.getText());
				String path = dialog.open();
				System.out.println("path: " + path);
				if (path != null) {
					fileLocation.setText(path);

				}
			}

		});

		//******************line 3**********************/
		//layout
		GridData gridData2 = new org.eclipse.swt.layout.GridData();
		gridData2.horizontalSpan = 2;
		gridData2.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		gridData2.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		
		//content
		label3 = new Label(this, SWT.NONE);
		label3.setText("Progression : ");
		
		progressBar = new ProgressBar(this, SWT.NONE);
		progressBar.setLayoutData(gridData2);
		
		//******************line 4**********************/
		//layout
		GridData gridData3 = new org.eclipse.swt.layout.GridData();
		gridData3.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData3.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		gridData3.horizontalSpan = 2;	
		
				
		new Label(this, SWT.NONE).setText("Status : ");
		
		//content
		progressLabel = new Label(this, SWT.NONE);		
		progressLabel.setLayoutData(gridData3);		
		
		
	}

	public void displayFiles(String[] files) {
		for (int i = 0; files != null && i < files.length; i++) {
			fileLocation.setText(files[i]);
			fileLocation.setEditable(true);
		}
	}



	public Combo getCombo() {
		return combo;
	}

	public void setCombo(Combo combo) {
		this.combo = combo;
	}

	public ProgressBar getProgressBar() {
		return progressBar;
	}

	public void setProgressBar(ProgressBar progressBar) {
		this.progressBar = progressBar;
	}

	public Label getProgressLabel() {
		return progressLabel;
	}

	public void setProgressLabel(Label progressLabel) {
		this.progressLabel = progressLabel;
	}
	

	public String getFileLocation() {
		return fileLocation.getText();
	}

	public void setFileLocation(String locationName) {
		fileLocation.setText(locationName);
	}
	

} // @jve:decl-index=0:visual-constraint="10,10"
