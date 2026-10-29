import { beforeEach, describe, expect, it } from 'vitest';
import {
	editorPage,
	forgetEditorPages,
	knownEditorRevisions,
	pickerRequest
} from './flowPickerRequests';

describe('Flow picker pages', () => {
	beforeEach(() => forgetEditorPages());

	it('reuses a page the service omitted because the session holds its revision', () => {
		expect(knownEditorRevisions()).toBe('');
		expect(editorPage({ html: '<p>editor</p>', htmlRevision: 'r1' })).toBe('<p>editor</p>');
		expect(knownEditorRevisions()).toBe('r1');
		expect(editorPage({ htmlRevision: 'r1' })).toBe('<p>editor</p>');
	});

	it('returns no page for an unknown revision, and a page sent without revision as is', () => {
		expect(editorPage({ htmlRevision: 'missing' })).toBe('');
		expect(editorPage({ html: '<p>older server</p>' })).toBe('<p>older server</p>');
		expect(knownEditorRevisions()).toBe('');
		expect(editorPage(null)).toBe('');
	});

	it('keeps the most recently used pages only', () => {
		for (const revision of ['r1', 'r2', 'r3', 'r4']) {
			editorPage({ html: revision, htmlRevision: revision });
		}
		editorPage({ htmlRevision: 'r1' });
		editorPage({ html: 'r5', htmlRevision: 'r5' });
		expect(knownEditorRevisions().split(',').sort()).toEqual(['r1', 'r3', 'r4', 'r5']);
	});
});

describe('Flow picker requests', () => {
	const target = { id: 'project.FlowEngine.frontends.svelte.title', propertyName: 'text' };
	const source = { category: 'local', name: 'copy', scopeId: 'page' };

	it('asks the paths of a deferred binding source to the picker service', async () => {
		/** @type {any[]} */
		const calls = [];
		const response = await pickerRequest(
			target,
			{ name: 'bindingSourceDetail', payload: { property: 'text', source } },
			async (service, data) => {
				calls.push([service, data]);
				return { bindingSources: [{ source, paths: [{ path: 'title', type: 'string' }] }] };
			}
		);
		expect(calls).toEqual([
			[
				'studio.flowpicker.Get',
				{ id: target.id, propertyName: 'text', bindingSource: JSON.stringify(source) }
			]
		]);
		expect(response).toEqual({
			ok: true,
			bindingSources: [{ source, paths: [{ path: 'title', type: 'string' }] }]
		});
	});

	it('reports a failed or unsupported request to the editor', async () => {
		const failing = async () => ({ error: { message: 'Unable to resolve parent Flow' } });
		expect(
			await pickerRequest(target, { name: 'bindingSourceDetail', payload: { source } }, failing)
		).toEqual({ ok: false, error: 'Unable to resolve parent Flow' });
		const never = async () => {
			throw new Error('not called');
		};
		expect((await pickerRequest(target, { name: 'context' }, never)).ok).toBe(false);
		expect((await pickerRequest(target, { name: 'bindingSourceDetail' }, never)).ok).toBe(false);
		expect(
			(await pickerRequest(null, { name: 'bindingSourceDetail', payload: { source } }, never)).ok
		).toBe(false);
	});
});
