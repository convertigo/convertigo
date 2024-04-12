import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';

export let usersStore = writable([]);
export let rolesStore = writable([
	{ name: 'View Roles', end: '_VIEW', toggle: false, roles: /** @type {any[]} */ ([]) },
	{ name: 'Config Roles', end: '_CONFIG', toggle: false, roles: [] },
	{ name: 'Other Roles', end: '', toggle: false, roles: [] }
]);

export async function usersList() {
	const res = await call('roles.List');

	if (res?.admin?.users?.user) {
		if (!Array.isArray(res.admin.users.user)) {
			res.admin.users.user = [res?.admin?.users?.user];
		}

		let usersWithRoles = res.admin.users.user.map((user) => {
			return {
				name: user['@_name'],
				role: Array.isArray(user.role)
					? user.role.map((role) => role['@_name']).join(', ')
					: 'No roles'
			};
		});

		usersStore.set(usersWithRoles);
	}

	rolesStore.update((store) => {
		if (store[0].roles.length == 0) {
			for (let role of res?.admin?.roles?.role) {
				for (let part of store) {
					if (role['@_name'].endsWith(part.end)) {
						part.roles.push({
							value: role['@_name'],
							description: role['@_description'],
							checked: false
						});
						break;
					}
				}
			}
		}
		return store;
	});
}
