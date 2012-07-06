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

package com.twinsoft.convertigo.eclipse.wizards.new_object;

import java.util.Vector;

import org.apache.xpath.XPathAPI;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLTable;
import com.twinsoft.convertigo.beans.common.XMLTableRow;
import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.eclipse.swt.KTable;
import com.twinsoft.convertigo.eclipse.swt.KTableCellEditor;
import com.twinsoft.convertigo.eclipse.swt.KTableCellRenderer;
import com.twinsoft.convertigo.eclipse.swt.KTableModel;
import com.twinsoft.convertigo.engine.util.StringUtils;


public class XMLTableWizardPage extends WizardPage {

	private Table table = null;
	private KTable ktable = null;
	private Button checkBoxHeadersFromRow = null;
	private Button checkBoxHeadersFromCol = null;
	private Button pushRowDown = null;
	private Button pushRowUp = null;
	private Button pushColDown = null;
	private Button pushColUp = null;
	
	private Text headerRow = null;
	private Text headerCol = null;
	
	private String xpath = null;
	private Document dom = null;
	private XMLVector<XMLVector<Object>> xDescription = null;
	private Vector<Vector<String>> data = null;
	private int numColumns = 2;
	
	public XMLTableWizardPage(String xpath, Document dom) {
		super("XMLTableWizardPage");
		setTitle("New Table");
		setMessage("Please configure rows and lines");
		
		this.xpath = xpath;
		this.dom = dom;
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = numColumns;
		layout.verticalSpacing = 10;
		
		createXPath(container);
		createInfos(container);
		createTable(container);
		//createKTable(container);
		
		fillXMLTableDescription();
		
		setControl(container);
	}
	
	@Override
	public void performHelp() {
		getPreviousPage().performHelp();
	}
	
	private void createXPath(Composite parent) {
        GridData gridData = new GridData();
        gridData.horizontalSpan = numColumns;
        gridData.horizontalAlignment = GridData.FILL;
		
		Label label = new Label(parent, SWT.NULL);
		label.setLayoutData(gridData);
		label.setText("&XPath : " + xpath);
	}
	
	private void createInfos(Composite parent) {
		GridLayout headerRowLayout = new GridLayout();
		headerRowLayout.numColumns = 4;
		
		Group headerRowGroup = new Group(parent, SWT.NONE);
		headerRowGroup.setLayout(headerRowLayout);
		headerRowGroup.setText("Headers");
        headerRowGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		checkBoxHeadersFromRow = new Button(headerRowGroup, SWT.CHECK);
		checkBoxHeadersFromRow.setText("Headers at row");
		checkBoxHeadersFromRow.setSelection(true);
		checkBoxHeadersFromRow.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				checkBoxFirstRowSelected();
			}
			public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
			}
		});
		
		headerRow = new Text(headerRowGroup, SWT.NULL);
        headerRow.setText("0");
        headerRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        pushRowDown = new Button(headerRowGroup, SWT.PUSH);
		pushRowDown.setText("+");
		pushRowDown.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				pushRowSelected(true);
			}
			public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
			}
		});
		
		pushRowUp = new Button(headerRowGroup, SWT.PUSH);
		pushRowUp.setText("-");
		pushRowUp.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				pushRowSelected(false);
			}
			public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
			}
		});

		
		GridLayout headerColLayout = new GridLayout();
		headerColLayout.numColumns = 4;
		
		Group headerColGroup = new Group(parent, SWT.NONE);
		headerColGroup.setLayout(headerColLayout);
		headerColGroup.setText("Headers");
		
		headerColGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		checkBoxHeadersFromCol = new Button(headerColGroup, SWT.CHECK);
		checkBoxHeadersFromCol.setText("Headers at col");
		checkBoxHeadersFromCol.setSelection(false);
		checkBoxHeadersFromCol.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				checkBoxFirstColSelected();
			}
			public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
			}
		});
		
		headerCol = new Text(headerColGroup, SWT.NULL);
		headerCol.setText("0");
		headerCol.setEnabled(false);
		headerCol.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		pushColDown = new Button(headerColGroup, SWT.PUSH);
		pushColDown.setText("+");
		pushColDown.setEnabled(false);
		pushColDown.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				pushColSelected(true);
			}
			public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
			}
		});
		
		pushColUp = new Button(headerColGroup, SWT.PUSH);
		pushColUp.setText("-");
		pushColUp.setEnabled(false);
		pushColUp.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				pushColSelected(false);
			}
			public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
			}
		});
	}
	
	private void checkBoxFirstRowSelected() {
		headerRow.setText(checkBoxHeadersFromRow.getSelection() ? headerRow.getText():"0");
		headerRow.setEnabled(checkBoxHeadersFromRow.getSelection());
		pushRowUp.setEnabled(checkBoxHeadersFromRow.getSelection());
		pushRowDown.setEnabled(checkBoxHeadersFromRow.getSelection());
		fillXMLTableDescription();
		setPageComplete(true);
	}
	
	private void checkBoxFirstColSelected() {
		headerCol.setText(checkBoxHeadersFromCol.getSelection() ? headerCol.getText():"0");
		headerCol.setEnabled(checkBoxHeadersFromCol.getSelection());
		pushColUp.setEnabled(checkBoxHeadersFromCol.getSelection());
		pushColDown.setEnabled(checkBoxHeadersFromCol.getSelection());
		fillXMLTableDescription();
		setPageComplete(true);
	}
	
	private void pushRowSelected(boolean bIncrease) {
		int number = Integer.parseInt(headerRow.getText().trim(), 10);
		number = (bIncrease ? ++number : ((number>0) ? --number : 0));
		headerRow.setText(String.valueOf(number));
		fillXMLTableDescription();
		setPageComplete(true);
	}
	
	private void pushColSelected(boolean bIncrease) {
		int number = Integer.parseInt(headerCol.getText().trim(), 10);
		number = (bIncrease ? ++number : ((number>0) ? --number : 0));
		headerCol.setText(String.valueOf(number));
		fillXMLTableDescription();
		setPageComplete(true);
	}

	private void createTable(Composite parent) {
		GridData gridData = new GridData();
		gridData.horizontalSpan = numColumns;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.verticalAlignment = GridData.FILL;
		table = new Table(parent, SWT.VIRTUAL | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLayoutData(gridData);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}
	
//	private void createKTable(Composite parent) {
//		GridData gridData = new GridData();
//		gridData.horizontalSpan = numColumns;
//		gridData.horizontalAlignment = GridData.FILL;
//		gridData.grabExcessHorizontalSpace = true;
//		gridData.grabExcessVerticalSpace = true;
//		gridData.verticalAlignment = GridData.FILL;		
//	    ktable = new KTable(parent, SWT.V_SCROLL | SWT.H_SCROLL);
//	    ktable.setRowSelectionMode(true);
//	    ktable.setModel(new XMLTableModel());
//	    ktable.setLayoutData(gridData);
//		
//	}
	
	private void fillXMLTableDescription() {
		
		NodeList tables;
		try {
			String theXPath = (xpath.indexOf("//TABLE") != -1) ? xpath: ( xpath.startsWith("/") ? xpath : "/"+xpath);
			tables = XPathAPI.eval(dom.getDocumentElement(), theXPath).nodelist();
			if (tables.getLength() > 0) {
				
				//------------------------------------------------------------- For Bean XMLTable
				xDescription = new XMLVector<XMLVector<Object>>();
				
				Element table = (Element)tables.item(0);
				
				int iHeaderRow = Integer.parseInt(headerRow.getText().trim(), 10);
				int iHeaderCol = Integer.parseInt(headerCol.getText().trim(), 10);
				
				boolean bHeadersFromRow = checkBoxHeadersFromRow.getSelection();
				boolean bHeadersFromCol = checkBoxHeadersFromCol.getSelection();

				boolean hasTableBody = XPathAPI.eval(table, "./TBODY").nodelist().getLength() > 0;
				
				int iTR = iHeaderRow + 1;
				int iTD = iHeaderCol + 1;
				
				if (!bHeadersFromCol && !bHeadersFromRow) {
					XMLTableRow xRow = XMLTableRow.create();
					xRow.setXpath(hasTableBody ? "./TBODY/TR":"./TR");
					xRow.addColumn("column", "./TD", false);
					xDescription.add(xRow.toXMLVector());
				}
				else {
					boolean bSameRowHeaderNames = false, bSameColHeaderNames = false;
					String oldRowName = null, oldColName = null;
					Vector<String> colNames = new Vector<String>();
					String dataXPath = (hasTableBody ? (bHeadersFromRow ? "(./TBODY/TR)[position()>"+ iHeaderRow +"]":"./TBODY/TR") : (bHeadersFromRow ? "(./TR)[position()>"+ iHeaderRow +"]":"./TR"));
					NodeList nlTR = XPathAPI.eval(table, dataXPath).nodelist();
					for(int i=0;i<nlTR.getLength();i++){
						int iRow = iTR + i ;
						XMLTableRow xRow = XMLTableRow.create();
						String rowXPath = (hasTableBody ? (bHeadersFromRow ? "(./TBODY/TR)[position()>"+ iTR +"]":"./TBODY/TR") : (bHeadersFromRow ? "(./TR)[position()>"+ iTR +"]":"./TR"));
						xRow.setXpath(bHeadersFromCol ? (hasTableBody ? "(./TBODY/TR)["+ iRow +"]":"(./TR)["+ iRow +"]") : rowXPath);
						
						NodeList nlTD = XPathAPI.eval(nlTR.item(i), "(./TD)[position()>"+ iHeaderCol +"] | (./TH)[position()>"+ iHeaderCol +"]").nodelist();
						for(int j=0;j<nlTD.getLength();j++){
							NodeList nltxt = XPathAPI.eval(nlTD.item(j), ".//text()").nodelist();
							String tmpVal = "";
							for(int k=0;k<nltxt.getLength();k++){
								tmpVal += StringUtils.normalize(nltxt.item(k).getNodeValue()); 
							}
							tmpVal.trim();
							tmpVal = tmpVal.equals("") ? "X":tmpVal;
							int iCol = iTD + j;
							if (bHeadersFromRow) {
								if (i == 0) {
									colNames.add(tmpVal);
								}
								else {
									if (bHeadersFromCol) {
										if (j == 0)
											xRow.setName(tmpVal);
										else {
											xRow.addColumn(colNames.get(j), "(./TD|./TH)["+ iCol +"]", true);
											if (oldColName == null) oldColName = colNames.get(j);
											else bSameColHeaderNames = oldColName.equals(colNames.get(j));
										}
									}
									else {
										xRow.addColumn(colNames.get(j), "(./TD|./TH)["+ iCol +"]", true);
										if (oldColName == null) oldColName = colNames.get(j);
										else bSameColHeaderNames = oldColName.equals(colNames.get(j));
									}
								}
							}
							else if (bHeadersFromCol) {
								if (j == 0)
									xRow.setName(tmpVal);
								else
									xRow.addColumn("column"+j, "(./TD|./TH)["+ iCol +"]", true);
							}
						}
						if (((i==1) && bHeadersFromRow && !bHeadersFromCol) || ((i>=1) && bHeadersFromRow && bHeadersFromCol) || (!bHeadersFromRow && bHeadersFromCol)) {
							if (bSameColHeaderNames) {// optimization for table's columns with same names
								int last;
								xRow.setColumnXPath(0,"(./TD|./TH)[position()>"+ (bHeadersFromCol?iTD:iHeaderCol) +"]");
								while ((last = xRow.getColumns().size())>1)
									xRow.getColumns().remove(last-1);
							}
							
							xDescription.add(xRow.toXMLVector());
							if (oldRowName == null) oldRowName = xRow.getName();
							else bSameRowHeaderNames = oldRowName.equals(xRow.getName());
						}
					}
					
					if (bSameRowHeaderNames) { // optimization for table's rows with same names
						XMLTableRow xRow = new XMLTableRow(xDescription.get(0));
						xRow.setXpath(hasTableBody ? "(./TBODY/TR)[position()>"+ (bHeadersFromRow?iTR:iHeaderRow) +"]":"(./TR)[position()>"+ (bHeadersFromRow?iTR:iHeaderRow) +"]");
						xDescription = new XMLVector<XMLVector<Object>>();
						xDescription.add(xRow.toXMLVector());
					}
				}
				
				//------------------------------------------------------------ For SWT Table
				data = new Vector<Vector<String>>();
				
				NodeList nlCol = XPathAPI.eval(table, (hasTableBody ? "./TBODY/TR":"./TR")).nodelist();
				for(int i=0;i<nlCol.getLength();i++){
					if (i < iHeaderRow)
						continue;
					
					NodeList nltd = XPathAPI.eval(nlCol.item(i), "./TD|./TH").nodelist();
					Vector<String> row = new Vector<String>();
					for(int j=0;j<nltd.getLength();j++){
						NodeList nltxt = XPathAPI.eval(nltd.item(j), ".//text()").nodelist();
						String tmpVal = "";
						for(int k=0;k<nltxt.getLength();k++){
							tmpVal += StringUtils.normalize(nltxt.item(k).getNodeValue()); 
						}
						if (j < iHeaderCol)
							continue;
						row.add(tmpVal.trim());
					}
					data.add(row);
				}
				
				if (table != null)
					setTableData();
				else if (ktable != null) {
					((XMLTableModel)ktable.getModel()).setTableData(data, bHeadersFromCol, bHeadersFromRow);
					ktable.redraw();
				}
			}
		}
		catch (Exception e) {
			;
		}
	}

	private void setTableData() {
		try {
			boolean bHeadersFromRow = checkBoxHeadersFromRow.getSelection();
			boolean bHeadersFromCol = checkBoxHeadersFromCol.getSelection();
			String columnText, itemText;
			
			// clear table
			for (int i = 0; i < table.getColumnCount(); i++)
				table.getColumn(i).setText("");
			table.removeAll();
			table.setHeaderVisible(bHeadersFromRow);
			
			// fill table
			if (xDescription != null) {
				for (int i=0; i<data.size(); i++) {
					Vector<String> row = data.get(i);
					if ((i == 0) && bHeadersFromRow) {
						for (int j=0; j<row.size(); j++) {
							TableColumn column = null;
				    		try {
				    			column = table.getColumn(j);
				    		}
				    		catch (IllegalArgumentException e) {}
				    		if (column == null)
				    			column = new TableColumn(table, SWT.NONE);
				    		
				    		columnText = row.get(j);
				    		columnText = (columnText.equals("") ? "*":columnText);
				    		column.setText(columnText);
				    		column.setWidth(50);
						}
					}
					else {
						TableItem item = null;
						try {
							item = table.getItem(i);
			    		}
			    		catch (IllegalArgumentException e) {}
			    		if (item == null)
			    			item = new TableItem(table, SWT.NONE);
			    		
			    		if (bHeadersFromCol)
			    			item.setBackground(0, Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			    		
			    		for (int j=0; j<row.size(); j++) {
			    			itemText = row.get(j);
			    			item.setText(j, itemText);
					    }
					}
		    		
					if (i == data.size()-1) {
						for (int j=0; j<row.size(); j++) {
						     table.getColumn(j).pack();
						}
					}

				}
			}
		}
		catch (Exception e) {}
	}

	public boolean isPageComplete() {
		boolean complete = isCurrentPage();
		if (complete) setXMLTableBeanDescription();
		return complete;
	}

	public void setPageComplete(boolean complete) {
		if (complete) setXMLTableBeanDescription();
		super.setPageComplete(complete);
	}

	private void setXMLTableBeanDescription() {
		try {
			XMLTable xmlTable = (XMLTable)((ObjectExplorerWizardPage)getWizard().getPage("ObjectExplorerWizardPage")).getCreatedBean();
			if (xmlTable != null) {
				if (xDescription != null)
					xmlTable.setDescription(xDescription);
			}
		}
		catch (NullPointerException e) {
			return;
		}
	}
	
	class XMLTableModel implements KTableModel {

		private int rowHeight = 18;
	
		private int[] colWidths = new int[] {};
		
		private Vector<Vector<String>> data = null;
		
		private boolean fixedRow = false;
		
		private boolean fixedCol = false;
		
		public XMLTableModel() {
			data = new Vector<Vector<String>>();
		}
		
		synchronized public void setTableData(Vector<Vector<String>> v, boolean fixedCol, boolean fixedRow) {
			if (v != null) {
				data = new Vector<Vector<String>>(v);
			    colWidths = new int[getColumnCount()];
			    for (int i = 0; i < colWidths.length; i++) {
			      colWidths[i] = 60;
			    }
			}
			this.fixedRow = fixedRow;
			this.fixedCol = fixedCol;
		}
		
		public KTableCellEditor getCellEditor(int col, int row) {
			return null;
		}

		public KTableCellRenderer getCellRenderer(int col, int row) {
			return KTableCellRenderer.defaultRenderer;
		}

		public int getColumnCount() {
			if (data.size() > 0) {
				Vector<String> v = data.get(0);
				if (v != null) {
					return v.size();
				}
			}
			return 0;
		}

		public int getColumnWidth(int col) {
			if (colWidths.length > 0)
				return colWidths[col];
			return 0;
		}

		public Object getContentAt(int col, int row) {
			Vector<String> v = data.get(row);
			if (v != null) {
				return v.get(col);
			}
			return "?";
		}

		public int getFirstRowHeight() {
			return rowHeight;
		}

		public int getFixedColumnCount() {
			return (fixedCol ? 1:0);
		}

		public int getFixedRowCount() {
			return (fixedRow ? 1:0);
		}

		public int getRowCount() {
			return data.size();
		}

		public int getRowHeight() {
			return rowHeight;
		}

		public int getRowHeightMinimum() {
			return 10;
		}

		public boolean isColumnResizable(int col) {
			return true;
		}

		public boolean isRowResizable() {
			return true;
		}

		public void setColumnWidth(int col, int value) {
			colWidths[col] = value;
		}

		public void setContentAt(int col, int row, Object value) {
		}

		public void setRowHeight(int value) {
			rowHeight = value;
		}
	}
}
