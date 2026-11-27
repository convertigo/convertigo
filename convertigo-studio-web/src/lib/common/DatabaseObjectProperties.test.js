import { randomUUID } from 'node:crypto';
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
	it('omits an untouched masked property when saving another edit', async () => {
		vi.mocked(call)
			.mockResolvedValueOnce({
				properties: {
					...propertiesResponse('old comment').properties,
					'SMTP password': {
						class: 'java.lang.String',
						name: 'smtpPassword',
						isMasked: true,
						hasMaskedValue: true
					}
				}
			})
			.mockResolvedValueOnce({ done: true });
		const editor = createDatabaseObjectProperties();
		await editor.onSelectionChange({ selectedValue: ['Project.Sequence.SendMail'] });
		const [comment, masked] = editor.properties;

		expect(masked.value).toBeUndefined();
		expect(editor.getChanges()).toEqual([]);
		comment.name = 'comment';
		comment.value = 'new comment';
		await editor.save();
		expect(JSON.parse(vi.mocked(call).mock.lastCall[1].props)).toEqual([
			{ name: 'comment', value: 'new comment' }
		]);
	});

	it('distinguishes an explicit clear from an unchanged masked value and discards saved drafts', async () => {
		vi.mocked(call)
			.mockResolvedValueOnce({
				properties: {
					'SMTP password': {
						class: 'java.lang.String',
						name: 'smtpPassword',
						isMasked: true,
						hasMaskedValue: true
					}
				}
			})
			.mockResolvedValue({ done: true });
		const editor = createDatabaseObjectProperties();
		await editor.onSelectionChange({ selectedValue: ['Project.Sequence.SendMail'] });
		const [masked] = editor.properties;

		masked.value = '';
		expect(editor.hasChanges).toBe(true);
		editor.cancel();
		expect(editor.hasChanges).toBe(false);
		masked.value = '';
		await editor.save();
		expect(JSON.parse(vi.mocked(call).mock.lastCall[1].props)).toEqual([
			{ name: 'smtpPassword', value: '' }
		]);
		expect(masked.hasMaskedValue).toBe(false);

		for (const replacement of [randomUUID(), '${mail.fixture.secret}']) {
			masked.value = replacement;
			await editor.save();
			expect(JSON.parse(vi.mocked(call).mock.lastCall[1].props)[0].value === replacement).toBe(
				true
			);
			expect(masked.hasMaskedValue).toBe(true);
			expect(masked.value).toBeUndefined();
			expect(editor.hasChanges).toBe(false);
		}
	});
});
