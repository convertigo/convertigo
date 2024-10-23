let browser = $state(new Date());
let serverDiff = $state(0);
let server = $derived(new Date(browser.getTime() - serverDiff));

setInterval(() => {
	browser = new Date();
}, 1000);

export default {
	get browserTime() {
		return browser.toTimeString().split(' ')[0];
	},
	get serverTime() {
		return server.toTimeString().split(' ')[0];
	},
	set serverTimestamp(timestamp) {
		serverDiff = new Date().getTime() - timestamp;
	}
};
