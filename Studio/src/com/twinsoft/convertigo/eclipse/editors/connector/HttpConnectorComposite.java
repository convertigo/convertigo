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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ConnectorEvent;
import com.twinsoft.convertigo.beans.core.ConnectorListener;
import com.twinsoft.convertigo.beans.core.Transaction;

public class HttpConnectorComposite extends AbstractConnectorComposite implements ConnectorListener, IRefreshable {

	private Text httpData;
	
	public HttpConnectorComposite(ConnectorEditorPart connectorEditorPart, Connector connector, Composite parent, int style) {
		super(connectorEditorPart, connector, parent, style);
		connector.addConnectorListener(this);
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.editors.connector.AbstractConnectorComposite#close()
	 */
	public void close() {
		connector.removeConnectorListener(this);
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
		httpData = new Text(this, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		httpData.setLayoutData(gridData);
		httpData.setText("");
		this.setLayout(new GridLayout());
		setSize(new Point(300, 200));
	}
	
	protected void clearContent() {
		httpData.setText("");
	}

	public void dataChanged(ConnectorEvent connectorEvent) {
		if (!checkEventSource(connectorEvent))
			return;
		setTextData((String) connectorEvent.data);
	}
	
	private void setTextData(final String data) {
		if (data != null) {
			httpData.getDisplay().asyncExec(new Runnable() {
				public void run() {
					try {
						if (data.length() < 10000) {
							httpData.setText(data);	
						} else {
							httpData.setText(data.substring(0, 10000));
						}
					}
					catch (Exception e) {;}
				};
			});
		}
	}
	
	public void initConnector(Transaction transaction) {
	}

	public void renew() {
		// TODO gerer le renew
	}

	public void refresh() {
		// TODO gérer le refresh
	}

	public void monitor(ToolItem ti) {
		// TODO gérer le monitoring du connecteur pour l'affichage du bouton
	}
}
