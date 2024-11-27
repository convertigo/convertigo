let extras = $state([]);
const parts = $derived([
	[
		{
			title: 'Projects',
			icon: 'lucide:layout-panel-top',
			page: '/(app)/dashboard'
		},
		...extras
	],
	[
		{
			title: 'Admin',
			icon: 'carbon:panel-expansion',
			page: '/(app)/admin'
		},
		{
			title: 'Documentation',
			icon: 'grommet-icons:resources',
			url: 'https://doc.convertigo.com/'
		},
		{
			title: 'Convertigo',
			icon: 'mdi:marketplace-outline',
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
