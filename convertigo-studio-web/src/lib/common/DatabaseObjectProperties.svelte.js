import { call } from '$lib/utils/service';
import { SvelteSet } from 'svelte/reactivity';

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
		const propertyCategories = [
			...new SvelteSet(properties.map((p) => p.category).filter(Boolean))
		];
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
	let valid = $derived(
		properties.every(
			(property) =>
				property.validation?.value !== property.value || property.validation?.valid !== false
		)
	);

	function updateDraft(name, value, validation = {}) {
		const property = properties.find((candidate) => candidate.name === name);
		if (!property) return;
		property.value = value;
		property.validation = { ...validation, value };
	}

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
			delete p.validation;
			if ('mode' in p) {
				p.mode = p.originalMode;
			}
		});
	}

	function getChanges() {
		return properties.filter(propertyChanged);
	}

	/**
	 * @param {{ persist?: boolean, onSaved?: (id: string, result: any) => void | Promise<void> }} options
	 */
	async function save({ persist = true, onSaved } = {}) {
		if (!valid) return false;
		const changes = getChanges();
		if (changes.length === 0) {
			return true;
		}
		const saveId = id;
		const submitted = changes.map(({ validation, ...property }) => property);
		const res = await call('studio.properties.Set', {
			id: saveId,
			props: JSON.stringify(submitted),
			save: persist
		});
		if (res?.done) {
			changes.forEach((p, index) => {
				p.originalValue = submitted[index].value;
				if ('mode' in p) {
					p.originalMode = submitted[index].mode;
				}
			});
			await onSaved?.(saveId, res);
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
		get valid() {
			return valid;
		},
		get loading() {
			return loading;
		},
		onSelectionChange,
		updateDraft,
		cancel,
		getChanges,
		save
	};
}
