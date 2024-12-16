import { call, checkArray } from '$lib/utils/service';
import ServiceHelper from '$lib/common/ServiceHelper.svelte';

const defValues = {
	users: new Array(1).fill({
		name: 'Loading...',
		roles: []
	}),
	roles: new Array(1).fill({
		name: 'Loading...',
		description: 'Loading...'
	})
};

let calling = $state(false);

/**
 * @param {string} service - The service name to call.
 * @param {Object} eventOrPayload - The payload data for the service call.
 */
async function doCall(service, eventOrPayload) {
	// const formData =
	// 	eventOrPayload?.target && eventOrPayload.preventDefault
	// 		? new FormData(eventOrPayload.target) // Handle event with form submission
	// 		: eventOrPayload instanceof FormData
	// 		? eventOrPayload // Handle pre-created FormData
	// 		: (() => {
	// 				// Convert plain object to FormData
	// 				const fd = new FormData();
	// 				for (const key in eventOrPayload) {
	// 					if (eventOrPayload[key] instanceof Array) {
	// 						eventOrPayload[key].forEach((value) => fd.append(`${key}[]`, value));
	// 					} else {
	// 						fd.append(key, eventOrPayload[key]);
	// 					}
	// 				}
	// 				return fd;
	// 		  })();

	calling = true;
	try {
		const res = await call(service, eventOrPayload);
		await values.refresh();
		return res;
	} finally {
		calling = false;
	}
}

let values = {
	get calling() {
		return calling;
	},

	async addRole(roleName, roleDescription) {
		const payload = {
			'@_xml': true,
			admin: {
				'@_service': 'roles.Add',
				role: {
					'@_name': roleName,
					description: roleDescription
				}
			}
		};
		return await doCall('roles.Add', payload);
	},

	async deleteRoles(username) {
		const formData = new FormData();
		formData.append('username', username);
		return await doCall('roles.Delete', formData);
	},

	async importRoles(file) {
		try {
			const formData = new FormData();
			formData.append('action-import', 'on');
			console.log('FormData for roles import:', Array.from(formData.entries()));

			return await doCall('roles.List', formData);
		} catch (error) {
			console.error('Error during role import:', error);
			throw new Error('Failed to import roles. Please ensure the file format is correct.');
		}
	},
	async exportRoles() {
		console.log('Exporting roles...');
		return await doCall('roles.Export', {});
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
		res.users = checkArray(res.users).map((user) => ({
			name: user.name || 'Unknown User',
			roles: checkArray(user.role).map((role) => ({
				name: role.name || 'Unknown Role'
			}))
		}));

		res.roles = checkArray(res.roles).map((role) => ({
			name: role.name || 'Unknown Role',
			description: role.description || 'No Description'
		}));
		console.log('Processed roles:', res.roles);

		return res;
	}
});
