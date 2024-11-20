let category = $state('Main');
let advanced = $state([]);

export default {
	get category() {
		return category;
	},
	set category(value) {
		category = value;
	},
	get advanced() {
		return advanced;
	},
	set advanced(value) {
		advanced = value;
	}
};
