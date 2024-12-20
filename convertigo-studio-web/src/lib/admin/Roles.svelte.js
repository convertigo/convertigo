import { call, checkArray } from '$lib/utils/service';
import ServiceHelper from '$lib/common/ServiceHelper.svelte';

const defValues = {
	users: new Array(5).fill({
		name: null,
		roles: []
	}),
	roles: new Array(10).fill({
		name: null
	})
};

let calling = $state(false);

/**
 * @param {string} roleName - The role name to format.
 * @returns {string} - The formatted role name.
 */
function formatRoleName(roleName) {
	return roleName
		.toLowerCase() // Convert to lowercase
		.replace(/_/g, ' ') // Replace underscores with spaces
		.replace(/(?:^|\s)\S/g, (match) => match.toUpperCase()); // Capitalize each word
}

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

	async addUser(event, row) {
		event.preventDefault();
		await call(`roles.${row ? 'Edit' : 'Add'}`, new FormData(event.target));
		values.refresh();
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
		return await doCall('roles.Export', {});
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
			// .map((role) => ({
			// 	name: formatRoleName(role.name || 'Unknown Role')
			// }));
		}
		res.roles = res.roles.map(({ name }) => name);
		return res;
	}
});
