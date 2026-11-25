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

package com.twinsoft.convertigo.engine.sqlcrud;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UICompEvent;
import com.twinsoft.convertigo.beans.ngx.components.UICompVariable;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIControlDirective;
import com.twinsoft.convertigo.beans.ngx.components.UIControlEvent;
import com.twinsoft.convertigo.beans.ngx.components.UICustomAction;
import com.twinsoft.convertigo.beans.ngx.components.UICustomAsyncAction;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIStyle;
import com.twinsoft.convertigo.beans.ngx.components.UIText;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.beans.ngx.components.UIUseVariable;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.mobile.ComponentRefManager;
import com.twinsoft.convertigo.engine.util.StringUtils;

/**
 * Generates the NGX user interface of a SQL table on top of its existing CRUD sequences: a shared component with a
 * toolbar and the grid of {@value CrudUiDefaults#LIB_PROJECT_NAME}, and optionally a host page. Existing objects are
 * never overwritten, the generated ones contain no business rule and can be changed freely.
 */
public final class NgxCrudGenerator {

	/** What the generation produced for a table, or why it was skipped. */
	public static final class Result {
		private final List<String> warnings = new ArrayList<>();
		private UISharedRegularComponent component;
		private UISharedRegularComponent form;
		private PageComponent page;
		private String skipReason;

		public List<String> getWarnings() {
			return warnings;
		}

		public UISharedRegularComponent getComponent() {
			return component;
		}

		public PageComponent getPage() {
			return page;
		}

		/** The form of the table, null when its rows can be neither created nor updated. */
		public UISharedRegularComponent getForm() {
			return form;
		}

		public boolean isSkipped() {
			return skipReason != null;
		}

		public String getSkipReason() {
			return skipReason;
		}
	}

	private final Project project;
	private final ApplicationComponent app;
	private final SqlTableInfo table;
	private final String baseName;
	private final Map<String, String> sequences = new LinkedHashMap<>();

	public NgxCrudGenerator(Project project, SqlTableInfo table) throws EngineException {
		this.project = project;
		this.table = table;
		this.app = ngxApplication(project);
		if (app == null) {
			throw new EngineException("The project " + project.getName() + " has no NGX application");
		}
		this.baseName = StringUtils.normalize(table.getTableName());
		for (var verb : List.of("LIST", "COUNT", "INSERT", "UPDATE", "DELETE")) {
			var name = baseName + "_" + verb;
			if (hasSequence(project, name)) {
				sequences.put(verb, project.getName() + "." + name);
			}
		}
	}

	/** The NGX application of the project, null when it has none (the generator is then not available). */
	public static ApplicationComponent ngxApplication(Project project) {
		var mobileApplication = project == null ? null : project.getMobileApplication();
		if (mobileApplication != null && mobileApplication.getApplicationComponent() instanceof ApplicationComponent app) {
			return app;
		}
		return null;
	}

	private static boolean hasSequence(Project project, String name) {
		try {
			return project.getSequenceByName(name) != null;
		} catch (Exception e) {
			return false;
		}
	}

	/** Name of the shared component of a table: the singular of its normalized name, first letter lower case. */
	public static String componentName(String tableName) {
		var name = singular(baseIdentifier(tableName));
		name = name.substring(0, 1).toLowerCase(Locale.ROOT) + name.substring(1);
		// a shared component name must begin with a letter
		return Character.isLetter(name.charAt(0)) ? name : "t" + name;
	}

	/** Name of the host page of a table: its normalized name, first letter upper case ("clients" gives "Clients"). */
	public static String pageName(String tableName) {
		var name = baseIdentifier(tableName);
		name = name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
		return Character.isLetter(name.charAt(0)) ? name : "P" + name;
	}

	/**
	 * The normalized table name in camel case: lower case when the database gives it in upper case, words separated by
	 * underscores joined ("CLIENTS" gives "clients", "audit_log" gives "auditLog").
	 */
	private static String baseIdentifier(String tableName) {
		var name = StringUtils.normalize(tableName);
		if (name.equals(name.toUpperCase(Locale.ROOT))) {
			name = name.toLowerCase(Locale.ROOT);
		}
		var sb = new StringBuilder();
		for (var word : name.split("_+")) {
			if (!word.isEmpty()) {
				sb.append(sb.length() == 0 ? word : word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
			}
		}
		return sb.length() == 0 ? name : sb.toString();
	}

	/** A naive English singular: "countries" gives "country", "boxes" gives "box", "clients" gives "client". */
	static String singular(String name) {
		var lower = name.toLowerCase(Locale.ROOT);
		if (name.length() > 4 && lower.endsWith("ies")) {
			return name.substring(0, name.length() - 3) + (Character.isUpperCase(name.charAt(name.length() - 1)) ? "Y" : "y");
		}
		if (name.length() > 4 && (lower.endsWith("sses") || lower.endsWith("xes") || lower.endsWith("ches") || lower.endsWith("shes"))) {
			return name.substring(0, name.length() - 2);
		}
		if (name.length() > 3 && lower.endsWith("s") && !lower.endsWith("ss") && !lower.endsWith("us")) {
			return name.substring(0, name.length() - 1);
		}
		return name;
	}

	public String getComponentName() {
		return componentName(table.getTableName());
	}

	/** Name of the form shared component: the component name followed by Form ("clientForm"). */
	public String getFormName() {
		return getComponentName() + "Form";
	}

	public String getPageName() {
		return pageName(table.getTableName());
	}

	/** Verbs whose sequence exists in the project (LIST, INSERT, UPDATE, DELETE). */
	public Map<String, String> getSequences() {
		return sequences;
	}

	/** Creates the shared component of the table in the NGX application, and its host page when asked. */
	public Result generate(boolean withPage) throws EngineException {
		var result = new Result();
		result.warnings.addAll(table.getWarnings());
		if (!sequences.containsKey("LIST")) {
			result.skipReason = "the sequence " + baseName + "_LIST is missing";
			return result;
		}
		var componentName = getComponentName();
		if (findSharedComponent(componentName) != null) {
			// never overwritten, even with "Override", which only applies to the transactions
			result.skipReason = "the shared component " + componentName + " already exists";
			return result;
		}
		var formName = getFormName();
		// the form also shows a row (view mode), even when the table cannot be changed
		var withForm = !table.getDisplayedColumns().isEmpty();
		if (withForm && findSharedComponent(formName) != null) {
			result.skipReason = "the shared component " + formName + " already exists";
			return result;
		}
		for (var verb : List.of("INSERT", "UPDATE", "DELETE")) {
			if (!sequences.containsKey(verb)) {
				result.warnings.add("no " + verb + " in the user interface: the sequence " + baseName + "_" + verb + " is missing");
			}
		}
		if (!sequences.containsKey("COUNT") && table.hasListPagination()) {
			result.warnings.add("no row count: the sequence " + baseName + "_COUNT is missing, the grid finds the number of rows while scrolling");
		}

		if (withForm) {
			// the form comes first: the component opens it in a modal by its qualified name
			var form = buildForm(formName);
			app.add(form);
			loadTemplateBeans(form);
			result.form = form;
		}
		var component = buildComponent(componentName, result.form);
		app.add(component);
		loadTemplateBeans(component);
		result.component = component;

		if (withPage) {
			var pageName = getPageName();
			var segment = pageName.toLowerCase(Locale.ROOT);
			if (findPage(pageName, segment) != null) {
				result.warnings.add("the page " + pageName + " already exists: no host page generated");
			} else {
				var page = buildPage(pageName, component);
				app.add(page);
				loadTemplateBeans(page);
				// a new page gets the default title and segment when added
				page.setSegment(segment);
				page.setTitle(SqlColumnInfo.prettify(table.getTableName()));
				result.page = page;
			}
		}
		return result;
	}

	/**
	 * Loads the template components of a generated object with the components of the application template. They are
	 * built before being attached to the application, and a component loaded then gets the default models. Also
	 * declares the shared components and actions it uses, as the Studio does for an added object, so that the
	 * Mobile Builder writes their files (the grid of the library) in the application.
	 */
	private void loadTemplateBeans(DatabaseObject root) throws EngineException {
		loadTemplateBeans(app, root);
	}

	/** Same as {@link #loadTemplateBeans(DatabaseObject)}, for objects that a developer adds next to the generated ones. */
	static void loadTemplateBeans(ApplicationComponent app, DatabaseObject root) throws EngineException {
		var cm = ComponentManager.of(app);
		var objects = new ArrayList<DatabaseObject>();
		objects.add(root);
		for (int i = 0; i < objects.size(); i++) {
			var dbo = objects.get(i);
			if (dbo instanceof UIDynamicElement element) {
				try {
					element.loadBean(cm);
				} catch (Exception e) {
					throw new EngineException("Unable to load the template component of " + dbo.getName(), e);
				}
			}
			if (dbo instanceof UIDynamicInvoke invoke && !invoke.getSharedActionQName().isEmpty()) {
				ComponentRefManager.get(ComponentRefManager.Mode.use).addConsumer(invoke.getSharedActionQName(), invoke.getQName());
			}
			if (dbo instanceof UIUseShared use && !use.getSharedComponentQName().isEmpty()) {
				ComponentRefManager.get(ComponentRefManager.Mode.use).addConsumer(use.getSharedComponentQName(), use.getQName());
			}
			objects.addAll(dbo.getAllChildren());
		}
	}

	private UISharedComponent findSharedComponent(String name) {
		for (var comp : app.getSharedComponentList()) {
			if (comp.getName().equalsIgnoreCase(name)) {
				return comp;
			}
		}
		return null;
	}

	private PageComponent findPage(String name, String segment) {
		for (var page : app.getPageComponentList()) {
			if (page.getName().equalsIgnoreCase(name) || segment.equalsIgnoreCase(page.getSegment())) {
				return page;
			}
		}
		return null;
	}

	boolean canCreate() {
		return sequences.containsKey("INSERT") && table.canCreate();
	}

	boolean canUpdate() {
		return sequences.containsKey("UPDATE") && table.canUpdate();
	}

	boolean canDelete() {
		return sequences.containsKey("DELETE") && table.canDelete();
	}

	/* ------------------------------------------------------------------ shared component */

	private UISharedRegularComponent buildComponent(String name, UISharedRegularComponent form) throws EngineException {
		var comp = new UISharedRegularComponent();
		comp.setName(name);
		comp.bNew = true;
		comp.hasChanged = true;
		comp.setComment("Generated on " + LocalDate.now() + " from the table " + table.getTableName()
				+ " and the sequences " + String.join(", ", sequences.values())
				+ ". Configure it with its variables, connect its events. Nothing is regenerated: change it freely.");
		comp.setScriptContent(new FormatedContent(NgxCrudScript.componentScript(this, table, sequences)));

		var title = SqlColumnInfo.prettify(table.getTableName());
		add(comp, compVariable("enableAdd", "true", "false: no Add button."));
		add(comp, compVariable("enableEdit", "true", "false: no Edit button."));
		add(comp, compVariable("enableView", "true", "false: no View button (the form in read-only mode)."));
		add(comp, compVariable("enableDelete", "true", "false: no Delete button, and a single selection."));
		add(comp, compVariable("multiSelect", "true", "false: a single row can be selected, without check boxes."));
		add(comp, compVariable("showId", "true", "false: the columns of the primary key are not shown in the grid."));
		add(comp, compVariable("hiddenColumns", "[]", "Names of the columns not shown in the grid, for example ['notes', 'created_at']."));
		add(comp, compVariable("pageSize", Integer.toString(CrudUiDefaults.PAGE_SIZE), "Number of rows loaded by each call of the list sequence, and rows of a page."));
		add(comp, compVariable("pagination", Boolean.toString(CrudUiDefaults.PAGINATION), "false: continuous scrolling, true: pages of pageSize rows."));
		add(comp, compVariable("height", templateString(CrudUiDefaults.HEIGHT), "Height of the grid, 'auto' to fit the rows."));
		add(comp, compVariable("showTitle", "true", "false: no title in the toolbar (when the page already shows one)."));
		add(comp, compVariable("reloadKey", "0", "Change its value (for example Date.now()) to reload the rows."));
		add(comp, compVariable("labels", NgxCrudScript.labelsLiteral(table, title), "Texts of the buttons, the messages, the form and the columns."));
		add(comp, compEvent("SelectionChanged", "Fired when the selection changes. Data: {rows}, the selected rows."));
		add(comp, compEvent("RowSaved", "Fired when a row is created or updated with the form. Data: {row, mode}."));
		add(comp, compEvent("RowsDeleted", "Fired after a deletion. Data: {rows}, the deleted rows."));
		add(comp, compEvent("FormCancelled", "Fired when the form is closed without saving. Data: {mode}."));

		// no empty bar when the page shows the title and nothing can be done
		var toolbarIf = directive("ToolbarIf", "this.showTitle !== false || this.crudCanCreate() || this.crudCanView() || this.crudCanUpdate() || this.crudCanDelete()");
		add(toolbarIf, buildToolbar());
		add(comp, toolbarIf);
		add(comp, buildGrid(name));
		if (form != null) {
			add(comp, buildModal(form));
		}
		var style = new UIStyle();
		style.setName("Style");
		style.bNew = true;
		style.setStyleContent(new FormatedContent(NgxCrudScript.componentStyle()));
		add(comp, style);
		return comp;
	}

	/** The toolbar: the title, then the actions that the selection allows. */
	private UIComponent buildToolbar() throws EngineException {
		var toolbar = element("Toolbar", "ToolBar", "ion-toolbar", Map.of());
		var titleIf = directive("TitleIf", "this.showTitle !== false");
		var barTitle = element("Title", "BarTitle", "ion-title", Map.of());
		add(barTitle, text("Text", "this.labels?.title"));
		add(titleIf, barTitle);
		add(toolbar, titleIf);

		var buttons = element("Buttons", "ButtonSet", "ion-buttons", Map.of("SetPosition", "plain:end"));
		add(buttons, actionButton("ClearSelection", "this.crudSelectedCount > 1", "close-circle-outline", "medium", "clear",
				"this.crudLabel('clearSelection')", "\t\tpage.crudClearSelection();\n\t\tresolve(null);\n"));
		if (canCreate()) {
			add(buttons, actionButton("Add", "this.crudCanCreate()", "add", "primary", "solid",
					"this.crudLabel('add')", "\t\tpage.crudOpenForm('create', null);\n\t\tresolve(null);\n"));
		}
		add(buttons, actionButton("View", "this.crudCanView() && this.crudSelectedCount == 1", "eye-outline", "primary", "clear",
				"this.crudLabel('view')", "\t\tpage.crudOpenSelected('view');\n\t\tresolve(null);\n"));
		if (canUpdate()) {
			add(buttons, actionButton("Edit", "this.crudCanUpdate() && this.crudSelectedCount == 1", "create-outline", "primary", "clear",
					"this.crudLabel('edit')", "\t\tpage.crudOpenSelected('edit');\n\t\tresolve(null);\n"));
		}
		if (canDelete()) {
			add(buttons, actionButton("Delete", "this.crudCanDelete() && this.crudSelectedCount > 0", "trash-outline", "danger", "clear",
					"this.crudLabel('delete') + (this.crudSelectedCount > 1 ? ' (' + this.crudSelectedCount + ')' : '')",
					"\t\tpage.crudDeleteSelection().then(resolve, reject);\n"));
		}
		add(toolbar, buttons);
		return toolbar;
	}

	/** A button of the toolbar, shown when its condition is true. */
	private static UIComponent actionButton(String name, String condition, String iconName, String color, String fill,
			String label, String code) throws EngineException {
		var shown = directive(name + "If", condition);
		var button = element(name, "Button", "ion-button", ordered("IonColor", "plain:" + color, "IonFill", "plain:" + fill));
		add(button, icon(iconName));
		add(button, text("Text", label));
		add(button, clickAction(name, code));
		add(shown, button);
		return shown;
	}

	private UIComponent buildGrid(String componentName) throws EngineException {
		var grid = new UIUseShared();
		grid.setName("Grid");
		grid.bNew = true;
		grid.setSharedComponentQName(CrudUiDefaults.LIB_GRID);
		var paginated = table.hasListPagination();
		add(grid, useVariable("id", templateString(componentName + "Grid")));
		add(grid, useVariable("rowModelType", paginated ? "'infinite'" : "'clientSide'"));
		if (paginated) {
			add(grid, useVariable("cacheBlockSize", "this.pageSize"));
		} else {
			add(grid, useVariable("rowData", "this.crudRows"));
		}
		add(grid, useVariable("pagination", "this.pagination"));
		add(grid, useVariable("paginationPageSize", "this.pageSize"));
		add(grid, useVariable("paginationPageSizeSelector", "false"));
		add(grid, useVariable("columnDefs", "this.crudColumnDefs"));
		add(grid, useVariable("defaultColDef", "this.crudDefaultColDef"));
		// a click selects one row, the check boxes add rows to the selection
		add(grid, useVariable("rowSelection", "this.crudMultiSelect() ? 'multiple' : 'single'"));
		add(grid, useVariable("suppressRowClickSelection", "false"));
		add(grid, useVariable("suppressFieldDotNotation", "true"));
		add(grid, useVariable("autoSizeColumns", "false"));
		add(grid, useVariable("class", templateString(CrudUiDefaults.GRID_THEME)));
		add(grid, useVariable("height", "this.height"));
		add(grid, useVariable("localeText", templateString(CrudUiDefaults.GRID_LOCALE)));

		var ready = controlEvent("OnGridReady", "GridReady");
		add(ready, customAction("GridReady", "\t\tpage.crudGridReady(event);\n\t\tresolve(null);\n"));
		add(grid, ready);

		if (paginated) {
			var getRows = controlEvent("OnGetRows", "GetRows");
			var load = new UICustomAsyncAction();
			load.setName("LoadRows");
			load.bNew = true;
			load.setComment("Calls the list sequence for the block asked by the grid: order_by, limit and offset.");
			load.setActionValue(new FormatedContent("\t\treturn await page.crudLoadRows(event);\n"));
			var update = new UIDynamicInvoke();
			update.setName("UpdateRows");
			update.bNew = true;
			update.setBeanData(beanData("InvokeAction", Map.of()));
			update.setSharedActionQName(CrudUiDefaults.LIB_GRID_UPDATE_ROWS);
			add(update, useVariable("data", "parent.out"));
			add(update, useVariable("agGridEvent", "event"));
			add(load, update);
			add(getRows, load);
			add(grid, getRows);
		}

		var selection = controlEvent("OnSelectionChanged", "SelectionChanged");
		add(selection, customAction("SelectionChanged", "\t\tpage.crudSelectionChanged();\n\t\tresolve(null);\n"));
		add(grid, selection);
		return grid;
	}

	/** The modal of the component: the form, created again at each opening, to create, edit or view a row. */
	private UIComponent buildModal(UISharedRegularComponent form) throws EngineException {
		var modal = element("FormModal", "InlineModal", "ion-modal", ordered("IsOpen", "script:this.crudFormOpen", "BackdropDismiss", "plain:false"));
		add(modal, attribute("Class", "class", "crud-modal"));
		var dismissed = controlEvent("OnDidDismiss", "didDismiss");
		add(dismissed, customAction("Dismissed", "\t\tpage.crudFormDismissed();\n\t\tresolve(null);\n"));
		add(modal, dismissed);
		var page = element("FormPage", "DivTag", "div", Map.of());
		add(page, attribute("Class", "class", "ion-page"));
		var formIf = directive("FormIf", "this.crudFormOpen");
		var use = new UIUseShared();
		use.setName("Form");
		use.bNew = true;
		use.setSharedComponentQName(form.getQName());
		add(use, useVariable("row", "this.crudFormRow"));
		add(use, useVariable("mode", "this.crudFormMode"));
		add(use, useVariable("labels", "this.labels"));
		var saved = controlEvent("OnRowSaved", "RowSaved");
		add(saved, customAction("RowSaved", "\t\tpage.crudCloseForm(event);\n\t\tresolve(null);\n"));
		add(use, saved);
		var cancelled = controlEvent("OnFormCancelled", "FormCancelled");
		add(cancelled, customAction("FormCancelled", "\t\tpage.crudFormCancelled();\n\t\tresolve(null);\n"));
		add(use, cancelled);
		add(formIf, use);
		add(page, formIf);
		add(modal, page);
		return modal;
	}

	/* ------------------------------------------------------------------ form */

	private UISharedRegularComponent buildForm(String name) throws EngineException {
		var form = new UISharedRegularComponent();
		form.setName(name);
		form.bNew = true;
		form.hasChanged = true;
		form.setComment("Generated on " + LocalDate.now() + " from the table " + table.getTableName()
				+ ": shows a row (mode 'view'), creates it with " + sequences.getOrDefault("INSERT", "(no INSERT)")
				+ " (mode 'create') or updates it with " + sequences.getOrDefault("UPDATE", "(no UPDATE)")
				+ " (mode 'edit'). It can be used in a modal or directly in a page. Nothing is regenerated: change it freely.");
		form.setScriptContent(new FormatedContent(NgxCrudScript.formScript(this, table, sequences)));
		add(form, compVariable("row", "{}", "The row to show or edit, or the initial values of a new row."));
		add(form, compVariable("mode", templateString("create"), "'create', 'edit' or 'view' (read-only)."));
		add(form, compVariable("showTitle", "true", "false: no title above the fields."));
		add(form, compVariable("labels", NgxCrudScript.labelsLiteral(table, SqlColumnInfo.prettify(table.getTableName())), "Texts of the form and labels of the fields."));
		add(form, compEvent("RowSaved", "Fired when the row is saved. Data: {row, mode}."));
		add(form, compEvent("FormCancelled", "Fired when the user cancels or closes the form. Data: {mode}."));

		var headerIf = directive("HeaderIf", "this.showTitle !== false");
		var header = element("Header", "Header", "ion-header", Map.of());
		var toolbar = element("ToolBar", "ToolBar", "ion-toolbar", Map.of());
		var title = element("Title", "BarTitle", "ion-title", Map.of());
		add(title, text("Text", "this.crudLabel(this.mode == 'edit' ? 'editTitle' : this.mode == 'view' ? 'viewTitle' : 'createTitle')"));
		add(toolbar, title);
		add(header, toolbar);
		add(headerIf, header);
		add(form, headerIf);

		var body = element("Body", "DivTag", "div", Map.of());
		add(body, attribute("Class", "class", "crud-form-body ion-padding"));
		var list = element("Fields", "List", "ion-list", Map.of());
		for (var column : table.getDisplayedColumns()) {
			add(list, field(column));
		}
		add(body, list);
		add(form, body);

		var footer = element("Footer", "Footer", "ion-footer", Map.of());
		var footerBar = element("ToolBar", "ToolBar", "ion-toolbar", Map.of());
		var buttons = element("Buttons", "ButtonSet", "ion-buttons", Map.of("SetPosition", "plain:end"));
		var cancel = element("Cancel", "Button", "ion-button", ordered("IonFill", "plain:clear"));
		add(cancel, text("Text", "this.crudLabel(this.mode == 'view' ? 'close' : 'cancel')"));
		add(cancel, clickAction("Cancel", "\t\tpage.crudCancel();\n\t\tresolve(null);\n"));
		add(buttons, cancel);
		var saveIf = directive("SaveIf", "this.mode != 'view'");
		var save = element("Save", "Button", "ion-button", ordered("IonColor", "plain:primary", "IonFill", "plain:solid",
				"Disabled", "script:!this.crudIsValid() || this.crudSaving"));
		add(save, text("Text", "this.crudLabel('save')"));
		add(save, clickAction("Save", "\t\tpage.crudSave().then(resolve, reject);\n"));
		add(saveIf, save);
		add(buttons, saveIf);
		add(footerBar, buttons);
		add(footer, footerBar);
		add(form, footer);

		var style = new UIStyle();
		style.setName("Style");
		style.bNew = true;
		style.setStyleContent(new FormatedContent(NgxCrudScript.formStyle()));
		add(form, style);
		return form;
	}

	/** One field of the form: a typed input, a text area or a toggle, bound to the draft row. */
	private UIComponent field(SqlColumnInfo column) throws EngineException {
		var name = column.getName();
		var label = "script:this.crudFieldLabel(" + templateString(name) + ")";
		var binding = "script:crudDraft[" + templateString(name) + "]";
		var readOnly = column.isReadOnlyType() ? "script:true"
				: column.isReadOnlyOnEdit() ? "script:this.mode != 'create'" : "script:this.mode == 'view'";
		var kind = column.getKind();
		UIDynamicElement widget;
		if (kind == SqlFieldKind.BOOLEAN) {
			widget = element("Toggle", "Toggle", "ion-toggle", ordered("ControlName", "plain:" + name, "DoubleBinding", binding,
					"Disabled", readOnly.equals("script:true") ? readOnly : readOnly + " || this.crudSaving", "LabelPlacement", "plain:start", "Justify", "plain:space-between"));
			add(widget, text("Text", "this.crudFieldLabel(" + templateString(name) + ")"));
		} else {
			var properties = ordered("ControlName", "plain:" + name, "DoubleBinding", binding, "Label", label,
					"LabelPlacement", "plain:stacked", "Readonly", readOnly, "Required", "script:" + column.isRequired());
			if (!column.getRemarks().isEmpty()) {
				// a plain attribute value: no double quote
				properties.put("HelperText", "plain:" + column.getRemarks().replace('"', '\''));
			}
			if (kind == SqlFieldKind.LONG_TEXT) {
				properties.put("AutoGrow", "script:true");
				widget = element("TextArea", "TextArea", "ion-textarea", properties);
			} else {
				properties.put("Type", "plain:" + (kind.isNumber() ? "number" : kind == SqlFieldKind.OTHER ? "text" : kind.inputType()));
				if (!column.getStep().isEmpty()) {
					properties.put("Step", "plain:" + column.getStep());
				}
				if (column.getMaxLength() > 0) {
					properties.put("MaxLength", "plain:" + column.getMaxLength());
				}
				widget = element("Input", "Input", "ion-input", properties);
			}
		}
		var item = element("Field_" + StringUtils.normalize(name), "FormItem", "ion-item", Map.of());
		add(item, widget);
		if (!column.isHiddenOnCreate()) {
			return item;
		}
		var onlyInEdition = directive("If_" + StringUtils.normalize(name), "this.mode != 'create'");
		add(onlyInEdition, item);
		return onlyInEdition;
	}

	private static com.twinsoft.convertigo.beans.ngx.components.UIAttribute attribute(String name, String attrName, String value) throws EngineException {
		var attribute = new com.twinsoft.convertigo.beans.ngx.components.UIAttribute();
		attribute.setName(name);
		attribute.bNew = true;
		attribute.setAttrName(attrName);
		attribute.setAttrSmartType(new MobileSmartSourceType(value));
		return attribute;
	}

	/* ------------------------------------------------------------------ host page */

	private PageComponent buildPage(String name, UISharedRegularComponent component) throws EngineException {
		var page = new PageComponent();
		page.setName(name);
		page.bNew = true;
		page.hasChanged = true;
		var title = SqlColumnInfo.prettify(table.getTableName());
		page.setInAutoMenu(true);
		page.setComment("Generated on " + LocalDate.now() + " to host the shared component " + component.getName() + ".");

		var header = element("Header", "Header", "ion-header", Map.of());
		var toolbar = element("ToolBar", "ToolBar", "ion-toolbar", Map.of());
		var start = element("ButtonSet", "ButtonSet", "ion-buttons", Map.of("SetPosition", "plain:start"));
		add(start, element("MenuButton", "MenuButton", "ion-menu-button", Map.of()));
		add(toolbar, start);
		var barTitle = element("BarTitle", "BarTitle", "ion-title", Map.of());
		var text = new UIText();
		text.setName("Text");
		text.bNew = true;
		text.setTextSmartType(new MobileSmartSourceType(title));
		add(barTitle, text);
		add(toolbar, barTitle);
		add(header, toolbar);
		add(page, header);

		var content = element("Content", "Content", "ion-content", Map.of());
		var use = new UIUseShared();
		use.setName(component.getName());
		use.bNew = true;
		use.setSharedComponentQName(component.getQName());
		add(use, useVariable("showTitle", "false"));
		add(content, use);
		add(page, content);
		return page;
	}

	/* ------------------------------------------------------------------ bean helpers */

	private static void add(DatabaseObject parent, DatabaseObject child) throws EngineException {
		parent.add(child);
	}

	private static UICompVariable compVariable(String name, String value, String comment) throws EngineException {
		var variable = new UICompVariable();
		variable.setName(name);
		variable.bNew = true;
		variable.setVariableValue(value);
		variable.setComment(comment);
		return variable;
	}

	private static UICompEvent compEvent(String name, String comment) throws EngineException {
		var event = new UICompEvent();
		event.setName(name);
		event.bNew = true;
		event.setComment(comment);
		return event;
	}

	private static UIUseVariable useVariable(String name, String script) throws EngineException {
		var variable = new UIUseVariable();
		variable.setName(name);
		variable.bNew = true;
		variable.setVarSmartType(script(script));
		return variable;
	}

	private static UIControlEvent controlEvent(String name, String eventName) throws EngineException {
		var event = new UIControlEvent();
		event.setName(name);
		event.bNew = true;
		event.setEventName(eventName);
		return event;
	}

	private static UICustomAction customAction(String name, String code) throws EngineException {
		var action = new UICustomAction();
		action.setName(name);
		action.bNew = true;
		action.setActionValue(new FormatedContent(code));
		return action;
	}

	private static UIControlEvent clickAction(String name, String code) throws EngineException {
		var event = controlEvent("OnClick", "onClick");
		add(event, customAction(name, code));
		return event;
	}

	private static UIControlDirective directive(String name, String condition) throws EngineException {
		var directive = new UIControlDirective();
		directive.setName(name);
		directive.bNew = true;
		directive.setDirectiveName(UIControlDirective.AttrDirective.If.name());
		directive.setSourceSmartType(script(condition));
		return directive;
	}

	private static UIText text(String name, String script) throws EngineException {
		var text = new UIText();
		text.setName(name);
		text.bNew = true;
		text.setTextSmartType(script(script));
		return text;
	}

	private static UIDynamicElement icon(String iconName) throws EngineException {
		return element("Icon", "Icon", "ion-icon", ordered("IconName", "plain:" + iconName, "IconSlot", "plain:start"));
	}

	static UIDynamicElement element(String name, String ionBean, String tag, Map<String, String> properties) throws EngineException {
		var element = new UIDynamicElement(tag);
		element.setName(name);
		element.bNew = true;
		element.setBeanData(beanData(ionBean, properties));
		return element;
	}

	static Map<String, String> ordered(String... keyValues) {
		var map = new LinkedHashMap<String, String>();
		for (int i = 0; i < keyValues.length; i += 2) {
			map.put(keyValues[i], keyValues[i + 1]);
		}
		return map;
	}

	/**
	 * The bean data of a template component: its name and only the properties set here ("mode:value"), the others
	 * come from the model of the template when the bean is loaded.
	 */
	static String beanData(String ionBean, Map<String, String> properties) {
		var sb = new StringBuilder("{\"name\":").append(jsString(ionBean)).append(",\"properties\":{");
		var first = true;
		for (var entry : properties.entrySet()) {
			var smart = entry.getValue().split(":", 2);
			if (!first) {
				sb.append(',');
			}
			first = false;
			sb.append(jsString(entry.getKey())).append(":{\"name\":").append(jsString(entry.getKey()))
				.append(",\"mode\":").append(jsString(smart[0]))
				.append(",\"value\":").append(jsString(smart.length > 1 ? smart[1] : "")).append('}');
		}
		return sb.append("}}").toString();
	}

	static MobileSmartSourceType script(String expression) {
		var msst = new MobileSmartSourceType();
		msst.setMode(Mode.SCRIPT);
		msst.setSmartValue(expression);
		return msst;
	}

	/**
	 * A TypeScript string literal for an expression of the generated template: single quotes, because the template
	 * writes the expression between double quotes ([attr]="expression").
	 */
	static String templateString(String value) {
		return "'" + value.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\x22") + "'";
	}

	/** A JSON (and TypeScript) string literal. */
	static String jsString(String value) {
		var sb = new StringBuilder("\"");
		for (var c : value.toCharArray()) {
			switch (c) {
			case '"' -> sb.append("\\\"");
			case '\\' -> sb.append("\\\\");
			case '\n' -> sb.append("\\n");
			case '\r' -> sb.append("\\r");
			case '\t' -> sb.append("\\t");
			// a value never closes the comment markers of the generated sources
			case '/' -> sb.append(sb.charAt(sb.length() - 1) == '*' ? "\\/" : "/");
			default -> {
				if (c < 0x20) {
					sb.append(String.format("\\u%04x", (int) c));
				} else {
					sb.append(c);
				}
			}
			}
		}
		return sb.append('"').toString();
	}
}
