<script>
	import Tables from '$lib/admin-console/admin-components/Tables.svelte';
	import Card from '$lib/admin-console/admin-components/Card.svelte';
	import Icon from '@iconify/svelte';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { call } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import ModalAddRoles from '$lib/admin-console/modals/ModalAddRoles.svelte';
	import { writable } from 'svelte/store';

	const rolesModalStore = getModalStore();

	let usersStore = writable([]);

	onMount(() => {
		usersList();
	});

	async function usersList() {
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
			usersList();
			if (res.success) {
				usersStore.update((users) => {
					return users.filter((user) => user['@_name'] !== userName);
				});
			}
		} catch (error) {
			console.error('Error deleting user role:', error);
		}
	}

	function openAddUserModal() {
		rolesModalStore.trigger({
			type: 'component',
			component: { ref: ModalAddRoles },
			meta: { mode: 'add' }
		});
	}

	function openImportUserModal() {
		rolesModalStore.trigger({
			type: 'component',
			component: { ref: ModalAddRoles },
			meta: { mode: 'import' }
		});
	}

	function openExportUserModal() {
		rolesModalStore.trigger({
			type: 'component',
			component: { ref: ModalAddRoles },
			meta: { mode: 'export' }
		});
	}

	function openDeleteAllModal() {
		rolesModalStore.trigger({
			type: 'component',
			component: { ref: ModalAddRoles },
			meta: { mode: 'delete all' }
		});
	}
</script>

<Card title="Roles">
	<div class="flex gap-5 mb-10">
		<button class="btn bg-buttons text-white" on:click={openAddUserModal}>
			<Icon icon="material-symbols-light:add" class="w-7 h-7 mr-3" />
			Add user</button
		>
		<button class="btn bg-buttons text-white" on:click={openImportUserModal}>
			<Icon icon="material-symbols-light:key-outline" class="w-7 h-7 mr-3" />
			Import Users
		</button>

		<button class="btn bg-buttons text-white" on:click={openExportUserModal}
			><Icon icon="solar:import-line-duotone" class="w-7 h-7 mr-3" />export users</button
		>
		<button class="btn bg-buttons text-white" on:click={openDeleteAllModal}
			><Icon icon="solar:export-line-duotone" class="w-7 h-7 mr-3" />Delete all</button
		>
	</div>
	<Tables headers={['Name', 'Value', 'Edit', 'Delete']}>
		{#each $usersStore as users}
			<tr>
				<td>{users['@_name']}</td>
				<td>{users.role}</td>
				<td class="align">
					<button class="btn p-1 px-2 shadow-md">
						<Icon icon="bitcoin-icons:edit-outline" class="w-7 h-7" />
					</button>
				</td>
				<td>
					<button class="btn p-1 px-2 shadow-md" on:click={() => deleteUsersRoles(users['@_name'])}
						><Icon icon="material-symbols-light:delete-outline" class="w-7 h-7" />
					</button>
				</td>
			</tr>
		{/each}
	</Tables>
</Card>
