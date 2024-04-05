<script>
	import Card from '$lib/admin/components/Card.svelte';
	import Icon from '@iconify/svelte';
	import { getModalStore, getToastStore } from '@skeletonlabs/skeleton';
	import { call } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import { writable } from 'svelte/store';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';

	const rolesModalStore = getModalStore();

	let usersStore = writable([]);

	onMount(() => {
		usersList();
	});

	export async function usersList() {
		const res = await call('roles.List');
		console.log('roles List', res);

		if (res?.admin?.users?.user) {
			if (!Array.isArray(res.admin.users.user)) {
				res.admin.users.user = [res?.admin?.users?.user];
			}

			let usersWithRoles = res.admin.users.user.map((user) => {
				return {
					...user,
					role: Array.isArray(user.role)
						? user.role.map((role) => role['@_name']).join(', ')
						: 'No roles'
				};
			});

			usersStore.set(usersWithRoles);
		}
	}

	async function deleteUsersRoles(userName) {
		const formData = new FormData();
		formData.append('username', userName);
		try {
			//@ts-ignore
			const res = await call('roles.Delete', formData);
			console.log('service delete roles', res);
		} catch (error) {
			console.error('Error deleting user role:', error);
		}
	}

	async function deleteAllRoles() {
		const response = await call('roles.DeleteAll');

		console.log('delete all', response);

		if (response.admin.response['@_state'] === 'success') {
			rolesModalStore.trigger({
				type: 'component',
				component: 'modalWarning',
				title: 'Success',
				body: 'All Roles deleted with success',
				meta: { mode: 'Success' },
				response: (confirmed) => {
					if (confirmed) {
						usersList();
					}
				}
			});
		} else if (response.admin.response['@_state'] === 'error') {
			rolesModalStore.trigger({
				type: 'component',
				component: 'modalWarning',
				title: 'Error',
				body: 'An error occurred',
				meta: { mode: 'Error' },
				response: (confirmed) => {
					if (confirmed) {
					}
				}
			});
		}
	}

	function openAddUserModal() {
		rolesModalStore.trigger({
			type: 'component',
			component: 'modalRoles',
			meta: { mode: 'add' },
			response: (confirmed) => {
				if (confirmed) {
				}
			}
		});
	}

	function openImportUserModal() {
		rolesModalStore.trigger({
			type: 'component',
			component: 'modalRoles',
			meta: { mode: 'import' }
		});
	}

	function openExportUserModal() {
		rolesModalStore.trigger({
			type: 'component',
			component: 'modalRoles',
			meta: { mode: 'export' }
		});
	}

	function openDeleteAllModal() {
		rolesModalStore.trigger({
			type: 'component',
			component: 'modalWarning',
			title: 'You are going to delete All Roles',
			body: 'Are you sure you want to ?',
			meta: { mode: 'Confirm' },
			response: (confirmed) => {
				if (confirmed) {
					deleteAllRoles();
				}
			}
		});
	}

	function openDeleteModal(userName) {
		rolesModalStore.trigger({
			type: 'component',
			component: 'modalWarning',
			title: 'Please Confirm',
			body: 'Are you sure you want to delete the role ?',
			meta: { mode: 'Confirm' },
			response: (confirmed) => {
				if (confirmed) {
					deleteUsersRoles(userName);
				}
			}
		});
	}
</script>

<Card title="Roles">
	<div slot="cornerOption">
		<button class="w-full bg-error-400-500-token" on:click={openDeleteAllModal}>
			<Icon icon="material-symbols-light:delete-outline" class="w-7 h-7 mr-3" />
			Delete All Roles
		</button>
	</div>
	<div class="flex flex-wrap gap-5 mb-10 mt-10">
		<div class="flex-1">
			<button class="w-full bg-primary-400-500-token" on:click={openAddUserModal}>
				<Icon icon="material-symbols-light:add" class="w-7 h-7 mr-3" />
				Add User
			</button>
		</div>
		<div class="flex-1">
			<button class="w-full bg-primary-400-500-token" on:click={openImportUserModal}>
				<Icon icon="solar:import-line-duotone" class="w-7 h-7 mr-3" />
				Import Users
			</button>
		</div>
		<div class="flex-1">
			<button class="w-full bg-primary-400-500-token" on:click={openExportUserModal}>
				<Icon icon="solar:export-line-duotone" class="w-7 h-7 mr-3" />
				Export Users
			</button>
		</div>
	</div>

	{#if $usersStore.length > 0}
		<TableAutoCard
			definition={[
				{ name: 'Name', key: '@_name' },
				{ name: 'Role', key: 'role' },
				{ name: 'Edit', custom: true },
				{ name: 'Delete', custom: true }
			]}
			data={$usersStore}
			let:row
			let:def
		>
			{#if def.name === 'Edit'}
				<button class="p-1 px-2 shadow-md bg-tertiary-400-500-token">
					<Icon icon="bitcoin-icons:edit-outline" class="w-7 h-7" />
				</button>
			{:else if def.name === 'Delete'}
				<button
					class="p-1 px-2 shadow-md bg-error-400-500-token"
					on:click={() => openDeleteModal(row['@_name'])}
				>
					<Icon icon="material-symbols-light:delete-outline" class="w-7 h-7" />
				</button>
			{/if}
		</TableAutoCard>
	{:else}
		<div class="table-container">
			<table class="rounded-token table">
				<thead class="rounded-token">
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
