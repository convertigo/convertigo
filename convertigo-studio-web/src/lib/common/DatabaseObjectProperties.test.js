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
				name: 'comment',
				category: 'Base properties',
				class: 'java.lang.String',
				value
			}
		}
	};
}

describe('createDatabaseObjectProperties', () => {
	it('retains invalid picker drafts, blocks submission and accepts a corrected value', async () => {
		vi.mocked(call).mockResolvedValueOnce(propertiesResponse('local.answer'));
		const model = createDatabaseObjectProperties();
		await model.onSelectionChange({ selectedValue: ['Project.flow.block'] });
		model.updateDraft('comment', 'input.readOnly', { valid: false, error: 'Read-only scope' });
		expect(model.hasChanges).toBe(true);
		expect(model.valid).toBe(false);
		const calls = vi.mocked(call).mock.calls.length;
		expect(await model.save()).toBe(false);
		expect(vi.mocked(call).mock.calls).toHaveLength(calls);
		model.updateDraft('comment', 'local.changed', { valid: true, error: '' });
		expect(model.valid).toBe(true);
		vi.mocked(call).mockResolvedValueOnce({ done: true });
		expect(await model.save()).toBe(true);
		const submitted = JSON.parse(vi.mocked(call).mock.calls.at(-1)[1].props);
		expect(submitted[0]).not.toHaveProperty('validation');
	});

	it('clears validation on cancel, selection and replacement of the validated value', async () => {
		vi.mocked(call).mockResolvedValueOnce(propertiesResponse('before'));
		const model = createDatabaseObjectProperties();
		await model.onSelectionChange({ selectedValue: ['Project.first'] });
		model.updateDraft('comment', 'invalid', { valid: false });
		model.cancel();
		expect(model.valid).toBe(true);
		expect(model.hasChanges).toBe(false);
		model.updateDraft('comment', 'invalid', { valid: false });
		model.properties[0].value = 'different inline edit';
		expect(model.valid).toBe(true);
		model.updateDraft('comment', 'invalid', { valid: false });
		vi.mocked(call).mockResolvedValueOnce(propertiesResponse('next'));
		await model.onSelectionChange({ selectedValue: ['Project.second'] });
		expect(model.valid).toBe(true);
	});

	it.each([undefined, { persist: false }, { persist: true }])(
		'keeps Admin persistence by default and allows Studio drafts: %j',
		async (options) => {
			vi.mocked(call).mockResolvedValueOnce(propertiesResponse('before'));
			const model = createDatabaseObjectProperties();
			await model.onSelectionChange({ selectedValue: ['Project.old'] });
			model.properties[0].value = 'after';
			const result = {
				done: true,
				id: 'Project.new',
				projectedRootPath: 'nodes',
				previousId: 'Project.old'
			};
			vi.mocked(call).mockResolvedValueOnce(result);
			const onSaved = vi.fn();
			expect(await model.save({ ...options, onSaved })).toBe(true);
			expect(call).toHaveBeenLastCalledWith('studio.properties.Set', {
				id: 'Project.old',
				props: expect.any(String),
				save: options?.persist ?? true
			});
			expect(onSaved).toHaveBeenCalledWith('Project.old', result);
			expect(model.getChanges()).toHaveLength(0);
		}
	);

	it('applies the changes to the other selected objects of the same type, their name apart', async () => {
		vi.mocked(call).mockResolvedValueOnce({
			properties: {
				Comment: { name: 'comment', category: 'Base properties', value: 'before' },
				Name: { name: 'name', category: 'Base properties', value: 'first' }
			}
		});
		const model = createDatabaseObjectProperties();
		await model.onSelectionChange({ selectedValue: ['Project.sq:First'] });
		model.properties[0].value = 'after';
		model.properties[1].value = 'renamed';
		vi.mocked(call).mockResolvedValueOnce({ done: true });
		vi.mocked(call).mockResolvedValueOnce({ done: true });
		const onSaved = vi.fn();
		expect(await model.save({ persist: false, onSaved, alsoIds: ['Project.sq:Second'] })).toBe(
			true
		);
		const [, other] = vi.mocked(call).mock.calls.at(-1) ?? [];
		expect(other).toMatchObject({ id: 'Project.sq:Second', sameAs: 'Project.sq:First' });
		expect(JSON.parse(other?.props ?? '[]').map((/** @type {any} */ p) => p.name)).toEqual([
			'comment'
		]);
		expect(onSaved).toHaveBeenCalledWith('Project.sq:Second', { done: true });
	});

	it('saves a value set to null and cancels it', async () => {
		vi.mocked(call).mockResolvedValueOnce({
			properties: {
				Value: {
					name: 'value',
					category: 'Base properties',
					value: 'text',
					nillable: true,
					isNull: false
				}
			}
		});
		const model = createDatabaseObjectProperties();
		await model.onSelectionChange({ selectedValue: ['Project.sq:Sequence.vr:v'] });
		const row = model.properties[0];
		row.isNull = true;
		row.value = '';
		expect(model.hasChanges).toBe(true);
		model.cancel();
		expect(row.isNull).toBe(false);
		expect(row.value).toBe('text');
		row.isNull = true;
		vi.mocked(call).mockResolvedValueOnce({ done: true });
		expect(await model.save({ persist: false })).toBe(true);
		expect(JSON.parse(vi.mocked(call).mock.calls.at(-1)?.[1]?.props ?? '[]')[0]).toMatchObject({
			isNull: true
		});
		expect(model.hasChanges).toBe(false);
	});

	it('offers to create the undefined global symbols of the saved properties', async () => {
		vi.mocked(call).mockResolvedValueOnce(propertiesResponse('before'));
		const model = createDatabaseObjectProperties();
		await model.onSelectionChange({ selectedValue: ['Project.cn:Http'] });
		model.properties[0].value = '${server}';
		vi.mocked(call).mockResolvedValueOnce({ done: true, undefinedSymbols: ['server'] });
		vi.mocked(call).mockResolvedValueOnce({ symbols: ['server'] });
		const confirm = vi.fn(() => true);
		vi.stubGlobal('window', { confirm });
		try {
			expect(await model.save({ persist: false })).toBe(true);
		} finally {
			vi.unstubAllGlobals();
		}
		expect(confirm).toHaveBeenCalledWith(expect.stringContaining('server is undefined'));
		expect(call).toHaveBeenLastCalledWith('studio.project.DeclareSymbols', {
			projectName: 'Project',
			symbols: '["server"]'
		});
	});

	it('acknowledges only submitted values and keeps the original target during a slow save', async () => {
		vi.mocked(call).mockResolvedValueOnce(propertiesResponse('before'));
		const model = createDatabaseObjectProperties();
		await model.onSelectionChange({ selectedValue: ['Project.first'] });
		const row = model.properties[0];
		row.value = 'submitted';
		const pending = deferred();
		vi.mocked(call).mockReturnValueOnce(pending.promise);
		const onSaved = vi.fn();
		const saving = model.save({ persist: false, onSaved });
		row.value = 'newer edit';
		vi.mocked(call).mockResolvedValueOnce(propertiesResponse('second'));
		await model.onSelectionChange({ selectedValue: ['Project.second'] });
		const result = { done: true, id: 'Project.renamed' };
		pending.resolve(result);
		await saving;
		expect(row.originalValue).toBe('submitted');
		expect(row.value).toBe('newer edit');
		expect(model.id).toBe('Project.second');
		expect(model.properties[0].value).toBe('second');
		expect(onSaved).toHaveBeenCalledWith('Project.first', result);
	});

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
