/** @type {import("svelte").Snippet | undefined} */
let snippet = $state();
let owner = $state(undefined);

function claim(token, value) {
	owner = token;
	snippet = value;
}

function release(token) {
	if (owner === token) {
		owner = undefined;
		snippet = undefined;
	}
}

export default {
	get snippet() {
		return snippet;
	},
	set snippet(value) {
		owner = undefined;
		snippet = value;
	},
	claim,
	release
};
