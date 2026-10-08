import { resolve as svelteResolve } from '$app/paths';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ensureTrailingSlash, resolve } from './route.js';

vi.mock('$app/paths', () => ({ resolve: vi.fn((path) => `/convertigo/${path}`) }));

describe('Kit 3 route compatibility', () => {
	beforeEach(() => vi.clearAllMocks());

	it('normalizes absolute pathnames while preserving the deployment base', () => {
		expect(resolve('/admin/logs/view')).toBe('/convertigo/admin/logs/view/');
		expect(svelteResolve).toHaveBeenCalledWith('admin/logs/view', undefined);
	});

	it('keeps route IDs and their parameters intact', () => {
		const params = { project: 'sample' };
		resolve('/(app)/dashboard/[[project]]', params);
		expect(svelteResolve).toHaveBeenCalledWith('/(app)/dashboard/[[project]]', params);
	});

	it('resolves the app root', () => {
		expect(resolve('/')).toBe('/convertigo/');
		expect(svelteResolve).toHaveBeenCalledWith('', undefined);
	});

	it('puts the trailing slash before the query and hash', () => {
		expect(ensureTrailingSlash('/studio?view=logs#selected')).toBe('/studio/?view=logs#selected');
	});

	it('does not rewrite external URLs or standalone popout pages', () => {
		expect(ensureTrailingSlash('https://example.com/studio')).toBe('https://example.com/studio');
		expect(ensureTrailingSlash('/studio-popout.html')).toBe('/studio-popout.html');
	});
});
