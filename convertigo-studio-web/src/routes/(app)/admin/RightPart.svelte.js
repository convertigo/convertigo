/** @type {import("svelte").Snippet | undefined} */
let snippet = $state();

export default {
	get snippet() {
		return snippet;
	},
	set snippet(value) {
		snippet = value;
	}
};
