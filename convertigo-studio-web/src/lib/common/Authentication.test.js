import Instances from '$lib/admin/Instances.svelte';
import { call } from '$lib/utils/service';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('$lib/utils/service', () => ({
	call: vi.fn(),
	abortPendingCalls: vi.fn(),
	checkArray: (value) => (value == null ? [] : Array.isArray(value) ? value : [value])
}));
const { default: Authentication } = await import('./Authentication.svelte');
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
		let finishCheck;
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
