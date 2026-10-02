/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.search;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.enums.DatabaseObjectTypes;

public class DatabaseObjectSearchPage extends DialogPage implements ISearchPage {
	private Combo cType = null;
	private Text tSearch = null;
	private Button bMatchCase = null;
	private Button bRegExp = null;
	private Button bBrokenSources = null;
	private Button bInactive = null;
	private Button bSymbols = null;
	private Button bUnknownSymbols = null;

	private ISearchPageContainer container;
	private DatabaseObject root = null;
	private RadioGroupFieldEditor scope = null;

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		var store = ConvertigoPlugin.getDefault().getPreferenceStore();
		var search = store.getString("tSearch");
		var matchCase = store.getBoolean("bMatchCase");
		var regExp = store.getBoolean("bRegExp");
		var type = store.getInt("cType");

		var self = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5, 3).applyTo(self);

		var label = new Label(self, SWT.NONE);
		label.setFont(parent.getFont());
		label.setText("Containing text:");

		var part = new Composite(self, SWT.NONE);
		part.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(part);

		tSearch = new Text(part, SWT.BORDER);
		tSearch.setText(search);
		tSearch.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
		tSearch.addModifyListener((e) -> updateActionEnabled());
		tSearch.setSelection(0, search.length());
		tSearch.setFocus();

		var sub = new Composite(part, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(sub);

		bMatchCase = new Button(sub, SWT.CHECK);
		bMatchCase.setText("Case sensitive");
		bMatchCase.setSelection(matchCase);

		bRegExp = new Button(sub, SWT.CHECK);
		bRegExp.setText("Regular expression");
		bRegExp.setSelection(regExp);

		label = new Label(self, SWT.NONE);
		label.setFont(parent.getFont());
		label.setText("Object type:");

		cType = new Combo(self, SWT.READ_ONLY);
		cType.add("*");
		for (var v: DatabaseObjectTypes.values()) {
			cType.add(v.name());
		}
		cType.select(type);
		cType.addListener(SWT.Selection, (e) -> updateActionEnabled());

		var filters = new Group(self, SWT.NONE);
		filters.setText("Object filters (AND)");
		filters.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayoutFactory.fillDefaults().margins(5, 3).numColumns(2).applyTo(filters);
		bBrokenSources = createFilter(filters, "Broken sources", "search.bBrokenSources",
				"Objects with a broken step source, including step variables and SmartType sources.");
		bInactive = createFilter(filters, "Inactive objects", "search.bInactive",
				"Objects whose own Is active / Is enabled property is false.");
		bSymbols = createFilter(filters, "Uses symbols", "search.bSymbols",
				"Objects using global symbols, including known symbols and symbols with a default value.");
		bUnknownSymbols = createFilter(filters, "Unknown symbols", "search.bUnknownSymbols",
				"Objects with an undefined symbol in one of their own properties.");

		scope = root == null ? new RadioGroupFieldEditor("scope", "Scope", 1, new String[][] {
			{"Workspace", "workspace"}
		}, self,true) : new RadioGroupFieldEditor("scope", "Scope", 1, new String[][] {
			{"Selected object: " + StringUtils.abbreviateMiddle(root.getFullQName(), "…", 60), "selected"},
			{"Enclosing project: " + root.getProject().getName(), "project"},
			{"Workspace", "workspace"}
		}, self,true);
		scope.setPreferenceStore(store);
		scope.load();

		setControl(self);
	}

	private Button createFilter(Composite parent, String label, String preference, String tooltip) {
		var button = new Button(parent, SWT.CHECK);
		button.setText(label);
		button.setToolTipText(tooltip);
		button.setSelection(ConvertigoPlugin.getDefault().getPreferenceStore().getBoolean(preference));
		button.addListener(SWT.Selection, (e) -> updateActionEnabled());
		return button;
	}

	private void updateActionEnabled() {
		container.setPerformActionEnabled(!tSearch.getText().isEmpty()
				|| cType != null && cType.getSelectionIndex() > 0
				|| bBrokenSources != null && bBrokenSources.getSelection()
				|| bInactive != null && bInactive.getSelection()
				|| bSymbols != null && bSymbols.getSelection()
				|| bUnknownSymbols != null && bUnknownSymbols.getSelection());
	}

	@Override
	public boolean performAction() {
		var store = ConvertigoPlugin.getDefault().getPreferenceStore();
		var search = tSearch.getText();
		var matchCase = bMatchCase.getSelection();
		var regExp = bRegExp.getSelection();
		var type = cType.getSelectionIndex();

		store.setValue("tSearch", search);
		store.setValue("bMatchCase", matchCase);
		store.setValue("bRegExp", regExp);
		store.setValue("cType", type);
		store.setValue("search.bBrokenSources", bBrokenSources.getSelection());
		store.setValue("search.bInactive", bInactive.getSelection());
		store.setValue("search.bSymbols", bSymbols.getSelection());
		store.setValue("search.bUnknownSymbols", bUnknownSymbols.getSelection());
		scope.store();

		var dbo = root;
		switch (scope.getSelectionValue()) {
		case "workspace": dbo = null; break;
		case "project": dbo = dbo.getProject();
		}
		var query = new DatabaseObjectSearchQuery(dbo, search, matchCase, regExp, cType.getText(),
				bBrokenSources.getSelection(), bInactive.getSelection(), bSymbols.getSelection(), bUnknownSymbols.getSelection());
		NewSearchUI.runQueryInBackground(query);
		return true;
	}

	@Override
	public void setContainer(ISearchPageContainer container) {
		this.container = container;
		try {
			root = (DatabaseObject) ConvertigoPlugin.getDefault().getProjectExplorerView().getFirstSelectedDatabaseObjectTreeObject().getObject();
		} catch (Exception e) {
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			updateActionEnabled();
		}
	}
}
