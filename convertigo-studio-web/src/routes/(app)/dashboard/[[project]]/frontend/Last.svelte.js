let model = $state('iPhone 12 Pro');
let orientation = $state('vertical');

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
