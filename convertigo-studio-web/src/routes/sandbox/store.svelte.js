let inc = 0;
let name = $state('bob');

export const store = {
	get name() {
		return name;
	},
	inc: () => {
		inc += 1;
		console.log('inc ' + inc);
		name = `bob ${inc}`;
	}
};
