import Bezels from '$lib/dashboard/Bezels';

let model = $state(Object.keys(Bezels)[0]);
let orientation = $state('v');

export default {
	get model() {
		return model;
	},
	set model(value) {
		model = value;
	},
	get orientation() {
		return orientation;
	},
	set orientation(value) {
		orientation = value;
	}
};
