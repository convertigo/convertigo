import { call } from '$lib/utils/service';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { loadSequenceFlow } from './sequenceLoader';

vi.mock('$lib/utils/service', () => ({
	call: vi.fn()
}));

const sequenceId = 'Project.sq:Sequence';
const callId = `${sequenceId}.st:Call_Sequence`;
const variablesFolderId = `${callId}.variables`;
const simpleVariableId = `${callId}.v:group`;
const multiVariableId = `${callId}.v:returnedAttributes`;
const nextId = `${sequenceId}.st:next`;

beforeEach(() => {
	vi.clearAllMocks();
});

describe('loadSequenceFlow', () => {
	it('hydrates all requestable step variables for the vars port', async () => {
		vi.mocked(call).mockImplementation(async (service, params) => {
			if (service === 'studio.palette.Get') {
				return {
					categories: [
						{
							name: 'Steps',
							items: [
								paletteItem('SequenceStep'),
								paletteItem('TransactionStep'),
								paletteItem('XMLElementStep')
							]
						}
					]
				};
			}
			if (service === 'studio.treeview.Get' && params.id === sequenceId) {
				return {
					children: [
						treeItem('SequenceStep', callId, 'Call_Sequence', true),
						treeItem('XMLElementStep', nextId, '<next>')
					]
				};
			}
			if (service === 'studio.treeview.Get' && params.id === callId) {
				return {
					children: [
						folder(variablesFolderId, 'Variables', [
							variable('StepVariable', simpleVariableId, 'group'),
							variable('StepMultiValuedVariable', multiVariableId, 'returnedAttributes')
						])
					]
				};
			}
			throw new Error(`Unexpected service call ${service} ${JSON.stringify(params)}`);
		});

		const flow = await loadSequenceFlow('Project', 'Sequence');
		const nodeIds = flow.nodes.map((node) => node.id);

		expect(nodeIds).toEqual(expect.arrayContaining([callId, simpleVariableId, multiVariableId]));
		expect(flow.nodes.find((node) => node.id === simpleVariableId)).toEqual(
			expect.objectContaining({ data: expect.objectContaining({ isVariable: true }) })
		);
		expect(flow.nodes.find((node) => node.id === multiVariableId)).toEqual(
			expect.objectContaining({ data: expect.objectContaining({ isVariable: true }) })
		);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: callId, portIndex: 1 },
				to: { nodeId: simpleVariableId, portIndex: 0 }
			})
		);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: callId, portIndex: 1 },
				to: { nodeId: multiVariableId, portIndex: 0 }
			})
		);
	});
});

/**
 * @param {string} simpleType
 * @returns {{ name: string, classname: string }}
 */
function paletteItem(simpleType) {
	return {
		name: simpleType,
		classname: `com.twinsoft.convertigo.beans.steps.${simpleType}`
	};
}

/**
 * @param {string} simpleType
 * @param {string} id
 * @param {string} label
 * @param {true | import('./types').TreeviewItem[]} children
 * @returns {import('./types').TreeviewItem}
 */
function treeItem(simpleType, id, label, children = []) {
	return {
		id,
		label,
		name: label,
		classname: `com.twinsoft.convertigo.beans.steps.${simpleType}`,
		icon: '',
		children
	};
}

/**
 * @param {string} id
 * @param {string} label
 * @param {import('./types').TreeviewItem[]} children
 * @returns {import('./types').TreeviewItem}
 */
function folder(id, label, children) {
	return {
		id,
		label,
		name: label,
		classname: '',
		icon: 'folder',
		children
	};
}

/**
 * @param {string} simpleType
 * @param {string} id
 * @param {string} label
 * @returns {import('./types').TreeviewItem}
 */
function variable(simpleType, id, label) {
	return {
		id,
		label,
		name: label,
		classname: `com.twinsoft.convertigo.beans.variables.${simpleType}`,
		icon: '',
		children: []
	};
}
