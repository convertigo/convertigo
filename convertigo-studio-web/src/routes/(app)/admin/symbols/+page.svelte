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
	import { getContext } from 'svelte';

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
	let filter = $state('');
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
				{#if sec && !row}
					<input type="hidden" name="symbolName" value="{secretName}.secret" />
					<div class="layout-x-end-low">
						<PropertyType label="Name" bind:value={secretName} /><span>.secret</span>
					</div>
				{:else}
					<PropertyType name="symbolName" label="Name" value={row?.name} />
				{/if}
				{#if sec}
					<PropertyType type="password" name="symbolValue" label="Value" value={row?.value} />
				{:else}
					<PropertyType type="textarea" name="symbolValue" label="Value" value={row?.value} />
				{/if}
				<fieldset class="w-full layout-x justify-end" disabled={waiting}>
					<Button
						type="submit"
						class="!w-fit  basic-button"
						icon={edit ? 'mdi:edit-outline' : 'grommet-icons:add'}
						size="btn"
						label={edit ? 'Edit' : 'Add'}
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
	<Card title="Drop or choose a .properties file and Import">
		<form
			onsubmit={async (event) => {
				await importSymbols(event);
				modalImport.close();
			}}
		>
			<fieldset class="layout-y-stretch" disabled={waiting}>
				<FileUpload
					name="file"
					accept={{ 'application/text': ['.properties'] }}
					maxFiles={1}
					subtext="then press Import"
					classes="w-full"
					required
					allowDrop
				>
					{#snippet iconInterface()}<Ico
							icon="material-symbols:hotel-class-outline"
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
					<div>Current symbols will be kept.</div>
				{/if}
				<div>Actual symbols list will be saved aside in a backup file.</div>
				<div class="w-full layout-x justify-end">
					<Button
						label="Import"
						icon="material-symbols:hotel-class-outline"
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
					onclick: (event) => modalOpen({ event })
				},
				{
					label: 'Add Secret',
					icon: 'vaadin:key-o',
					cls: 'yellow-button',
					hidden: exporting,
					onclick: (event) => modalOpen({ event, secret: true })
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
					hidden: !exporting || fsymbols.every((user) => user.export),
					onclick: () => fsymbols.forEach((user) => (user.export = true))
				},
				{
					label: 'Unselect All',
					icon: 'mdi:check-all',
					cls: 'yellow-button',
					hidden: !exporting || fsymbols.every((user) => !user.export),
					onclick: () => fsymbols.forEach((user) => (user.export = false))
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
					label: `Export [${fsymbols.filter((user) => user.export).length}]`,
					icon: 'bytesize:export',
					cls: 'green-button',
					hidden: !exporting,
					disabled: fsymbols.every((user) => !user.export),
					onclick: exportSymbols
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
	<div
		class="w-full input-group bg-surface-200-800 divide-surface-700-300 preset-outlined-surface-700-300 divide-x grid-cols-[auto_1fr_auto]"
	>
		<div class="input-group-cell"><Ico icon="mdi:magnify" /></div>
		<input type="search" placeholder="Filter symbols..." bind:value={filter} />
	</div>
	<TableAutoCard
		definition={[
			{ name: 'Actions', custom: true, class: 'max-w-28' },
			{ name: 'Name', key: 'name' },
			{ name: 'Value', key: 'value', class: 'truncate max-w-xl text-xs' }
		]}
		data={fsymbols.filter((s) => !exporting || !s.project)}
	>
		{#snippet children({ row, def })}
			{#if def.name == 'Actions'}
				<fieldset class="layout-grid-low-5 w-full" disabled={!init}>
					{#if row.project}
						<Button
							label={row.project}
							class="green-button text-xs overflow-hidden justify-start gap-1 px-1"
							size={4}
							icon="grommet-icons:add"
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
							class="basic-button"
							size={4}
							icon="mdi:edit-outline"
							onclick={(event) => {
								modalOpen({ event, row });
							}}
						/>
						<Button
							class="delete-button"
							size={4}
							icon="mingcute:delete-line"
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
		These environment variables can be used in Symbols values, using the following syntax: <strong
			>{'%variable_name[=default_value]%'}</strong
		>, default_value is optional.
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
