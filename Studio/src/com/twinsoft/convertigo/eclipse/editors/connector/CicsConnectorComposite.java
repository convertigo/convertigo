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

package com.twinsoft.convertigo.eclipse.editors.connector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;

import com.ibm.ctg.client.GatewayRequest;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ConnectorEvent;
import com.twinsoft.convertigo.beans.core.ConnectorListener;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.util.COBOLUtils;
import com.twinsoft.convertigo.engine.util.HexUtils;

public class CicsConnectorComposite extends AbstractConnectorComposite implements ConnectorListener {

	private Text cicsData;
	
	public CicsConnectorComposite(ConnectorEditorPart connectorEditorPart, Connector connector, Composite parent, int style) {
		super(connectorEditorPart, connector, parent, style);
		this.connector.addConnectorListener(this);
		
		// Check the CTG client implementation
		try {
			new GatewayRequest();
		}
		catch(RuntimeException e) {
			// We are using the fake implementation
			ConvertigoPlugin.logError(e.getMessage(), true);
		}
		catch(Exception e) {
			// IBM implementation, nothing to do, just ignore
		}
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.editors.connector.AbstractConnectorComposite#close()
	 */
	public void close() {
		this.connector.removeConnectorListener(this);
		super.close();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	public void dispose() {
		super.dispose();
	}

	protected void initialize() {
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		cicsData = new Text(this, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		cicsData.setEditable(false);
		cicsData.setBackground(new Color(null,253,253,244));
		cicsData.setFont(new Font(null,"Courier New",10,1));
		cicsData.setLayoutData(gridData);
		cicsData.setText("");
		this.setLayout(new GridLayout());
		setSize(new Point(300, 200));
	}
	
	public void dataChanged(ConnectorEvent connectorEvent) {
		if (!checkEventSource(connectorEvent))
			return;
		setTextData((byte[]) connectorEvent.data);
	}
	
	protected void clearContent() {
		cicsData.setText("");
	}

	private void setTextData(byte[] data) {
		if (data != null) {
			final byte[] buf = data;
			cicsData.getDisplay().asyncExec(new Runnable() {
				public void run() {
					try {
						String ascii = COBOLUtils.decodeEBCDICToASCII(buf, 0x2e);
						StringBuffer text = new StringBuffer("");
						String asciiLine = "";
						int nLine = 0;
						byte b;
						
						for (int i = 0 ; i < buf.length ; i++) {
							if ((i != 0) && (i % 16 == 0)) {
								asciiLine = ascii.substring(nLine * 16, (nLine + 1) * 16);
								text.append(" ");
								text.append(asciiLine);
								text.append("\n");
								nLine++;
							}
							b = buf[i];
							text.append(HexUtils.toHexString(b) + " ");
						}

						// Last line
						for (int i = 0 ; i < 16 - buf.length % 16 ; i++) {
							text.append("   ");
						}
						asciiLine = ascii.substring(nLine * 16);
						text.append(" ");
						text.append(asciiLine);
						text.append("\n");
						
						cicsData.setText(text.toString());
					}
					catch (Exception e) {;}
				};
			});
		}
	}

	public void initConnector(Transaction transaction) {
		// TODO Auto-generated method stub

	}

	public void renew() {
		// TODO: gérer le renew
	}

	public void monitor(ToolItem ti) {
		// TODO gérer le monitoring du connecteur pour l'affichage du bouton
	}
}
