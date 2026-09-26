import { describe, expect, it } from 'vitest';
import { readProjectStatistics } from './projectStatistics';

describe('readProjectStatistics', () => {
	it('reads the counters of each kind of objects and the summary of the project', () => {
		const statistics = readProjectStatistics({
			project: 'TestBackEnd',
			Sequencer:
				'&nbsp;sequenceCount = 2<br/>&nbsp;stepCount = 5<br/>&nbsp;javascriptCode = 0 functions in 4 lines',
			TestBackEnd: '14 object(s)<br/>2 connector(s)',
			HTTP_connector: '&nbsp;connectorCount = 1<br/>&nbsp;JSONTransactionCount = 2<br/>'
		});
		expect(statistics.project).toBe('TestBackEnd');
		expect(statistics.summary).toEqual(['14 object(s)', '2 connector(s)']);
		expect(statistics.groups).toEqual([
			{
				title: 'HTTP connector',
				rows: [
					{ label: 'Connector count', value: '1' },
					{ label: 'JSON transaction count', value: '2' }
				]
			},
			{
				title: 'Sequencer',
				rows: [
					{ label: 'Sequence count', value: '2' },
					{ label: 'Step count', value: '5' },
					{ label: 'Javascript code', value: '0 functions in 4 lines' }
				]
			}
		]);
	});

	it('gives nothing for a missing answer', () => {
		expect(readProjectStatistics(null)).toEqual({ project: '', summary: [], groups: [] });
	});
});
