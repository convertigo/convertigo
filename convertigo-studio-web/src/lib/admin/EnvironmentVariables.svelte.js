import ServiceHelper from '$lib/common/ServiceHelper.svelte';

const defValues = {
	variables: Array(10).fill({ name: null, value: null })
};

export default ServiceHelper({
	defValues,
	service: 'engine.GetEnvironmentVariablesJson',
	beforeUpdate: (res) => {
		res.variables.sort((a, b) => a.name.localeCompare(b.name));
	}
});
