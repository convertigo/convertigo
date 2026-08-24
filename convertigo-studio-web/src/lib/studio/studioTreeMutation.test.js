import { describe, expect, it } from 'vitest';
import { applyProjectedTreeMutation } from './studioTreeMutation';

const idsEqual = (left, right) => left === right;

describe('Studio projected tree mutations', () => {
	it('inserts a confirmed palette block before the requested sibling', () => {
		const card = {
			id: 'Project.frontends.home.card',
			children: [
				{ id: 'Project.frontends.home.card.badge', label: 'Badge' },
				{ id: 'Project.frontends.home.card.title', label: 'Title' }
			]
		};
		const updated = applyProjectedTreeMutation(
			[card],
			{
				done: true,
				selectedId: 'Project.frontends.home.card.spinner',
				selectionId: 'spinner',
				selectionSourcePath: '/workspace/project/+page.flow.svelte',
				parentId: card.id,
				target: 'Project.frontends.home.card.title',
				position: 'before',
				payload: {
					type: 'paletteData',
					data: { name: 'Spinner', insert: { label: 'Loading' }, iconFile16: 'spinner.svg' }
				}
			},
			idsEqual
		);

		expect(updated).toBe(true);
		expect(card.children.map((node) => node.id)).toEqual([
			'Project.frontends.home.card.badge',
			'Project.frontends.home.card.spinner',
			'Project.frontends.home.card.title'
		]);
		expect(card.children[1]).toMatchObject({ label: 'Loading', icon: 'spinner.svg' });
	});

	it('reorders an existing projected node after its sibling', () => {
		const spinner = { id: 'Project.frontends.home.card.spinner', label: 'Loading' };
		const card = {
			id: 'Project.frontends.home.card',
			children: [spinner, { id: 'Project.frontends.home.card.title', label: 'Title' }]
		};
		const updated = applyProjectedTreeMutation(
			[card],
			{
				done: true,
				selectedId: spinner.id,
				selectionSourcePath: '/workspace/project/+page.flow.svelte',
				parentId: card.id,
				target: 'Project.frontends.home.card.title',
				position: 'after',
				payload: { type: 'treeData', data: { id: spinner.id } }
			},
			idsEqual
		);

		expect(updated).toBe(true);
		expect(card.children.map((node) => node.label)).toEqual(['Title', 'Loading']);
		expect(card.children[1]).toBe(spinner);
	});

	it('leaves non-projected and unloaded parents untouched', () => {
		const roots = [{ id: 'Project.frontends.home', children: true }];
		expect(
			applyProjectedTreeMutation(
				roots,
				{
					done: true,
					parentId: 'Project.frontends.home.card',
					payload: { type: 'paletteData', data: { name: 'Text' } }
				},
				idsEqual
			)
		).toBe(false);
	});

	it('accepts the loaded target parent as the mutation root', () => {
		const card = {
			id: 'Project.frontends.home.card',
			children: [{ id: 'Project.frontends.home.card.title', label: 'Title' }]
		};
		expect(
			applyProjectedTreeMutation(
				[card],
				{
					done: true,
					selectedId: 'Project.frontends.home.card.text',
					selectionSourcePath: '/workspace/project/+page.flow.svelte',
					parentId: card.id,
					target: card.children[0].id,
					position: 'before',
					payload: {
						type: 'paletteData',
						data: { name: 'Text', insert: { label: 'New text' } }
					}
				},
				idsEqual
			)
		).toBe(true);
		expect(card.children.map((node) => node.label)).toEqual(['New text', 'Title']);
	});
});
