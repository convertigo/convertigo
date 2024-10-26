let browser = $state(new Date());
let serverDiff = $state(0);
let serverTimezone = $state('');
let server = $derived(new Date(browser.getTime() - serverDiff));
let isSameTime = $derived(
	!serverTimezone ||
		browser.toLocaleTimeString() ==
			server.toLocaleTimeString(undefined, { timeZone: serverTimezone })
);

setInterval(() => {
	browser = new Date();
}, 1000);

export default {
	get isSameTime() {
		return isSameTime;
	},
	get browserTime() {
		return browser.toLocaleTimeString();
	},
	get serverTime() {
		return server.toLocaleTimeString(undefined, { timeZone: serverTimezone });
	},
	set serverTimestamp(timestamp) {
		serverDiff = new Date().getTime() - timestamp;
	},
	get serverTimezone() {
		return serverTimezone;
	},
	set serverTimezone(tz) {
		serverTimezone = tz;
	}
};
