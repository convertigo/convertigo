import { describe, expect, it, vi } from 'vitest';

const authenticationFixture = vi.hoisted(() => ({
	roles: ['PROJECTS_VIEW'],
	webStudio: 'true',
	serverBuild: 'false'
}));

vi.mock('#lib/utils/service.js', () => ({
	abortPendingCalls: vi.fn(),
	checkArray: (value) => (value == null ? [] : Array.isArray(value) ? value : [value]),
	call: vi.fn(async () => ({
		admin: {
			authenticated: true,
			roles: { role: authenticationFixture.roles.map((name) => ({ name })) },
			webStudio: authenticationFixture.webStudio,
			serverBuild: authenticationFixture.serverBuild
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

	it('hides Studio Web when the server does not allow it, even to an administrator', async () => {
		const Authentication = (await import('./Authentication.svelte.js')).default;

		authenticationFixture.roles = ['WEB_ADMIN'];
		authenticationFixture.webStudio = 'false';
		await Authentication.checkAuthentication();
		expect(Authentication.webStudio).toBe(false);
		expect(Authentication.canAccessAdminRoute('/studio/[[qname]]')).toBe(false);
		expect(Authentication.canAccessAdminRoute('/(app)/admin/projects')).toBe(true);

		authenticationFixture.webStudio = 'true';
		await Authentication.checkAuthentication();
		expect(Authentication.canAccessAdminRoute('/studio/[[qname]]')).toBe(true);
	});

	it('tells whether the server allows the builds', async () => {
		const Authentication = (await import('./Authentication.svelte.js')).default;

		authenticationFixture.serverBuild = 'false';
		await Authentication.checkAuthentication();
		expect(Authentication.serverBuild).toBe(false);

		authenticationFixture.serverBuild = 'true';
		await Authentication.checkAuthentication();
		expect(Authentication.serverBuild).toBe(true);
	});
});
