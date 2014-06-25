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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */
package com.twinsoft.convertigo.eclipse.editors.connector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ConnectorEvent;
import com.twinsoft.convertigo.beans.core.ConnectorListener;
import com.twinsoft.convertigo.beans.core.Transaction;

public class SapJcoConnectorComposite extends AbstractConnectorComposite implements ConnectorListener, IRefreshable {
	private Text httpData;

	public SapJcoConnectorComposite(ConnectorEditorPart connectorEditorPart, Connector connector, Composite parent, int style) {
		super(connectorEditorPart, connector, parent, style);
		connector.addConnectorListener(this);
	}

	@Override
	protected void initialize() {
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		httpData = new Text(this, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		httpData.setLayoutData(gridData);
		httpData.setText("");
		this.setLayout(new GridLayout());
		setSize(new Point(300, 200));
	}
	
	@Override
	public void close() {
		connector.removeConnectorListener(this);
		super.close();
	}
	
	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public void refresh() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dataChanged(ConnectorEvent connectorEvent) {
		if (!checkEventSource(connectorEvent))
			return;
		setTextData((String)connectorEvent.data);
	}
	
	private void setTextData(String data) {
		if (data != null) {
			final String jcoXML = data;
			httpData.getDisplay().asyncExec(new Runnable() {
				public void run() {
					try {
						httpData.setText(jcoXML);
					}
					catch (Exception e) {;}
				};
			});
		}
	}

	@Override
	protected void clearContent() {
		httpData.setText("");
	}

	@Override
	public void initConnector(Transaction transaction) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void renew() {
		// TODO Auto-generated method stub
		
	}

}
