import Instances from '#lib/admin/Instances.svelte.js';
import { call } from '#lib/utils/service.js';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('#lib/utils/service.js', () => ({
	call: vi.fn(),
	abortPendingCalls: vi.fn(),
	checkArray: (value) => (value == null ? [] : Array.isArray(value) ? value : [value])
}));
const { default: Authentication } = await import('./Authentication.svelte.js');
const authenticated = {
	admin: { authenticated: true, user: 'synthetic-admin', roles: { role: ['WEB_ADMIN'] } }
};

async function login() {
	vi.mocked(call).mockResolvedValueOnce(authenticated);
	await Authentication.authenticate({ authType: 'login' });
}

describe('Authentication and instance recovery', () => {
	beforeEach(async () => {
		vi.mocked(call).mockReset();
		vi.mocked(call).mockResolvedValueOnce({ admin: { authenticated: false } });
		await Authentication.logout();
		vi.mocked(call).mockReset();
	});

	it('forgets routing and cached discovery when the session expires', async () => {
		await login();
		Instances.current = 'removed-pod';
		vi.mocked(call).mockResolvedValueOnce({ admin: { authenticated: false } });
		await Authentication.checkAuthentication();
		expect(Authentication.authenticated).toBe(false);
		expect(Instances.current).toBe('');
		expect(Instances.storeMode).toBe('');
	});

	it('forgets the selection before login and before the logout request', async () => {
		Instances.current = 'removed-pod';
		vi.mocked(call).mockImplementationOnce(async () => {
			expect(Instances.current).toBe('');
			return authenticated;
		});
		await Authentication.authenticate({ authType: 'login' });
		Instances.current = 'removed-pod';
		vi.mocked(call).mockImplementationOnce(async () => {
			expect(Instances.current).toBe('');
			return { admin: { authenticated: false } };
		});
		await Authentication.logout();
		expect(Authentication.authenticated).toBe(false);
	});

	it('retries discovery locally once when a selected target is unreachable before login', async () => {
		Instances.current = 'removed-pod';
		vi.mocked(call).mockResolvedValueOnce({ instanceUnreachable: true });
		vi.mocked(call).mockImplementationOnce(async () => {
			expect(Instances.current).toBe('');
			return { admin: { authenticated: false } };
		});
		await Authentication.checkAuthentication();
		expect(call).toHaveBeenCalledTimes(2);
		expect(Authentication.authenticated).toBe(false);
		expect(Instances.current).toBe('');
	});

	it('preserves an authenticated session and its explicit recovery choice on forwarding failure', async () => {
		await login();
		Instances.current = 'removed-pod';
		Instances.update({
			headers: new Headers({
				'x-c8o-instance-unreachable': 'removed-pod',
				'x-c8o-instance-local': 'live-pod'
			})
		});
		vi.mocked(call).mockResolvedValueOnce({ instanceUnreachable: true });
		await Authentication.checkAuthentication();
		expect(Authentication.authenticated).toBe(true);
		expect(Instances.current).toBe('removed-pod');
		expect(Instances.unreachable?.instance).toBe('removed-pod');
	});

	it('does not let an obsolete check reset a later successful login', async () => {
		/** @type {(value: any) => void} */
		let finishCheck = () => {
			throw new Error('Pending response was not initialized');
		};
		vi.mocked(call).mockImplementationOnce(
			() =>
				new Promise((resolve) => {
					finishCheck = resolve;
				})
		);
		const checking = Authentication.checkAuthentication();
		await login();
		Instances.current = 'live-pod';
		finishCheck({ admin: { authenticated: false } });
		await checking;
		expect(Authentication.authenticated).toBe(true);
		expect(Instances.current).toBe('live-pod');
	});
});

const authenticationFixture = vi.hoisted(() => ({
	roles: ['PROJECTS_VIEW'],
	webStudio: 'true',
	serverBuild: 'false'
}));

describe('Authentication routes', () => {
	beforeEach(() => {
		authenticationFixture.roles = ['PROJECTS_VIEW'];
		authenticationFixture.webStudio = 'true';
		authenticationFixture.serverBuild = 'false';
		vi.mocked(call).mockReset();
		vi.mocked(call).mockImplementation(async () => ({
			admin: {
				authenticated: true,
				roles: { role: authenticationFixture.roles.map((name) => ({ name })) },
				webStudio: authenticationFixture.webStudio,
				serverBuild: authenticationFixture.serverBuild
			}
		}));
	});
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

	it('opens the embedded JavaScript debugger to administrators only', async () => {
		const Authentication = (await import('./Authentication.svelte.js')).default;
		const route = '/(embedded)/admin/eclipse/javascript-debugger';

		authenticationFixture.roles = ['WEB_ADMIN'];
		await Authentication.checkAuthentication();
		expect(Authentication.canAccessAdminRoute(route)).toBe(true);

		authenticationFixture.roles = ['LOGS_VIEW', 'PROJECTS_VIEW'];
		await Authentication.checkAuthentication();
		expect(Authentication.canAccessAdminRoute(route)).toBe(false);
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
