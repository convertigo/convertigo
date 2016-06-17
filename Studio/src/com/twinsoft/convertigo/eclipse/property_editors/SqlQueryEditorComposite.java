/*
 * Copyright (c) 2001-2013 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.property_editors;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridData;

public class SqlQueryEditorComposite extends AbstractDialogComposite {

	private Label labelSyntaxe = null;
	private Text textAreaSQLQuery = null;
	private Label labelSQLQuery = null;

	public SqlQueryEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		initialize();
		
		String sqlQuery = (String)cellEditor.databaseObjectTreeObject.getPropertyValue(cellEditor.propertyDescriptor.getId());
		textAreaSQLQuery.setText(sqlQuery);
	}

	private void initialize() {
		labelSyntaxe = new Label(this, SWT.NONE);
		
		labelSyntaxe.setText("SQL query syntax examples :\n");
		labelSyntaxe.setLayoutData(new GridData(SWT.FILL,SWT.CENTER,true,false));
		labelSQLQuery = new Label(this, SWT.NONE);
		labelSQLQuery.setFont(new Font(null,"Tahoma",8,1));
		labelSQLQuery.setText("SELECT * FROM EMPLOYEES WHERE (NAME='{parameter_name}')\n"
								+ "{? = CALL STORED_FUNCTION({parameter_name})}\n"
								+ "{CALL STORED_PROCEDURE({parameter_name})}\n\n");
		labelSQLQuery.setLayoutData(new GridData(SWT.FILL,SWT.CENTER,true,false));
		textAreaSQLQuery = new Text(this, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		textAreaSQLQuery.setFont(new Font(null,"Tahoma",10,0));
		textAreaSQLQuery.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
		GridLayout gridLayout = new GridLayout();
		this.setLayout(gridLayout);
		setSize(new org.eclipse.swt.graphics.Point(402,289));
	}

	public Object getValue() {
		String sqlQuery = textAreaSQLQuery.getText();
		return sqlQuery;
	}

	@Override
	public void performPostDialogCreation() {
		// mods jmc 26/07/2013
				
		int nWidth;
		int nHeight;
		int nLeft = 0;
		int nTop = 0;
		 
		Shell newShell = this.parentDialog.getShell();

		// mods jmc 22/10/2013
		nWidth = (int) (0.50 * newShell.getSize().x);
		nHeight = (int) (0.60 * newShell.getSize().y);
		Display display = newShell.getDisplay();
		
		Point pt = display.getCursorLocation();
	    Monitor [] monitors = display.getMonitors();

	    for (int i= 0; i<monitors.length; i++) {
	          if (monitors[i].getBounds().contains(pt)) {
	             Rectangle rect = monitors[i].getClientArea();
	             
	             if (rect.x < 0)
	         		nLeft = ((rect.width - nWidth) / 2) + rect.x;
	             else
	         		nLeft = (rect.width - nWidth) / 2;

	             if (rect.y < 0)
	         		nTop = ((rect.height - nHeight) / 2) + rect.y;
	             else
	         		nTop = (rect.height - nHeight) / 2;
	             
	             break;
	          }
	    }

	    newShell.setBounds(nLeft, nTop, nWidth, nHeight);
		
		super.performPostDialogCreation();
	}
}
