import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';

export let usersStore = writable([]);

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
