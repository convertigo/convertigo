// Plain $state and getters, without SvelteDate nor module-level $derived: those create derived
// values owned by the first component that reads them, and they freeze ("derived_inert") once
// that component is destroyed, as the login page is after signing in.
let now = $state(Date.now());
let serverDiff = $state(0);
let serverTimezone = $state('');

setInterval(() => {
	now = Date.now();
}, 1000);

const serverDate = () => new Date(now - serverDiff);
// same output as Date.toLocaleTimeString(undefined, options), from a timestamp
const formatTime = (timestamp, options = {}) =>
	new Intl.DateTimeFormat(undefined, {
		hour: 'numeric',
		minute: 'numeric',
		second: 'numeric',
		...options
	}).format(timestamp);

export default {
	get isSameTime() {
		return (
			!serverTimezone ||
			formatTime(now) == formatTime(now - serverDiff, { timeZone: serverTimezone })
		);
	},
	get browserTime() {
		return formatTime(now);
	},
	get server() {
		return serverDate();
	},
	get serverTime() {
		return formatTime(now - serverDiff, { timeZone: serverTimezone || undefined });
	},
	set serverTimestamp(timestamp) {
		const diff = Date.now() - timestamp;
		serverDiff = diff > 2000 ? diff : 0;
	},
	get serverTimezone() {
		return serverTimezone;
	},
	set serverTimezone(tz) {
		serverTimezone = tz;
	}
};
