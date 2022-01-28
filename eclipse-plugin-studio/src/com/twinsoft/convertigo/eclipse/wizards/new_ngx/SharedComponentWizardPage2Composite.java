/*
 * Copyright (c) 2001-2022 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.wizards.new_ngx;

import static org.eclipse.swt.events.SelectionListener.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.engine.util.StringUtils;

public class SharedComponentWizardPage2Composite extends Composite {

	private Table table;
	private TableEditor tableEditor;
	private SharedComponentWizardPage2 page;
	
	private Map<String, String> tableMap = null;
	
	public SharedComponentWizardPage2Composite(Composite parent, int style, SharedComponentWizardPage2 page) {
		super(parent, style);
		this.page = page;
		this.tableMap = page.initTableMap();

		initialize();
	}

	protected Map<String, String> getVariableMap() {
		List<String> list = tableMap.keySet().stream().collect(Collectors.toList());
		
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (int i = 0; i < table.getItemCount(); i++) {
			TableItem item = table.getItem(i);
			map.put(list.get(i), item.getText(1));
		}
		return map;
	}
	
	private void initialize() {
		setLayout(new GridLayout(1, false));
		
		Label labelTable = new Label(this, SWT.NONE);
		labelTable.setText("The table below helps selecting a variable to change its name. Click on the 'Finish' button when done.");
		
		table = new Table(this, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableColumn columnVar = new TableColumn(table, SWT.NONE);
		columnVar.setWidth(500);
		columnVar.setText("Informations");
		
		TableColumn columnInfos = new TableColumn(table, SWT.NONE);
		columnInfos.setWidth(100);
		columnInfos.setText("Variable");
		
        for (String var_name: tableMap.keySet())
        {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(0,tableMap.get(var_name));
            item.setText(1,var_name);
        }
		
    	tableEditor = new TableEditor(table);
    	tableEditor.horizontalAlignment = SWT.LEFT;
    	tableEditor.grabHorizontal = true;
    	tableEditor.minimumWidth = 50;
    	final int EDITABLECOLUMN = 1;
    	
    	table.addSelectionListener(widgetSelectedAdapter(e -> {
			Control oldEditor = tableEditor.getEditor();
			if (oldEditor != null)
				oldEditor.dispose();

			TableItem item = (TableItem) e.item;
			if (item == null)
				return;

			Text newEditor = new Text(table, SWT.NONE);
			newEditor.setText(item.getText(EDITABLECOLUMN));
			newEditor.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					updatePageStatus(((Text) tableEditor.getEditor()).getText(), false);
				}
			});
			newEditor.addTraverseListener(new TraverseListener() {
			    public void keyTraversed(TraverseEvent e) {
	                if (e.detail == SWT.TRAVERSE_ESCAPE || e.detail == SWT.TRAVERSE_RETURN) {
	                	updatePageStatus(null, true);
	                	
	                	Text text = (Text) tableEditor.getEditor();
	                    String name = text.getText();
	                    if (e.detail == SWT.TRAVERSE_RETURN && isValidName(name)) {
	                    	tableEditor.getItem().setText(EDITABLECOLUMN, name);
		                    updatePageStatus(name, true);
	                    }
	                    text.dispose();
	                    e.doit = false;
	                }
			    }
			});
			newEditor.addFocusListener(new FocusListener() {
				@Override
				public void focusLost(FocusEvent e) {
					updatePageStatus(null, true);
				}
				@Override
				public void focusGained(FocusEvent e) {
					updatePageStatus(null, false);
				}
			});
			newEditor.selectAll();
			newEditor.setFocus();
			tableEditor.setEditor(newEditor, item, EDITABLECOLUMN);
		}));
    	
		this.setSize(new org.eclipse.swt.graphics.Point(514,264));
	}

	private boolean existVariable(TableItem ti, String name) {
		if (ti != null) {
			for (int i = 0; i < table.getItemCount(); i++) {
				TableItem item = table.getItem(i);
				if (!item.equals(ti) && item.getText(1).equals(name)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean isValidName(String name) {
		TableItem item = tableEditor.getItem();
		if (name == null)
			return false;
		if (name.isEmpty())
			return false;
		if (existVariable(item, name))
			return false;
		return StringUtils.isNormalized(name);
	}
	
	private void updatePageStatus(String name, boolean complete) {
		TableItem item = tableEditor.getItem();
		
		String message = null;
		if (name != null) {
			if (name.isEmpty()) {
				message = "Empty name is not valid!";
			}
			if (!StringUtils.isNormalized(name)) {
				message = "Name must be normalized! Don't start with number and don't use non ASCII caracters.";
			}
			if (!complete && existVariable(item, name)) {
				message = "This name already exists!";
			}
		}
		
		if (page != null) {
			page.updateStatus(message, complete);
		}
	}
}  //  @jve:decl-index=0:visual-constraint="10,10"
