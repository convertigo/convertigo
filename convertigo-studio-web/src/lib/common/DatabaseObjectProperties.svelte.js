import { call } from '$lib/utils/service';

const _categories = ['Base properties', 'Properties', 'Expert', 'Information'];

/**
 * @param {string | undefined} category
 * @returns {string}
 */
function normalizeCategory(category) {
	if (!category) {
		return 'Base properties';
	}
	return category.startsWith('@') ? category.slice(1) : category;
}

export function createDatabaseObjectProperties() {
	let id = $state('');
	let properties = $state([]);
	let loading = $state(false);
	let selectionLoadSerial = 0;
	let categories = $derived.by(() => {
		const propertyCategories = [...new Set(properties.map((p) => p.category).filter(Boolean))];
		const extraCategories = propertyCategories.filter(
			(category) => !_categories.includes(category)
		);
		const orderedCategories = [
			..._categories.filter(
				(category) =>
					category !== 'Properties' || properties.some((property) => property.category === category)
			),
			...extraCategories
		];
		return orderedCategories.map((c) => ({
			category: c,
			properties: properties.filter((p) => p.category == c)
		}));
	});
	let hasChanges = $derived(properties.some(propertyChanged));

	function propertyChanged(property) {
		return (
			property.value != property.originalValue ||
			('mode' in property && property.mode != property.originalMode)
		);
	}

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
				originalMode: p.mode,
				...p,
				category: normalizeCategory(p.category)
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
			if ('mode' in p) {
				p.mode = p.originalMode;
			}
		});
	}

	function getChanges() {
		return properties.filter(propertyChanged);
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
				if ('mode' in p) {
					p.originalMode = p.mode;
				}
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
