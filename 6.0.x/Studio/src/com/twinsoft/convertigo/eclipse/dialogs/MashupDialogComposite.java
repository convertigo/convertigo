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

import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.twinsoft.convertigo.eclipse.MashupDataViewConfiguration;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class MashupDialogComposite extends MyAbstractDialogComposite {

	private Label description = null;
	private Label label = null;
	public Combo combo = null;
	 
	public MashupDialogComposite(Composite parent, int style) {
		super(parent, style);
	}

	public Object getValue(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	protected void initialize() {
        GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		this.setLayout(gridLayout);
		
		GridData data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.BEGINNING;
		data.grabExcessHorizontalSpace = true;
		description = new Label(this, SWT.NONE);
		description.setText ("This will add or remove a Convertigo DataView in the Mashup Composer\ncatalog. The dataview will be created in the 'Convertigo' category.\n\nSpecify the Dataview name in field below.\n\nYou will be able use the 'Add Mashup Event' or 'Add Mashup Action'\non this DataView later on.\n\n");
		description.setLayoutData(data);
		
        label = new Label(this, SWT.NONE);
        label.setText("Dataview name (from 4 up to 26 letter characters)");
        
        createCombo();
	}

	private void createCombo() {
		GridData gridData1 = new GridData();
		gridData1.horizontalAlignment = GridData.FILL;
		gridData1.verticalAlignment = GridData.CENTER;
		combo = new Combo(this, SWT.NONE);
		combo.setLayoutData(gridData1);
		
		VerifyListener verifyListener = new VerifyListener() {  
			public void verifyText(VerifyEvent e) {
				if (combo.getSelectionIndex() != -1) {
					String text = e.text;
					
				    // Max text length : 26
				    e.doit = text.length() <= 26;

				    // Min text length : 4
				    enableOK(text.length() >= 4);
				}
				else {
					char c = e.character;
					boolean isAllowed = isCharAllowed(c);
					e.doit = isAllowed;
					
					if (isAllowed) {
						StringBuffer buf = new StringBuffer(combo.getText());
						if (Character.isLetter(c) || (c == '_')) buf.append(c);
						else if (c == '\b') buf.deleteCharAt(buf.length()-1);
					    String text = new String(buf);
					    
					    // Max text length : 26
					    e.doit = text.length() <= 26;

					    // Min text length : 4
					    enableOK(text.length() >= 4);
					}
				}
			}
		};  
		combo.addVerifyListener(verifyListener);  
			
	}
	
	private boolean isCharAllowed(char c) {
		if (c == '\b')									// Allow backspace
			return true;
		else if (c == '_')								// Allow underscore
			return true;
		else if ((c >= (char) 65) && (c <= (char) 90))	// Allow uppercase letters
			return true;
		else if ((c >= (char) 97) && (c <= (char) 122))	// Allow lowercase letters
			return true;
		
		return false;
	}
	
	public void fillCombo(MashupDataViewConfiguration mdc) {
		if (mdc != null) {
	        Iterator<String> iterator = GenericUtils.cast(mdc.getDataViews().iterator());
	        while (iterator.hasNext()) {
	            combo.add(iterator.next());
	        }
            combo.select(0);
		}
	}
	
	private void enableOK(boolean enabled) {
		if (parentDialog != null) {
			((MashupDialog)parentDialog).enableOK(enabled);
		}
	}
	

}  //  @jve:decl-index=0:visual-constraint="10,10"
