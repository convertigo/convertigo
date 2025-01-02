<script>
	import Card from '$lib/admin/components/Card.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import Symbols from '$lib/admin/Symbols.svelte';
	import EnvironmentVariables from '$lib/admin/EnvironmentVariables.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import { FileUpload } from '@skeletonlabs/skeleton-svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { slide } from 'svelte/transition';

	let { symbols, importSymbols, addSymbol, waiting } = $derived(Symbols);

	let selectRow = $state(false);
	let allSelected = false;
	let exporting = $state(false);
	let modalImport = $state();
	let modal = $state();
	let actionImport = $state('clear-import');
	/*** @type {any} */
	let rowSelected = $state({});

	// const symbolsActions = {
	// 	add: {
	// 		name: 'Add Symbols',
	// 		icon: 'grommet-icons:add'
	// 	},
	// 	secret: {
	// 		name: 'Add secret symbol',
	// 		icon: 'vaadin:key-o'
	// 	},
	// 	import: {
	// 		name: 'Import Symbols',
	// 		icon: 'bytesize:import'
	// 	}
	// 	// export: {
	// 	// 	name: 'Export Symbols',
	// 	// 	icon: 'bytesize:export'
	// 	// }
	// };

	// function exportUserFile() {
	// 	const usersArray = Array.from(selectedUsers);
	// 	const jsonString = `data:text/json;chatset=utf-8,${encodeURIComponent(
	// 		JSON.stringify(usersArray)
	// 	)}`;
	// 	const link = document.createElement('a');
	// 	link.href = jsonString;
	// 	link.download = 'symbols_export.json';
	// 	link.click();
	// }
</script>

<ModalDynamic bind:this={modal}>
	{#snippet children({ close, params: { row } })}
		<Card title={`${row ? 'Edit' : 'Add'} Symbol`}>
			<form
				onsubmit={async (event) => {
					if (await addSymbol(event, row)) {
						close();
					}
				}}
				class="layout-y-stretch min-w-72 md:min-w-96"
			>
				<!-- <div class="layout-y sm:layout-x"> -->
				{#if row}
					<input type="hidden" name="oldSymbolName" value={row?.name} />
				{/if}
				<PropertyType name="symbolName" label="Name" value={row?.name} />
				<PropertyType type="textarea" name="symbolValue" label="Value" value={row?.value} />
				<!-- <PropertyType
						name={rowSelected.password.length > 0 ? 'password' : ''}
						label="Password"
						type="password"
						value={rowSelected.password}
					/> -->
				<!-- </div> -->

				<fieldset class="w-full layout-x justify-end" disabled={waiting}>
					<Button
						type="submit"
						class="!w-fit  basic-button"
						icon="bytesize:export"
						size="btn"
						label={row ? 'Edit' : 'Add'}
					/>
					<Button
						type="button"
						onclick={close}
						class="!w-fit cancel-button"
						icon="material-symbols-light:cancel-outline"
						label="Cancel"
					/>
				</fieldset>
			</form>
		</Card>
	{/snippet}
</ModalDynamic>
<ModalDynamic bind:this={modalImport}>
	<Card title="Drop or choose a .json file and Import">
		<form
			onsubmit={async (event) => {
				await importSymbols(event);
				modalImport.close();
			}}
		>
			<fieldset class="layout-y-stretch" disabled={waiting}>
				<FileUpload
					name="file"
					accept={{ 'application/json': ['.json'] }}
					maxFiles={1}
					subtext="then press Import"
					classes="w-full"
					required
					allowDrop
				>
					{#snippet iconInterface()}<Ico
							icon="material-symbols:supervised-user-circle-outline"
							size="8"
						/>{/snippet}
					{#snippet iconFile()}<Ico icon="mdi:briefcase-upload-outline" size="8" />{/snippet}
					{#snippet iconFileRemove()}<Ico
							icon="material-symbols-light:delete-outline"
							size="8"
						/>{/snippet}
				</FileUpload>
				<div>
					Import policy:
					<PropertyType
						type="segment"
						name="action-import"
						item={[
							{ text: 'Clear & Import', value: 'clear-import' },
							{ text: 'Merge users', value: 'on' }
						]}
						bind:value={actionImport}
						orientation="vertical"
					/>
				</div>
				{#if actionImport == 'on'}
					<div transition:slide>
						In case of name conflict, priority:
						<PropertyType
							type="segment"
							name="priority"
							item={[
								{ text: 'Server', value: 'priority-server' },
								{ text: 'Import', value: 'priority-import' }
							]}
							value="priority-import"
							orientation="vertical"
						/>
					</div>
					<div>Current users will be kept.</div>
				{/if}
				<div>Actual users list will be saved aside in a backup file.</div>
				<div class="w-full layout-x justify-end">
					<Button
						label="Import"
						icon="material-symbols:supervised-user-circle-outline"
						type="submit"
						class="!w-fit basic-button"
					/>
					<Button
						label="Cancel"
						icon="material-symbols-light:cancel-outline"
						type="button"
						class="!w-fit cancel-button"
						onclick={modalImport.close}
					/>
				</div>
			</fieldset>
		</form>
	</Card>
</ModalDynamic>
<Card title="Global Symbols">
	{#snippet cornerOption()}
		<ResponsiveButtons
			class="max-w-4xl"
			buttons={[
				{
					label: 'Add',
					icon: 'grommet-icons:add',
					cls: 'green-button',
					hidden: exporting,
					onclick: modal?.open
				},
				{
					label: 'Add Secret',
					icon: 'vaadin:key-o',
					cls: 'yellow-button',
					hidden: exporting,
					onclick: modal?.open
				},
				{
					label: 'Import',
					icon: 'bytesize:import',
					cls: 'basic-button',
					hidden: exporting,
					onclick: modalImport?.open
				},
				{
					label: 'Select All',
					icon: 'mdi:check-all',
					cls: 'green-button',
					hidden: !exporting || symbols.every((user) => user.export),
					onclick: () => symbols.forEach((user) => (user.export = true))
				},
				{
					label: 'Unselect All',
					icon: 'mdi:check-all',
					cls: 'yellow-button',
					hidden: !exporting || symbols.every((user) => !user.export),
					onclick: () => symbols.forEach((user) => (user.export = false))
				},
				{
					label: 'Export',
					icon: 'bytesize:export',
					cls: 'basic-button',
					hidden: exporting,
					onclick: () => {
						exporting = true;
					}
				},
				{
					label: `Export [${symbols.filter((user) => user.export).length}]`,
					icon: 'bytesize:export',
					cls: 'green-button',
					hidden: !exporting,
					disabled: symbols.every((user) => !user.export),
					href: '#',
					target: '_blank'
				},
				{
					label: 'Cancel',
					icon: 'material-symbols-light:cancel-outline',
					cls: 'delete-button',
					hidden: !exporting,
					onclick: () => {
						exporting = false;
					}
				},
				{
					label: 'Delete All',
					icon: 'mingcute:delete-line',
					cls: 'delete-button',
					hidden: exporting,
					onclick: async () => {}
				}
			]}
		/>
	{/snippet}

	<TableAutoCard
		comment="Global Symbols values can be fixed string, another Global Symbols or Environment Variables. If a
			symbol is defined for the Default value or if it contains a closing curly braces it must be
			escaped with a backslash."
		definition={[
			{ name: 'Actions', custom: true, class: 'max-w-28' },
			{ name: 'Name', key: 'name' },
			{ name: 'Value', key: 'value', class: 'truncate max-w-80' }
		]}
		data={symbols.filter((s) => !exporting || !s.project)}
	>
		{#snippet children({ row, def })}
			{#if def.name == 'Actions'}
				<div class="layout-x-low">
					{#if row.project}
						<Button
							label={`from ${row.project}`}
							class="green-button !truncate"
							size={4}
							icon="grommet-icons:add"
							onclick={(event) => {}}
						/>
					{:else if exporting}
						<PropertyType
							values={[false, true]}
							type="boolean"
							name="export"
							bind:value={row.export}
						/>
					{:else}
						<Button
							class="basic-button"
							size={4}
							icon="mdi:edit-outline"
							onclick={(event) => {
								modal.open({ event, row });
							}}
						/>
						<Button
							class="delete-button"
							size={4}
							icon="mingcute:delete-line"
							onclick={async () => {}}
						/>
					{/if}
				</div>
			{/if}
		{/snippet}
	</TableAutoCard>

	<!-- <TableAutoCard
		comment="These symbols are defined in projects with default values. You can modify these values by adding them to the symbols list"
		definition={[
			{ name: 'Actions', custom: true },
			{ name: 'Project', key: 'project' },
			{ name: 'Name', key: 'name' },
			{ name: 'Value', key: 'value' }
		]}
		data={Symbols.defaults}
	>
		{#snippet children({ row, def })}
			{#if def.name == 'Actions'}
				<button class="green-button" onclick={() => {}}>
					<Ico icon="grommet-icons:add" />
				</button>
			{/if}
		{/snippet}
	</TableAutoCard> -->

	<TableAutoCard
		comment="These environment variables can be used in Global Symbols values, using the following syntax: %variable_name[=default_value]%, default_value is optional."
		definition={[
			{ name: 'Name', key: 'name' },
			{ name: 'Value', key: 'value' }
		]}
		data={EnvironmentVariables.variables}
	></TableAutoCard>
</Card>
