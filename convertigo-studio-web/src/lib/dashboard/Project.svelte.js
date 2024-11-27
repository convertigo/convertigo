import Projects from '$lib/common/Projects.svelte';

/** @type {any} */
let page = $state(null);
let project = $derived(Projects.projects.find(({ name }) => name == page?.params?.project));
let hasRef = $derived(project?.ref?.length > 0);
let hasFrontend = $derived(project?.hasFrontend == 'true');
let hasPlatforms = $derived(project?.hasPlatform == 'true');

export default {
	set page(value) {
		page = value;
	},
	get project() {
		return project;
	},
	get hasRef() {
		return hasRef;
	},
	get hasFrontend() {
		return hasFrontend;
	},
	get hasPlatforms() {
		return hasPlatforms;
	}
};
