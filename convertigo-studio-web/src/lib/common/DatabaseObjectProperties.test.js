import { call } from '$lib/utils/service';
import { describe, expect, it, vi } from 'vitest';
import { createDatabaseObjectProperties } from './DatabaseObjectProperties.svelte.js';

vi.mock('$lib/utils/service', () => ({
	call: vi.fn()
}));

function deferred() {
	/** @type {(value: any) => void} */
	let resolve = () => {};
	const promise = new Promise((done) => {
		resolve = done;
	});
	return { promise, resolve };
}

function propertiesResponse(value) {
	return {
		properties: {
			Comment: {
				category: 'Base properties',
				class: 'java.lang.String',
				value
			}
		}
	};
}

describe('createDatabaseObjectProperties', () => {
	it('ignores property responses from stale selections', async () => {
		const first = deferred();
		const second = deferred();
		vi.mocked(call).mockImplementation((_service, params) => {
			if (params.id === 'Project.sq:First') {
				return first.promise;
			}
			return second.promise;
		});
		const dboProperties = createDatabaseObjectProperties();

		const firstLoad = dboProperties.onSelectionChange({
			selectedValue: ['Project.sq:First']
		});
		expect(dboProperties.loading).toBe(true);

		const secondLoad = dboProperties.onSelectionChange({
			selectedValue: ['Project.sq:Second']
		});
		expect(dboProperties.loading).toBe(true);

		second.resolve(propertiesResponse('second value'));
		await secondLoad;
		expect(dboProperties.id).toBe('Project.sq:Second');
		expect(dboProperties.loading).toBe(false);
		expect(dboProperties.properties).toEqual([
			expect.objectContaining({
				displayName: 'Comment',
				originalValue: 'second value',
				value: 'second value'
			})
		]);

		first.resolve(propertiesResponse('first value'));
		await firstLoad;
		expect(dboProperties.id).toBe('Project.sq:Second');
		expect(dboProperties.loading).toBe(false);
		expect(dboProperties.properties).toEqual([
			expect.objectContaining({
				displayName: 'Comment',
				originalValue: 'second value',
				value: 'second value'
			})
		]);
	});

	it('keeps engine property categories and normalizes studio category labels', async () => {
		vi.mocked(call).mockResolvedValue({
			properties: {
				Visible: {
					category: '@Properties',
					kind: 'ion',
					name: 'visible',
					value: 'true'
				},
				Custom: {
					category: 'Mobile',
					name: 'custom',
					value: 'x'
				}
			}
		});
		const dboProperties = createDatabaseObjectProperties();

		await dboProperties.onSelectionChange({
			selectedValue: ['Project.MobileApplication.Application']
		});

		expect(dboProperties.properties).toEqual([
			expect.objectContaining({ displayName: 'Visible', category: 'Properties' }),
			expect.objectContaining({ displayName: 'Custom', category: 'Mobile' })
		]);
		expect(dboProperties.categories.map(({ category }) => category)).toEqual([
			'Base properties',
			'Properties',
			'Expert',
			'Information',
			'Mobile'
		]);
		expect(
			dboProperties.categories.find(({ category }) => category === 'Properties')?.properties
		).toEqual([expect.objectContaining({ displayName: 'Visible' })]);
	});
});
