import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { call, setModalAlert } from './service.js';

const fixtures = vi.hoisted(() => ({
	modal: { open: vi.fn() },
	checkAuthentication: vi.fn(),
	loggingOut: false
}));
vi.mock('$app/env', () => ({ browser: true, building: false, dev: false }));
vi.mock('$app/navigation', () => ({ goto: vi.fn() }));
vi.mock('#lib/utils/route.js', () => ({ resolve: (path) => path }));
vi.mock('@skeletonlabs/skeleton-svelte', () => ({
	createToaster: () => ({ error: vi.fn(), success: vi.fn() })
}));
vi.mock('#lib/admin/Instances.svelte.js', () => ({
	default: { apply: () => 0, update: () => false }
}));
vi.mock('#lib/common/Authentication.svelte.js', () => ({
	default: {
		get loggingOut() {
			return fixtures.loggingOut;
		},
		canAccessDashboard: true,
		checkAuthentication: fixtures.checkAuthentication
	}
}));
vi.mock('#lib/common/ServerConnection.svelte.js', () => ({
	default: { markReachable: () => false },
	isServerUnavailableStatus: () => false
}));

const expired = {
	error: {
		message: 'getAttribute: Session already invalidated',
		exception: 'java.lang.IllegalStateException'
	}
};
function respond(error = expired) {
	vi.stubGlobal(
		'fetch',
		vi.fn(
			async () =>
				new Response(JSON.stringify(error), {
					status: 500,
					headers: { 'content-type': 'application/json' }
				})
		)
	);
}
beforeEach(() => {
	vi.clearAllMocks();
	fixtures.loggingOut = false;
	vi.stubGlobal('window', {
		location: { search: '?studioMode=true&theme=dark' },
		java: { receiveFromJS: vi.fn() }
	});
	vi.stubGlobal('localStorage', { getItem: () => null, setItem: vi.fn() });
	setModalAlert(fixtures.modal);
	respond();
});
afterEach(() => vi.unstubAllGlobals());

describe('embedded Studio session recovery through the service transport', () => {
	it.each(['logs.Get', 'engine.CheckAuthentication', 'engine.Authenticate'])(
		'asks Eclipse to reconnect after a stale session in %s without replaying the request',
		async (service) => {
			const response = await call(service);
			expect(response.isError).toBe(true);
			expect(window.__c8oSessionExpired).toBe(true);
			expect(fetch).toHaveBeenCalledTimes(1);
			expect(fixtures.modal.open).not.toHaveBeenCalled();
			expect(fixtures.checkAuthentication).not.toHaveBeenCalled();
		}
	);
	it('recognizes the XML error actually returned by servlet services', async () => {
		vi.stubGlobal(
			'fetch',
			vi.fn(
				async () =>
					new Response(
						'<error><message>getAttribute: Session already invalidated</message><exception>java.lang.IllegalStateException</exception></error>',
						{ status: 500, headers: { 'content-type': 'text/xml' } }
					)
			)
		);
		await call('logs.Get');
		expect(window.__c8oSessionExpired).toBe(true);
		expect(fixtures.modal.open).not.toHaveBeenCalled();
	});
	it('also reconnects on an authentication failure', async () => {
		respond({ error: { message: 'Authentication failure: no role defined' } });
		await call('logs.Get');
		expect(window.__c8oSessionExpired).toBe(true);
		expect(fixtures.checkAuthentication).not.toHaveBeenCalled();
	});
	it('accepts the Redis session invalidation wording', async () => {
		respond({ error: { ...expired.error, message: 'Session has been invalidated' } });
		await call('logs.Get');
		expect(window.__c8oSessionExpired).toBe(true);
	});
	it('continues showing unrelated IllegalStateExceptions', async () => {
		respond({ error: { ...expired.error, message: 'Project is already running' } });
		await call('logs.Get');
		expect(window.__c8oSessionExpired).toBeUndefined();
		expect(fixtures.modal.open).toHaveBeenCalled();
	});
	it('does not hide the error in an ordinary web browser', async () => {
		delete window.java;
		await call('logs.Get');
		expect(window.__c8oSessionExpired).toBeUndefined();
		expect(fixtures.modal.open).toHaveBeenCalled();
	});
	it('does not reconnect during an explicit logout', async () => {
		fixtures.loggingOut = true;
		await call('logs.Get');
		expect(window.__c8oSessionExpired).toBeUndefined();
	});
});
