/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

import java.io.File;
import java.util.Set;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class StubSaveDialogComposite extends MyAbstractDialogComposite {
	private Label status = null;
	private Combo combo = null;

	public StubSaveDialogComposite(Composite parent, int style) {
		super(parent, style);
	}

	@Override
	protected void initialize() {
		super.initialize();

		Label label = new Label(this, SWT.NONE);
		label.setText("File name:");

		GridData gridData = new GridData();
		gridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
		combo = new Combo(this, SWT.BORDER);
		combo.setLayoutData(gridData);
		combo.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				comboChanged();
			}
		});
		
		status = new Label(this, SWT.NONE);
	}

	protected void fillCombo(Set<String> set, String extension, String selection) {
		if (combo != null) {
			int i = 0, index = -1;
			for (String name : set) {
				if (name.endsWith(extension)) {
					combo.add(new File(name).getName());
					if (name.equals(selection)) {
						index = i;
					}
				}
				i++;
			}
			combo.select(index);
			combo.setText(selection);
		}
	}

	public void comboChanged() {
		String text = combo.getText();
		
		String message = null;
		if (text.isBlank()) {
			message = "empty name is not allowed !";
		} else if (!text.endsWith(".xml")) {
			message = ".xml extension is required !";
		}
		
		status.setText(message == null ? "" : message);
		status.pack();
		
		((StubSaveDialog) parentDialog).getButtonOK().setEnabled(message == null ? true : false);
	}
	
	@Override
	public Object getValue(String name) {
		return combo.getText();
	}
}
