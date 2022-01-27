/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;

public class DatabaseObjectFindDialogComposite extends MyAbstractDialogComposite {

	private Label label1 = null;
	private Combo combo = null;
	private Label label2 = null;
	private Text text = null;
	private Button checkBox1 = null;
	private Button checkBox2 = null;

	public DatabaseObjectFindDialogComposite(Composite parent, int style) {
		super(parent, style);
	}

	protected void initialize() {
		GridData gridData3 = new GridData();
		gridData3.grabExcessHorizontalSpace = false;
		GridData gridData2 = new GridData();
		gridData2.horizontalAlignment = GridData.BEGINNING;
		gridData2.grabExcessHorizontalSpace = false;
		gridData2.horizontalSpan = 2;
		gridData2.verticalAlignment = GridData.CENTER;
		GridData gridData = new GridData();
		label1 = new Label(this, SWT.NONE);
		label1.setText("Object type");
		label1.setLayoutData(gridData);
		createCombo();
		label2 = new Label(this, SWT.NONE);
		label2.setText("Substring");
		text = new Text(this, SWT.BORDER);
		text.setLayoutData(gridData3);
		checkBox1 = new Button(this, SWT.CHECK);
		checkBox1.setText("Match case");
		//checkBox1.setLayoutData(gridData2);
		checkBox2 = new Button(this, SWT.CHECK);
		checkBox2.setText("Regular expression");
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		this.setLayout(gridLayout);
		setSize(new Point(226, 125));
		
		combo.select(0);
		text.setText("");
	}

	/**
	 * This method initializes combo	
	 *
	 */
	private void createCombo() {
		GridData gridData1 = new GridData();
		gridData1.grabExcessHorizontalSpace = false;
		gridData1.verticalAlignment = GridData.CENTER;
		gridData1.horizontalAlignment = GridData.BEGINNING;
		combo = new Combo(this, SWT.NONE);
		combo.setLayoutData(gridData1);
		combo.add("*");
		combo.add("Mobile component");
		combo.add("Screen class");
		combo.add("Criteria");
		combo.add("Extraction rule");
		combo.add("Sheet");
		combo.add("Transaction");
		combo.add("Statement");
		combo.add("Sequence");
		combo.add("Step");
	}
	
	public int getObjectType() {
		return combo.getSelectionIndex();
	}
	
	public String getSubstring() {
		return text.getText();
	}
	
	public boolean matchCase() {
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
	
}  //  @jve:decl-index=0:visual-constraint="10,10"
