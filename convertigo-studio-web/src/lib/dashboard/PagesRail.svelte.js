let extras = $state([]);
const parts = $derived([
	[
		{
			title: 'Projects',
			icon: 'mdi:folder-outline',
			page: '/(app)/dashboard'
		},
		...extras
	],
	[
		{
			title: 'Admin',
			icon: 'mdi:land-plots',
			page: '/(app)/admin'
		},
		{
			title: 'Documentation',
			icon: 'mdi:file-question-outline',
			url: 'https://doc.convertigo.com/'
		},
		{
			title: 'Convertigo',
			icon: 'convertigo:logo',
			url: 'https://www.convertigo.com'
		}
	]
]);

export default {
	get parts() {
		return parts;
	},
	set extras(value) {
		extras = value;
	}
};
