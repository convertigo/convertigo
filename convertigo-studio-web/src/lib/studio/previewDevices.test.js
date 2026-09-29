import { describe, expect, it, vi } from 'vitest';
import { nativeOsOf, withDeviceOs } from './previewDevices.svelte.js';

describe('previewDevices', () => {
	it('gives the Ionic mode of iOS to the applications of the engine only', () => {
		vi.stubGlobal('location', new URL('http://studio.test/studio/'));
		try {
			expect(
				withDeviceOs('/convertigo/projects/App/DisplayObjects/mobile/index.html#/home', 'ios')
			).toBe(
				'http://studio.test/convertigo/projects/App/DisplayObjects/mobile/?ionic%3Amode=ios#/home'
			);
			expect(
				withDeviceOs('/convertigo/projects/App/DisplayObjects/mobile/index.html', 'android')
			).toBe('/convertigo/projects/App/DisplayObjects/mobile/index.html');
			expect(withDeviceOs('https://example.com/projects/App/', 'ios')).toBe(
				'https://example.com/projects/App/'
			);
		} finally {
			vi.unstubAllGlobals();
		}
	});

	it('takes the OS of the Apple devices', () => {
		expect(nativeOsOf({ id: 'iPhone-17-Pro' })).toBe('ios');
		expect(nativeOsOf({ id: 'iPad-Air' })).toBe('ios');
		expect(nativeOsOf({ id: 'Google-Pixel-9' })).toBe('android');
		expect(nativeOsOf({ id: 'custom-Tablet' })).toBe('android');
	});
});
