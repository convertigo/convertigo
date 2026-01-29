<script>
	import ActionBar from '$lib/admin/components/ActionBar.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import FileUploadField from '$lib/admin/components/FileUploadField.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import EnvironmentVariables from '$lib/admin/EnvironmentVariables.svelte';
	import Symbols from '$lib/admin/Symbols.svelte';
	import InputGroup from '$lib/common/components/InputGroup.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import { getContext } from 'svelte';
	import { persistedState } from 'svelte-persisted-state';
	import { slide } from 'svelte/transition';

	let {
		symbols,
		importSymbols,
		exportSymbols,
		addSymbol,
		deleteSymbol,
		deleteAllSymbols,
		waiting,
		init
	} = $derived(Symbols);

	let modalYesNo = getContext('modalYesNo');
	let filterState = persistedState('admin.symbols.filter', '', { syncTabs: false });
	let filter = $derived(filterState.current);
	let secretName = $state('');
	let exporting = $state(false);
	let modalImport = $state();
	let modal = $state();
	let actionImport = $state('on');
	/*** @type {any} */

	let fsymbols = $derived(
		symbols.filter((s) => JSON.stringify(s).toLowerCase().includes(filter.toLowerCase()))
	);

	function modalOpen(params) {
		secretName = '';
		modal.open(params);
	}
</script>

<ModalDynamic bind:this={modal}>
	{#snippet children({ close, params: { row, secret = false } })}
		{@const edit = row && !row.project}
		{@const sec = secret || row?.name?.endsWith('.secret')}
		<Card title={`${edit ? 'Edit' : 'Add'} ${sec ? 'Secret ' : ''}Symbol`}>
			<form
				onsubmit={async (event) => {
					if (await addSymbol(event, row)) {
						close();
					}
				}}
				class="layout-y-stretch min-w-72 md:min-w-96"
			>
				{#if edit}
					<input type="hidden" name="oldSymbolName" value={row?.name} />
				{/if}
				{#if sec}
					{#if row}
						<input type="hidden" name="symbolName" value={row?.name} />
						<div class="layout-x-end-low">
							<PropertyType label="Name" value={row?.name?.replace(/\.secret$/, '')} disabled />
							<span>.secret</span>
						</div>
					{:else}
						<input type="hidden" name="symbolName" value="{secretName}.secret" />
						<div class="layout-x-end-low">
							<PropertyType label="Name" bind:value={secretName} /><span>.secret</span>
						</div>
					{/if}
				{:else}
					<PropertyType name="symbolName" label="Name" value={row?.name} />
				{/if}
				{#if sec}
					<PropertyType type="password" name="symbolValue" label="Value" value={row?.value} />
				{:else}
					<PropertyType type="textarea" name="symbolValue" label="Value" value={row?.value} />
				{/if}
				<ActionBar disabled={waiting}>
					<Button
						type="submit"
						class="button-primary w-fit!"
						icon={edit ? 'mdi:edit-outline' : sec ? 'mdi:key-outline' : 'mdi:plus'}
						size="btn"
						label={edit ? 'Edit' : 'Add'}
					/>
					<Button
						onclick={close}
						class="button-secondary w-fit!"
						icon="mdi:close-circle-outline"
						label="Cancel"
					/>
				</ActionBar>
			</form>
		</Card>
	{/snippet}
</ModalDynamic>

<ModalDynamic bind:this={modalImport}>
	<Card title="Drop or choose a .properties file and Import">
		<form
			onsubmit={async (event) => {
				await importSymbols(event);
				modalImport.close();
			}}
		>
			<fieldset class="layout-y-stretch" disabled={waiting}>
				<FileUploadField
					name="file"
					accept={{ 'application/text': ['.properties'] }}
					required
					allowDrop
					dropIcon="mdi:star-outline"
					title="Drop or choose a .properties file"
					hint="then press Import"
				/>
				<div>
					Import policy
					<PropertyType
						type="segment"
						name="action-import"
						item={[
							{ text: 'Clear & Import', value: 'clear-import' },
							{ text: 'Merge symbols', value: 'on' }
						]}
						bind:value={actionImport}
						orientation="vertical"
					/>
				</div>
				{#if actionImport == 'on'}
					<div transition:slide>
						In case of name conflict, priority
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
					<div>Current symbols will be kept.</div>
				{/if}
				<div>Actual symbols list will be saved aside in a backup file.</div>
				<ActionBar>
					<Button
						label="Import"
						icon="mdi:star-outline"
						type="submit"
						class="button-primary w-fit!"
					/>
					<Button
						label="Cancel"
						icon="mdi:close-circle-outline"
						class="button-secondary w-fit!"
						onclick={modalImport?.close}
					/>
				</ActionBar>
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
					icon: 'mdi:plus',
					cls: 'button-primary',
					hidden: exporting,
					onclick: (event) => modalOpen({ event })
				},
				{
					label: 'Add Secret',
					icon: 'mdi:key-outline',
					cls: 'button-secondary',
					hidden: exporting,
					onclick: (event) => modalOpen({ event, secret: true })
				},
				{
					label: 'Import',
					icon: 'mdi:import',
					cls: 'button-secondary',
					hidden: exporting,
					onclick: modalImport?.open
				},
				{
					label: 'Select All',
					icon: 'mdi:check-all',
					cls: 'button-secondary',
					hidden: !exporting || fsymbols.every((user) => user.export),
					onclick: () => fsymbols.forEach((user) => (user.export = true))
				},
				{
					label: 'Unselect All',
					icon: 'mdi:check-all',
					cls: 'button-secondary',
					hidden: !exporting || fsymbols.every((user) => !user.export),
					onclick: () => fsymbols.forEach((user) => (user.export = false))
				},
				{
					label: 'Export',
					icon: 'mdi:export',
					cls: 'button-secondary',
					hidden: exporting,
					onclick: () => {
						exporting = true;
					}
				},
				{
					label: `Export [${fsymbols.filter((user) => user.export).length}]`,
					icon: 'mdi:export',
					cls: 'button-primary',
					hidden: !exporting,
					disabled: fsymbols.every((user) => !user.export),
					onclick: exportSymbols
				},
				{
					label: 'Cancel',
					icon: 'mdi:close-circle-outline',
					cls: 'button-secondary',
					hidden: !exporting,
					onclick: () => {
						exporting = false;
					}
				},
				{
					label: 'Delete All',
					icon: 'mdi:delete-outline',
					cls: 'button-secondary',
					hidden: exporting,
					onclick: async () => {
						if (
							await modalYesNo.open({
								title: 'Delete all symbols',
								message: `Are you sure you want to delete all symbols?`
							})
						) {
							deleteAllSymbols();
						}
					}
				}
			]}
			disabled={!init}
		/>
	{/snippet}

	<p>
		Symbols are defined here. Their values can be used in Convertigo objects using the <strong
			>{'${symbolName}'}</strong
		>
		or <strong class="text-nowrap">{'${symbolName=default value}'}</strong> syntax.
	</p>
	<p>
		The value can be a fixed string, another Symbol (using <strong>{'{symb\\}'}</strong>) or an
		Environment Variable (see below).
	</p>
	<InputGroup
		id="symbolsFilter"
		type="search"
		placeholder="Filter symbols..."
		autofocus
		class="bg-surface-200-800"
		icon="mdi:magnify"
		bind:value={filterState.current}
	/>
	<TableAutoCard
		definition={[
			{ name: 'Name', key: 'name' },
			{ name: 'Value', key: 'value', class: 'truncate max-w-xl text-xs' },
			{ name: 'Actions', custom: true, class: 'max-w-28' }
		]}
		data={fsymbols.filter((s) => !exporting || !s.project)}
		fnRowId={({ index }) => index}
	>
		{#snippet children({ row, def })}
			{#if def.name == 'Actions'}
				{@const size = 6}
				<fieldset class="layout-grid-low-5 w-full" disabled={!init}>
					{#if row.project}
						<Button
							label={row.project}
							class="button-primary justify-start gap-1 overflow-hidden px-1 text-xs"
							{size}
							icon="mdi:plus"
							onclick={(event) => modalOpen({ event, row })}
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
							class="button-ico-primary"
							{size}
							icon="mdi:edit-outline"
							onclick={(event) => {
								modalOpen({ event, row });
							}}
						/>
						<Button
							class="button-ico-primary"
							{size}
							icon="mdi:delete-outline"
							onclick={async () => {
								if (
									await modalYesNo.open({
										title: 'Delete symbol',
										message: `Are you sure you want to delete ${row.name}?`
									})
								) {
									deleteSymbol(row.name);
								}
							}}
						/>
					{/if}
				</fieldset>
			{/if}
		{/snippet}
	</TableAutoCard>
	<p>
		These environment variables can be used in Symbols values, using the following syntax:
		<strong>%variable_name[=default_value]%</strong>, default_value is optional.
	</p>
	<TableAutoCard
		comment=""
		definition={[
			{ name: 'Name', key: 'name' },
			{ name: 'Value', key: 'value', class: 'max-w-xl' }
		]}
		data={EnvironmentVariables.variables}
	></TableAutoCard>
</Card>
