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

package com.twinsoft.convertigo.eclipse.property_editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

import com.twinsoft.convertigo.engine.parsers.triggers.AbstractTrigger;
import com.twinsoft.convertigo.engine.parsers.triggers.DocumentCompletedTrigger;
import com.twinsoft.convertigo.engine.parsers.triggers.DownloadStartedTrigger;
import com.twinsoft.convertigo.engine.parsers.triggers.NoWaitTrigger;
import com.twinsoft.convertigo.engine.parsers.triggers.ScreenClassTrigger;
import com.twinsoft.convertigo.engine.parsers.triggers.TriggerXMLizer;
import com.twinsoft.convertigo.engine.parsers.triggers.WaitTimeTrigger;
import com.twinsoft.convertigo.engine.parsers.triggers.XpathTrigger;

public class HttpTriggerEditorComposite extends AbstractDialogComposite {

	private Composite commun_trigger = null;
	private Label help_label = null;
	private Composite custom_trigger = null;
	private AbstractHttpTriggerCustomEditorComposite custom_triggers[] = null;
	private Label type_trigger_label = null;
	private Combo type_trigger_combo = null;
	private Label timeout_label = null;
	private Spinner timeout_spin = null;
	private int last_index = -1;
	private AbstractTrigger trigger = null;
	private StackLayout stackLayout = new StackLayout();;
	
	static private String[] customTriggers = {
		"Document completed",
		"Xpath",
		"Wait time",
		"Screen Class",
		"Download started",
		"No wait"
	};
	
	public HttpTriggerEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		parent.setLayout(new GridLayout());
		TriggerXMLizer triggerXML = (TriggerXMLizer) cellEditor.getValue();
		trigger = triggerXML.getTrigger();
		initialize();	
		//setSize(400, 300);
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
        GridData gridData1 = new GridData();
        gridData1.horizontalAlignment = GridData.FILL;
        gridData1.verticalAlignment = GridData.CENTER;
        gridData1.grabExcessHorizontalSpace = true;
        
        GridLayout gridLayout = new GridLayout();
        this.setLayout(gridLayout);
        this.setLayoutData(gridData1);
        
        createCommun_trigger();

        GridData gridData3 = new GridData();
        gridData3.horizontalAlignment = GridData.FILL;
        gridData3.verticalAlignment = GridData.CENTER;
        gridData3.grabExcessHorizontalSpace = true;
        
        help_label = new Label(this, SWT.BORDER | SWT.WRAP);
        help_label.setLayoutData(gridData3);
        
        custom_trigger = new Composite(this,SWT.NONE);
        custom_trigger.setLayout(stackLayout);

//        GridData gridData2 = new GridData();
//        gridData2.horizontalAlignment = GridData.FILL;
//        gridData2.verticalAlignment = GridData.FILL;
//        gridData2.grabExcessVerticalSpace = true;
//        gridData2.grabExcessHorizontalSpace = true;
//        
//        custom_trigger.setLayoutData(gridData2);
        
        custom_triggers = new AbstractHttpTriggerCustomEditorComposite[] {
        	new HttpTriggerDocumentCompletedEditorComposite(this),
        	new HttpTriggerXpathEditorComposite(this),
        	new HttpTriggerWaitTimeEditorComposite(this),
        	new HttpTriggerScreenClassEditorComposite(this),
        	new HttpTriggerDownloadStartedEditorComposite(this),
        	new HttpTriggerNoWaitEditorComposite(this)
        };
        
        Class<?> cl = trigger.getClass();
        if (cl.equals(DocumentCompletedTrigger.class)){
        	type_trigger_combo.select(0);
        } else if (cl.equals(XpathTrigger.class)) {
        	type_trigger_combo.select(1);
        } else if (cl.equals(WaitTimeTrigger.class)) {
        	type_trigger_combo.select(2);
	    } else if (cl.equals(ScreenClassTrigger.class)) {
	    	type_trigger_combo.select(3);
	    } else if (cl.equals(DownloadStartedTrigger.class)) {
	    	type_trigger_combo.select(4);
	    } else if (cl.equals(NoWaitTrigger.class)) {
	    	type_trigger_combo.select(5);
	    }
	}

	/**
	 * This method initializes commun_trigger	
	 *
	 */
	private void createCommun_trigger() {
		commun_trigger = new Composite(this, SWT.NONE);
		
		GridData gridData1 = new GridData();
		gridData1.verticalAlignment = GridData.CENTER;
		gridData1.horizontalAlignment = GridData.FILL;
		gridData1.grabExcessHorizontalSpace = true;
		commun_trigger.setLayoutData(gridData1);
		
		GridLayout gridLayout = new GridLayout();
		gridLayout.makeColumnsEqualWidth = false;
		gridLayout.numColumns = 2;
		commun_trigger.setLayout(gridLayout);		

		type_trigger_label = new Label(commun_trigger, SWT.NONE);		
		type_trigger_label.setText("Type of synchronizer");		
		
		createType_trigger_combo();
		
		timeout_label = new Label(commun_trigger, SWT.NONE);
		timeout_label.setText("Timeout (ms)");
							
		timeout_spin = new Spinner(commun_trigger, SWT.BORDER);
		timeout_spin.setMinimum(0);
		timeout_spin.setMaximum(Integer.MAX_VALUE);
		timeout_spin.setSelection((int) trigger.getTimeout());
		
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.CENTER;
		gridData.grabExcessHorizontalSpace = true;
		timeout_spin.setLayoutData(gridData);
	}

	/**
	 * This method initializes type_trigger_combo	
	 *
	 */
	private void createType_trigger_combo() {
		GridData gridData2 = new GridData();
		gridData2.horizontalAlignment = GridData.FILL;
		gridData2.verticalAlignment = GridData.CENTER;
		gridData2.grabExcessHorizontalSpace = true;
		type_trigger_combo = new Combo(commun_trigger, SWT.READ_ONLY);
		type_trigger_combo.setItems(customTriggers);
		type_trigger_combo
				.addModifyListener(new org.eclipse.swt.events.ModifyListener() {
					public void modifyText(org.eclipse.swt.events.ModifyEvent e) {
						int index = type_trigger_combo.getSelectionIndex();
						if (index == -1) {
							type_trigger_combo.select(0);
						} else if (index != last_index && custom_triggers != null) {
							help_label.setText(custom_triggers[index].getHelp());
					        stackLayout.topControl = custom_triggers[index];
					        custom_trigger.layout();
							last_index = index;
							if (index == 5) { // NoWait case
								timeout_label.setVisible(false);
								timeout_spin.setVisible(false);
							} else {
								timeout_label.setVisible(true);
								timeout_spin.setVisible(true);
							}
						}
					}
				});
		type_trigger_combo.setLayoutData(gridData2);
	}

	public long getTimeout(){
		return (long) timeout_spin.getSelection();
	}
	
	public Object getValue() {
		return custom_triggers[last_index].getTriggerXMLizer();
	}

	public Composite getCustom_trigger() {
		return custom_trigger;
	}

	public AbstractTrigger getTrigger() {
		return trigger;
	}
}