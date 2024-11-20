let inc = 0;
let name = $state('bob');

$effect.root(() => {
	$effect(() => {
		console.log('name ' + name);
	});
});

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
