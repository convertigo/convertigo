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

import java.io.File;

import javax.swing.event.EventListenerList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.connectors.SapJcoConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.transactions.SapJcoTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.editors.CompositeListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;

public class SapJcoConnectorDesignComposite extends Composite {

	private Text text;
	private Table table;
	private TableColumn tblclmnBapiName;
	private TableColumn tblclmnDescription;
	private TableColumn tblclmnGroupName;
	
	private Button btnImportAsTransactions;

	private SapJcoConnector sapConnector;
	
	private ProjectExplorerView projectExplorerView = null;
	
	public SapJcoConnectorDesignComposite(Connector connector, Composite parent, int style) {
		super(parent, style);
		
		sapConnector = (SapJcoConnector)connector;
		
		// add ProjectExplorerView to the listeners of the associated sap connector
		projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (projectExplorerView != null) {
			addCompositeListener(projectExplorerView);
		}
		
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		this.setLayout(gridLayout);
		
		Composite composite = new Composite(this, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		composite.setLayout(new GridLayout(3, false));
		
		Label lblNewLabel = new Label(composite, SWT.NONE);
		lblNewLabel.setToolTipText("Type Here a BAPI Pattern such as USER*");
		lblNewLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel.setText("Search BAPI Pattern");
		
		text = new Text(composite, SWT.BORDER);
		text.setText("BAPI_*");
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text.addKeyListener(new org.eclipse.swt.events.KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == SWT.CR) {
					searchBapis(text.getText());
				}
			}
		});
		
		Button btnNewButton = new Button(composite, SWT.NONE);
		btnNewButton.setText("Search");
		btnNewButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				searchBapis(text.getText());
			}
		});
		
		table = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		tblclmnBapiName = new TableColumn(table, SWT.NONE);
		tblclmnBapiName.setWidth(200);
		tblclmnBapiName.setText("Bapi Name");
		
		tblclmnDescription = new TableColumn(table, SWT.NONE);
		tblclmnDescription.setWidth(500);
		tblclmnDescription.setText("Description");

		tblclmnGroupName = new TableColumn(table, SWT.NONE);
		tblclmnGroupName.setWidth(200);
		tblclmnGroupName.setText("Group");
		
		btnImportAsTransactions = new Button(composite, SWT.NONE);
		btnImportAsTransactions.setText("Import as transaction(s) in project");
		btnImportAsTransactions.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		btnImportAsTransactions.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				createBapiTransactions(table.getSelection());
			}
		});
	}

	public void close() {
		// Remove ProjectExplorerView from listeners of current composite view
		if (projectExplorerView != null) {
			removeCompositeListener(projectExplorerView);
		}
	}
	
	@Override
	public void dispose() {
		super.dispose();
	}

	private EventListenerList compositeListeners = new EventListenerList();

	public void addCompositeListener(CompositeListener compositeListener) {
		compositeListeners.add(CompositeListener.class, compositeListener);
	}

	public void removeCompositeListener(CompositeListener compositeListener) {
		compositeListeners.remove(CompositeListener.class, compositeListener);
	}

	public void fireObjectSelected(CompositeEvent compositeEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = compositeListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2 ; i >= 0 ; i -= 2) {
			if (listeners[i] == CompositeListener.class) {
				((CompositeListener) listeners[i+1]).objectSelected(compositeEvent);
			}
		}
	}

	public void fireObjectChanged(CompositeEvent compositeEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = compositeListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2 ; i >= 0 ; i -= 2) {
			if (listeners[i] == CompositeListener.class) {
				((CompositeListener) listeners[i+1]).objectChanged(compositeEvent);
			}
		}
	}

	private void searchBapis(String pattern)
	{
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		Shell shell = display.getActiveShell();
		if (shell != null) {
			try {
				shell.setCursor(waitCursor);
				
			    ConvertigoPlugin.logDebug("Searching repository...");
				Document jcoDoc = SapJcoConnector.executeJCoSearch(sapConnector, pattern);
		        ConvertigoPlugin.logDebug("Search done.");
		        
		        if (jcoDoc != null) {
		        	table.removeAll();
		        	
		        	NodeList items = jcoDoc.getElementsByTagName("item");
		        	for (int i=0; i<items.getLength(); i++) {
		        		Element item = (Element) items.item(i);
		        		
		        	    String funcName = item.getElementsByTagName("FUNCNAME").item(0).getTextContent();
		        	    String groupName ="";
		        	    try {
		        	    	groupName = item.getElementsByTagName("GROUPNAME").item(0).getTextContent();
		        	    } catch (Exception e) {}
		        	    String sText="";
		        	    try {
		        	    	sText = item.getElementsByTagName("STEXT").item(0).getTextContent();
		        	    } catch (Exception e) {}
		        	    
	      				TableItem tableItem = new TableItem(this.table, SWT.NONE);
	    			    tableItem.setText(new String[] {
	    			    	  	funcName,
	    			    	  	sText,
	    			    	  	groupName
	    			    });
		        	}
		        }
		        
			} catch (Exception ee) {
				ConvertigoPlugin.logException(ee, "Error while searching repository");
			} finally {
				shell.setCursor(null);
				waitCursor.dispose();
			}
		}
		
	}
	
	private void createBapiTransactions(final TableItem[] items)
	{
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		Shell shell = display.getActiveShell();
		if (shell != null) {
			try {
				shell.setCursor(waitCursor);
				
				for (int i=0; i < items.length; i++) {
					TableItem item = items[i];
					String bapiName = item.getText(0);
					String bapiDesc = item.getText(1);
					ConvertigoPlugin.logDebug("Creating transaction for BAPI '"+bapiName+"' ...");
					sapConnector.removeSerializedData(bapiName);
					SapJcoTransaction sapJcoTransaction = SapJcoConnector.createSapJcoTransaction(sapConnector, bapiName);
					if (sapJcoTransaction != null) {
						Transaction transaction = sapConnector.getTransactionByName(bapiName);
						if (transaction != null) {
							try {
								File xsdFile = new File(transaction.getSchemaFilePath());
								if (xsdFile.exists()) {
									xsdFile.delete();
								}
							}
							catch (Exception e) {}
							sapConnector.remove(transaction);			
						}
						sapJcoTransaction.setComment(bapiDesc);
						sapConnector.add(sapJcoTransaction);
						fireObjectChanged(new CompositeEvent(sapConnector));
						ConvertigoPlugin.logDebug("Transaction added.");
					}
				}
			} catch (Exception ee) {
				ConvertigoPlugin.logException(ee, "Error while creating transaction(s)");
			} finally {
				shell.setCursor(null);
				waitCursor.dispose();
			}
		}
		
	}
}
