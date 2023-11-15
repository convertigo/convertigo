<script>
	import { properties, dboProp, ionProp } from './propertiesStore';
	import StringEditor from './editors/StringEditor.svelte';
	import BooleanEditor from './editors/BooleanEditor.svelte';
	import ListEditor from './editors/ListEditor.svelte';
	import IonSmartEditor from './editors/IonSmartEditor.svelte';
	import { selectedId } from '$lib/treeview/treeStore';
	import { setDboProp } from './propertiesStore';

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

			if (prop.kind === 'ion') {
				return IonSmartEditor;
			}

			if (propValues.length > 0) {
				return ListEditor;
			}

			switch (propType) {
				case 'boolean':
				case 'java.lang.Boolean':
					return BooleanEditor;
				default:
					return StringEditor;
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

<div class="table-container">
	<!-- Native Table Element -->
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
</div>
