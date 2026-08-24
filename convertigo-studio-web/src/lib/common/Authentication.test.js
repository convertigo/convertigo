import { describe, expect, it, vi } from 'vitest';

const authenticationFixture = vi.hoisted(() => ({ roles: ['PROJECTS_VIEW'] }));

vi.mock('$lib/utils/service', () => ({
	abortPendingCalls: vi.fn(),
	checkArray: (value) => (value == null ? [] : Array.isArray(value) ? value : [value]),
	call: vi.fn(async () => ({
		admin: {
			authenticated: true,
			roles: { role: authenticationFixture.roles.map((name) => ({ name })) }
		}
	}))
}));
vi.mock('./Time.svelte', () => ({ default: {} }));

describe('Authentication routes', () => {
	it('grants Studio Web to project viewers and rejects unrelated roles', async () => {
		const Authentication = (await import('./Authentication.svelte.js')).default;

		authenticationFixture.roles = ['PROJECTS_VIEW'];
		await Authentication.checkAuthentication();
		expect(Authentication.canAccessAdminRoute('/studio/[[qname]]')).toBe(true);

		authenticationFixture.roles = ['LOGS_VIEW'];
		await Authentication.checkAuthentication();
		expect(Authentication.canAccessAdminRoute('/studio/[[qname]]')).toBe(false);
	});
});
