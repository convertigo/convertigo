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

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Parameter;

public class JavelinMashupEventEditorComposite extends AbstractDialogComposite {
	private Project project = null;
	private String value = null;
	private String jsonConnectorName = null;
	private String jsonTransactionName = null;
	private String jsonSequenceName = null;
	private String jsonEventName = null;
	
	private Button btnTransaction;
	private Button btnSequence;
	private Label con_label;
	private Label req_label;
	private Combo con_combo;
	private Combo req_combo;
	private Text evt_text;
	
	public JavelinMashupEventEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		project = cellEditor.databaseObjectTreeObject.getProjectTreeObject().getObject();
		value = (String) cellEditor.databaseObjectTreeObject.getPropertyValue(cellEditor.propertyDescriptor.getId());
		initJSON();
		initialize();
	}

	private void initialize() {
		setLayout(new GridLayout());
		
		GridLayout rGridLayout = new GridLayout();
		rGridLayout.numColumns = 2;
		GridData rGridData = new GridData();
		rGridData.horizontalAlignment = GridData.FILL;
		rGridData.verticalAlignment = GridData.CENTER;
		rGridData.grabExcessHorizontalSpace = true;
		Group rGroup = new Group(this, SWT.NONE);
		rGroup.setText("Requestable to call (optional)");
		rGroup.setLayout(rGridLayout);
		rGroup.setLayoutData(rGridData);
		
		SelectionListener btnSelectionListener = new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent event) {
			}
			public void widgetSelected(SelectionEvent event) {
				fillConnectorCombo();
			}
		};
		
		btnTransaction = new Button(rGroup, SWT.RADIO);
		btnTransaction.setText("transaction");
		btnTransaction.addSelectionListener(btnSelectionListener);
		
		btnSequence = new Button(rGroup, SWT.RADIO);
		btnSequence.setText("sequence");
		btnSequence.addSelectionListener(btnSelectionListener);
		
		SelectionListener conSelectionListener = new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent event) {
			}
			public void widgetSelected(SelectionEvent event) {
				fillRequestableCombo();
			}
		};
		
		GridData cGridData = new GridData();
		cGridData.horizontalAlignment = GridData.FILL;
		cGridData.verticalAlignment = GridData.CENTER;
		cGridData.grabExcessHorizontalSpace = true;
		cGridData.minimumWidth = 200;
		
		con_label = new Label(rGroup, SWT.NONE);
		con_label.setText("Connector");
		con_combo = new Combo(rGroup, SWT.NONE | SWT.READ_ONLY);
		con_combo.addSelectionListener(conSelectionListener);
		con_combo.setLayoutData(cGridData);
		
		req_label = new Label(rGroup, SWT.NONE);
		req_label.setText("Transaction");
		req_combo = new Combo(rGroup, SWT.NONE | SWT.READ_ONLY);
		req_combo.setLayoutData(cGridData);
		
		GridLayout eGridLayout = new GridLayout();
		eGridLayout.numColumns = 2;
		GridData eGridData = new GridData();
		eGridData.horizontalAlignment = GridData.FILL;
		eGridData.verticalAlignment = GridData.CENTER;
		eGridData.grabExcessHorizontalSpace = false;
		Group eGroup = new Group(this, SWT.NONE);
		eGroup.setText("Event to fire (optional) ");
		eGroup.setLayout(eGridLayout);
		eGroup.setLayoutData(eGridData);

		GridData vGridData = new GridData();
		vGridData.horizontalAlignment = GridData.FILL;
		vGridData.verticalAlignment = GridData.CENTER;
		vGridData.grabExcessHorizontalSpace = true;
		vGridData.minimumWidth = 200;
		
		Label evt_label = new Label(eGroup, SWT.NONE);
		evt_label.setText("Event name   ");
		evt_text = new Text(eGroup, SWT.BORDER);
		evt_text.setLayoutData(vGridData);
		evt_text.setText(jsonEventName == null ? "":jsonEventName);
		
		boolean btnSeqSelected = (jsonSequenceName!=null) && (!jsonSequenceName.equals(""));
		btnTransaction.setSelection(!btnSeqSelected);
		btnSequence.setSelection(btnSeqSelected);
		fillConnectorCombo();
	}
	
	private void fillConnectorCombo() {
		if (project != null) {
			int index = -1;
			con_combo.removeAll();
			con_combo.add("");
			if (btnTransaction.getSelection()) {
				con_combo.setEnabled(true);
				req_label.setText("Transaction");
				for (Connector connector: project.getConnectorsList())
					con_combo.add(connector.getName());
				if (jsonConnectorName != null)
					index = con_combo.indexOf(jsonConnectorName);
			}
			if (btnSequence.getSelection()) {
				con_combo.setEnabled(false);
				req_label.setText("Sequence");
			}
			con_combo.select(index>=0 ? index:0);
		}
		fillRequestableCombo();
	}
	
	private void fillRequestableCombo() {
		if (project != null) {
			int index = -1;
			req_combo.removeAll();
			if (getComboConnectorName().equals("")) req_combo.add("");
			if (btnTransaction.getSelection()) {
				try {
					Connector connector = project.getConnectorByName(getComboConnectorName());
					for (Transaction transaction: connector.getTransactionsList())
						req_combo.add(transaction.getName());
					if (jsonTransactionName != null)
						index = req_combo.indexOf(jsonTransactionName);
				} catch (EngineException e) {
				}
			}
			if (btnSequence.getSelection()) {
				for (Sequence sequence: project.getSequencesList())
					req_combo.add(sequence.getName());
				if (jsonSequenceName != null)
					index = req_combo.indexOf(jsonSequenceName);
			}
			req_combo.select(index>=0 ? index:0);
		}
	}
	
	private String getComboConnectorName() {
		try {
			int index = con_combo.getSelectionIndex();
			return con_combo.getItem(index);
		}
		catch (Exception e) {
			return "";
		}
	}
	
	private String getComboRequestableName() {
		try {
			int index = req_combo.getSelectionIndex();
			return req_combo.getItem(index);
		}
		catch (Exception e) {
			return "";
		}
	}
	
	private String getTextEventName() {
		return evt_text.getText();
	}

	private void initJSON() {
		// Handles empty value
		if (value.equals(""))
			return;
		
		// Handles old value (only event name)
		if (!value.startsWith("{") && !value.endsWith("}")) {
			jsonEventName = value;
			return;
		}
		
		// Handles json value
		try {
			JSONObject jsonob = new JSONObject(value);
			JSONObject jsonr = getJSONObject(jsonob, "requestable");
			jsonConnectorName = getJSONString(jsonr,Parameter.Connector.getName());
			jsonTransactionName = getJSONString(jsonr,Parameter.Transaction.getName());
			jsonSequenceName = getJSONString(jsonr,Parameter.Sequence.getName());
			jsonEventName = getJSONString(jsonob,"event");
		} catch (JSONException e) {
			ConvertigoPlugin.logException(e, "Unable to retrieve mashup event value", Boolean.TRUE);
		}
	}
	
	private JSONObject getJSONObject(JSONObject json, String key) {
		try {
			return json.getJSONObject(key);
		} catch (Exception e) {}
		return null;
	}

	private String getJSONString(JSONObject json, String key) {
		try {
			return json.getString(key);
		} catch (Exception e) {}
		return null;
	}
	
	private String getJSONValue() {
		String connectorName = getComboConnectorName();
		String requestableName = getComboRequestableName();
		String eventName = getTextEventName();
		
		if (connectorName.equals("") && requestableName.equals("") && eventName.equals(""))
			return "";
		
		try {
			JSONObject jsonob = new JSONObject();
			if (!connectorName.equals("") || !requestableName.equals("")) {
				JSONObject jsonr = new JSONObject();
				if (!requestableName.equals(""))
					jsonr.accumulate((connectorName.equals("") ? Parameter.Sequence.getName():Parameter.Transaction.getName()), requestableName);
				if (!connectorName.equals(""))
					jsonr.accumulate(Parameter.Connector.getName(), connectorName);
				jsonob.accumulate("requestable", jsonr);
			}
			if (!eventName.equals(""))
				jsonob.accumulate("event", eventName);
			
			return jsonob.toString();
			
		} catch (JSONException e) {
			ConvertigoPlugin.logException(e, "Unable to set mashup event value", Boolean.TRUE);
		}
		return value;
	}
	
	public Object getValue() {
		return getJSONValue();
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
