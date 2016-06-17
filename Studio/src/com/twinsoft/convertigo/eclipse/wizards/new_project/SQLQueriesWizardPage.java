/*
 * Copyright (c) 2001-2014 Convertigo SA.
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
 * $URL: http://sourceus.twinsoft.fr/svn/CEMS/trunk/Studio/src/com/twinsoft/convertigo/eclipse/wizards/SQLQueriesWizardPage.java $
 * $Author: julienda $
 */

package com.twinsoft.convertigo.eclipse.wizards.new_project;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class SQLQueriesWizardPage extends WizardPage {
	
	private Text sqlQueries = null;
	private Composite container;

	public SQLQueriesWizardPage() {
		super("SQLQueriesWizardPage");
		setTitle("Define SQL queries");
		setDescription("This step configures SQL queries");
	}
	
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NULL);
		GridLayout gridLayout = new GridLayout();
		gridLayout.verticalSpacing = 10;
		container.setLayout(gridLayout);
		
		Label label = new Label(container, SWT.NONE);
		label.setText("Please enter your SQL query\n\n");
		label.setLayoutData(new GridData (GridData.FILL, GridData.CENTER, false, false, 2, 0) );
		
		label = new Label(container, SWT.NONE);
		label.setText("SQL query syntax examples :");
		label.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, false, false) );
		
		label = new Label(container, SWT.NONE);
		label.setText("SELECT * FROM EMPLOYEES WHERE (NAME='{parameter_name}')\n"
				+ "{? = CALL STORED_FUNCTION({parameter_name})}\n"
				+ "{CALL STORED_PROCEDURE({parameter_name})}");
		FontData fontData = label.getFont().getFontData()[0];
		Font font = new Font(container.getDisplay(), new FontData(fontData.getName(), fontData
		    .getHeight(), SWT.BOLD));
		label.setFont(font);
		label.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, false, false) );
		
		sqlQueries = new Text(container, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		sqlQueries.setFont(new Font(container.getDisplay(), "Tahoma", 10, 0));
		sqlQueries.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		setControl(container);
	}
		
	public String getSQLQueries() {
		final String[] queries = new String[1];
		container.getDisplay().syncExec(new Runnable() {
			public void run() {
				queries[0] = sqlQueries.getText();
			}
		});
		return queries[0];
	}
	
	@Override
	public IWizardPage getNextPage() {
		return null;
	}
}
