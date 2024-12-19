<script>
	import Card from '$lib/admin/components/Card.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import Roles from '$lib/admin/Roles.svelte';
	import { rolesStore } from '$lib/admin/stores/rolesStore';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import ModalYesNo from '$lib/common/components/ModalYesNo.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { call } from '$lib/utils/service';

	let { users, roles, deleteRoles, importRoles, exportRoles } = $derived(Roles);

	let modalDelete = $state();
	let modalImport = $state();

	let importMode = 'clear';
	let nameConflict = 'server';

	let showExportColumn = $state(false);

	let file = $state(null);

	/*** @type {any} */
	let rowSelected = $state(null);
	let modal = $state();

	function openRoleModal({ mode, row = undefined }) {
		rowSelected = {
			name: '',
			password: '',
			...(row ?? {})
		};
		modal.open({ mode, row });
	}

	let tableDefinition = $state([
		{ name: 'Name', key: 'name' },
		{ name: 'Roles', custom: true },
		{ name: 'Actions', custom: true }
	]);

	let viewRoles = $derived(roles.filter((role) => role.name.endsWith('_VIEW')));
	let configRoles = $derived(roles.filter((role) => role.name.endsWith('_CONFIG')));
	let otherRoles = $derived(
		roles.filter((role) => !role.name.endsWith('_VIEW') && !role.name.endsWith('_CONFIG'))
	);

	function toggleExportColumn() {
		showExportColumn = !showExportColumn;
		if (showExportColumn) {
			tableDefinition = [{ name: 'Export', custom: true }, ...tableDefinition];
		} else {
			tableDefinition = tableDefinition.filter((col) => col.name !== 'Export');
		}
	}

	let calling, yesNo;
</script>

<ModalDynamic bind:this={modal}>
	{#snippet children({ close, params: { mode, row } })}
		{@const { name, password } = row ?? {}}
		<Card title={mode === 'edit' ? 'Edit Role' : mode === 'addRoles' ? 'Add Role' : 'All Roles'}>
			{#if mode === 'edit'}
				<form
					onsubmit={async (event) => {
						event.preventDefault();
					}}
					class="layout-y"
				>
					<div>
						<PropertyType
							name="username"
							label="Username"
							bind:value={rowSelected.name}
							placeholder="Enter role name"
						/>
						<PropertyType
							name="password"
							label="Password"
							bind:value={rowSelected.password}
							placeholder="Enter password"
						/>
					</div>

					<div class="w-full layout-x justify-end">
						<button type="submit" class="basic-button" disabled={calling}>
							<span><Ico icon="bytesize:export" size="btn" /></span>
							<span>{modal.params?.mode === 'add' ? 'Add' : 'Save'}</span>
						</button>
						<button type="button" onclick={modal.close} class="cancel-button">
							<span><Ico icon="material-symbols-light:cancel-outline" size="btn" /></span>
							<span>Cancel</span>
						</button>
					</div>
				</form>
			{:else if mode === 'addRoles'}
				<form
					onsubmit={async (event) => {
						event.preventDefault();
						calling = true;
						const formData = new FormData(event.target);
						function collectRoles(roles) {
							roles.forEach((role) => {
								if (formData.get(role.name) === 'true') {
									formData.append('roles', role.name);
								}
								formData.delete(role.name);
							});
						}

						collectRoles(viewRoles);
						collectRoles(configRoles);
						collectRoles(otherRoles);

						await call('roles.Add', formData);
						calling = false;
						modal.close();
					}}
					class="layout-y-low"
				>
					<div class="w-full layout-x justify-start">
						<PropertyType name="username" label="Username" placeholder="Username" />
						<PropertyType name="password" label="Password" placeholder="Password" type="password" />
					</div>

					<div class="w-full grid gap grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-3">
						{#each [{ title: 'VIEW Roles', roles: viewRoles }, { title: 'CONFIG Roles', roles: configRoles }, { title: 'Other Roles', roles: otherRoles }] as { title, roles }}
							<div class="col-span-1 flex flex-col gap-2">
								<p class="font-bold">{title}</p>
								{#each roles as role}
									<PropertyType name={role.name} type="boolean" description={role.name} />
								{/each}
							</div>
						{/each}
					</div>

					<div class="w-full layout-x justify-end">
						<button type="submit" class="basic-button">
							<span><Ico icon="bytesize:import" size="btn" /></span>
							<span>Add user</span>
						</button>
						<button type="button" onclick={() => modal.close()} class="cancel-button">
							<span><Ico icon="material-symbols-light:cancel-outline" size="btn" /></span>
							<span>Cancel</span>
						</button>
					</div>
				</form>
			{:else if mode === 'watchAllRoles'}
				<div class="w-full grid gap grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-3">
					{#each [{ title: 'VIEW Roles', roles: viewRoles }, { title: 'CONFIG Roles', roles: configRoles }, { title: 'Other Roles', roles: otherRoles }] as { title, roles }}
						<div class="col-span-1 flex flex-col gap-2">
							<p class="font-bold">{title}</p>
							{#each roles as role}
								<span class="chip">{role.name}</span>
							{/each}
						</div>
					{/each}
				</div>
			{/if}
		</Card>
	{/snippet}
</ModalDynamic>

<Card title="Roles Management">
	{#snippet cornerOption()}
		<ResponsiveButtons
			class="max-w-4xl"
			buttons={[
				{
					label: 'Add Role',
					icon: 'grommet-icons:add',
					cls: 'green-button',
					onclick: () => openRoleModal({ mode: 'addRoles' })
				},
				{
					label: 'Import Users',
					icon: 'bytesize:import',
					cls: 'basic-button',
					onclick: modalImport?.open
				},
				{
					label: showExportColumn ? 'Hide Export Column' : 'Show Export Column',
					icon: 'bytesize:export',
					cls: 'basic-button',
					onclick: toggleExportColumn
				},
				{
					label: 'Delete All Users',
					icon: 'mingcute:delete-line',
					cls: 'delete-button',
					onclick: async () => {
						if (
							await modalDelete.open({
								title: 'Delete all users',
								message: `Are you sure you want to delete all users?`
							})
						) {
							deleteRoles('');
						}
					}
				}
			]}
			size="4"
		/>
	{/snippet}
</Card>

<Card title="Roles and Permissions" class="mt-5">
	{#snippet cornerOption()}
		{#if showExportColumn}
			<ResponsiveButtons
				buttons={[
					{
						label: 'Select all users',
						icon: 'bytesize:export',
						cls: 'yellow-button',
						onclick: toggleExportColumn
					}
				]}
				size="4"
			/>
		{/if}
	{/snippet}
	{#if roles?.length > 0 && users?.length > 0}
		<TableAutoCard definition={tableDefinition} data={users}>
			{#snippet children({ row, def })}
				{#if def.name === 'Roles'}
					{#if row.roles.length > 5}
						<div class="chips-container">
							{#each row.roles.slice(0, 5) as role}
								<span class="chip">{role.name}</span>
							{/each}
							<button
								class="yellow-button"
								onclick={() => openRoleModal({ mode: 'watchAllRoles', row })}
							>
								+{row.roles.length - 5} more
							</button>
						</div>
					{:else}
						<div class="chips-container">
							{#each row.roles as role}
								<span class="chip">{role.name}</span>
							{/each}
						</div>
					{/if}
				{:else if def.name === 'Actions'}
					<div class="layout-x-low">
						<button class="basic-button" onclick={() => openRoleModal({ mode: 'edit', row })}>
							<Ico icon="mdi:edit-outline" />
						</button>
						<button
							class="delete-button"
							onclick={async () => {
								if (
									await modalDelete.open({
										title: 'Delete user',
										message: `Are you sure you want to delete ${row.name}?`
									})
								) {
									deleteRoles(row.name);
								}
							}}
						>
							<Ico icon="mingcute:delete-line" />
						</button>
					</div>
				{:else if def.name === 'Export'}
					<PropertyType type="boolean" name="export" bind:this={row.export} />
				{/if}
			{/snippet}
		</TableAutoCard>
	{:else}
		<p>Loading roles and users...</p>
	{/if}
</Card>
<ModalYesNo bind:this={modalDelete} />

<style>
	.chips-container {
		display: flex;
		flex-wrap: wrap;
		gap: 0.5rem; /* Spacing between chips */
	}
</style>
