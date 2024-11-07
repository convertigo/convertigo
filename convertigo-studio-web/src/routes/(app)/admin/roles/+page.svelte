<script>
	import Card from '$lib/admin/components/Card.svelte';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { call } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import { usersList, usersStore } from '$lib/admin/stores/rolesStore';
	import Ico from '$lib/utils/Ico.svelte';
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';

	const modalStore = getModalStore();

	let selectRow = $state(false);
	let selectedUsers = new Set();
	let allSelected = false;
	onMount(() => {
		usersList();
	});

	function DisplaySelectRow() {
		selectRow = !selectRow;
	}

	function toggleUserSelection(user) {
		if (selectedUsers.has(user)) {
			selectedUsers.delete(user);
		} else {
			selectedUsers.add(user);
		}
	}

	function selectAllRoles() {
		if (allSelected) {
			selectedUsers.clear();
			document
				.querySelectorAll('input[type="checkbox"]')
				//@ts-ignore
				.forEach((checkbox) => (checkbox.checked = false));
		} else {
			$usersStore.forEach((user) => selectedUsers.add(user));
			document
				.querySelectorAll('input[type="checkbox"]')
				//@ts-ignore
				.forEach((checkbox) => (checkbox.checked = true));
		}
		allSelected = !allSelected; // Toggle the selection state
	}

	async function deleteUsersRoles(username) {
		const formData = new FormData();
		formData.append('username', username);
		try {
			//@ts-ignore
			const res = await call('roles.Delete', formData);
			if (res?.admin?.response?.['@_state'] == 'success') {
				usersList();
				modalStore.close();
			}
		} catch (error) {
			console.error('Error deleting user role:', error);
		}
	}

	async function deleteAllRoles() {
		try {
			const res = await call('roles.DeleteAll');
			if (res?.admin?.response?.['@_state'] == 'success') {
				usersList();
				modalStore.close();
			}
		} catch (err) {
			console.error(err);
		}
	}

	function openModals(mode, row) {
		modalStore.trigger({
			type: 'component',
			component: 'modalRoles',
			meta: { mode, row },
			title: row ? `Edit roles` : `New roles`
		});
	}

	function openDeleteAllModal() {
		modalStore.trigger({
			type: 'component',
			component: 'modalWarning',
			title: 'You are going to delete All Roles',
			body: 'Are you sure you want to ?',
			meta: { mode: 'Confirm' },
			response: async (confirmed) => {
				if (confirmed) {
					deleteAllRoles();
				}
			}
		});
	}

	function openDeleteModal(row) {
		modalStore.trigger({
			type: 'component',
			component: 'modalWarning',
			title: 'Please Confirm',
			body: 'Are you sure you want to delete the role ?',
			meta: { mode: 'Confirm' },
			response: (confirmed) => {
				if (confirmed) {
					deleteUsersRoles(row);
				}
			}
		});
	}

	function exportUserFile() {
		const usersArray = Array.from(selectedUsers);
		const jsonString = `data:text/json;chatset=utf-8,${encodeURIComponent(
			JSON.stringify(usersArray)
		)}`;
		const link = document.createElement('a');
		link.href = jsonString;
		link.download = 'users_export.json';

		link.click();
	}

	const userActions = {
		add: {
			name: 'Add User',
			icon: 'grommet-icons:add'
		},
		import: {
			name: 'Import Users',
			icon: 'bytesize:import'
		}
		// export: {
		// 	name: 'Export Users',
		// 	icon: 'bytesize:export'
		// }
	};

	const sortRoles = (a, b) =>
		['_VIEW', '_CONFIG', ''].reduce((res, suffix) => {
			return res || (a.endsWith(suffix) ? 1 : 0) - (b.endsWith(suffix) ? 1 : 0);
		}, 0) || a.localeCompare(b);
</script>

<Card title="Roles">
	{#snippet cornerOption()}
		<button class="delete-button" onclick={openDeleteAllModal}>
			<Ico icon="mingcute:delete-line" />
			<p>Delete All Roles</p>
		</button>
	{/snippet}
	<ButtonsContainer class="mb-10">
		{#each Object.entries(userActions) as [type, { name, icon }]}
			<button class="basic-button" onclick={() => openModals(type)}>
				<p>{name}</p>
				<Ico {icon} />
			</button>
		{/each}

		<button class={selectRow ? 'delete-button' : 'basic-button'} onclick={DisplaySelectRow}>
			<p>{selectRow ? 'Cancel Export' : 'Export Users'}</p>
			<Ico icon={selectRow ? 'material-symbols-light:cancel-outline' : 'bytesize:export'} />
		</button>

		{#if selectRow}
			<button class="yellow-button" onclick={exportUserFile}>
				<p>Validate export</p>
				<Ico icon="bytesize:export" />
			</button>
			<button class="green-button" onclick={selectAllRoles}>
				<p>Select all users</p>
				<Ico icon="bytesize:export" />
			</button>
		{/if}
	</ButtonsContainer>

	{#if $usersStore.length >= 0}
		<TableAutoCard
			definition={[
				{ name: 'Export', custom: true },
				{ name: 'Name', key: 'name' },
				{ name: 'Role', key: 'role', custom: true },
				{ name: 'Edit', custom: true },
				{ name: 'Delete', custom: true }
			].filter((elt) => selectRow || elt.name != 'Export')}
			data={$usersStore}
		>
			{#snippet children(row, def)}
				{#if def.name === 'Role'}
					{#each row.role.split(', ').sort(sortRoles) as role}
						<span
							class={role.endsWith('_VIEW')
								? 'role-view'
								: role.endsWith('_CONFIG')
									? 'role-config'
									: 'role-other'}
						>
							{role.replace(/_/g, '-').charAt(0).toUpperCase() +
								role.replace(/_/g, ' ').slice(1).toLowerCase()}
						</span>
					{/each}
				{:else if def.name === 'Edit'}
					<button class="yellow-button" onclick={() => openModals('add', row)}>
						<Ico icon="mdi:edit-outline" />
					</button>
				{:else if def.name === 'Delete'}
					<button class="delete-button" onclick={() => openDeleteModal(row.name)}>
						<Ico icon="mingcute:delete-line" />
					</button>
				{:else if def.name === 'Export'}
					<!-- <SlideToggle
					active="min-w-12 bg-success-400 dark:bg-success-700"
					background="min-w-12 bg-error-400 dark:bg-error-700"
					name="slide"
					bind:checked={value}
					size="sm"
				/> -->
					<input type="checkbox" onchange={() => toggleUserSelection(row)} />
				{/if}
			{/snippet}
		</TableAutoCard>
	{:else}
		<div class="table-container">
			<table class="rounded table">
				<thead class="rounded">
					<tr>
						{#each Array(5) as _}
							<th class="header dark:bg-surface-800">
								<div class="my-2 h-8 placeholder animate-pulse"></div>
							</th>
						{/each}
					</tr>
				</thead>
				<tbody>
					{#each Array(5) as _}
						<tr>
							{#each Array(5) as _}
								<td>
									<div class="my-2 h-8 placeholder animate-pulse"></div>
								</td>
							{/each}
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{/if}
</Card>

<style lang="postcss">
	.role-view {
		@apply mr-1 px-2 dark:bg-secondary-500 bg-secondary-400 font-light gap-2 rounded dark:bg-opacity-80;
	}
	.role-config {
		@apply mr-1 px-2 dark:bg-primary-500 bg-primary-400 gap-2 font-light rounded dark:bg-opacity-80;
	}
	.role-other {
		@apply mr-1 px-2 dark:bg-tertiary-600 bg-tertiary-400 gap-2 font-light rounded dark:bg-opacity-80;
	}
</style>
