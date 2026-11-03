import { call } from '#lib/utils/service.js';
import { render } from 'svelte/server';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
	debugCommand,
	debugSession,
	followDebugger,
	frameScopeGroups,
	inspectFrame,
	variablePreviews,
	watchDebugger
} from './debugSession.svelte.js';
import { debugHover } from './editor/debugHover.js';
import { javascriptDebugParameters } from './javascriptDebugRequest.js';
import StudioDebugPanel from './StudioDebugPanel.svelte';

vi.mock('#lib/utils/service.js', () => ({
	call: vi.fn(async () => ({})),
	callRequestable: vi.fn()
}));
const state = (values = {}) => ({
	breakOnExceptions: false,
	enabled: true,
	attached: true,
	sessionId: 'test-session',
	serial: 1,
	pauseId: 1,
	stopped: true,
	sources: ['probe'],
	frames: [{ url: 'probe', line: 7, function: 'add' }],
	...values
});
beforeEach(() => {
	vi.useFakeTimers();
	vi.clearAllMocks();
	debugSession.state = null;
	followDebugger(state());
});
afterEach(() => {
	followDebugger(state({ attached: false, stopped: false, serial: 100 }));
	vi.useRealTimers();
});
describe('JavaScript debugger component and session', () => {
	it('preserves participant input and output groups and the legacy Rhino receiver', () => {
		const variables = {
			scope: [{ name: 'value', value: '41' }],
			this: [],
			scopes: [
				{ name: 'Inputs', variables: [{ name: 'value', value: '42' }] },
				{ name: 'Output', variables: [{ name: 'value', value: '42' }] }
			]
		};
		expect(frameScopeGroups(variables, 'example.plugin')).toBe(variables.scopes);
		expect(frameScopeGroups(variables, 'rhino').map((group) => group.name)).toEqual([
			'Local',
			'this',
			'Output'
		]);
		expect(frameScopeGroups(variables, undefined)[0].variables[0].value).toBe('41');
	});

	it('renders controls, stack and the shared editor container when paused', () => {
		const html = render(StudioDebugPanel, { props: { active: true, standalone: true } }).body;
		for (const label of [
			'Step into',
			'Step over',
			'Step out',
			'Continue',
			'Call stack',
			'Source editor',
			'Evaluate an expression',
			'Run sequence',
			'add:7'
		])
			expect(html).toContain(label);
		expect(html).not.toContain('studio-debug__gutter');
	});
	it('keeps native launches implicit and presents inspection in a vertical sequence', () => {
		for (const props of [{ eclipse: true, standalone: true }, {}]) {
			const html = render(StudioDebugPanel, { props }).body;
			for (const label of [
				'Scope',
				'Call stack',
				'Console',
				'Add watch',
				'Break on function entry',
				'Break on function return'
			])
				expect(html).toContain(label);
			expect(html).not.toContain('Debug Eclipse launches');
			expect(html).not.toContain('Source editor');
			expect(html).not.toContain('Choose a script');
			expect(html).not.toContain('Run a sequence from this view');
			expect(html).not.toContain('c8o-switch');
			expect(html).toContain('role="toolbar"');
			expect(html).toContain('aria-pressed="false"');
			const headings = [...html.matchAll(/<h3[^>]*>([^<]+)<\/h3>/g)].map((match) => match[1]);
			expect(headings).toEqual(['Watch', 'Scope', 'Call Stack', 'Console']);
		}
	});

	it('shows a running state between pauses with a persistent inspection shell', () => {
		followDebugger(state({ stopped: false, running: true, frames: [], serial: 2 }));
		const html = render(StudioDebugPanel).body;
		expect(html).toContain('Running — waiting for the next pause');
		expect(html).toContain('Variables will appear at the next pause');
		expect(html).toContain('Call stack');
	});

	it('offers late attachment and hides Rhino controls on a production server', () => {
		followDebugger(
			state({
				rhinoAvailable: false,
				rhinoRequested: false,
				stopped: false,
				running: false,
				targets: [{ id: 'opaque-request', label: 'Probe', selected: false }],
				serial: 2
			})
		);
		const html = render(StudioDebugPanel).body;
		expect(html).toContain('Attach to running request: Probe');
		expect(html).toContain('library state stay shared');
		expect(html).not.toContain('Break on function entry');
		expect(html).not.toContain('Break on exceptions');
	});
	it('warns about fresh library state when source breakpoints select Rhino', () => {
		followDebugger(state({ rhinoAvailable: true, rhinoRequested: true, serial: 2 }));
		expect(render(StudioDebugPanel).body).toContain('fresh library state');
	});

	it('shows a disabled server gate', () => {
		followDebugger(state({ enabled: false, attached: false, stopped: false, serial: 2 }));
		expect(render(StudioDebugPanel).body).toContain('JavaScript debugging is disabled');
		expect(render(StudioDebugPanel).body).toMatch(
			/<button[^>]*disabled[^>]*aria-label="Step into"/
		);
	});
	it('sends the pause generation and explicit frame, and marks Studio requests', async () => {
		await debugCommand('eval', { frame: '1', expression: 'value' });
		expect(call).toHaveBeenCalledWith('studio.debug.Debugger', {
			action: 'eval',
			pauseId: '1',
			frame: '1',
			expression: 'value'
		});
		expect(javascriptDebugParameters()).toEqual({ __javascriptDebug: 'test-session' });
		followDebugger(state({ attached: false, stopped: false, serial: 2 }));
		expect(javascriptDebugParameters()).toEqual({});
	});
	it('deduplicates inspection and invalidates it at the next pause', async () => {
		await Promise.all([inspectFrame(0), inspectFrame(0)]);
		expect(call).toHaveBeenCalledTimes(1);
		followDebugger(state({ pauseId: 2, serial: 2 }));
		await inspectFrame(0);
		expect(call).toHaveBeenCalledTimes(2);
		followDebugger(state({ serial: 1 }));
		expect(debugSession.state?.pauseId).toBe(2);
	});
	it('stops polling when the panel unmounts, including an in-flight response', async () => {
		let resolve = (_value) => {};
		vi.mocked(call).mockImplementationOnce(
			() =>
				new Promise((done) => {
					resolve = done;
				})
		);
		const stop = watchDebugger();
		stop();
		resolve({ state: state() });
		await vi.advanceTimersByTimeAsync(5000);
		expect(call).toHaveBeenCalledTimes(1);
		expect(javascriptDebugParameters()).toEqual({});
	});
	it('does not reattach from a source response arriving after stop', async () => {
		let resolve = (_value) => {};
		vi.mocked(call).mockImplementationOnce(
			() =>
				new Promise((done) => {
					resolve = done;
				})
		);
		const pending = debugCommand('source', { url: 'probe' });
		vi.mocked(call).mockResolvedValueOnce({
			state: state({ attached: false, stopped: false, sessionId: undefined, serial: 0 })
		});
		await debugCommand('stop');
		resolve({ state: state() });
		await pending;
		expect(debugSession.state?.attached).toBe(false);
		expect(javascriptDebugParameters()).toEqual({});
	});
	it('uses nearest scope previews and a safe, model-specific Monaco hover', () => {
		const values = variablePreviews({
			scopes: [
				{ variables: [{ name: 'value', value: '<script>*42*' }] },
				{ variables: [{ name: 'value', value: 'wrong' }] }
			]
		});
		const model = { getWordAtPosition: () => ({ word: 'value', startColumn: 1, endColumn: 6 }) };
		const hover = debugHover(
			() => model,
			() => values
		);
		expect(hover.provideHover({}, { lineNumber: 2 })).toBeNull();
		const result = hover.provideHover(model, { lineNumber: 2 });
		expect(result?.contents[0]).toEqual({
			value: 'value = \\<script\\>\\*42\\*',
			isTrusted: false,
			supportHtml: false
		});
		expect(
			debugHover(
				() => model,
				() => null
			).provideHover(model, { lineNumber: 2 })
		).toBeNull();
	});
});
