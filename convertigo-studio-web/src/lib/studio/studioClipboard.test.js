import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const service = vi.hoisted(() => ({
	copyDbo: vi.fn(),
	cutDbo: vi.fn(),
	pasteDbo: vi.fn()
}));
vi.mock('$lib/utils/service', () => service);

const {
	clearStudioClipboard,
	isStudioClipboardText,
	pasteStudioClipboard,
	putInStudioClipboard,
	studioClipboardContent
} = await import('./studioClipboard.svelte.js');

const WEB_COPY =
	'<?xml version="1.0" encoding="UTF-8"?>\n<convertigo clipboard="copy"><transaction classname="x"/></convertigo>';
const ECLIPSE_COPY =
	'<?xml version="1.0" encoding="ISO-8859-1"?>\n<convertigo-clipboard>\n<sequence classname="y"/></convertigo-clipboard>';
const FLOW_COPY = JSON.stringify({
	protocol: 'convertigo.flow.virtual.clipboard.v1',
	kind: 'node'
});

/** @type {string} */
let systemText;

beforeEach(() => {
	systemText = '';
	vi.stubGlobal('navigator', {
		clipboard: {
			readText: vi.fn(async () => systemText),
			writeText: vi.fn(async (/** @type {string} */ text) => {
				systemText = text;
			})
		}
	});
	clearStudioClipboard();
});

afterEach(() => {
	vi.unstubAllGlobals();
	vi.clearAllMocks();
});

describe('isStudioClipboardText', () => {
	it('recognizes the copies of the web and Eclipse Studios', () => {
		expect(isStudioClipboardText(WEB_COPY)).toBe(true);
		expect(isStudioClipboardText(`  ${ECLIPSE_COPY}\n`)).toBe(true);
		expect(isStudioClipboardText(FLOW_COPY)).toBe(true);
	});

	it('ignores other text and cut objects', () => {
		expect(isStudioClipboardText('hello')).toBe(false);
		expect(isStudioClipboardText('<convertigo clipboard="cut"><dbo id="a"/></convertigo>')).toBe(
			false
		);
		expect(isStudioClipboardText('{"protocol":"other"}')).toBe(false);
		expect(isStudioClipboardText('{not json')).toBe(false);
	});
});

describe('system clipboard', () => {
	it('writes a copy to the system clipboard', async () => {
		service.copyDbo.mockResolvedValue({ done: true, xml: WEB_COPY, text: WEB_COPY });
		expect(await putInStudioClipboard('copy', ['p.cn:c.tr:t'])).toBe(true);
		await vi.waitFor(() => expect(systemText).toBe(WEB_COPY));
	});

	it('keeps a cut out of the system clipboard', async () => {
		systemText = 'before';
		service.cutDbo.mockResolvedValue({ done: true, xml: '<convertigo clipboard="cut"/>' });
		expect(await putInStudioClipboard('cut', ['p.cn:c.tr:t'])).toBe(true);
		expect(systemText).toBe('before');
		expect(await studioClipboardContent()).toMatchObject({ kind: 'cut', ids: ['p.cn:c.tr:t'] });
	});

	it('pastes the objects another Studio copied, without their ids', async () => {
		service.copyDbo.mockResolvedValue({ done: true, xml: WEB_COPY, text: WEB_COPY });
		await putInStudioClipboard('copy', ['p.cn:c.tr:t']);
		expect(await studioClipboardContent()).toEqual({
			kind: 'copy',
			xml: WEB_COPY,
			ids: ['p.cn:c.tr:t']
		});
		expect(await studioClipboardContent(ECLIPSE_COPY)).toEqual({
			kind: 'copy',
			xml: ECLIPSE_COPY,
			ids: []
		});
	});

	it('falls back to the Studio copy when the system clipboard holds other text', async () => {
		service.copyDbo.mockResolvedValue({ done: true, xml: WEB_COPY, text: WEB_COPY });
		await putInStudioClipboard('copy', ['p.cn:c.tr:t']);
		systemText = 'some text';
		expect(await studioClipboardContent()).toMatchObject({ kind: 'copy', xml: WEB_COPY });
		clearStudioClipboard();
		expect(await studioClipboardContent()).toMatchObject({ kind: '', xml: '' });
	});

	it('pastes the given content', async () => {
		service.pasteDbo.mockResolvedValue({ done: true, ids: ['p.sq:s'], target: 'p' });
		const result = await pasteStudioClipboard('p', 'auto', {
			kind: 'copy',
			xml: ECLIPSE_COPY,
			ids: []
		});
		expect(service.pasteDbo).toHaveBeenCalledWith('p', ECLIPSE_COPY, 'auto');
		expect(result).toMatchObject({ done: true, ids: ['p.sq:s'], kind: 'copy', target: 'p' });
	});
});
