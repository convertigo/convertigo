import { call } from '$lib/utils/service';

const _categories = ['Base properties', 'Expert', 'Information'];

export function createDatabaseObjectProperties() {
	let id = $state('');
	let properties = $state([]);
	let loading = $state(false);
	let selectionLoadSerial = 0;
	let categories = $derived(
		_categories.map((c) => ({
			category: c,
			properties: properties.filter((p) => p.category == c)
		}))
	);
	let hasChanges = $derived(properties.some((p) => p.value != p.originalValue));

	async function onSelectionChange(e) {
		const nextId = e.selectedValue[0];
		const serial = ++selectionLoadSerial;
		id = nextId;
		loading = true;
		try {
			const res = await call('studio.properties.Get', {
				id: nextId
			});
			if (serial !== selectionLoadSerial || id !== nextId) {
				return;
			}
			properties = Object.entries(res?.properties ?? {}).map(([k, p]) => ({
				displayName: k,
				originalValue: p.value,
				...p
			}));
		} finally {
			if (serial === selectionLoadSerial && id === nextId) {
				loading = false;
			}
		}
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
		if (changes.length === 0) {
			return true;
		}
		const saveId = id;
		const res = await call('studio.properties.Set', {
			id: saveId,
			props: JSON.stringify(changes),
			save: true
		});
		if (res?.done) {
			changes.forEach((p) => {
				p.originalValue = p.value;
			});
			return true;
		}
		if (id === saveId) {
			onSelectionChange({ selectedValue: [saveId] });
		}
		return false;
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
		get loading() {
			return loading;
		},
		onSelectionChange,
		cancel,
		getChanges,
		save
	};
}
