/*
 * Copyright (c) 2001-2023 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

class DatabaseObjectFindDialogComposite extends MyAbstractDialogComposite {
	
	private Combo combo = null;
	private Text text = null;
	private Button checkBox1 = null;
	private Button checkBox2 = null;

	public DatabaseObjectFindDialogComposite(Composite parent, int style) {
		super(parent, style);
	}

	protected void initialize() {
		setLayout(new GridLayout(2, false));
		
		Label label = new Label(this, SWT.NONE);
		label.setText("Object type");
		
		createCombo();
		
		label = new Label(this, SWT.NONE);
		label.setText("Substring");
		text = new Text(this, SWT.BORDER);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		checkBox1 = new Button(this, SWT.CHECK);
		checkBox1.setText("Match case");
		
		checkBox2 = new Button(this, SWT.CHECK);
		checkBox2.setText("Regular expression");
		
		combo.select(0);
		text.setText("");
	}

	/**
	 * This method initializes combo	
	 *
	 */
	private void createCombo() {
		combo = new Combo(this, SWT.NONE);
		combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		combo.add("*");
		combo.add("Mobile component");
		combo.add("Screen class");
		combo.add("Criteria");
		combo.add("Extraction rule");
		combo.add("Sheet");
		combo.add("Transaction");
		combo.add("Sequence");
		combo.add("Step");
	}
	
	public int getObjectType() {
		return combo.getSelectionIndex();
	}
	
	public String getSubstring() {
		return text.getText();
	}
	
	private boolean matchCase() {
		return checkBox1.getSelection();
	}

	public boolean isRegExp() {
		return checkBox2.getSelection();
	}
	
	public Object getValue(String name) {
		if (name.equals("ObjectType"))
			return String.valueOf(getObjectType());
		if (name.equals("Substring"))
			return getSubstring();
		if (name.equals("matchCase"))
			return Boolean.valueOf(matchCase());
		if (name.equals("isRegExp"))
			return Boolean.valueOf(isRegExp());
		return null;
	}
	
}
