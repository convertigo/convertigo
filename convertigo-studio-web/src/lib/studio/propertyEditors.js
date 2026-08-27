const CODE_HINT_RE =
	/(javascript|sequencejs|script|expression|xpath|sql|query|code|source|template|payload|body|json|xml|yaml|html|css|formula|condition)/i;
const JAVASCRIPT_HINT_RE = /(javascript|sequencejs|typescript|\.js\b|\bjs\b)/i;
const EXPRESSION_HINT_RE = /\b(expression|condition|script|sequencejs)\b/i;
const SEMANTIC_COLOR_VALUES = new Set([
	'current',
	'auto',
	'neutral',
	'primary',
	'secondary',
	'tertiary',
	'success',
	'warning',
	'danger',
	'error',
	'muted'
]);

export const SMART_TYPE_MODES = [
	{ value: 'plain', text: 'TX' },
	{ value: 'script', text: 'JS' },
	{ value: 'source', text: 'SC' }
];

/**
 * @param {any} current
 * @param {any} target
 * @returns {boolean}
 */
export function isSamePropertyPickerTarget(current, target) {
	return (
		Boolean(current?.id) &&
		current.id === target?.id &&
		current.propertyName === target?.propertyName
	);
}

/**
 * Keeps the inline picker exclusive: clicking the same property closes it,
 * while clicking another property transfers the picker to that property.
 *
 * @param {any} current
 * @param {any} target
 * @param {number} serial
 * @returns {any | null}
 */
export function togglePropertyPickerTarget(current, target, serial = Date.now()) {
	if (!target?.id) {
		return current ?? null;
	}
	if (isSamePropertyPickerTarget(current, target)) {
		return null;
	}
	return { ...target, serial };
}

/**
 * @param {any} value
 * @returns {string}
 */
export function asEditorValue(value) {
	if (value == null) {
		return '';
	}
	if (Array.isArray(value)) {
		return value.join('\n');
	}
	if (typeof value === 'object') {
		return JSON.stringify(value, null, 2);
	}
	return String(value);
}

/**
 * Renders a Flow binding as the expression an author recognizes, without
 * exposing the persisted JSON contract.
 *
 * @param {any} binding
 * @returns {string}
 */
export function flowBindingPreview(binding) {
	const parsed = parseFlowBinding(binding);
	if (!parsed) {
		return asEditorValue(binding) || 'empty';
	}
	if (parsed.mode === 'literal') {
		return `Literal: ${asEditorValue(parsed.value) || 'empty'}`;
	}
	if (parsed.mode === 'expression' && Array.isArray(parsed.parts)) {
		return expressionPartsPreview(parsed.parts) || 'Expression';
	}
	if (parsed.mode === 'source') {
		return sourceBindingPreview(parsed);
	}
	return asEditorValue(binding) || 'empty';
}

/** @param {any} value */
function parseFlowBinding(value) {
	try {
		const parsed = typeof value === 'string' ? JSON.parse(value) : value;
		return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : null;
	} catch {
		return null;
	}
}

/**
 * @param {any} row
 * @returns {any}
 */
export function propertyChoiceValue(row) {
	if (row?.editorClass !== 'flow-binding-editor') {
		return row?.value;
	}
	const binding = parseFlowBinding(row?.value);
	return binding?.mode === 'literal' ? binding.value : row?.value;
}

/**
 * Keeps legacy or externally authored values editable as text while exposing
 * declared closed vocabularies as choices for their valid current values.
 *
 * @param {any} row
 * @returns {boolean}
 */
export function hasPropertyPossibleValues(row) {
	if (!Array.isArray(row?.values) || row.values.length === 0) {
		return false;
	}
	const current = propertyChoiceValue(row);
	return row.values.some((option) => String(option?.value ?? option) === String(current));
}

/**
 * Detects a closed semantic color vocabulary from its values rather than from
 * a component or property name. This keeps the presentation metadata-driven.
 *
 * @param {any} row
 * @returns {boolean}
 */
export function isSemanticColorProperty(row) {
	if (!hasPropertyPossibleValues(row)) {
		return false;
	}
	const values = row.values.map((option) => String(option?.value ?? option).toLowerCase());
	return (
		values.some((value) => ['primary', 'secondary', 'tertiary'].includes(value)) &&
		values.every((value) => SEMANTIC_COLOR_VALUES.has(value))
	);
}

/** @param {any[]} parts */
function expressionPartsPreview(parts) {
	let preview = '';
	let previousWasValue = false;
	for (const part of parts) {
		if (part?.kind === 'expression') {
			const expression = String(part.expression ?? '').trim();
			if (expression) {
				preview += `${preview ? ' ' : ''}${expression} `;
				previousWasValue = false;
			}
			continue;
		}
		const value =
			part?.kind === 'source'
				? sourceBindingPreview(part)
				: part?.kind === 'literal'
					? JSON.stringify(part.value ?? '')
					: '';
		if (!value) continue;
		if (previousWasValue) preview += ' + ';
		preview += value;
		previousWasValue = true;
	}
	return preview.trim();
}

/** @param {any} binding */
function sourceBindingPreview(binding) {
	const source = binding?.source ?? {};
	const category = String(source.category ?? '').trim();
	const name = String(
		source.name ?? source.value ?? source.actionId ?? source.scopeId ?? source.operation ?? ''
	).trim();
	const origin = source.label ? String(source.label) : [category, name].filter(Boolean).join('.');
	const path = Array.isArray(binding?.path)
		? binding.path
				.map((part) =>
					part?.kind === 'index' || part?.index != null
						? `[${part.index ?? part.value}]`
						: part?.name != null
							? `.${part.name}`
							: ''
				)
				.join('')
		: '';
	return `${origin || 'Source'}${path}`;
}

/**
 * @param {any} row
 * @returns {string}
 */
function rowHints(row) {
	return [row?.name, row?.displayName, row?.class, row?.description, row?.category]
		.filter(Boolean)
		.join(' ');
}

/**
 * @param {any} row
 * @returns {boolean}
 */
function isTextProperty(row) {
	const cls = String(row?.class ?? '');
	if (!cls.startsWith('java.lang.')) {
		return false;
	}
	return !/(Boolean|Integer|Long|Double|Float|Short|Byte)$/.test(cls);
}

/**
 * @param {any} row
 * @returns {boolean}
 */
export function isSmartTypeProperty(row) {
	return row?.smartType === true;
}

/**
 * @param {any} row
 * @returns {boolean}
 */
export function isIonProperty(row) {
	return row?.kind === 'ion';
}

/**
 * @param {any} row
 * @returns {boolean}
 */
export function isNgxSmartSourceProperty(row) {
	return row?.editorClass === 'NgxSmartSourcePropertyDescriptor';
}

/**
 * @param {any} row
 * @returns {boolean}
 */
export function isSmartSourceProperty(row) {
	return (
		isSmartTypeProperty(row) ||
		isNgxSmartSourceProperty(row) ||
		(isIonProperty(row) && ['script', 'source'].includes(row?.mode))
	);
}

/**
 * @param {any} value
 * @returns {boolean}
 */
function isTextEditorValue(value) {
	return (
		value == null ||
		typeof value === 'string' ||
		typeof value === 'number' ||
		typeof value === 'boolean' ||
		Array.isArray(value) ||
		typeof value === 'object'
	);
}

/**
 * @param {string} value
 * @returns {boolean}
 */
function looksLikeJavaScript(value) {
	return /(^|\n)\s*(\/\/|\/\*|include\s*\(|import\s+|export\s+|const\s+|let\s+|var\s+|function\s+|async\s+function|try\s*\{|if\s*\(|for\s*\(|while\s*\(|return\b|console\.|JSON\.|context\.|fsclient\.)/.test(
		value
	);
}

/**
 * @param {any} row
 * @param {string} selectedId
 * @returns {boolean}
 */
export function isMonacoProperty(row, selectedId = '') {
	if (
		!row ||
		row.category === 'Information' ||
		row.editorClass === 'flow-binding-editor' ||
		!isTextProperty(row)
	) {
		return false;
	}
	const value = asEditorValue(row.value);
	const hints = `${selectedId} ${rowHints(row)}`;
	return row.isMultiline || value.includes('\n') || value.length > 140 || CODE_HINT_RE.test(hints);
}

/**
 * @param {any} row
 * @param {string} selectedId
 * @returns {boolean}
 */
export function isCodeEditorProperty(row, selectedId = '') {
	if (!isMonacoProperty(row, selectedId)) {
		return false;
	}
	const hints = `${selectedId} ${rowHints(row)}`;
	return CODE_HINT_RE.test(hints) || getPropertyLanguage(row, selectedId) !== 'text';
}

/**
 * Indicates whether a property can be opened explicitly in the Studio code editor.
 * This is broader than isMonacoProperty because it is only used for user-triggered actions,
 * not for automatic tab selection.
 *
 * @param {any} row
 * @param {string} selectedId
 * @returns {boolean}
 */
export function canOpenCodeProperty(row, selectedId = '') {
	if (!row || row.category === 'Information' || !isTextEditorValue(row.value)) {
		return false;
	}
	// Flow bindings have a structured Literal/Source/Compose editor. Opening
	// their persisted JSON in Monaco exposes an implementation detail and can
	// corrupt an otherwise valid typed binding.
	if (row.editorClass === 'flow-binding-editor') {
		return false;
	}
	if (isSmartSourceProperty(row) && row.mode === 'script') {
		return true;
	}
	if (isMonacoProperty(row, selectedId)) {
		return true;
	}
	const value = asEditorValue(row.value);
	const hints = `${selectedId} ${rowHints(row)}`;
	return value.includes('\n') || value.length > 140 || CODE_HINT_RE.test(hints);
}

/**
 * @param {any[]} properties
 * @param {string} selectedId
 * @returns {any}
 */
export function findPrimaryEditorProperty(properties, selectedId = '') {
	const candidates = (properties ?? []).filter((row) => isMonacoProperty(row, selectedId));
	if (candidates.length === 0) {
		return undefined;
	}
	return candidates
		.map((row) => ({
			row,
			score:
				(CODE_HINT_RE.test(`${selectedId} ${rowHints(row)}`) ? 1000 : 0) +
				(row.isMultiline ? 100 : 0) +
				Math.min(asEditorValue(row.value).length, 500)
		}))
		.sort((a, b) => b.score - a.score)[0].row;
}

/**
 * @param {any} row
 * @param {string} selectedId
 * @returns {string}
 */
export function getPropertyLanguage(row, selectedId = '') {
	if (isSmartSourceProperty(row) && row?.mode === 'script') {
		return 'javascript';
	}
	const hints = `${selectedId} ${rowHints(row)}`.toLowerCase();
	const value = asEditorValue(row?.value).trim();
	const expressionLike = EXPRESSION_HINT_RE.test(hints);
	if (/typescript/.test(hints)) {
		return 'typescript';
	}
	if (JAVASCRIPT_HINT_RE.test(hints)) {
		return 'javascript';
	}
	if (/(xml|xpath|xsl)/.test(hints) || value.startsWith('<')) {
		return 'xml';
	}
	if (/sql|query/.test(hints)) {
		return 'sql';
	}
	if (/json/.test(hints) || value.startsWith('{') || value.startsWith('[')) {
		return 'json';
	}
	if (/ya?ml/.test(hints)) {
		return 'yaml';
	}
	if (/html/.test(hints)) {
		return 'html';
	}
	if (/css/.test(hints)) {
		return 'css';
	}
	if (expressionLike || looksLikeJavaScript(value)) {
		return 'javascript';
	}
	return 'text';
}
