import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('$lib/admin/AdminDocumentation.svelte', () => ({
	getAdminPageDocHref: () => '/docs/'
}));
vi.mock('$lib/common/Authentication.svelte', () => ({
	default: {
		canAccessAdmin: true,
		canAccessAdminRoute: () => true
	}
}));
vi.mock('$lib/common/Status.svelte', () => ({ default: { cloud: false } }));
vi.mock('$lib/utils/service', () => ({ getUrl: (path) => `/convertigo/${path}` }));

describe('Admin page rail', () => {
	beforeEach(() => {
		vi.resetModules();
	});

	it('links the admin navigation to Studio Web', async () => {
		const PagesRail = (await import('./PagesRail.svelte.js')).default;
		const studio = PagesRail.parts[0].find((part) => part.title === 'Studio');

		expect(studio).toEqual({
			title: 'Studio',
			icon: 'mdi:wrench',
			id: '/studio/[[qname]]',
			page: '/studio/'
		});
	});
});
