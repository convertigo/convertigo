import { redirect } from '@sveltejs/kit';

export function entries() {
	return [{ project: '_' }];
}

export function load() {
	redirect(302, 'backend/');
}
