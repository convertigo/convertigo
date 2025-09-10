let extras = $state([]);
const parts = $derived([
	[
		{
			title: 'Projects',
			icon: 'mdi:panel-top',
			page: '/(app)/dashboard'
		},
		...extras
	],
	[
		{
			title: 'Admin',
			icon: 'mdi:unfold-more-horizontal',
			page: '/(app)/admin'
		},
		{
			title: 'Documentation',
			icon: 'mdi:book-multiple',
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
