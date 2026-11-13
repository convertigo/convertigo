import { describe, expect, it } from 'vitest';
import { elementsOf } from './ngxAuthoring';

/**
 * @param {string} name
 * @param {{ hiddenPage?: boolean, overlay?: boolean }} [options]
 */
function element(name, { hiddenPage = false, overlay = false } = {}) {
	return {
		name,
		getAttribute: (/** @type {string} */ attribute) =>
			attribute === 'data-c8o-authoring' && overlay ? '' : null,
		closest: (/** @type {string} */ selector) =>
			selector === '.ion-page-hidden' && hiddenPage ? {} : null
	};
}

/** @param {Record<string, any[]>} byClass */
function documentOf(byClass) {
	return /** @type {any} */ ({ getElementsByClassName: (name) => byClass[name] ?? [] });
}

describe('NGX preview authoring', () => {
	it('finds the elements of the component, else of its first parent shown', () => {
		const text = element('text');
		const doc = documentOf({ class2: [text], class3: [element('content')] });
		expect(elementsOf(doc, ['class1', 'class2', 'class3'])).toEqual([text]);
		expect(elementsOf(doc, ['class4'])).toEqual([]);
	});

	it('does not take a page the application left, which Ionic keeps hidden, for a page shown', () => {
		// the component of the login page is new: its page, left for the home page, still holds its parent
		const doc = documentOf({
			classContent: [element('login content', { hiddenPage: true })],
			classPage: [element('login page', { hiddenPage: true })]
		});
		expect(elementsOf(doc, ['classText', 'classContent', 'classPage'])).toEqual([]);
	});

	it('ignores the overlays of the Studio', () => {
		const doc = documentOf({ class1: [element('overlay', { overlay: true })] });
		expect(elementsOf(doc, ['class1'])).toEqual([]);
	});
});
