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

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.graphics.Point;

public class GenerateJavaStubDialogComposite extends MyAbstractDialogComposite {

	public ProgressBar progressBar = null;
	public Label labelProgression = null;

	public GenerateJavaStubDialogComposite(Composite parent, int style) {
		super(parent, style);
	}

	protected void initialize() {
		GridData gridData1 = new GridData();
		gridData1.grabExcessHorizontalSpace = true;
		gridData1.verticalAlignment = GridData.FILL;
		gridData1.verticalSpan = 2;
		gridData1.grabExcessVerticalSpace = true;
		gridData1.horizontalAlignment = GridData.FILL;
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = GridData.CENTER;
		progressBar = new ProgressBar(this, SWT.NONE);
		progressBar.setLayoutData(gridData);
		labelProgression = new Label(this, SWT.NONE);
		labelProgression.setText("Progression");
		labelProgression.setLayoutData(gridData1);
		GridLayout gridLayout = new GridLayout();
		this.setLayout(gridLayout);
		setSize(new Point(402, 99));
	}

	public Object getValue(String name) {
		return null;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
