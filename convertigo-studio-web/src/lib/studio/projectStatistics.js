/**
 * @typedef {{ label: string, value: string }} StatisticRow
 * @typedef {{ title: string, rows: StatisticRow[] }} StatisticGroup
 */

/**
 * Makes a label of a counter of the engine, as "JSONTransactionCount" to "JSON transaction count".
 * @param {string} name
 * @returns {string}
 */
function label(name) {
	const words = name
		.replace(/([a-z0-9])([A-Z])/g, '$1 $2')
		.replace(/([A-Z]+)([A-Z][a-z])/g, '$1 $2')
		.split(/\s+/)
		.filter(Boolean)
		.map((word) => (/^[A-Z0-9]{2,}$/.test(word) ? word : word.toLowerCase()));
	const text = words.join(' ');
	return text.charAt(0).toUpperCase() + text.slice(1);
}

/**
 * @param {any} text
 * @returns {string[]}
 */
function lines(text) {
	return String(text ?? '')
		.replace(/&nbsp;| /g, ' ')
		.split(/<br\s*\/?>/i)
		.map((line) => line.trim())
		.filter(Boolean);
}

/**
 * Reads the statistics of a project as the projects.GetStatistic service gives them: for each kind of
 * objects, lines separated by <br/>, the counters written "name = value"; the entry named after the
 * project sums them up.
 * @param {Record<string, any> | null | undefined} statistics the statistics element of the answer
 * @returns {{ project: string, summary: string[], groups: StatisticGroup[] }}
 */
export function readProjectStatistics(statistics) {
	const project = String(statistics?.project ?? '');
	/** @type {string[]} */
	let summary = [];
	/** @type {StatisticGroup[]} */
	const groups = [];
	for (const [key, value] of Object.entries(statistics ?? {})) {
		if (key === 'project') {
			continue;
		}
		if (key === project) {
			summary = lines(value);
			continue;
		}
		const rows = lines(value).map((line) => {
			const match = line.match(/^([^=]+?)\s*=\s*(.*)$/);
			return match ? { label: label(match[1]), value: match[2] } : { label: line, value: '' };
		});
		if (rows.length) {
			groups.push({ title: key.replace(/_/g, ' '), rows });
		}
	}
	groups.sort((a, b) => a.title.localeCompare(b.title));
	return { project, summary, groups };
}
