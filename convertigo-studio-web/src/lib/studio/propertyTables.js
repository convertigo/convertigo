/**
 * The table properties of the objects, as the table editors of the Eclipse Studio describe them: their
 * columns, the cells of a new row and the choices of a column. A table of another editor gets numbered
 * columns. An engine without the Eclipse Studio gives no editor class, the editor is then found by the
 * name of the property. Some columns are step sources, a priority and an xpath the source picker
 * chooses.
 *
 * @typedef {{ title: string, columns: string[], template: (string | string[])[], choices?: Record<number, string[]>, sources?: number[] }} PropertyTableEditor
 */

/** @type {Record<string, PropertyTableEditor>} */
export const PROPERTY_TABLE_EDITORS = {
	ActionsEditor: {
		title: 'Actions',
		columns: ['Label', 'Command'],
		template: ['label', 'command']
	},
	ActionStepSourcesEditor: {
		title: 'Action sources',
		columns: ['Description', 'Source', 'Default value'],
		template: ['description', [], ''],
		sources: [1]
	},
	ActionsForSelectionColumnEditor: {
		title: 'Actions for selection column',
		columns: ['Value', 'Key', 'Label'],
		template: ['value', 'key', 'label']
	},
	ArrayOrNullEditor: { title: 'Array', columns: ['Value'], template: ['val'] },
	BrowserDefinitionEditor: {
		title: 'Browsers definition',
		columns: ['Label', 'Keyword'],
		template: ['label', 'keyword']
	},
	ConnectionsParameterEditor: {
		title: 'Connections parameter',
		columns: ['Context number', 'Parameter value'],
		template: ['0', 'parameter value']
	},
	DomainsListingEditor: {
		title: 'Domains listing',
		columns: ['Domain', 'Black listed'],
		template: ['regular-expression', 'false'],
		choices: { 1: ['false', 'true'] }
	},
	EnvParametersEditor: {
		title: 'Environment parameters',
		columns: ['Variable', 'Value'],
		template: ['variable', 'value']
	},
	FieldListEditor: {
		title: 'Field list',
		columns: ['Line', 'Column', 'Text', 'Field name'],
		template: ['-1', '-1', 'text', 'field_name']
	},
	HttpHeaderForwardEditor: {
		title: 'HTTP headers forwarded',
		columns: ['Header name', 'Forward policy'],
		template: ['header-name', 'Replace'],
		choices: { 1: ['Merge', 'Ignore', 'Replace'] }
	},
	HttpParametersEditor: {
		title: 'HTTP parameters',
		columns: ['Variable', 'Value'],
		template: ['variable', 'value']
	},
	JsonIndexEditor: { title: 'JSON index', columns: ['Field'], template: ['key.subkey'] },
	KeywordEditor: {
		title: 'Keywords',
		columns: ['Keyword', 'Sent data', 'Replace text', 'Action'],
		template: ['keyword', 'sent_data', 'replace_text', 'action']
	},
	MobileBuildAssetsEditor: {
		title: 'Assets imports',
		columns: ['Asset'],
		template: ['{ "glob": "**/*", "input": "node_modules/package", "output": "/package/" }']
	},
	MobileBuildScriptsEditor: {
		title: 'Scripts imports',
		columns: ['Script'],
		template: ['node_modules/package/script.min.js']
	},
	MobileBuildStylesEditor: {
		title: 'Styles imports',
		columns: ['Style'],
		template: ['src/theme/variables.scss']
	},
	MobileConfigNgImportsEditor: {
		title: 'NgModule imports',
		columns: ['Import'],
		template: ['BrowserModule']
	},
	MobileConfigNgProvidersEditor: {
		title: 'NgModule providers',
		columns: ['Provider'],
		template: ['StatusBar']
	},
	MobileConfigPackagesEditor: {
		title: 'Packages dependencies',
		columns: ['Package', 'Version'],
		template: ['package', '1.0.0']
	},
	MobileConfigPluginsEditor: {
		title: 'Cordova plugins',
		columns: ['Plugin', 'Version', 'Variables'],
		template: ['cordova-plugin-statusbar', '~2.1.2', '{}']
	},
	MobileConfigTsImportsEditor: {
		title: 'Needed import declarations',
		columns: ['Import', 'Package'],
		template: ['{ Component }', '@angular/core']
	},
	NgxThrottleEventsEditor: {
		title: 'Throttled events declaration',
		columns: ['Control event', 'Throttle time (ms)'],
		template: ['(click)', '400']
	},
	RemovableHeadersEditor: {
		title: 'Removable headers',
		columns: ['Header to remove', 'Comment'],
		template: ['header name', 'comment']
	},
	ReplacementsEditor: {
		title: 'String replacements',
		columns: ['Mime type', 'Find', 'Replaced by', 'Comment'],
		template: ['mime/type', 'regex', 'replaced by', 'comment']
	},
	SmtpAttachmentEditor: {
		title: 'SMTP attachments',
		columns: ['File path', 'File name'],
		template: ['""', '']
	},
	SplitFieldsEditor: { title: 'Columns names', columns: ['Label'], template: ['label'] },
	StartTransactionVariablesEditor: {
		title: 'Starting transaction variables',
		columns: ['Context number', 'Parameter name', 'Parameter value'],
		template: ['*', 'name', 'value']
	},
	WebClipperAttributesEditor: {
		title: 'WebClipper attributes',
		columns: ['Attribute'],
		template: ['attribute']
	},
	XMLRecordEditor: {
		title: 'XMLRecord description',
		columns: ['Name', 'XPath', 'Children'],
		template: ['data', '//TR', 'false'],
		choices: { 2: ['true', 'false'] }
	},
	XMLTableColumnEditor: {
		title: 'XMLTableColumn description',
		columns: ['Column tag name', 'XPath', 'Children'],
		template: ['column', './TD', 'true'],
		choices: { 2: ['true', 'false'] }
	}
};

/** The editors of the table properties, by the name of the property, as the bean infos declare them. */
export const PROPERTY_TABLE_EDITOR_NAMES = /** @type {Record<string, string>} */ ({
	actions: 'ActionsEditor',
	app_ts_imports: 'MobileConfigTsImportsEditor',
	attachments: 'SmtpAttachmentEditor',
	browserDefinitions: 'BrowserDefinitionEditor',
	build_assets: 'MobileBuildAssetsEditor',
	build_scripts: 'MobileBuildScriptsEditor',
	build_styles: 'MobileBuildStylesEditor',
	columns: 'SplitFieldsEditor',
	connectionsParameter: 'ConnectionsParameterEditor',
	cordova_plugins: 'MobileConfigPluginsEditor',
	domainsListing: 'DomainsListingEditor',
	envParameters: 'EnvParametersEditor',
	fields: 'JsonIndexEditor',
	httpHeaderForward: 'HttpHeaderForwardEditor',
	httpParameters: 'HttpParametersEditor',
	keywordTable: 'KeywordEditor',
	local_module_ng_imports: 'MobileConfigNgImportsEditor',
	local_module_ng_providers: 'MobileConfigNgProvidersEditor',
	local_module_ts_imports: 'MobileConfigTsImportsEditor',
	module_ng_imports: 'MobileConfigNgImportsEditor',
	module_ng_providers: 'MobileConfigNgProvidersEditor',
	module_ts_imports: 'MobileConfigTsImportsEditor',
	options: 'ActionsEditor',
	package_dependencies: 'MobileConfigPackagesEditor',
	page_ts_imports: 'MobileConfigTsImportsEditor',
	removableHeaders: 'RemovableHeadersEditor',
	replacements: 'ReplacementsEditor',
	sourcesDefinition: 'ActionStepSourcesEditor',
	startTransactionVariables: 'StartTransactionVariablesEditor',
	throttleEvents: 'NgxThrottleEventsEditor'
});

/**
 * @param {string | undefined} editorClass
 * @param {any[][]} rows
 * @param {string} [property] the name of the property, for an engine that gives no editor class
 * @returns {PropertyTableEditor} the editor of a table property, with numbered columns when unknown
 */
export function propertyTableEditor(editorClass, rows, property = '') {
	const name =
		String(editorClass ?? '')
			.split('.')
			.pop() || PROPERTY_TABLE_EDITOR_NAMES[property];
	const known = name ? PROPERTY_TABLE_EDITORS[name] : undefined;
	if (known) {
		return known;
	}
	const width = Math.max(1, ...rows.map((row) => row.length));
	return {
		title: 'Table',
		columns: Array.from({ length: width }, (_, index) => `Column ${index + 1}`),
		template: Array.from({ length: width }, () => '')
	};
}
