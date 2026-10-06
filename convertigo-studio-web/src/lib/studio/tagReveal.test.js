import { describe, expect, it } from 'vitest';
import { tagRevealRows } from './tagReveal.js';

/** A project shown in the tag groups A, B and C, holding an application with two pages */
function grouped() {
	const project = (group) => ({
		id: 'P',
		rowId: `tag-row:${group}/P`,
		children: [
			{
				id: 'P.app',
				children: [
					{
						id: 'P.app:pg',
						children: [
							{ id: 'P.app.pg:page1', children: true },
							{ id: 'P.app.pg:page2', children: true }
						]
					}
				]
			}
		]
	});
	return ['A', 'B', 'C'].map((group) => ({
		id: `tag-row:${group}`,
		rowId: `tag-row:${group}`,
		tagGroup: true,
		children: [project(group)]
	}));
}

/** @param {string[]} rows */
const expanded = (rows) => ({ isExpanded: (/** @type {string} */ row) => rows.includes(row) });

describe('tagRevealRows', () => {
	it('opens the occurrence already opened the deepest toward the target', () => {
		const rows = tagRevealRows(
			grouped(),
			'P.app.pg:page1.ui:button',
			expanded([
				'tag-row:A',
				'tag-row:B',
				'tag-row:C',
				'tag-row:A/P',
				'tag-row:B/P',
				'tag-row:C/P',
				'tag-row:A/P/P.app',
				'tag-row:B/P/P.app',
				'tag-row:B/P/P.app:pg',
				'tag-row:B/P/P.app.pg:page1'
			])
		);
		expect([...rows]).toEqual([
			'tag-row:B',
			'tag-row:B/P',
			'tag-row:B/P/P.app',
			'tag-row:B/P/P.app:pg',
			'tag-row:B/P/P.app.pg:page1'
		]);
	});

	it('only counts the rows opened toward the target', () => {
		const rows = tagRevealRows(
			grouped(),
			'P.app.pg:page1',
			expanded([
				'tag-row:A',
				'tag-row:B',
				'tag-row:A/P',
				'tag-row:B/P',
				'tag-row:B/P/P.app',
				'tag-row:B/P/P.app:pg',
				'tag-row:B/P/P.app.pg:page2',
				'tag-row:A/P/P.app'
			])
		);
		expect(rows.has('tag-row:B/P/P.app.pg:page1')).toBe(true);
	});

	it('opens the first occurrence on a tie', () => {
		const rows = tagRevealRows(grouped(), 'P.app.pg:page1', expanded([]));
		expect([...rows][0]).toBe('tag-row:A');
		expect(rows.has('tag-row:B')).toBe(false);
	});

	it('keeps the occurrence already showing the target', () => {
		const rows = tagRevealRows(
			grouped(),
			'P.app.pg:page1',
			expanded([
				'tag-row:A',
				'tag-row:A/P',
				'tag-row:C',
				'tag-row:C/P',
				'tag-row:C/P/P.app',
				'tag-row:C/P/P.app:pg'
			])
		);
		expect(rows.has('tag-row:C/P/P.app.pg:page1')).toBe(true);
		expect(rows.has('tag-row:A')).toBe(false);
	});

	it('follows a single path in a tree without tags', () => {
		const rows = tagRevealRows(
			[
				{ id: 'P', children: [{ id: 'P:sq', children: [{ id: 'P.sq:Main', children: true }] }] },
				{ id: 'Q', children: true }
			],
			'P.sq:Main.st:Init',
			expanded([])
		);
		expect([...rows]).toEqual(['P', 'P:sq', 'P.sq:Main']);
	});
});
