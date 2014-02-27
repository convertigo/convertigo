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

package com.twinsoft.convertigo.eclipse.dialogs;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

public class WsReferenceImportDialogComposite extends MyAbstractDialogComposite {

	public ProgressBar progressBar = null;
	public Label labelProgression = null;
	public Combo combo = null;
	public Button browseButton = null;
	/**
	 * @param parent
	 * @param style
	 */
	public WsReferenceImportDialogComposite(Composite parent, int style) {
		super(parent, style);
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.dialogs.MyAbstractDialogComposite#initialize()
	 */
	@Override
	protected void initialize() {
		final Composite container = this;
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.makeColumnsEqualWidth = false;

		container.setLayout(gridLayout);
		
		Label description = new Label(this, SWT.NONE);
		description.setText("Please enter a valid WSDL url or select your WSDL file using the \"Browse\" button:");
		GridData data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		data.grabExcessHorizontalSpace = true;
		description.setLayoutData (data);
		
		combo = new Combo(this, SWT.NONE);
		combo.add("http://www.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?WSDL");
		combo.select(0);
		GridData data0 = new GridData ();
		data0.horizontalAlignment = GridData.FILL;
		data0.grabExcessHorizontalSpace = true;
		combo.setLayoutData (data0);
		
		browseButton = new Button(this, SWT.NONE);
		browseButton.setText("Browse...");
		data0 = new GridData ();
		data0.horizontalAlignment = GridData.FILL;
		data0.grabExcessHorizontalSpace = true;
		combo.setLayoutData (data0);
		
		//Event click browse button
		browseButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(container.getShell(), SWT.NULL);
				dialog.setFilterExtensions(new String[]{"*.wsdl"});
				dialog.setText("Select your WSDL file");
				String path = dialog.open();
				if (path != null) {
					File file = new File(path);
					if (file.isFile()) {
						combo.add("file:///"+file.getAbsolutePath());
						combo.select(combo.getItemCount()-1);
					}
				}
				
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		progressBar = new ProgressBar(this, SWT.NONE);
		GridData data1 = new GridData ();
		data1.horizontalAlignment = GridData.FILL;
		data1.horizontalSpan = 2;
		data1.grabExcessHorizontalSpace = true;
		progressBar.setLayoutData (data1);

		labelProgression = new Label(this, SWT.NONE);
		labelProgression.setText("Progression");
		GridData data2 = new GridData ();
		data2.horizontalAlignment = GridData.FILL;
		data2.horizontalSpan = 2;
		data2.grabExcessHorizontalSpace = true;
		labelProgression.setLayoutData (data2);
		
		setSize(new Point(402, 99));
	}


	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.dialogs.MyAbstractDialogComposite#getValue(java.lang.String)
	 */
	@Override
	public Object getValue(String name) {
		return null;
	}

}
