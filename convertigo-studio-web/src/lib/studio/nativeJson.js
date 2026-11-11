/**
 * The JSON of the native steps, as the JSON picker and the JSON trees of the Eclipse Studio name it: the
 * JavaScript expression and the JSON path of a value, the labels of a JSON tree, the summaries of the samples.
 */

/** a segment of a path in a JSON value: a key, an index, or null for the wildcard of a JSON path */
/** @typedef {string | number | null} JsonSegment */

const IDENTIFIER = /^[A-Za-z_$][A-Za-z0-9_$]*$/;
const XPATH_NAME = /^[A-Za-z_][A-Za-z0-9_-]*$/;

/** items shown under an array or an object before a "more" item */
export const MAX_CHILDREN = 500;
/** characters of a text shown in a label */
export const MAX_TEXT = 200;

/**
 * The JavaScript expression of a path under a variable: rows[0].name, rows[0]["a b"].
 * @param {string} variable
 * @param {JsonSegment[]} path
 */
export function toJavaScript(variable, path) {
	let expression = variable ?? '';
	for (const segment of path) {
		if (typeof segment === 'number') {
			expression += `[${segment}]`;
		} else if (IDENTIFIER.test(String(segment))) {
			expression += `.${segment}`;
		} else {
			expression += `["${String(segment).replaceAll('\\', '\\\\').replaceAll('"', '\\"')}"]`;
		}
	}
	return expression;
}

/**
 * The JSON path of a path, under a variable or not: $.rows[0].name, $['a b'], $.
 * @param {string} variable
 * @param {JsonSegment[]} path
 */
export function toJsonPath(variable, path) {
	let jsonPath = '$';
	if (variable) {
		jsonPath += `.${variable}`;
	}
	for (const segment of path) {
		if (segment === null) {
			jsonPath += '[*]';
		} else if (typeof segment === 'number') {
			jsonPath += `[${segment}]`;
		} else if (IDENTIFIER.test(segment)) {
			jsonPath += `.${segment}`;
		} else {
			jsonPath += `['${segment.replaceAll("'", "\\'")}']`;
		}
	}
	return jsonPath;
}

/**
 * The path of a simple JavaScript expression under a variable (variable.a[0]["b"]), or null for another
 * expression.
 * @param {string} variable
 * @param {string} expression
 * @returns {JsonSegment[] | null}
 */
export function parseJavaScript(variable, expression) {
	if (!variable || typeof expression !== 'string') {
		return null;
	}
	let rest = expression.trim();
	if (!rest.startsWith(variable)) {
		return null;
	}
	rest = rest.slice(variable.length);
	/** @type {JsonSegment[]} */
	const path = [];
	const pattern = /\.([A-Za-z_$][A-Za-z0-9_$]*)|\[(\d+)\]|\["((?:[^"\\]|\\.)*)"\]/y;
	let end = 0;
	let match;
	while ((match = pattern.exec(rest))) {
		if (match[1] !== undefined) {
			path.push(match[1]);
		} else if (match[2] !== undefined) {
			path.push(Number(match[2]));
		} else {
			path.push(match[3].replaceAll('\\"', '"').replaceAll('\\\\', '\\'));
		}
		end = pattern.lastIndex;
	}
	return end === rest.length ? path : null;
}

/**
 * The segments of a JSON path of the engine ($, .key, ['key'], [index], .*, [*]), or null when it is not one.
 * @param {string} jsonPath
 * @returns {JsonSegment[] | null}
 */
export function parseJsonPath(jsonPath) {
	if (typeof jsonPath !== 'string') {
		return null;
	}
	const text = jsonPath.trim();
	if (!text.startsWith('$')) {
		return null;
	}
	const rest = text.slice(1);
	/** @type {JsonSegment[]} */
	const path = [];
	const pattern =
		/\.([^.[\]'*\s]+)|\.\*|\[(\d+)\]|\[\*\]|\['((?:[^'\\]|\\.)*)'\]|\["((?:[^"\\]|\\.)*)"\]/y;
	let end = 0;
	let match;
	while ((match = pattern.exec(rest))) {
		if (match[1] !== undefined) {
			path.push(match[1]);
		} else if (match[2] !== undefined) {
			path.push(Number(match[2]));
		} else if (match[3] !== undefined) {
			path.push(match[3].replaceAll("\\'", "'").replaceAll('\\\\', '\\'));
		} else if (match[4] !== undefined) {
			path.push(match[4].replaceAll('\\"', '"').replaceAll('\\\\', '\\'));
		} else {
			path.push(null);
		}
		end = pattern.lastIndex;
	}
	return end === rest.length ? path : null;
}

/**
 * Whether a source definition reads a JSON path ($...) instead of an XPath.
 * @param {string} xpath
 */
export function isJsonPath(xpath) {
	return typeof xpath === 'string' && xpath.trim().startsWith('$');
}

/**
 * The JSON path that reads, on the value of a native step, what a simple XPath reads on its XML view:
 * ".", "./key", "./item[n]", a last "text()"; null for another XPath.
 * @param {string} xpath
 */
export function xpathToJsonPath(xpath) {
	if (typeof xpath !== 'string') {
		return null;
	}
	const x = xpath.trim();
	if (x === '.') {
		return '$';
	}
	if (!x.startsWith('./')) {
		return null;
	}
	const segments = x.slice(2).split('/');
	let path = '$';
	for (let i = 0; i < segments.length; i++) {
		const segment = segments[i];
		const item = /^item\[(\d+)\]$/.exec(segment);
		if (segment === 'text()' && i === segments.length - 1 && i > 0) {
			break;
		} else if (item && Number(item[1]) > 0) {
			path += `[${Number(item[1]) - 1}]`;
		} else if (XPATH_NAME.test(segment) && segment !== 'item') {
			path += `.${segment}`;
		} else {
			return null;
		}
	}
	return path;
}

/**
 * The value at a path (no wildcard), or undefined.
 * @param {any} value
 * @param {JsonSegment[]} path
 */
export function valueAt(value, path) {
	let current = value;
	for (const segment of path) {
		if (current === null || typeof current !== 'object' || segment === null) {
			return undefined;
		}
		current = current[segment];
	}
	return current;
}

/**
 * The label of a node of a JSON tree: key: {n}, [i]: [n], key: "text".
 * @param {string} name
 * @param {any} value
 */
export function nodeLabel(name, value) {
	const prefix = name ? `${name}: ` : '';
	if (Array.isArray(value)) {
		return `${prefix}[${value.length}]`;
	}
	if (value !== null && typeof value === 'object') {
		return `${prefix}{${Object.keys(value).length}}`;
	}
	const text = typeof value === 'string' ? `"${value}"` : (JSON.stringify(value) ?? 'undefined');
	return prefix + (text.length > MAX_TEXT ? `${text.slice(0, MAX_TEXT)}…` : text);
}

/**
 * The children of an array or an object, the first MAX_CHILDREN ones.
 * @param {any} value
 * @returns {{ entries: { segment: string | number, value: any }[], more: number }}
 */
export function childrenOf(value) {
	if (Array.isArray(value)) {
		return {
			entries: value
				.slice(0, MAX_CHILDREN)
				.map((child, index) => ({ segment: index, value: child })),
			more: Math.max(0, value.length - MAX_CHILDREN)
		};
	}
	if (value !== null && typeof value === 'object') {
		const keys = Object.keys(value);
		return {
			entries: keys.slice(0, MAX_CHILDREN).map((key) => ({ segment: key, value: value[key] })),
			more: Math.max(0, keys.length - MAX_CHILDREN)
		};
	}
	return { entries: [], more: 0 };
}

/**
 * What the last sample of a step holds: none, its note, N items, N keys or value.
 * @param {{ found?: boolean, count?: number, note?: string, json?: string }} sample
 */
export function sampleSummary(sample) {
	if (!sample?.found) {
		return 'none';
	}
	if (sample.note && !sample.json) {
		return sample.note;
	}
	const count = Number(sample.count ?? -1);
	if (count >= 0) {
		return `${count} ${count === 1 ? 'item' : 'items'}`;
	}
	return 'value';
}

/**
 * The time of a native step execution: x µs below 10 ms, else x ms.
 * @param {number} micros
 */
export function formatDuration(micros) {
	const value = Number(micros ?? 0);
	if (value < 10000) {
		return `${value} µs`;
	}
	return `${Math.round(value / 1000)} ms`;
}

/**
 * The items of a row of the native steps: its count, or the note of a value written in the response.
 * @param {{ count?: number, note?: string, size?: number }} row
 */
export function rowItems(row) {
	if (row?.note && !row.size) {
		return row.note;
	}
	const count = Number(row?.count ?? -1);
	return count >= 0 ? String(count) : (row?.note ?? '');
}

/**
 * Parses the JSON of a sample, or gives the text as is when it is not JSON.
 * @param {string} json
 * @returns {{ ok: true, value: any } | { ok: false, text: string }}
 */
export function parseSample(json) {
	try {
		return { ok: true, value: JSON.parse(json) };
	} catch {
		return { ok: false, text: String(json ?? '') };
	}
}

/** the functions of an aggregation of Group and aggregate */
export const AGGREGATION_TYPES = ['count', 'sum', 'avg', 'min', 'max'];

/**
 * The fields of a row of a JSON list editor: a column name, or the type, column, key and label of an
 * aggregation.
 * @param {'columns' | 'aggregations'} kind
 */
export function jsonListFields(kind) {
	return kind === 'aggregations'
		? [
				{ key: 'type', label: 'Type', choices: AGGREGATION_TYPES },
				{ key: 'column', label: 'Column' },
				{ key: 'key', label: 'Key' },
				{ key: 'label', label: 'Label' }
			]
		: [{ key: 'column', label: 'Column' }];
}

/**
 * Reads a value of a JSON list property: a JSON array of names (or of flat objects of known keys), null, or
 * any other JavaScript expression.
 * @param {string} value
 * @param {'columns' | 'aggregations'} kind
 * @returns {{ mode: 'list' | 'null' | 'expression', rows: Record<string, string>[] }}
 */
export function parseJsonList(value, kind) {
	const fields = jsonListFields(kind);
	const text = String(value ?? '').trim();
	if (text === 'null') {
		return { mode: 'null', rows: [] };
	}
	let parsed;
	try {
		parsed = text ? JSON.parse(text) : [];
	} catch {
		return { mode: 'expression', rows: [] };
	}
	if (!Array.isArray(parsed)) {
		return { mode: 'expression', rows: [] };
	}
	/** @type {Record<string, string>[]} */
	const rows = [];
	for (const item of parsed) {
		if (fields.length === 1 && typeof item === 'string') {
			rows.push({ [fields[0].key]: item });
		} else if (fields.length > 1 && item && typeof item === 'object' && !Array.isArray(item)) {
			/** @type {Record<string, string>} */
			const row = {};
			for (const [key, cell] of Object.entries(item)) {
				if (
					(cell !== null && typeof cell === 'object') ||
					!fields.some((field) => field.key === key)
				) {
					return { mode: 'expression', rows: [] };
				}
				row[key] = String(cell);
			}
			rows.push(row);
		} else {
			return { mode: 'expression', rows: [] };
		}
	}
	return { mode: 'list', rows };
}

/**
 * Writes the rows of a JSON list editor: ["a", "b"], [{"type": "sum", "column": "x"}], the empty rows and
 * cells left out.
 * @param {Record<string, string>[]} rows
 * @param {'columns' | 'aggregations'} kind
 */
export function jsonListValue(rows, kind) {
	const fields = jsonListFields(kind);
	const items = [];
	for (const row of rows) {
		if (fields.every((field) => !row[field.key])) {
			continue;
		}
		if (fields.length === 1) {
			items.push(JSON.stringify(row[fields[0].key] ?? ''));
		} else {
			const cells = fields
				.filter((field) => row[field.key])
				.map((field) => `${JSON.stringify(field.key)}: ${JSON.stringify(row[field.key])}`);
			items.push(`{${cells.join(', ')}}`);
		}
	}
	return `[${items.join(', ')}]`;
}

/**
 * The values a JSON path selects in a JSON value: one for a path without wildcard, all the matches else.
 * @param {any} value
 * @param {JsonSegment[]} path
 * @returns {{ wildcard: boolean, matches: any[] }}
 */
export function selectJsonPath(value, path) {
	let current = [value];
	let wildcard = false;
	for (const segment of path) {
		/** @type {any[]} */
		const next = [];
		for (const node of current) {
			if (node === null || typeof node !== 'object') {
				continue;
			}
			if (segment === null) {
				wildcard = true;
				next.push(...(Array.isArray(node) ? node : Object.values(node)));
			} else if (Array.isArray(node) ? typeof segment === 'number' : typeof segment === 'string') {
				if (segment in node) {
					next.push(node[segment]);
				}
			}
		}
		current = next;
	}
	return { wildcard, matches: current };
}

/**
 * What a JSON path reads in a JSON value, as the JSON path editor of the Eclipse Studio says it.
 * @param {any} value
 * @param {string} jsonPath
 */
export function jsonPathPreview(value, jsonPath) {
	const path = parseJsonPath(jsonPath);
	if (!path) {
		return `Invalid JSON path: ${jsonPath}`;
	}
	const { wildcard, matches } = selectJsonPath(value, path);
	const selected = wildcard ? matches : matches[0];
	if (!wildcard && matches.length === 0) {
		return 'Value: nothing (the default value)';
	}
	if (Array.isArray(selected)) {
		return `Value: array of ${selected.length}`;
	}
	if (selected !== null && typeof selected === 'object') {
		return `Value: object of ${Object.keys(selected).length}`;
	}
	const text = JSON.stringify(selected);
	return `Value: ${text.length > MAX_TEXT ? `${text.slice(0, MAX_TEXT)}…` : text}`;
}
