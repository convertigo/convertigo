let extras = $state([]);
const parts = $derived([
	[
		{
			title: 'Projects',
			icon: 'lucide:layout-panel-top',
			url: ''
		},
		...extras
	],
	[
		{
			title: 'Admin',
			icon: 'carbon:panel-expansion',
			url: '../admin'
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
