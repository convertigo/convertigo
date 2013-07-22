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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolItem;

import com.twinsoft.convertigo.beans.connectors.SqlData;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ConnectorEvent;
import com.twinsoft.convertigo.beans.core.ConnectorListener;
import com.twinsoft.convertigo.beans.core.Transaction;

public class SqlConnectorComposite extends AbstractConnectorComposite implements ConnectorListener {

	private Table table;
	
	public SqlConnectorComposite(ConnectorEditorPart connectorEditorPart, Connector connector, Composite parent, int style) {
		super(connectorEditorPart, connector, parent, style);
		this.connector.addConnectorListener(this);
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.editors.connector.AbstractConnectorComposite#close()
	 */
	@Override
	public void close() {
		this.connector.removeConnectorListener(this);
		super.close();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
	}
	
	@Override
	protected void initialize() {
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		createTable();
		setData(null);
		this.setLayout(gridLayout);
		setSize(new Point(300, 200));
	}
	
	private void createTable() {
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		table = new Table(this, SWT.VIRTUAL | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLayoutData(gridData);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}

	@Override
	public void initConnector(Transaction transaction) {
		// TODO Auto-generated method stub
	}

	public void dataChanged(ConnectorEvent connectorEvent) {
		if (!checkEventSource(connectorEvent))
			return;
		setTableData((SqlData)connectorEvent.data);
	}
	
	private void setTableData(SqlData data) {
		if (data != null) {
			final SqlData sqlData = data;
			table.getDisplay().asyncExec(new Runnable() {
				public void run() {
					try {
						// clear table
						for (int i = 0; i < table.getColumnCount(); i++)
							table.getColumn(i).setText("");
						table.clearAll();
						
						// fill table
						if (sqlData != null) {
							int j = 0;
						    for (String columnHeader : sqlData.columnHeaders) {
					    		TableColumn column = null;
					    		try {
					    			column = table.getColumn(j++);
					    		} catch (IllegalArgumentException e) {}
					    		if (column == null) column = new TableColumn(table, SWT.NULL);
					    		column.setText(columnHeader);
						    }
						    j=0;
							for (List<String> row : sqlData.data) {
								TableItem item = null;
								try {
									item = table.getItem(j++);
					    		} catch (IllegalArgumentException e) {}
					    		if (item == null) item = new TableItem(table, SWT.NULL);
					    		int k = 0;
					    		for (String cel : row)
							    	item.setText(k++ , cel);
							}
							for (int i = 0; i < table.getColumnCount(); i++)
							      table.getColumn(i).pack();
						}
					}
					catch (Exception e) {;}
				};
			});
		}
	}

	public void renew() {
		// TODO gérer le renew		
	}
	
	public void monitor(ToolItem ti) {
		// TODO gérer le monitoring du connecteur pour l'affichage du bouton
	}

	protected void clearContent() {
		setData(null);
	}
}
