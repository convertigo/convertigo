<script>
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import PagesRail from '$lib/admin/PagesRail.svelte';
	import Roles from '$lib/admin/Roles.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import ModalYesNo from '$lib/common/components/ModalYesNo.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { addInArray, removeInArray } from '$lib/utils/service';

	let { users, roles, deleteRoles, addUser, importRoles, exportRoles, formatRoleName } =
		$derived(Roles);

	let modalDelete = $state();
	let modalImport = $state();

	let showExportColumn = $state(false);

	/*** @type {any} */
	let rowSelected = $state({});
	let modal = $state();

	/** @param row {any} */
	function openRoleModal({ event, row = undefined }) {
		rowSelected = {
			name: row?.name ?? '',
			oldUsername: row?.name ?? '',
			password: row?.password ?? '',
			roles: [...(row?.roles ?? [])]
		};
		modal.open({ event, row });
	}

	let calling = $state(false);

	const tpItems = [
		{ text: 'No', value: ' ' },
		{ text: 'View', value: 'TEST_PLATFORM' },
		{ text: 'Hidden', value: 'TEST_PLATFORM_HIDDEN' },
		{ text: 'Private', value: 'TEST_PLATFORM_PRIVATE' }
	];

	let tpSelected = $derived.by(() => {
		let lastFound;
		for (const { value } of tpItems) {
			if (rowSelected.roles?.includes(value)) {
				lastFound = value;
			}
		}
		return lastFound ?? ' ';
	});
</script>

{#snippet roleCard({ roles, role })}
	{#if role.startsWith('TEST_PLATFORM')}
		{@render tpCard({ roles, role })}
	{:else}
		{@render catCard({ roles, role })}
	{/if}
{/snippet}

{#snippet catCard({ roles, role })}
	{@const isView = role.endsWith('_VIEW')}
	{@const isConfig = role.endsWith('_CONFIG')}
	{@const n = role.replace('_VIEW', '').replace('_CONFIG', '')}
	{@const hasView = roles.includes(`${n}_VIEW`)}
	{@const hasConfig = roles.includes(`${n}_CONFIG`)}
	{@const draw = isView || (isConfig && !hasView)}
	{@const part = PagesRail.parts[0].find(
		({ title }) => title.substring(0, 3).toUpperCase() == n.substring(0, 3)
	)}

	{#if draw}
		<div
			class="mini-card text-xs"
			class:preset-filled-success-500={hasView && hasConfig}
			class:preset-filled-primary-500={hasView && !hasConfig}
			class:preset-filled-warning-500={!hasView && hasConfig}
			class:preset-filled-surface-800-200={!hasView && !hasConfig}
		>
			{#if part}<Ico icon={part.icon} />{/if}
			{formatRoleName(n)}
			{#if hasView}<Ico icon="mdi:eye" />{/if}
			{#if hasConfig}<Ico icon="mdi:edit-outline" />{/if}
		</div>
	{/if}
{/snippet}

{#snippet tpCard({ roles, role })}
	{@const isTp = role == 'TEST_PLATFORM'}
	{@const isTpHidden = role == 'TEST_PLATFORM_HIDDEN'}
	{@const isTpPrivate = role == 'TEST_PLATFORM_PRIVATE'}
	{@const hasTpPrivate = roles.includes('TEST_PLATFORM_PRIVATE')}
	{@const hasTpHidden = hasTpPrivate || roles.includes('TEST_PLATFORM_HIDDEN')}
	{@const hasTp = hasTpHidden || roles.includes('TEST_PLATFORM')}
	{@const draw = isTpPrivate || (isTpHidden && !hasTpPrivate) || (isTp && !hasTpHidden)}
	{@const icon = PagesRail.parts[1][0].icon}

	{#if draw}
		<div
			class="mini-card text-xs"
			class:preset-filled-warning-500={hasTp && !hasTpHidden}
			class:preset-filled-primary-500={hasTpHidden && !hasTpPrivate}
			class:preset-filled-success-500={hasTpPrivate}
			class:preset-filled-surface-500={!hasTp}
		>
			<Ico {icon} />{formatRoleName(role)}
		</div>
	{/if}
{/snippet}

<ModalYesNo bind:this={modalDelete} />
<ModalDynamic bind:this={modal}>
	{#snippet children({ close, params: { row } })}
		<Card title={`${row ? 'Edit' : 'Add'} Role`}>
			<form
				onsubmit={async (event) => {
					addUser(event, row);
					close();
				}}
				class="layout-y"
			>
				<div class="layout-y sm:layout-x">
					{#if row}
						<input type="hidden" name="oldUsername" value={rowSelected.oldUsername} />
					{/if}
					<PropertyType name="username" label="Username" bind:value={rowSelected.name} />
					<PropertyType
						name={rowSelected.password.length > 0 ? 'password' : ''}
						label="Password"
						type="password"
						bind:value={rowSelected.password}
					/>
				</div>

				<TableAutoCard
					definition={[
						{ name: 'Role', custom: true, class: '!py-0' },
						{ name: 'View', custom: true, class: '!py-0' },
						{ name: 'Config', custom: true, class: '!py-0' }
					]}
					data={roles.filter((role) => role.endsWith('VIEW'))}
				>
					{#snippet children({ row: role, def })}
						{#if def.name == 'Role'}
							<div class="layout-x">
								{@render roleCard({ roles: rowSelected.roles, role })}
							</div>
						{:else}
							{@const value = def.name == 'View' ? role : role.replace('_VIEW', '_CONFIG')}
							<PropertyType
								name="roles"
								type="check"
								size="sm"
								{value}
								bind:checked={() => rowSelected.roles.includes(value),
								(v) => {
									if (v) addInArray(rowSelected.roles, value);
									else removeInArray(rowSelected.roles, value);
								}}
							/>
						{/if}
					{/snippet}
				</TableAutoCard>

				<div class="layout-x max-sm:flex-wrap w-full">
					{@render roleCard({
						roles: rowSelected.roles,
						role: tpSelected == ' ' ? 'TEST_PLATFORM' : tpSelected
					})}
					<PropertyType
						type="segment"
						name="roles"
						item={tpItems}
						bind:value={() => tpSelected,
						(v) => {
							const other = rowSelected.roles.filter((r) => !r.startsWith('TEST_PLATFORM'));
							if (v != ' ') {
								other.push(v);
							}
							rowSelected.roles = other;
						}}
					/>
				</div>

				<div class="w-full layout-x justify-end">
					<button type="submit" class="basic-button" disabled={calling}>
						<span><Ico icon="bytesize:export" size="btn" /></span>
						<span>{row ? 'Edit' : 'Add'}</span>
					</button>
					<button type="button" onclick={close} class="cancel-button">
						<span><Ico icon="material-symbols-light:cancel-outline" size="btn" /></span>
						<span>Cancel</span>
					</button>
				</div>
			</form>
		</Card>
	{/snippet}
</ModalDynamic>

<Card title="Roles">
	{#snippet cornerOption()}
		<ResponsiveButtons
			class="max-w-4xl"
			buttons={[
				{
					label: 'Add User',
					icon: 'grommet-icons:add',
					cls: 'green-button',
					onclick: (event) => openRoleModal({ event, mode: 'addRoles' })
				},
				{
					label: 'Import',
					icon: 'bytesize:import',
					cls: 'basic-button',
					onclick: modalImport?.open
				},
				{
					label: 'Export',
					icon: 'bytesize:export',
					cls: 'basic-button',
					onclick: () => {
						showExportColumn = !showExportColumn;
					}
				},
				{
					label: 'Delete All',
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
	<TableAutoCard
		definition={[
			{ name: 'Actions', custom: true },
			{ name: 'User', key: 'name', class: 'font-medium' },
			{ name: 'Roles', custom: true }
		]}
		data={users}
	>
		{#snippet children({ row, def })}
			{#if def.name == 'Actions'}
				<div class="layout-x-low">
					{#if showExportColumn}
						<PropertyType type="boolean" name="export" bind:this={row.export} />
					{/if}
					<Button
						class="basic-button"
						size={4}
						icon="mdi:edit-outline"
						onclick={(event) => openRoleModal({ event, row })}
					/>
					<Button
						class="delete-button"
						size={4}
						icon="mingcute:delete-line"
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
					/>
				</div>
			{:else}
				<div class="layout-x-low flex-wrap text-xs">
					{#each row.roles as role}
						{@render roleCard({ roles: row.roles, role })}
					{/each}
				</div>
			{/if}
		{/snippet}
	</TableAutoCard>
</Card>
