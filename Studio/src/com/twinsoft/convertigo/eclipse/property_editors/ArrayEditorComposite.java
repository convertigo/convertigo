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

import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class ArrayEditorComposite extends AbstractDialogComposite {

	private ToolBar toolBar = null;
	private Table table = null;
	private XMLVector<Object> tableData;
	private ArrayEditorRowList  rowList = null;
	
	public ArrayEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		//tableData = (XMLVector) cellEditor.databaseObjectTreeObject.getPropertyValue(cellEditor.propertyDescriptor.getId());
		tableData = GenericUtils.cast(cellEditor.getEditorData());
		initialize();
	}

	private void initialize() {
		createToolBar();
		this.setLayout(new GridLayout());
		createTable();

		createTableViewer();
		createTableColumns();
		
		rowList = new ArrayEditorRowList(tableData);
		tableViewer.setInput(rowList);
	}

	/**
	 * This method initializes toolBar	
	 *
	 */
	private void createToolBar() {
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = false;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		toolBar = new ToolBar(this, SWT.FLAT);
		toolBar.setLayoutData(gridData);
		ToolItem newLine = new ToolItem(toolBar, SWT.PUSH);
		newLine.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/property_editors/images/table_editor/new_line.png")));
		newLine.setDisabledImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/property_editors/images/table_editor/new_line.d.png")));
		newLine.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				Object newLine = null;
				int len = ((ArrayEditor) cellEditor).templateData.length;
				for (int i = 0 ; i < len ; i++)
					newLine = (((ArrayEditor) cellEditor).templateData[i]);
				rowList.addRow(new ArrayEditorRow(newLine));
			}
		});
		ToolItem deleteLine = new ToolItem(toolBar, SWT.PUSH);
		deleteLine.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/property_editors/images/table_editor/delete.png")));
		deleteLine.setDisabledImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/property_editors/images/table_editor/delete.d.png")));
		deleteLine.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection)tableViewer.getSelection();
				ArrayEditorRow row = (ArrayEditorRow)selection.getFirstElement();
				if (row != null) {
					ArrayEditorRow nextRow = null;
					int index = rowList.getRows().indexOf(row);
					int size = rowList.getRows().size();
					if (size == 1) {			//only one row
					} else {
						nextRow = rowList.getRows().get(index +( (size == index+1)?-1:1 ));
					}
					
					rowList.removeRow(row);
					if (nextRow != null) {
						StructuredSelection newSelection = new StructuredSelection(nextRow);
						tableViewer.setSelection(newSelection);
					}
				}
			}
		});
		ToolItem moveUp = new ToolItem(toolBar, SWT.PUSH);
		moveUp.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/property_editors/images/table_editor/move_up.png")));
		moveUp.setDisabledImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/property_editors/images/table_editor/move_up.d.png")));
		moveUp.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				ArrayEditorRow row = (ArrayEditorRow) ((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
				if (row != null) {
					List<ArrayEditorRow> v = rowList.getRows();
					if (v.contains(row)) {
						int index = v.indexOf(row);
						if (index > 0) {
							ArrayEditorRow upRow = new ArrayEditorRow(row.getLine());
							rowList.insertRow(upRow, ((index-1>0) ? index-1:0));
							rowList.removeRow(row);
							StructuredSelection newSelection = new StructuredSelection(upRow);
							tableViewer.setSelection(newSelection);
						}
					}
				}
			}
		});
		ToolItem moveDown = new ToolItem(toolBar, SWT.PUSH);
		moveDown.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/property_editors/images/table_editor/move_down.png")));
		moveDown.setDisabledImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/property_editors/images/table_editor/move_down.d.png")));
		moveDown.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				ArrayEditorRow row = (ArrayEditorRow) ((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
				if (row != null) {
					List<ArrayEditorRow> v = rowList.getRows();
					if (v.contains(row)) {
						int len = v.size()-1;
						int index = v.indexOf(row);
						if (index < len) {
							ArrayEditorRow dowRow = new ArrayEditorRow(row.getLine());
							rowList.insertRow(dowRow, ((index+2 <= len) ? index+2:-1));
							rowList.removeRow(row);
							StructuredSelection newSelection = new StructuredSelection(dowRow);
							tableViewer.setSelection(newSelection);
						}
					}
				}
			}
		});
	}

	/**
	 * This method initializes table	
	 *
	 */
	private void createTable() {
		int style = SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | 
		SWT.FULL_SELECTION | SWT.HIDE_SELECTION;
		
		table = new Table(this, style);
		
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalSpan = 3;
		table.setLayoutData(gridData);		
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
	}

	private TableViewer tableViewer;

	private void createTableViewer() {
		ArrayEditor arrayEditor = ((ArrayEditor) cellEditor);

		tableViewer = new TableViewer(table);
		tableViewer.setUseHashlookup(true);
		tableViewer.setColumnProperties(arrayEditor.columnNames);

		// Assign the cell editors to the viewer
		CellEditor[] editors = arrayEditor.getColumnEditors(table);
		tableViewer.setCellEditors(editors);

		// Set the cell modifier for the viewer
		tableViewer.setCellModifier(new ArrayEditorCellModifier(this, tableViewer));
		
		// Set the label provider for the viewer
		tableViewer.setLabelProvider(new ArrayEditorLabelProvider(cellEditor.getStyle()));
		
		// Set the content provider
		tableViewer.setContentProvider(new ArrayEditorContentProvider());
	}
	
	/**
	 * InnerClass that acts as a proxy for the ExampleTaskList 
	 * providing content for the Table. It implements the ITaskListViewer 
	 * interface since it must register changeListeners with the 
	 * ExampleTaskList 
	 */
	class ArrayEditorContentProvider implements IStructuredContentProvider, IArrayListViewer {
		
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			if (newInput != null)
				((ArrayEditorRowList) newInput).addChangeListener(this);
			if (oldInput != null)
				((ArrayEditorRowList) oldInput).removeChangeListener(this);
		}

		public void dispose() {
			rowList.removeChangeListener(this);
		}

		public Object[] getElements(Object parent) {
			return rowList.getRows().toArray();
		}

		public void addRow(ArrayEditorRow row) {
			tableViewer.add(row);
		}

		public void insertRow(ArrayEditorRow row, int position) {
			tableViewer.insert(row, position);
		}
		
		public void removeRow(ArrayEditorRow row) {
			tableViewer.remove(row);			
		}

		public void updateRow(ArrayEditorRow row) {
			tableViewer.update(row, null);
		}
	}
	
	private void createTableColumns() {
		ArrayEditor arrayEditor = ((ArrayEditor) cellEditor);
		
		TableColumn column;
		for (int i = 0 ; i < arrayEditor.columnNames.length ; i++)  {
			column = new TableColumn(table, arrayEditor.columnAlignments == null ? SWT.LEFT : arrayEditor.columnAlignments[i]);
			column.setText(arrayEditor.columnNames[i]);
			column.setWidth(arrayEditor.columnSizes == null ? 100 : arrayEditor.columnSizes[i]);
		}
	}

	@Override
	public void dispose() {
		tableViewer.getLabelProvider().dispose();
	}

	public Object getValue() {
		XMLVector<Object> data = new XMLVector<Object>();
		for(ArrayEditorRow row : rowList.getRows())
			data.add(row.cloneValue());
		return data;
	}

	public ISelection getSelection() {
		return tableViewer.getSelection();
	}

	public ArrayEditorRowList getRowList() {
		return rowList;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"