/**
 * The response lifetime of a requestable, as the cache editor of the Eclipse Studio generates it: a
 * number of seconds, or a time of each day, of a day of each week or of a day of each month.
 */

export const LIFETIME_MODES = /** @type {const} */ (['absolute', 'daily', 'weekly', 'monthly']);

/** the days of the week as the engine numbers them, from Sunday */
export const WEEK_DAYS = [
	'Sunday',
	'Monday',
	'Tuesday',
	'Wednesday',
	'Thursday',
	'Friday',
	'Saturday'
];

const TIME = '([01][0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])';
const PATTERNS = {
	absolute: /^absolute,([0-9]+)$/,
	daily: new RegExp(`^daily,${TIME}$`),
	weekly: new RegExp(`^weekly,${TIME},([1-7])$`),
	monthly: new RegExp(`^monthly,${TIME},(0[1-9]|[12][0-9]|3[01])$`)
};

/**
 * @typedef {{ mode: typeof LIFETIME_MODES[number], seconds: number, time: string, day: number }} Lifetime
 */

/**
 * @param {string} value
 * @returns {Lifetime | null} the lifetime the value describes, or null when it is none of them
 */
export function parseLifetime(value) {
	for (const mode of LIFETIME_MODES) {
		const match = PATTERNS[mode].exec(String(value ?? '').trim());
		if (!match) {
			continue;
		}
		if (mode === 'absolute') {
			return { mode, seconds: Number(match[1]), time: '00:00:00', day: 1 };
		}
		return {
			mode,
			seconds: 0,
			time: `${match[1]}:${match[2]}:${match[3]}`,
			day: match[4] ? Number(match[4]) : 1
		};
	}
	return null;
}

/**
 * @param {Lifetime} lifetime
 * @returns {string} the value of the response lifetime property
 */
export function formatLifetime({ mode, seconds, time, day }) {
	if (mode === 'absolute') {
		return `absolute,${Math.max(0, Math.floor(Number(seconds) || 0))}`;
	}
	const [h = 0, m = 0, s = 0] = String(time ?? '')
		.split(':')
		.map((part) => Number(part) || 0);
	const pad = (/** @type {number} */ n) => String(n).padStart(2, '0');
	const clock = `${pad(Math.min(h, 23))}:${pad(Math.min(m, 59))}:${pad(Math.min(s, 59))}`;
	if (mode === 'weekly') {
		return `weekly,${clock},${Math.min(Math.max(day, 1), 7)}`;
	}
	if (mode === 'monthly') {
		return `monthly,${clock},${pad(Math.min(Math.max(day, 1), 31))}`;
	}
	return `daily,${clock}`;
}
