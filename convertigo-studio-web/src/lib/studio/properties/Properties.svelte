<script>
	import { Accordion, AccordionItem } from '@skeletonlabs/skeleton';
	import { selectedId } from '$lib/studio/treeview/treeStore';
	import { onDestroy } from 'svelte';
	import BooleanEditor from './editors/BooleanEditor.svelte';
	import ListEditor from './editors/ListEditor.svelte';
	import StaticEditor from './editors/StaticEditor.svelte';
	import StringEditor from './editors/StringEditor.svelte';
	import { dboProp, ionProp, properties, setDboProp } from './propertiesStore';

	//import IonSmartEditor from './editors/IonSmartEditor.svelte.disable';

	let categories = $state({});

	const unsubscribeProperties = properties.subscribe((value) => {
		let cats = [
			...new Set(
				Object.values(value)
					.map((item) => item['category'])
					.sort()
			)
		];

		categories = {};
		cats.forEach((cat) => {
			categories[cat] = [];
		});
		Object.entries($properties).forEach((entry) => {
			let category = entry[1].category;
			categories[category].push(entry);
			categories[category].sort();
		});
	});

	onDestroy(() => {
		unsubscribeProperties;
	});

	/**
	 * @param {dboProp | ionProp} prop
	 */
	function getEditor(prop) {
		if (prop) {
			let propType = prop.kind === 'ion' ? prop.type : prop.class;

			let propValues = prop.values
				? prop.values.filter((value) => {
						return typeof value != 'boolean' && value != 'false' && value != 'true';
					})
				: [];

			let propDisabled = prop.isDisabled ?? false;

			// if (prop.kind === 'ion' || prop.editorClass === 'NgxSmartSourcePropertyDescriptor') {
			// 	return IonSmartEditor;
			// }

			if (propValues.length > 0) {
				return ListEditor;
			}

			switch (propType) {
				case 'boolean':
				case 'java.lang.Boolean':
					return BooleanEditor;
				default:
					return propDisabled ? StaticEditor : StringEditor;
			}
		}
	}

	function findStoreProperty(propertyName) {
		for (const [key, value] of Object.entries($properties)) {
			if (value.name === propertyName) {
				return value;
			}
		}
		return undefined;
	}

	async function valueChanged(e) {
		let propertyName = e.detail.name;
		if (propertyName) {
			let property = findStoreProperty(propertyName);
			let oldProp = { ...property };
			let newProp = { ...property, ...e.detail };

			//console.log('@Properties valueChanged', newProp);
			//let b = JSON.parse(JSON.stringify($properties))
			//console.log('@Properties before', b);

			let result = await setDboProp($selectedId, JSON.stringify(newProp));
			if (result.done) {
				let key = newProp.kind === 'dbo' ? newProp.displayName : newProp.label;
				properties.update((m) => Object.assign({}, m, { [key]: newProp }));
			} else {
				let key = oldProp.kind === 'dbo' ? oldProp.displayName : oldProp.label;
				properties.update((m) => Object.assign({}, m, { [key]: oldProp }));
			}
			//let a = JSON.parse(JSON.stringify($properties));
			//console.log('@Properties after', a);
		}
	}
</script>

<!--<div class="table-container">
	<table class="table table-hover table-compact">
		<thead>
			<tr>
				<th>Name</th>
				<th>Value</th>
			</tr>
		</thead>
		<tbody>
			{#each Object.entries($properties) as [name, prop] (JSON.stringify(prop))}
				<tr>
					<td>{name}</td>
					<td
						><svelte:component
							this={getEditor(prop)}
							{...prop}
							on:valueChanged={valueChanged}
						/></td
					>
				</tr>
			{/each}
		</tbody>
	</table>
</div>-->

<div class="flex bg-surface-50 dark:bg-surface-800">
	<Accordion caretOpen="rotate-0" caretClosed="-rotate-90" regionControl="preset-soft-primary">
		<div>
			{#each Object.entries(categories) as [category, items] (category)}
				<AccordionItem open rounded="none">
					<svelte:fragment slot="summary"
						><b class="mt-5 text-[11.5px] font-light dark:text-surface-200">{category}</b
						></svelte:fragment
					>
					<svelte:fragment slot="content">
						{#each items as item (JSON.stringify(item[1]))}
							{@const Editor = getEditor(item[1])}
							<div
								class="flex flex-row flex-nowrap text-[11.5px] text-surface-900 dark:font-light dark:text-surface-200"
							>
								<div class="ml-2.5 basis-1/3">{item[0]}</div>
								<div class="basis-2/3 pl-1">
									<Editor {...item[1]} onvalueChanged={valueChanged} />
								</div>
							</div>
						{/each}
					</svelte:fragment>
				</AccordionItem>
			{/each}
		</div>
	</Accordion>
</div>
