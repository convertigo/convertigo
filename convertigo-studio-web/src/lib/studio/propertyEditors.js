const CODE_HINT_RE =
	/(javascript|sequencejs|script|expression|xpath|sql|query|code|source|template|payload|body|json|xml|yaml|html|css|formula|condition)/i;
const JAVASCRIPT_HINT_RE = /(javascript|sequencejs|typescript|\.js\b|\bjs\b)/i;
const EXPRESSION_HINT_RE = /\b(expression|condition|script|sequencejs)\b/i;

export const SMART_TYPE_MODES = [
	{ value: 'plain', text: 'TX' },
	{ value: 'script', text: 'JS' },
	{ value: 'source', text: 'SC' }
];

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
	if (!row || row.category === 'Information' || !isTextProperty(row)) {
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
	if (isSmartTypeProperty(row) && row.mode === 'script') {
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
	if (isSmartTypeProperty(row) && row?.mode === 'script') {
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
