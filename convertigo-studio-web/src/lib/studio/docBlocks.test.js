import { describe, expect, it } from 'vitest';
import { documentationBlocks, documentationSegments, isMarkdown, parseHtml } from './docBlocks.js';

describe('docBlocks', () => {
	it('keeps the links, the code blocks and the emphasis of the HTML of the beans', () => {
		const blocks = documentationBlocks(
			'<p>See <a href="https://daneden.github.io/animate.css/">animate.css</a>, <u>really</u>.</p>' +
				'<pre>&lt;ion-list&gt;\n  &lt;ion-item&gt;</pre><ul><li><i>Comment</i></br>Describes it</li></ul>'
		);
		expect(blocks.map((block) => block.type)).toEqual(['paragraph', 'code-block', 'list']);
		expect(blocks[0].segments).toEqual([
			{ type: 'text', text: 'See ' },
			{ type: 'link', text: 'animate.css', href: 'https://daneden.github.io/animate.css/' },
			{ type: 'text', text: ', ' },
			{ type: 'underline', text: 'really' },
			{ type: 'text', text: '.' }
		]);
		expect(blocks[1].text).toBe('<ion-list>\n  <ion-item>');
		expect(blocks[2].items[0].segments.map((segment) => segment.type)).toEqual([
			'emphasis',
			'break',
			'text'
		]);
	});

	it('renders Markdown with the HTML it holds', () => {
		const source =
			'Lightweight **select**\n\nFeatures :\n- [x] Custom <b>binding</b>\n- A <a href="https://ng-select.github.io">demo</a><br>here\n\n| A | B |\n|---|---|\n| 1 | `2` |';
		expect(isMarkdown(source)).toBe(true);
		const blocks = documentationBlocks(source);
		expect(blocks.map((block) => block.type)).toEqual(['paragraph', 'paragraph', 'list', 'table']);
		expect(blocks[0].segments).toContainEqual({ type: 'strong', text: 'select' });
		expect(blocks[2].items[0].segments).toContainEqual({ type: 'strong', text: 'binding' });
		expect(blocks[2].items[1].segments).toContainEqual({
			type: 'link',
			text: 'demo',
			href: 'https://ng-select.github.io'
		});
		expect(blocks[3].rows[0].header).toBe(true);
		expect(blocks[3].rows[1].cells[1]).toEqual([{ type: 'code', text: '2' }]);
	});

	it('keeps no script nor link of another scheme', () => {
		const blocks = documentationBlocks(
			'<p>A <a href="javascript:alert(1)">trap</a><script>alert(2)</script><img src=x onerror=alert(3) alt="picture"></p>'
		);
		expect(blocks[0].segments).toEqual([{ type: 'text', text: 'A trappicture' }]);
	});

	it('closes the paragraphs and items left open', () => {
		const nodes = parseHtml('<p>one<p>two<ul><li>a<li>b</ul>');
		expect(nodes.map((node) => (node.type === 'element' ? node.tag : 'text'))).toEqual([
			'p',
			'p',
			'ul'
		]);
		expect(documentationBlocks('<ul><li>a<li>b</ul>')[0].items).toHaveLength(2);
	});

	it('gives the segments of a short documentation', () => {
		expect(documentationSegments('Defines a <b>Page</b>')).toEqual([
			{ type: 'text', text: 'Defines a ' },
			{ type: 'strong', text: 'Page' }
		]);
	});
});
