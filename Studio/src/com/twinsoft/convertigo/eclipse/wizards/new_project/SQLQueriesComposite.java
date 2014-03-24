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
 * $URL: http://sourceus.twinsoft.fr/svn/CEMS/trunk/Studio/src/com/twinsoft/convertigo/eclipse/wizards/NewProjectWizardComposite3.java $
 * $Author: fabienb $
 * $Revision: 28379 $
 * $Date: 2011-09-27 11:38:59 +0200 (mar., 27 sept. 2011) $
 */

package com.twinsoft.convertigo.eclipse.wizards.new_project;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class SQLQueriesComposite extends Composite {

	private Text sqlQueries = null;
	private ModifyListener modifyListener;

	public SQLQueriesComposite(Composite parent, int style, ModifyListener modifyListener) {
		super(parent, style);
		this.modifyListener = modifyListener;
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
		Label label = new Label(this, SWT.NONE);
		label.setText("Please enter yours SQL queries\n\n");
		label.setLayoutData(new GridData (GridData.FILL, GridData.CENTER, false, false, 2, 0) );
		
		label = new Label(this, SWT.NONE);
		label.setText("SQL query syntax example:");
		label.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, false, false) );
		
		label = new Label(this, SWT.NONE);
		label.setText("SELECT * FROM EMPLOYEES WHERE (NAME='{parameter_name}')");
		FontData fontData = label.getFont().getFontData()[0];
		Font font = new Font(this.getDisplay(), new FontData(fontData.getName(), fontData
		    .getHeight(), SWT.BOLD));
		label.setFont(font);
		label.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, false, false) );
		
		sqlQueries = new Text(this, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		sqlQueries.setFont(new Font(null, "Tahoma", 10, 0));
		sqlQueries.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		GridLayout gridLayout = new GridLayout();
		gridLayout.verticalSpacing = 10;
		this.setLayout(gridLayout);
	}

	public ModifyListener getModifyListener() {
		return modifyListener;
	}

	public void setModifyListener(ModifyListener modifyListener) {
		this.modifyListener = modifyListener;
	}

	public String getSQLQueries() {
		return sqlQueries.getText();
	}
}