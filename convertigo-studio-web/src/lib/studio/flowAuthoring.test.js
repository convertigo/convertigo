import { describe, expect, it } from 'vitest';
import {
	authoringActionDoneMessage,
	authoringActionRequest,
	authoringActionsMessage,
	authoringActionsRequest,
	authoringDropRequest,
	authoringModeFromMessage,
	authoringModeMessage,
	authoringMoveRequest,
	contextAuthoringMutation,
	FLOW_AUTHORING_PROTOCOL,
	highlightAuthoringMessage,
	isFlowAuthoringMessage,
	isFlowAuthoringReference,
	isFrontendAuthoringNodeId,
	selectedAuthoringReference,
	themeContextFromMessage,
	themeContextRequestMessage
} from './flowAuthoring';

const reference = {
	nodeId: 'title',
	sourceProject: 'sampleKitchenSinkFlow',
	sourceRelativePath: 'model/sampleKitchenSinkFlow/src/routes/+page.flow.svelte',
	sourceMutationPath: 'frontAst.slots.structure.children[0]'
};

describe('Flow visual authoring protocol', () => {
	it('reconciles menu mutations like other authoring commands, preserving provider metadata', () => {
		const mutation = {
			ok: true,
			id: 'project.renamed',
			parentId: 'project',
			projectedRootPath: 'nodes',
			selectionVirtualPath: 'nodes[0]'
		};
		expect(contextAuthoringMutation({ ok: true, mutationResult: mutation }, 'project.old')).toEqual(
			{
				...mutation,
				done: true,
				target: 'project.old',
				source: 'context'
			}
		);
		for (const result of [
			{ ok: false, mutationResult: mutation },
			{ ok: true },
			{ ok: true, mutationResult: { ok: false } },
			{ ok: true, mutationResult: { ...mutation, changed: false } }
		]) {
			expect(contextAuthoringMutation(result, 'project.old')).toBeNull();
		}
	});
	it('accepts only complete source-backed references', () => {
		expect(isFlowAuthoringReference(reference)).toBe(true);
		expect(isFlowAuthoringReference({ ...reference, sourceMutationPath: '' })).toBe(false);
		expect(isFlowAuthoringReference({ nodeId: 'title' })).toBe(false);
	});

	it('builds deterministic highlight and clear messages', () => {
		const message = highlightAuthoringMessage(new Proxy(reference, {}));
		expect(message).toEqual({
			protocol: FLOW_AUTHORING_PROTOCOL,
			type: 'authoring.highlight',
			reference
		});
		expect(message.reference).not.toBe(reference);
		expect(() => structuredClone(message)).not.toThrow();
		expect(highlightAuthoringMessage(null)).toEqual({
			protocol: FLOW_AUTHORING_PROTOCOL,
			type: 'authoring.highlight.clear'
		});
	});

	it('reads select and legacy reveal messages without accepting unrelated payloads', () => {
		for (const type of ['authoring.select', 'authoring.reveal']) {
			const message = { protocol: FLOW_AUTHORING_PROTOCOL, type, reference };
			expect(isFlowAuthoringMessage(message)).toBe(true);
			expect(selectedAuthoringReference(message)).toEqual(reference);
		}
		expect(
			selectedAuthoringReference({ protocol: FLOW_AUTHORING_PROTOCOL, type: 'viewer.ready' })
		).toBe(null);
		expect(
			selectedAuthoringReference({ protocol: 'other', type: 'authoring.select', reference })
		).toBe(null);
	});

	it('round-trips browse, select and move mode messages', () => {
		for (const mode of ['browse', 'select', 'move']) {
			const request = authoringModeMessage(/** @type {'browse' | 'select' | 'move'} */ (mode));
			expect(request).toEqual({
				protocol: FLOW_AUTHORING_PROTOCOL,
				type: 'authoring.mode.set',
				mode
			});
			expect(authoringModeFromMessage({ ...request, type: 'authoring.mode.changed' })).toBe(mode);
		}
		expect(
			authoringModeFromMessage({ protocol: FLOW_AUTHORING_PROTOCOL, type: 'viewer.ready' })
		).toBe(null);
		expect(
			authoringModeFromMessage({
				protocol: FLOW_AUTHORING_PROTOCOL,
				type: 'authoring.mode.changed',
				mode: 'resize'
			})
		).toBe(null);
	});

	it('requests and validates viewer-computed theme colors', () => {
		expect(themeContextRequestMessage()).toEqual({
			protocol: FLOW_AUTHORING_PROTOCOL,
			type: 'viewer.theme.request'
		});
		const context = {
			mode: 'dark',
			palette: 'convertigo',
			tokens: [
				{
					value: 'secondary',
					label: 'Secondary',
					cssVariable: '--c8o-color-secondary',
					light: 'rgb(242, 140, 40)',
					dark: 'rgb(255, 186, 112)',
					current: 'rgb(255, 186, 112)'
				}
			]
		};
		expect(
			themeContextFromMessage({
				protocol: FLOW_AUTHORING_PROTOCOL,
				type: 'viewer.theme',
				themeContext: context
			})
		).toEqual(context);
		expect(
			themeContextFromMessage({
				protocol: FLOW_AUTHORING_PROTOCOL,
				type: 'viewer.theme',
				themeContext: { ...context, tokens: [] }
			})
		).toBe(null);
	});

	it('recognizes only generated frontend authoring tree nodes', () => {
		expect(
			isFrontendAuthoringNodeId(
				'sample.FlowEngine.frontends.builder_svelte.authoring_routes.authoring_home.authoring_title'
			)
		).toBe(true);
		expect(isFrontendAuthoringNodeId('sample.FlowEngine.frontends.builder_svelte')).toBe(false);
		expect(isFrontendAuthoringNodeId('sample.FlowEngine.sequences.demo.steps.text')).toBe(false);
	});

	it('accepts only precise authoring drop requests', () => {
		const payload = { type: 'paletteData', data: { name: 'Text' }, options: {} };
		const message = {
			protocol: FLOW_AUTHORING_PROTOCOL,
			type: 'authoring.drop',
			reference,
			position: 'before',
			payload
		};
		expect(authoringDropRequest(message)).toEqual({ reference, position: 'before', payload });
		expect(authoringDropRequest({ ...message, position: 'around' })).toBe(null);
		expect(authoringDropRequest({ ...message, payload: { type: 'unknown' } })).toBe(null);
	});

	it('accepts only precise visual move requests', () => {
		const target = {
			...reference,
			nodeId: 'target',
			sourceMutationPath: 'frontAst.slots.structure.children[1]'
		};
		const message = {
			protocol: FLOW_AUTHORING_PROTOCOL,
			type: 'authoring.move',
			source: reference,
			reference: target,
			position: 'after'
		};
		expect(authoringMoveRequest(message)).toEqual({
			source: reference,
			reference: target,
			position: 'after'
		});
		expect(authoringMoveRequest({ ...message, source: null })).toBe(null);
		expect(authoringMoveRequest({ ...message, position: 'around' })).toBe(null);
	});

	it('answers the chip of the preview with the actions of the tree on its object', () => {
		const request = { protocol: FLOW_AUTHORING_PROTOCOL, type: 'authoring.actions.request', reference };
		expect(authoringActionsRequest(request)).toEqual({ reference });
		expect(authoringActionsRequest({ ...request, reference: { nodeId: 'title' } })).toBe(null);
		expect(authoringActionsRequest({ ...request, type: 'authoring.select' })).toBe(null);
		expect(
			authoringActionsMessage(reference, [
				{ id: 'flow.node.disable', label: 'Disable', description: 'Skip this node.' },
				{ id: 'object.delete', label: 'Delete', danger: true, enabled: false },
				/** @type {any} */ ({ label: 'no id' }),
				/** @type {any} */ ({ id: 'no.label' })
			])
		).toEqual({
			protocol: FLOW_AUTHORING_PROTOCOL,
			type: 'authoring.actions',
			reference,
			actions: [
				{ id: 'flow.node.disable', label: 'Disable', description: 'Skip this node.' },
				{ id: 'object.delete', label: 'Delete', danger: true, enabled: false }
			]
		});
		expect(authoringActionsMessage(reference, /** @type {any} */ (null)).actions).toEqual([]);
	});

	it('runs an action of the chip once and tells when it is over', () => {
		const message = {
			protocol: FLOW_AUTHORING_PROTOCOL,
			type: 'authoring.action',
			reference,
			action: 'flow.node.disable'
		};
		expect(authoringActionRequest(message)).toEqual({ reference, action: 'flow.node.disable' });
		expect(authoringActionRequest({ ...message, action: '' })).toBe(null);
		expect(authoringActionRequest({ ...message, action: 3 })).toBe(null);
		expect(authoringActionRequest({ ...message, reference: null })).toBe(null);
		expect(authoringActionDoneMessage(reference, 'object.delete')).toEqual({
			protocol: FLOW_AUTHORING_PROTOCOL,
			type: 'authoring.action.done',
			reference,
			action: 'object.delete'
		});
	});
});
