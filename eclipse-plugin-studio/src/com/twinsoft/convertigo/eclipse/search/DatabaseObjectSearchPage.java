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
		tSearch.addModifyListener((e) -> {
			container.setPerformActionEnabled(!tSearch.getText().isEmpty());
		});
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
		scope.store();

		var dbo = root;
		switch (scope.getSelectionValue()) {
		case "workspace": dbo = null; break;
		case "project": dbo = dbo.getProject();
		}
		var query = new DatabaseObjectSearchQuery(dbo, search, matchCase, regExp, cType.getText());
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
			container.setPerformActionEnabled(!tSearch.getText().isEmpty());
		}
	}
}
