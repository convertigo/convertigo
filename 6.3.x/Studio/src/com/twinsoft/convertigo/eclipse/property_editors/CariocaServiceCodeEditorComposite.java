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

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;

public class CariocaServiceCodeEditorComposite extends AbstractDialogComposite {

	private Label labelHelp = null;
	private Label labelConnectionParameter = null;
	private Text textConnectionParameter = null;
	private Label labelHost = null;
	private Text textHost = null;
	private Label labelPort = null;
	private Text textPort = null;
	private Label labelConnectionType = null;
	private Combo comboConnectionType = null;

	public CariocaServiceCodeEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		initialize();
		
		String cariocaServiceCode = (String) cellEditor.databaseObjectTreeObject.getPropertyValue(cellEditor.propertyDescriptor.getId());
		
		try {
			int idxComa = cariocaServiceCode.indexOf(',');
			int idxPipe = cariocaServiceCode.indexOf('|');
			int idxColon = cariocaServiceCode.indexOf(':');
			
			String connectionParameter = "";
			String host = "";
			String port = "";
			String connectionType = "";
			
			if (idxComa == -1 && idxPipe == -1) {
				// no , or | ==> no connection type nor host ==> only connection parameter
				connectionParameter = cariocaServiceCode;
			} else {
				// standard case
				if (idxComa != -1) {
					connectionParameter = cariocaServiceCode.substring(0, idxComa);
				}
				if (idxPipe != -1) {
					connectionType = cariocaServiceCode.substring(idxComa + 1, idxPipe);
					if (idxColon != -1) {
						host = cariocaServiceCode.substring(idxPipe + 1, idxColon);
						port = cariocaServiceCode.substring(idxColon + 1);
					} else {
						host = cariocaServiceCode.substring(idxPipe + 1);
					}
				} else {
					if (idxColon != -1) {
						host = cariocaServiceCode.substring(0, idxColon);
						port = cariocaServiceCode.substring(idxColon + 1);
					} else {
						host = cariocaServiceCode;
					}
				}
			}
			// filling composite fields with values
			textConnectionParameter.setText(connectionParameter);
			textHost.setText(host);
			textPort.setText(port);
			comboConnectionType.select(comboConnectionType.indexOf(connectionType));
		}
		catch(StringIndexOutOfBoundsException e) {
			textConnectionParameter.setText("");
			textHost.setText(cariocaServiceCode);
			comboConnectionType.select(0);
		}
	}

	private void initialize() {
		GridData gridData2 = new org.eclipse.swt.layout.GridData();
		gridData2.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData2.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridData gridData1 = new org.eclipse.swt.layout.GridData();
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData1.grabExcessHorizontalSpace = false;
		gridData1.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalSpan = 2;
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.verticalSpacing = 5;
		labelHelp = new Label(this, SWT.NONE);
		labelHelp.setText("Defines the connection address to use for this ''screen'' connector.\n\n This connector needs:\n\t a destination address, as a hostname (or IP adress) and optionally a port,\n\t a connection parameter, optional.\n\n" +
				"The connection parameter has different meanings according to the emulator technology:\n\t3270: TN3270 device name\n\t5250: TN5250 device name\n\tDKU: MAILBOX\n\tMinitel: service code (e.g. '3615SNCF')\n\n");
		labelHelp.setLayoutData(gridData);
		labelConnectionParameter = new Label(this, SWT.NONE);
		labelConnectionParameter.setText("Connection parameter");
		textConnectionParameter = new Text(this, SWT.BORDER);
		textConnectionParameter.setLayoutData(gridData1);
		labelHost = new Label(this, SWT.NONE);
		labelHost.setText("Host name");
		textHost = new Text(this, SWT.BORDER);
		textHost.setLayoutData(gridData2);
		labelPort = new Label(this, SWT.NONE);
		labelPort.setText("Port");
		textPort = new Text(this, SWT.BORDER);
		textPort.setLayoutData(gridData2);
		labelConnectionType = new Label(this, SWT.NONE);
		labelConnectionType.setText("Connection type");
		createComboConnectionType();
		this.setLayout(gridLayout);
		setSize(new org.eclipse.swt.graphics.Point(402,289));
	}

	public Object getValue() {
		String param = textConnectionParameter.getText();
		String type = comboConnectionType.getItem(comboConnectionType.getSelectionIndex()) ;
		String hostName = textHost.getText();
		String hostPort = textPort.getText();
		String host = "";
		if (!hostName.equals("")) {
			if (!hostPort.equals("")) {
				host = hostName + ":" + hostPort;
			} else {
				host = hostName;
			}
		}
		
		if (type.equals(""))
			return param;
		else
			return param + "," + type + "|" + host;
	}

	/**
	 * This method initializes comboConnectionType	
	 *
	 */
	private void createComboConnectionType() {
		comboConnectionType = new Combo(this, SWT.NONE);
		comboConnectionType.add("");
		comboConnectionType.add("DIR");
		comboConnectionType.add("EIC");
		comboConnectionType.add("TCP");
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
