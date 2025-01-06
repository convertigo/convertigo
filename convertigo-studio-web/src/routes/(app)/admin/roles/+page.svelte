<script>
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import PagesRail from '$lib/admin/PagesRail.svelte';
	import Roles from '$lib/admin/Roles.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { addInArray, removeInArray } from '$lib/utils/service';
	import { FileUpload } from '@skeletonlabs/skeleton-svelte';
	import { getContext } from 'svelte';
	import { slide } from 'svelte/transition';

	let {
		users,
		roles,
		deleteRoles,
		deleteAllRoles,
		addUser,
		importRoles,
		exportRoles,
		formatRoleName,
		waiting,
		init
	} = $derived(Roles);

	let modalYesNo = getContext('modalYesNo');
	let modalImport = $state();
	let actionImport = $state('on');

	let exporting = $state(false);

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

<ModalDynamic bind:this={modal}>
	{#snippet children({ close, params: { row } })}
		<Card title={`${row ? 'Edit' : 'Add'} User`}>
			<form
				onsubmit={async (event) => {
					if (await addUser(event, row)) {
						close();
					}
				}}
				class="layout-y-stretch"
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
					data={['All', ...roles.filter((role) => role.endsWith('VIEW'))]}
				>
					{#snippet children({ row: role, def })}
						{#if def.name == 'Role'}
							<div class="layout-x">
								{#if role == 'All'}
									<strong>For all roles</strong>
								{:else}
									{@render roleCard({ roles: rowSelected.roles, role })}
								{/if}
							</div>
						{:else if role == 'All'}
							{@const subRoles = roles.filter((r) =>
								r.endsWith(def.name == 'View' ? '_VIEW' : '_CONFIG')
							)}
							<PropertyType
								type="check"
								size="sm"
								bind:checked={() => subRoles.every((r) => rowSelected.roles.includes(r)),
								(v) => {
									for (const value of subRoles) {
										if (v) addInArray(rowSelected.roles, value);
										else removeInArray(rowSelected.roles, value);
									}
								}}
							/>
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

				<div class="layout-x max-sm:flex-wrap">
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

				<fieldset class="layout-x justify-end" disabled={waiting}>
					<Button
						type="submit"
						class="!w-fit  basic-button"
						icon={row ? 'mdi:edit-outline' : 'grommet-icons:add'}
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
				await importRoles(event);
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

<Card title="Roles">
	{#snippet cornerOption()}
		<ResponsiveButtons
			class="max-w-4xl"
			buttons={[
				{
					label: 'Add User',
					icon: 'grommet-icons:add',
					cls: 'green-button',
					hidden: exporting,
					onclick: (event) => openRoleModal({ event, mode: 'addRoles' })
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
					hidden: !exporting || users.every((user) => user.export),
					onclick: () => users.forEach((user) => (user.export = true))
				},
				{
					label: 'Unselect All',
					icon: 'mdi:check-all',
					cls: 'yellow-button',
					hidden: !exporting || users.every((user) => !user.export),
					onclick: () => users.forEach((user) => (user.export = false))
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
					label: `Export [${users.filter((user) => user.export).length}]`,
					icon: 'bytesize:export',
					cls: 'green-button',
					hidden: !exporting,
					disabled: users.every((user) => !user.export),
					onclick: exportRoles
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
								title: 'Delete all users',
								message: `Are you sure you want to delete all users?`
							})
						) {
							deleteAllRoles();
						}
					}
				}
			]}
			disabled={!init}
			size="4"
		/>
	{/snippet}
	<TableAutoCard
		definition={[
			{ name: 'Actions', custom: true },
			{ name: 'User', key: 'name', class: 'font-medium' },
			{ name: 'Roles', custom: true, class: 'w-full' }
		]}
		data={users}
	>
		{#snippet children({ row, def })}
			{#if def.name == 'Actions'}
				<fieldset class="layout-x-low" disabled={!init}>
					{#if exporting}
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
							onclick={(event) => openRoleModal({ event, row })}
						/>
						<Button
							class="delete-button"
							size={4}
							icon="mingcute:delete-line"
							onclick={async () => {
								if (
									await modalYesNo.open({
										title: 'Delete user',
										message: `Are you sure you want to delete ${row.name}?`
									})
								) {
									deleteRoles(row.name);
								}
							}}
						/>
					{/if}
				</fieldset>
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
