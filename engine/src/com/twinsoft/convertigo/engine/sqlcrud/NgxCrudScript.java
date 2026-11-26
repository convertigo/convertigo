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

import static com.twinsoft.convertigo.engine.sqlcrud.NgxCrudGenerator.jsString;
import static com.twinsoft.convertigo.engine.sqlcrud.NgxCrudGenerator.templateString;

import java.util.Map;

/** TypeScript and style sources of the generated CRUD shared component. */
final class NgxCrudScript {

	private NgxCrudScript() {
	}

	/** The labels variable: the texts of the user interface, the title and the label of each column. */
	static String labelsLiteral(SqlTableInfo table, String title) {
		// single quotes only: a default value with double quotes breaks the template of a UIUseShared
		var sb = new StringBuilder("{'title': ").append(templateString(title));
		for (var entry : CrudUiDefaults.labels().entrySet()) {
			sb.append(", ").append(templateString(entry.getKey())).append(": ").append(templateString(entry.getValue()));
		}
		sb.append(", 'fields': {");
		var first = true;
		for (var column : table.getDisplayedColumns()) {
			sb.append(first ? "" : ", ").append(templateString(column.getName())).append(": ").append(templateString(column.getLabel()));
			first = false;
		}
		return sb.append("}}").toString();
	}

	/** The metadata of the displayed columns, written in the component as a literal. */
	static String fieldsLiteral(SqlTableInfo table) {
		var sb = new StringBuilder("[");
		var first = true;
		for (var column : table.getDisplayedColumns()) {
			sb.append(first ? "\n" : ",\n").append("\t\t{");
			first = false;
			sb.append("name: ").append(jsString(column.getName()));
			sb.append(", kind: ").append(jsString(column.getKind().name()));
			sb.append(", input: ").append(jsString(column.getKind().inputType()));
			sb.append(", key: ").append(column.isPrimaryKey());
			sb.append(", required: ").append(column.isRequired());
			sb.append(", hiddenOnCreate: ").append(column.isHiddenOnCreate());
			sb.append(", readOnlyOnEdit: ").append(column.isReadOnlyOnEdit());
			sb.append(", readOnly: ").append(column.isReadOnlyType());
			sb.append(", maxLength: ").append(column.getMaxLength());
			sb.append(", step: ").append(jsString(column.getStep()));
			sb.append(", scale: ").append(Math.max(0, column.getScale()));
			sb.append(", nullable: ").append(!column.isNotNull());
			sb.append(", createDefault: ").append(column.getCreateDefault() == null ? "null" : jsString(column.getCreateDefault()));
			sb.append(", help: ").append(jsString(column.getRemarks()));
			sb.append('}');
		}
		return sb.append(first ? "]" : "\n\t]").toString();
	}

	static String componentScript(NgxCrudGenerator generator, SqlTableInfo table, Map<String, String> sequences) {
		var seq = new StringBuilder("{");
		for (var verb : new String[] { "list", "count", "insert", "update", "delete" }) {
			var qname = sequences.get(verb.toUpperCase());
			seq.append(seq.length() > 1 ? ", " : "").append(verb).append(": ").append(qname == null ? "null" : jsString(qname));
		}
		seq.append('}');
		var keys = new StringBuilder("[");
		for (var key : table.getPrimaryKeys()) {
			keys.append(keys.length() > 1 ? ", " : "").append(jsString(key.getName()));
		}
		keys.append(']');
		var can = "{create: " + generator.canCreate() + ", update: " + generator.canUpdate() + ", delete: " + generator.canDelete() + "}";

		return SCRIPT
				.replace("@@TABLE@@", table.getTableName().replace("*/", "* /"))
				.replace("@@SEQUENCES@@", seq)
				.replace("@@FIELDS@@", fieldsLiteral(table))
				.replace("@@KEYS@@", keys)
				.replace("@@MAX_LIMIT@@", Integer.toString(SqlCrudQuery.MAX_LIMIT))
				.replace("@@PAGINATED@@", Boolean.toString(table.hasListPagination()))
				.replace("@@CAN@@", can)
				.replace("@@EMPTY_NULL@@", Boolean.toString(CrudUiDefaults.EMPTY_FIELD_IS_NULL))
				.replace("@@FILTER@@", Boolean.toString(CrudUiDefaults.COLUMN_FILTERS))
				.replace("@@COMMON@@\n", COMMON);
	}

	static String formScript(NgxCrudGenerator generator, SqlTableInfo table, Map<String, String> sequences) {
		var seq = "{insert: " + qname(sequences, "INSERT", generator.canCreate()) + ", update: " + qname(sequences, "UPDATE", generator.canUpdate()) + "}";
		return FORM_SCRIPT
				.replace("@@TABLE@@", table.getTableName().replace("*/", "* /"))
				.replace("@@SEQUENCES@@", seq)
				.replace("@@FIELDS@@", fieldsLiteral(table))
				.replace("@@UPDATE_PARAMETER@@", jsString(table.getUpdateParameter()))
				.replace("@@EMPTY_NULL@@", Boolean.toString(CrudUiDefaults.EMPTY_FIELD_IS_NULL))
				.replace("@@COMMON@@\n", COMMON);
	}

	private static String qname(Map<String, String> sequences, String verb, boolean allowed) {
		var qname = sequences.get(verb);
		return allowed && qname != null ? jsString(qname) : "null";
	}

	static String formStyle() {
		return """
				:host {
					display: block;
				}
				.crud-form-body {
					max-height: 65vh;
					overflow: auto;
				}
				""";
	}

	static String componentStyle() {
		return """
				:host {
					display: block;
				}
				:host ::ng-deep .ag-theme-quartz {
					--ag-header-height: 44px;
					--ag-border-color: #eef2f6;
					--ag-row-hover-color: #f8fafc;
					--ag-wrapper-border-radius: 12px;
					--ag-font-family: inherit;
				}
				::ng-deep .crud-alert-danger {
					color: var(--ion-color-danger) !important;
				}
				::ng-deep ion-modal.crud-modal {
					--width: min(720px, 100%);
					--height: auto;
					--max-height: 90vh;
					--border-radius: 12px;
				}
				::ng-deep ion-modal.crud-modal .ion-page {
					position: relative;
					display: block;
					contain: content;
				}
				""";
	}

	private static final String SCRIPT = """
			/*Begin_c8o_CompImport*/
			/*End_c8o_CompImport*/
			/*Begin_c8o_CompDeclaration*/
				// Generated from the table @@TABLE@@: its sequences, its columns and what the user interface may do with them.
				public crudSequences: any = @@SEQUENCES@@;
				public crudFields: any[] = @@FIELDS@@;
				public crudKeys: string[] = @@KEYS@@;
				public crudPaginated: boolean = @@PAGINATED@@;
				public crudCan: any = @@CAN@@;
				public crudEmptyIsNull: boolean = @@EMPTY_NULL@@;
				public crudDefaultColDef: any = {sortable: true, resizable: true, filter: @@FILTER@@, cellDataType: false};
				public crudColumnDefs: any[] = [];
				public crudGridApi: any = null;
				public crudRows: any[] = [];
				public crudTotal: Promise<any> = null;
				public crudSelectedRows: any[] = [];
				public crudSelectedCount: number = 0;
				public crudFormOpen: boolean = false;
				public crudFormMode: string = "create";
				public crudFormRow: any = {};
				/*End_c8o_CompDeclaration*/
			/*Begin_c8o_CompConstructor*/
					/*End_c8o_CompConstructor*/
			/*Begin_c8o_CompInitialization*/
					this.crudColumnDefs = this.crudBuildColumnDefs();
					/*End_c8o_CompInitialization*/
			/*Begin_c8o_CompFinalization*/
					/*End_c8o_CompFinalization*/
			/*Begin_c8o_CompChanges*/
					if (["labels", "hiddenColumns", "showId", "multiSelect", "enableDelete"].some((name: string) => changes?.[name] != null)) {
						this.crudColumnDefs = this.crudBuildColumnDefs();
					}
					if (changes?.reloadKey != null && !changes.reloadKey.firstChange) {
						this.crudRefresh();
					}
					/*End_c8o_CompChanges*/
			/*Begin_c8o_CompDoCheck*/
					/*End_c8o_CompDoCheck*/
			/*Begin_c8o_CompAfterContentInit*/
					/*End_c8o_CompAfterContentInit*/
			/*Begin_c8o_CompAfterContentChecked*/
					/*End_c8o_CompAfterContentChecked*/
			/*Begin_c8o_CompAfterViewInit*/
					/*End_c8o_CompAfterViewInit*/
			/*Begin_c8o_CompAfterViewChecked*/
					/*End_c8o_CompAfterViewChecked*/
			/*Begin_c8o_CompFunction*/
			@@COMMON@@
				crudCanCreate(): boolean {
					return this.crudCan.create && this.enableAdd !== false;
				}

				crudCanUpdate(): boolean {
					return this.crudCan.update && this.enableEdit !== false;
				}

				crudCanView(): boolean {
					return this.enableView !== false;
				}

				crudCanDelete(): boolean {
					return this.crudCan.delete && this.enableDelete !== false;
				}

				/** Check boxes and a multiple selection, only useful to delete several rows. */
				crudMultiSelect(): boolean {
					return this.multiSelect !== false && this.crudCanDelete();
				}

				crudBuildColumnDefs(): any[] {
					const defs: any[] = [];
					if (this.crudMultiSelect()) {
						defs.push({colId: "_crudSelect", headerName: "", checkboxSelection: true, width: 44, minWidth: 44, maxWidth: 44,
							suppressSizeToFit: true, pinned: "left", lockPosition: "left", sortable: false, resizable: false, suppressMovable: true});
					}
					const hidden: string[] = this.hiddenColumns ?? [];
					for (const field of this.crudFields) {
						if (hidden.includes(field.name) || (field.key && this.showId === false)) {
							continue;
						}
						const def: any = {field: field.name, colId: field.name, headerName: this.labels?.fields?.[field.name] ?? field.name};
						if (field.help) {
							def.headerTooltip = field.help;
						}
						switch (field.kind) {
							case "INTEGER": case "BIGINT": case "FLOAT":
								def.type = "rightAligned";
								break;
							case "DECIMAL":
								def.type = "rightAligned";
								def.valueFormatter = (params: any) => this.crudFormatDecimal(params.value, field.scale);
								break;
							case "BOOLEAN":
								def.cellRenderer = "agCheckboxCellRenderer";
								def.valueGetter = (params: any) => params.data == null ? null : this.crudBoolean(params.data[field.name]);
								break;
							case "TIMESTAMP":
								def.valueFormatter = (params: any) => this.crudDateTime(params.value);
								break;
							case "TIME":
								def.valueFormatter = (params: any) => params.value ? String(params.value).substring(0, 5) : "";
								break;
							case "LONG_TEXT":
								def.tooltipField = field.name;
								break;
						}
						defs.push(def);
					}
					return defs;
				}

				crudFormatDecimal(value: any, scale: number): string {
					if (value == null || value === "") {
						return "";
					}
					const n = Number(value);
					if (isNaN(n)) {
						return String(value);
					}
					return n.toLocaleString(undefined, {minimumFractionDigits: scale, maximumFractionDigits: Math.max(scale, 3)});
				}

				crudBoolean(value: any): boolean {
					return value === true || value === 1 || value === "1" || String(value).toLowerCase() === "true";
				}

				crudDateTime(value: any): string {
					if (value == null || value === "") {
						return "";
					}
					const text = String(value).replace("T", " ");
					const dot = text.indexOf(".");
					return dot > 0 ? text.substring(0, dot) : text;
				}

				crudPageSize(): number {
					return Math.max(1, Math.min(@@MAX_LIMIT@@, Math.floor(Number(this.pageSize)) || 50));
				}

				crudSort(sortModel: any[]): any {
					const sort = (sortModel ?? [])[0];
					const field = sort == null ? null : this.crudFields.find((f: any) => f.name == sort.colId);
					return field == null ? {} : {sort_key: field.name, sort_direction: sort.sort == "desc" ? "desc" : "asc"};
				}

				/** The number of rows, by the count sequence, once per reload; undefined without it or when it fails. */
				crudCount(): Promise<any> {
					if (this.crudSequences.count == null) {
						return Promise.resolve(undefined);
					}
					if (this.crudTotal == null) {
						this.crudTotal = this.crudCall(this.crudSequences.count, {}).then((res: any) => {
							const row = ([] as any[]).concat(res?.row ?? [])[0];
							const total = row == null ? NaN : Number(Object.values(row)[0]);
							return isNaN(total) ? undefined : total;
						}).catch(() => undefined);
					}
					return this.crudTotal;
				}

				async crudLoadRows(params: any): Promise<any> {
					const limit = params.limit ?? (params.endRow - params.startRow);
					const offset = params.skip ?? params.startRow ?? 0;
					try {
						const [res, total] = await Promise.all([
							this.crudCall(this.crudSequences.list, {...this.crudSort(params.sortModel), limit: limit, offset: offset}),
							this.crudCount()
						]);
						const rows = ([] as any[]).concat(res?.row ?? []);
						// with the count, the grid knows its size at once: pages, last page and scroll bar
						const lastRow = total != null ? total : rows.length < limit ? offset + rows.length : undefined;
						return {RowData: rows, lastRow: lastRow};
					} catch (error) {
						this.crudToast(this.crudLabel("loadFailed") + ": " + this.crudMessage(error), "danger");
						return {RowData: [], lastRow: offset};
					}
				}

				async crudLoadAll(): Promise<void> {
					try {
						const res = await this.crudCall(this.crudSequences.list, {});
						this.crudRows = ([] as any[]).concat(res?.row ?? []);
					} catch (error) {
						this.crudRows = [];
						this.crudToast(this.crudLabel("loadFailed") + ": " + this.crudMessage(error), "danger");
					}
					this.ref.markForCheck();
				}

				crudGridReady(event: any): void {
					this.crudGridApi = event?.api ?? event?.object?.api ?? null;
					if (!this.crudPaginated) {
						this.crudLoadAll();
					}
				}

				crudSelectionChanged(): void {
					const rows = (this.crudGridApi?.getSelectedNodes() ?? []).map((node: any) => node.data).filter((row: any) => row != null);
					this.crudSelectedRows = rows;
					this.crudSelectedCount = rows.length;
					this.ref.markForCheck();
					this.actionBeans.EmitEventAction(this, {event: ".SelectionChanged", data: {rows: rows}}, null);
				}

				crudClearSelection(): void {
					this.crudGridApi?.deselectAll();
				}

				crudRefresh(): void {
					this.crudReload(false);
				}

				/** Reloads the rows and the count; the selection is kept after an edition, the rows do not move. */
				crudReload(keepSelection: boolean): void {
					this.crudTotal = null;
					if (!keepSelection) {
						this.crudClearSelection();
						this.crudSelectedRows = [];
						this.crudSelectedCount = 0;
					}
					if (this.crudPaginated) {
						this.crudGridApi?.refreshInfiniteCache();
					} else {
						this.crudLoadAll();
					}
					this.ref.markForCheck();
				}

				crudNewRow(): any {
					const row: any = {};
					for (const field of this.crudFields) {
						if (field.createDefault != null) {
							row[field.name] = field.kind == "BOOLEAN" ? this.crudBoolean(field.createDefault) : field.createDefault;
						}
					}
					return row;
				}

				/** Opens the form in a modal: 'create' a row, 'edit' or 'view' the given one. */
				crudOpenForm(mode: string, row: any): void {
					const allowed = mode == "create" ? this.crudCanCreate() : mode == "edit" ? this.crudCanUpdate() : this.crudCanView();
					if (!allowed) {
						return;
					}
					this.crudFormMode = mode;
					this.crudFormRow = mode == "create" ? this.crudNewRow() : {...row};
					this.crudFormOpen = true;
					this.ref.markForCheck();
				}

				/** Opens the form on the only selected row. */
				crudOpenSelected(mode: string): void {
					if (this.crudSelectedRows.length == 1) {
						this.crudOpenForm(mode, this.crudSelectedRows[0]);
					}
				}

				crudFormDismissed(): void {
					if (this.crudFormOpen) {
						this.crudFormOpen = false;
						this.ref.markForCheck();
					}
				}

				crudCloseForm(saved: any): void {
					this.crudFormOpen = false;
					if (saved != null) {
						this.crudToast(this.crudLabel("saved"), "success");
						const edited = this.crudFormMode == "edit";
						if (edited && this.crudSelectedRows.length == 1) {
							// the selection stays on the edited row, with its saved values, while the grid reloads
							this.crudSelectedRows = [{...this.crudSelectedRows[0], ...(saved.row ?? {})}];
						}
						this.crudReload(edited);
						this.actionBeans.EmitEventAction(this, {event: ".RowSaved", data: {row: saved.row ?? saved, mode: this.crudFormMode}}, null);
					}
					this.ref.markForCheck();
				}

				crudFormCancelled(): void {
					this.crudCloseForm(null);
					this.actionBeans.EmitEventAction(this, {event: ".FormCancelled", data: {mode: this.crudFormMode}}, null);
				}

				crudKey(row: any): any {
					const key: any = {};
					for (const name of this.crudKeys) {
						key[name] = row[name];
					}
					return key;
				}

				async crudDeleteSelection(): Promise<void> {
					const rows = [...this.crudSelectedRows];
					if (rows.length == 0 || !this.crudCanDelete()) {
						return;
					}
					const alert = await this.getInstance(AlertController).create({
						header: this.crudLabel("confirmTitle"),
						message: this.crudLabel("confirmDelete", rows.length),
						buttons: [
							{text: this.crudLabel("cancel"), role: "cancel"},
							{text: this.crudLabel("delete"), role: "destructive", cssClass: "crud-alert-danger"}
						]
					});
					await alert.present();
					const choice = await alert.onDidDismiss();
					if (choice.role != "destructive") {
						return;
					}
					const deleted: any[] = [];
					const failures: string[] = [];
					for (const row of rows) {
						try {
							await this.crudCall(this.crudSequences.delete, this.crudKey(row));
							deleted.push(row);
						} catch (error) {
							failures.push(this.crudMessage(error));
						}
					}
					let message = this.crudLabel("deleted", deleted.length);
					if (failures.length > 0) {
						message += " - " + this.crudLabel("deleteFailed", deleted.length, failures.length) + ": " + failures[0];
					}
					this.crudToast(message, failures.length > 0 ? "warning" : "success");
					this.crudRefresh();
					if (deleted.length > 0) {
						this.actionBeans.EmitEventAction(this, {event: ".RowsDeleted", data: {rows: deleted}}, null);
					}
				}
				/*End_c8o_CompFunction*/
			""";

	/** Methods of both components. */
	private static final String COMMON = """
				crudLabel(key: string, n?: any, m?: any): string {
					const text = String(this.labels?.[key] ?? key);
					return text.split("{n}").join(n ?? "").split("{m}").join(m ?? "");
				}

				crudMessage(error: any): string {
					return error?.message ?? String(error);
				}

				async crudCall(requestable: string, data: any): Promise<any> {
					const res = await this.call(requestable, data);
					if (res?.error != null) {
						const error = res.error;
						throw new Error(typeof error == "string" ? error : (error.message ?? error.exception ?? JSON.stringify(error)));
					}
					return res;
				}

				async crudToast(message: string, color: string): Promise<void> {
					const toast = await this.getInstance(ToastController).create({message: message, color: color, duration: 3000, position: "top"});
					await toast.present();
				}
			""";

	private static final String FORM_SCRIPT = """
			/*Begin_c8o_CompImport*/
			/*End_c8o_CompImport*/
			/*Begin_c8o_CompDeclaration*/
				// Generated from the table @@TABLE@@: the editable columns and the sequences that save a row.
				public crudSequences: any = @@SEQUENCES@@;
				public crudFields: any[] = @@FIELDS@@;
				public crudEmptyIsNull: boolean = @@EMPTY_NULL@@;
				public crudDraft: any = {};
				public crudOriginalDraft: any = {};
				public crudUpdateParameter: string = @@UPDATE_PARAMETER@@;
				public crudSaving: boolean = false;
				/*End_c8o_CompDeclaration*/
			/*Begin_c8o_CompConstructor*/
					/*End_c8o_CompConstructor*/
			/*Begin_c8o_CompInitialization*/
					this.crudPrepare();
					/*End_c8o_CompInitialization*/
			/*Begin_c8o_CompFinalization*/
					/*End_c8o_CompFinalization*/
			/*Begin_c8o_CompChanges*/
					if (changes?.row != null || changes?.mode != null) {
						this.crudPrepare();
					}
					/*End_c8o_CompChanges*/
			/*Begin_c8o_CompDoCheck*/
					/*End_c8o_CompDoCheck*/
			/*Begin_c8o_CompAfterContentInit*/
					/*End_c8o_CompAfterContentInit*/
			/*Begin_c8o_CompAfterContentChecked*/
					/*End_c8o_CompAfterContentChecked*/
			/*Begin_c8o_CompAfterViewInit*/
					/*End_c8o_CompAfterViewInit*/
			/*Begin_c8o_CompAfterViewChecked*/
					/*End_c8o_CompAfterViewChecked*/
			/*Begin_c8o_CompFunction*/
			@@COMMON@@
				crudFieldLabel(name: string): string {
					const field = this.crudFields.find((f: any) => f.name == name);
					const label = this.labels?.fields?.[name] ?? name;
					return field?.required && this.mode != "view" ? label + " *" : label;
				}

				crudVisible(field: any): boolean {
					return !(field.hiddenOnCreate && this.mode == "create");
				}

				crudIsEmpty(value: any): boolean {
					return value == null || (typeof value == "string" && value.trim() == "");
				}

				crudBoolean(value: any): boolean {
					return value === true || value === 1 || value === "1" || String(value).toLowerCase() === "true";
				}

				/** Store the initial input representation separately from the original database row. */
				crudPrepare(): void {
					const row = this.row ?? {};
					const draft: any = Object.create(null);
					for (const field of this.crudFields) {
						const value = row[field.name];
						if (field.kind == "BOOLEAN") {
							const bool = this.crudBoolean(value);
							draft[field.name] = !field.nullable ? bool
								: this.crudIsEmpty(value) ? "unset" : String(bool);
						} else if (value != null) {
							draft[field.name] = field.kind == "TIMESTAMP" ? String(value).replace(" ", "T")
								: field.kind == "DATE" ? String(value).substring(0, 10) : value;
						}
					}
					this.crudDraft = draft;
					this.crudOriginalDraft = {...draft};
				}

				crudIsValid(): boolean {
					if (this.mode == "view") {
						return true;
					}
					for (const field of this.crudFields) {
						if (field.required && field.kind != "BOOLEAN" && this.crudVisible(field) && this.crudIsEmpty(this.crudDraft[field.name])) {
							return false;
						}
					}
					return true;
				}

				crudValue(field: any, value: any): any {
					if (this.crudIsEmpty(value) || (field.kind == "BOOLEAN" && value === "unset")) {
						return undefined;
					}
					switch (field.kind) {
						case "INTEGER": case "BIGINT": case "DECIMAL": case "FLOAT":
							return String(value).split(" ").join("").replace(",", ".");
						case "BOOLEAN":
							return String(this.crudBoolean(value));
						case "DATE":
							return String(value).substring(0, 10);
						case "TIMESTAMP": {
							const text = String(value).replace("T", " ");
							return text.length == 16 ? text + ":00" : text;
						}
						case "TIME": {
							const text = String(value);
							return text.length == 5 ? text + ":00" : text;
						}
						default:
							return value;
					}
				}

				/** For edits, only the columns in the field list are assigned. An empty value clears one. */
				crudNormalize(): any {
					const data: any = {};
					const changed: string[] = [];
					for (const field of this.crudFields) {
						if (!this.crudVisible(field) || field.readOnly) {
							continue;
						}
						const name = field.name;
						const value = this.crudValue(field, this.crudDraft[name]);
						if (this.mode == "edit" && !field.key) {
							if (field.readOnlyOnEdit || value === this.crudValue(field, this.crudOriginalDraft[name])) {
								continue;
							}
							changed.push(name);
						}
						if (value !== undefined) {
							data[name] = value;
						} else if (this.mode == "edit" || !this.crudEmptyIsNull) {
							data[name] = "";
						}
					}
					if (this.mode == "edit") {
						data[this.crudUpdateParameter] = changed.join(",");
					}
					return data;
				}

				async crudSave(): Promise<void> {
					const sequence = this.mode == "edit" ? this.crudSequences.update : this.mode == "create" ? this.crudSequences.insert : null;
					if (this.crudSaving || sequence == null || !this.crudIsValid()) {
						return;
					}
					this.crudSaving = true;
					this.ref.markForCheck();
					try {
						const data = this.crudNormalize();
						const editing = this.mode == "edit";
						const changed = editing ? data[this.crudUpdateParameter] : "";
						if (!editing || changed !== "") {
							await this.crudCall(sequence, data);
						}
						const row = editing ? {...this.row} : {};
						for (const name of editing ? changed.split(",").filter(Boolean) : Object.keys(data)) {
							const field = this.crudFields.find((f: any) => f.name === name);
							row[name] = data[name] === "" ? null : field?.kind === "BOOLEAN" ? data[name] === "true" : this.crudDraft[name];
						}
						this.actionBeans.EmitEventAction(this, {event: ".RowSaved", data: {row: row, mode: this.mode}}, null);
					} catch (error) {
						this.crudToast(this.crudMessage(error), "danger");
					} finally {
						this.crudSaving = false;
						this.ref.markForCheck();
					}
				}

				crudCancel(): void {
					this.actionBeans.EmitEventAction(this, {event: ".FormCancelled", data: {mode: this.mode}}, null);
				}
				/*End_c8o_CompFunction*/
			""";
}
