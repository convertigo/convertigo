import ServiceHelper from '$lib/common/ServiceHelper.svelte';
import { call, checkArray } from '$lib/utils/service';

const defValues = {
	users: new Array(5).fill({
		name: null,
		roles: []
	}),
	roles: new Array(10).fill({
		name: null
	})
};

let waiting = $state(false);

async function doCall(action, param) {
	waiting = true;
	try {
		param?.preventDefault?.();
		const res = await call(`roles.${action}`, param?.target ? new FormData(param?.target) : param);
		if (!res.isError) {
			values.refresh();
		}
		return res;
	} finally {
		waiting = false;
	}
}

let values = {
	get waiting() {
		return waiting;
	},

	async addUser(event, row) {
		const res = await doCall(row ? 'Edit' : 'Add', event);
		return !res.isError;
	},

	async deleteRoles(username) {
		return await doCall('Delete', { username });
	},

	async deleteAllRoles() {
		return await doCall('DeleteAll');
	},

	async importRoles(event) {
		const res = await doCall('Import', event);
		return !res.isError;
	},

	async exportRoles() {
		const res = await doCall('Export', {
			users: JSON.stringify(
				values.users.filter((user) => user.export).map((user) => ({ name: user.name }))
			).replace(/(^\[)|(\]$)/g, '')
		});
		return !res.isError;
	},

	formatRoleName(roleName) {
		return roleName
			.toLowerCase()
			.replace(/_/g, ' ')
			.replace(/(?:^|\s)\S/g, (match) => match.toUpperCase());
	}
};

export default ServiceHelper({
	defValues,
	values,
	service: 'roles.List',
	arrays: ['admin.users.user', 'admin.roles.role'],
	mapping: {
		users: 'admin.users.user',
		roles: 'admin.roles.role'
	},
	beforeUpdate: (res) => {
		for (const user of res.users) {
			user.roles = checkArray(user.role).map(({ name }) => name);
			delete user.role;
			user.export = false;
		}
		res.roles = res.roles.map(({ name }) => name).filter((name) => !name.startsWith('TRACE_'));
		return res;
	}
});
