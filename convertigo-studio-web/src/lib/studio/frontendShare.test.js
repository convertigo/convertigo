import { afterEach, describe, expect, it, vi } from 'vitest';
import { isLocalAddress, publicFrontendUrl } from './frontendShare.js';

describe('Address of a frontend shared with a phone', () => {
	afterEach(() => vi.unstubAllGlobals());

	it('keeps the path of the server address on the origin of the Studio', () => {
		vi.stubGlobal('window', {
			location: {
				href: 'https://beta.example.net/convertigo/studio/App/',
				origin: 'https://beta.example.net'
			}
		});
		expect(
			publicFrontendUrl('http://127.0.0.1:28080/convertigo/gw/ticket.signature/shop?x=1#top')
		).toBe('https://beta.example.net/convertigo/gw/ticket.signature/shop?x=1#top');
		expect(publicFrontendUrl('/convertigo/projects/App/DisplayObjects/mobile/')).toBe(
			'https://beta.example.net/convertigo/projects/App/DisplayObjects/mobile/'
		);
		expect(publicFrontendUrl('')).toBe('');
	});

	it('tells the addresses a phone cannot reach', () => {
		expect(isLocalAddress('http://localhost:18080/convertigo/gw/t/')).toBe(true);
		expect(isLocalAddress('http://127.0.0.1:28080/')).toBe(true);
		expect(isLocalAddress('https://beta.example.net/convertigo/gw/t/')).toBe(false);
		expect(isLocalAddress('not an address')).toBe(false);
	});
});
