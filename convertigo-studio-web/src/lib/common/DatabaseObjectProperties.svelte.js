import { call } from '$lib/utils/service';

const _categories = ['Base properties', 'Expert', 'Information'];

export function createDatabaseObjectProperties() {
	let id = $state('');
	let properties = $state([]);
	let categories = $derived(
		_categories.map((c) => ({
			category: c,
			properties: properties.filter((p) => p.category == c)
		}))
	);
	let hasChanges = $derived(properties.some((p) => p.value != p.originalValue));

	async function onSelectionChange(e) {
		id = e.selectedValue[0];
		const res = await call('studio.properties.Get', {
			id
		});
		properties = Object.entries(res?.properties ?? {}).map(([k, p]) => ({
			displayName: k,
			originalValue: p.value,
			...p
		}));
	}

	function cancel() {
		properties.forEach((p) => {
			p.value = p.originalValue;
		});
	}

	function getChanges() {
		return properties.filter((p) => p.value != p.originalValue);
	}

	async function save() {
		const changes = getChanges();
		if (changes.length > 0) {
			const res = await call('studio.properties.Set', {
				id,
				props: JSON.stringify(changes),
				save: true
			});
			if (res?.done) {
				changes.forEach((p) => {
					p.originalValue = p.value;
				});
			} else {
				onSelectionChange({ selectedValue: [id] });
			}
		}
	}

	return {
		get id() {
			return id;
		},
		get categories() {
			return categories;
		},
		get properties() {
			return properties;
		},
		get hasChanges() {
			return hasChanges;
		},
		onSelectionChange,
		cancel,
		getChanges,
		save
	};
}
