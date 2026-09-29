/**
 * The zones of a generated NGX class, or of the function of a custom action, where the code of the
 * component is written: between its Begin_c8o and End_c8o comments. The code outside them is
 * generated again by the builder, which keeps only the code of the zones.
 */

const BEGIN = /\/\*Begin_c8o_(.+?)\*\//g;

/** what the zones of the templates of the builder are for, by the end of their names */
const PURPOSES = [
	['Import', 'Imports', 'the imports of the class'],
	['Declarations', 'Declarations', 'the declarations of the module'],
	['Declaration', 'Declarations', 'the fields and properties of the class'],
	['Constructor', 'Constructor', 'runs when the class is created'],
	['Initialization', 'On init', 'runs in ngOnInit'],
	['AfterViewInit', 'After view init', 'runs in ngAfterViewInit'],
	['AfterViewChecked', 'After view checked', 'runs in ngAfterViewChecked'],
	['AfterContentInit', 'After content init', 'runs in ngAfterContentInit'],
	['AfterContentChecked', 'After content checked', 'runs in ngAfterContentChecked'],
	['DoCheck', 'Do check', 'runs in ngDoCheck'],
	['Changes', 'On changes', 'runs in ngOnChanges'],
	['Finalization', 'On destroy', 'runs in ngOnDestroy'],
	['Function', 'Functions', 'the methods of the class'],
	['Settings', 'Settings', 'the settings of the application'],
	['Modules', 'Modules', 'the Angular modules imported'],
	['Providers', 'Providers', 'the Angular providers'],
	['Components', 'Components', 'the Angular components']
];

/**
 * @typedef {Object} EditableZone
 * @property {string} name the name of its markers, as PageConstructor
 * @property {string} label
 * @property {string} hint what the zone is for
 * @property {number} beginOffset where its Begin comment starts
 * @property {number} contentStart where its code starts, after its Begin comment
 * @property {number} contentEnd where its code ends, before its End comment
 * @property {number} endOffset after its End comment
 * @property {number} beginLine the line of its Begin comment, from 1
 * @property {number} endLine the line of its End comment
 * @property {boolean} empty whether it holds no code
 */

/**
 * @param {string} name the name of the markers of a zone
 * @returns {{ label: string, hint: string }}
 */
export function zonePurpose(name) {
	const action = /^function:(.+)$/.exec(name);
	if (action) {
		return { label: action[1], hint: 'the code of the action' };
	}
	const purpose = PURPOSES.find(([suffix]) => name.endsWith(suffix));
	if (purpose) {
		return { label: purpose[1], hint: purpose[2] };
	}
	return { label: name.replace(/([a-z])([A-Z])/g, '$1 $2'), hint: '' };
}

/**
 * @param {string} text
 * @returns {EditableZone[]} the zones of the text, in their order, those whose End comment is missing left
 */
export function findZones(text) {
	/** @type {EditableZone[]} */
	const zones = [];
	const lineStarts = [0];
	for (let index = text.indexOf('\n'); index >= 0; index = text.indexOf('\n', index + 1)) {
		lineStarts.push(index + 1);
	}
	/** @param {number} offset */
	const lineOf = (offset) => {
		let low = 0;
		let high = lineStarts.length - 1;
		while (low < high) {
			const middle = (low + high + 1) >> 1;
			if (lineStarts[middle] <= offset) {
				low = middle;
			} else {
				high = middle - 1;
			}
		}
		return low + 1;
	};
	let from = 0;
	BEGIN.lastIndex = 0;
	for (let match = BEGIN.exec(text); match; match = BEGIN.exec(text)) {
		if (match.index < from) {
			continue;
		}
		const name = match[1];
		const contentStart = match.index + match[0].length;
		const endMarker = `/*End_c8o_${name}*/`;
		const contentEnd = text.indexOf(endMarker, contentStart);
		if (contentEnd < 0) {
			continue;
		}
		const endOffset = contentEnd + endMarker.length;
		zones.push({
			name,
			...zonePurpose(name),
			beginOffset: match.index,
			contentStart,
			contentEnd,
			endOffset,
			beginLine: lineOf(match.index),
			endLine: lineOf(contentEnd),
			empty: !text.slice(contentStart, contentEnd).trim()
		});
		from = endOffset;
		BEGIN.lastIndex = endOffset;
	}
	return zones;
}

/**
 * @typedef {Object} GeneratedRun
 * @property {string} key what the run is between, stable while the code of the zones changes
 * @property {number} startLine
 * @property {number} endLine
 * @property {string} summary its first line of code that is not only punctuation
 */

/**
 * @param {EditableZone[]} zones
 * @param {string[]} lines the lines of the text
 * @param {number} [minLines] the fewest lines a run has to be folded
 * @returns {GeneratedRun[]} the runs of whole lines of generated code, between the lines of the zones
 */
export function generatedRuns(zones, lines, minLines = 3) {
	/** @type {GeneratedRun[]} */
	const runs = [];
	let startLine = 1;
	let previous = 'start';
	for (const zone of [...zones, null]) {
		const endLine = zone ? zone.beginLine - 1 : lines.length;
		if (endLine - startLine + 1 >= minLines) {
			// the first line that tells something, rather than the braces that close the code before
			const run = lines.slice(startLine - 1, endLine);
			const first = run.find((line) => /\w/.test(line)) ?? run.find((line) => line.trim()) ?? '';
			runs.push({
				key: `${previous}>${zone?.name ?? 'end'}`,
				startLine,
				endLine,
				summary: first.trim()
			});
		}
		if (zone) {
			startLine = zone.endLine + 1;
			previous = zone.name;
		}
	}
	return runs;
}

/**
 * @param {EditableZone[]} zones
 * @param {number} start
 * @param {number} end
 * @returns {EditableZone | undefined} the zone whose code holds the offsets
 */
export function zoneAt(zones, start, end = start) {
	return zones.find((zone) => zone.contentStart <= start && end <= zone.contentEnd);
}

/**
 * @param {EditableZone[]} zones the zones before the change
 * @param {{ rangeOffset: number, rangeLength: number }[]} changes
 * @returns {boolean} whether the changes only change the code of zones
 */
export function changesInZones(zones, changes) {
	return changes.every((change) =>
		zoneAt(zones, change.rangeOffset, change.rangeOffset + change.rangeLength)
	);
}
