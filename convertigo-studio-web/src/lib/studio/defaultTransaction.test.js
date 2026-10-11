import { describe, expect, it } from 'vitest';
import { connectorIdOf, implicitChildId, implicitTransactionId } from './defaultTransaction';

/**
 * @param {Record<string, any[]>} folders the children of each tree folder
 */
function tree(folders) {
	/** @type {string[]} */
	const asked = [];
	return {
		asked,
		/** @param {string} id */
		childrenOf: async (id) => {
			asked.push(id);
			return folders[id] ?? [];
		}
	};
}

describe('Ctrl+F5 transaction choice', () => {
	it('finds the connector holding the selection', () => {
		expect(connectorIdOf('P.cn:Main')).toBe('P.cn:Main');
		expect(connectorIdOf('P.cn:Main:tr')).toBe('P.cn:Main');
		expect(connectorIdOf('P.cn:Main.tr:a')).toBe('P.cn:Main');
		expect(connectorIdOf('P.cn:Main.tr:a.va:v')).toBe('P.cn:Main');
		expect(connectorIdOf('P.sq:Seq')).toBe('');
		expect(connectorIdOf('P:cn')).toBe('');
		expect(connectorIdOf('P')).toBe('');
		expect(connectorIdOf('P//src/a.cn:b')).toBe('');
	});

	it('chooses the default child, else the only one, as the engine', () => {
		const mark = 'Default transaction';
		expect(implicitChildId([{ id: 'a' }, { id: 'b', default: mark }], mark)).toBe('b');
		expect(implicitChildId([{ id: 'a' }], mark)).toBe('a');
		expect(implicitChildId([{ id: 'a' }, { id: 'b' }], mark)).toBe('');
		expect(implicitChildId([], mark)).toBe('');
		expect(implicitChildId(/** @type {any} */ (undefined), mark)).toBe('');
	});

	it('runs the transaction of the selected connector first', async () => {
		const { childrenOf, asked } = tree({
			'P.cn:Other:tr': [{ id: 'P.cn:Other.tr:alone' }]
		});
		expect(await implicitTransactionId('P', 'P.cn:Other.tr:alone', childrenOf)).toBe(
			'P.cn:Other.tr:alone'
		);
		expect(asked).toEqual(['P.cn:Other:tr']);
	});

	it('else the default or the only connector of the project', async () => {
		const defaults = tree({
			'P:cn': [{ id: 'P.cn:Main', default: 'Default connector' }, { id: 'P.cn:Other' }],
			'P.cn:Main:tr': [
				{ id: 'P.cn:Main.tr:a' },
				{ id: 'P.cn:Main.tr:b', default: 'Default transaction' }
			]
		});
		expect(await implicitTransactionId('P', 'P.sq:Seq', defaults.childrenOf)).toBe(
			'P.cn:Main.tr:b'
		);

		const single = tree({
			'P:cn': [{ id: 'P.cn:Only' }],
			'P.cn:Only:tr': [{ id: 'P.cn:Only.tr:alone' }]
		});
		expect(await implicitTransactionId('P', 'P', single.childrenOf)).toBe('P.cn:Only.tr:alone');
	});

	it('chooses nothing without a default nor a single object', async () => {
		const several = tree({ 'P:cn': [{ id: 'P.cn:A' }, { id: 'P.cn:B' }] });
		expect(await implicitTransactionId('P', 'P', several.childrenOf)).toBe('');
		expect(several.asked).toEqual(['P:cn']);

		const transactions = tree({ 'P.cn:A:tr': [{ id: 'P.cn:A.tr:a' }, { id: 'P.cn:A.tr:b' }] });
		expect(await implicitTransactionId('P', 'P.cn:A', transactions.childrenOf)).toBe('');

		expect(await implicitTransactionId('P', 'P', tree({}).childrenOf)).toBe('');
	});
});
