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

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.util.GenericUtils;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.GridData;

public class StepDatesEditorComposite extends AbstractDialogComposite {

	private XMLVector<XMLVector<? extends Object>> definitions = null;
	private DateTime start = null;
	private DateTime stop = null;
	private Group groupDefinition = null;
	private Button checkMonday = null;
	private Button checkTuesday = null;
	private Button checkWednesday = null;
	private Button checkThursday = null;
	private Button checkFriday = null;
	private Button checkSaturday = null;
	private Button checkSunday = null;

	public StepDatesEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		initialize();
		
		//XMLVector xmlv  = (XMLVector)((XMLVector)cellEditor.getEditorData()).clone();
		XMLVector<XMLVector<? extends Object>> xmlv  = GenericUtils.cast(GenericUtils.clone(cellEditor.getEditorData()));
        
		// Set start and stop dates
		if (!xmlv.isEmpty()) {
			XMLVector<String> dates = GenericUtils.cast(xmlv.get(0));
			String startDate = dates.get(0);
			String[] starts = startDate.split("/");
			if (starts.length == 3) {
				start.setDay(Integer.valueOf(starts[0]).intValue());
				start.setMonth(Integer.valueOf(starts[1]).intValue() - 1);
				start.setYear(Integer.valueOf(starts[2]).intValue());
			}

			String stopDate = dates.get(1);
			String[] stops = stopDate.split("/");
			if (stops.length == 3) {
				stop.setDay(Integer.valueOf(stops[0]).intValue());
				stop.setMonth(Integer.valueOf(stops[1]).intValue() - 1);
				stop.setYear(Integer.valueOf(stops[2]).intValue());
			}
		}

		// Set days
		if (!xmlv.isEmpty()) {
			XMLVector<Boolean> days = GenericUtils.cast(xmlv.get(1));
	        checkMonday.setSelection(days.get(0).booleanValue());
	        checkTuesday.setSelection(days.get(1).booleanValue());
	        checkWednesday.setSelection(days.get(2).booleanValue());
	        checkThursday.setSelection(days.get(3).booleanValue());
	        checkFriday.setSelection(days.get(4).booleanValue());
	        checkSaturday.setSelection(days.get(5).booleanValue());
	        checkSunday.setSelection(days.get(6).booleanValue());
		}
		
	}

	private void initialize() {
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		createCalendars();
		createGroupDefinition();
		this.setLayout(gridLayout);
		this.setSize(new org.eclipse.swt.graphics.Point(264,190));
	}
	
	public Object getValue() {
		
		definitions = new XMLVector<XMLVector<? extends Object>>();
		
		// Add start and stop dates
		XMLVector<String> dates = new XMLVector<String>();
		dates.add(start.getDay() + "/" + String.valueOf(start.getMonth() + 1) + "/" + start.getYear());
		dates.add(stop.getDay() + "/" + String.valueOf(stop.getMonth() + 1) + "/" + stop.getYear());
		definitions.add(dates);
		
		// Add day(s)
		XMLVector<Boolean> days = new XMLVector<Boolean>();
        days.add(checkMonday.getSelection() ? Boolean.TRUE:Boolean.FALSE);
        days.add(checkTuesday.getSelection() ? Boolean.TRUE:Boolean.FALSE);
        days.add(checkWednesday.getSelection() ? Boolean.TRUE:Boolean.FALSE);
        days.add(checkThursday.getSelection() ? Boolean.TRUE:Boolean.FALSE);
        days.add(checkFriday.getSelection() ? Boolean.TRUE:Boolean.FALSE);
        days.add(checkSaturday.getSelection() ? Boolean.TRUE:Boolean.FALSE);
        days.add(checkSunday.getSelection() ? Boolean.TRUE:Boolean.FALSE);
        definitions.add(days);
        
        return definitions;
	}

	private void createCalendars() {
		Label labelStart = new Label(this, SWT.NONE);
		labelStart.setText("Date de début");
		start = new DateTime(this, SWT.DATE | SWT.LONG);
		
		Label labelStop = new Label(this, SWT.NONE);
		labelStop.setText("Date de fin");
		stop = new DateTime(this, SWT.DATE | SWT.LONG);
	}
	
	/**
	 * This method initializes groupDefinition	
	 *
	 */
	private void createGroupDefinition() {
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		gridData.horizontalSpan = 2;
		
		groupDefinition = new Group(this, SWT.NONE);
		groupDefinition.setText("Jour(s)");
		groupDefinition.setLayoutData(gridData);
		
		// first line
		checkMonday = new Button(groupDefinition, SWT.CHECK);
		checkMonday.setBounds(new org.eclipse.swt.graphics.Rectangle(8,18,42,16));
		checkMonday.setText("Lundi");
		
		checkTuesday = new Button(groupDefinition, SWT.CHECK);
		checkTuesday.setBounds(new org.eclipse.swt.graphics.Rectangle(54,18,42,16));
		checkTuesday.setText("Mardi");
		
		checkWednesday = new Button(groupDefinition, SWT.CHECK);
		checkWednesday.setBounds(new org.eclipse.swt.graphics.Rectangle(100,18,60,16));
		checkWednesday.setText("Mercredi");
		
		checkThursday = new Button(groupDefinition, SWT.CHECK);
		checkThursday.setBounds(new org.eclipse.swt.graphics.Rectangle(160,18,42,16));
		checkThursday.setText("Jeudi");
		
		checkFriday = new Button(groupDefinition, SWT.CHECK);
		checkFriday.setBounds(new org.eclipse.swt.graphics.Rectangle(204,18,60,16));
		checkFriday.setText("Vendredi");

		checkSaturday = new Button(groupDefinition, SWT.CHECK);
		checkSaturday.setBounds(new org.eclipse.swt.graphics.Rectangle(268,18,50,16));
		checkSaturday.setText("Samedi");
		
		checkSunday = new Button(groupDefinition, SWT.CHECK);
		checkSunday.setBounds(new org.eclipse.swt.graphics.Rectangle(322,18,60,16));
		checkSunday.setText("Dimanche");
}


}  //  @jve:decl-index=0:visual-constraint="10,10"
