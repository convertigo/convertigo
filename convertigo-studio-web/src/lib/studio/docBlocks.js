import { marked } from 'marked';

/**
 * The documentation of the Doc view as blocks the view renders itself, never as HTML: the HTML of the
 * descriptions of the beans, and the Markdown of the comments with the HTML they hold, links, code
 * blocks, tables and emphasis included.
 */

const VOID = new Set(['br', 'img', 'hr', 'wbr', 'input', 'meta', 'link', 'col', 'source']);
const BLOCK = new Set([
	'p',
	'div',
	'section',
	'article',
	'header',
	'footer',
	'h1',
	'h2',
	'h3',
	'h4',
	'h5',
	'h6',
	'ul',
	'ol',
	'pre',
	'blockquote',
	'table',
	'hr',
	'dl'
]);
const CONTAINERS = new Set(['ul', 'ol', 'li', 'td', 'th', 'blockquote', 'div', 'table', 'pre']);
const LISTS = new Set(['ul', 'ol']);
const SKIPPED = new Set(['script', 'style', 'iframe', 'object', 'embed', 'template', 'head']);
const ENTITIES = {
	amp: '&',
	lt: '<',
	gt: '>',
	quot: '"',
	apos: "'",
	nbsp: ' ',
	copy: '©',
	reg: '®'
};

/**
 * @typedef {{ type: 'text', text: string } | { type: 'element', tag: string, attrs: Record<string, string>, children: HtmlNode[] }} HtmlNode
 */

/**
 * @param {string} text
 * @returns {string}
 */
export function decodeEntities(text) {
	return text.replace(/&(#x[\da-f]+|#\d+|[a-z]+);/gi, (entity, code) => {
		if (code[0] === '#') {
			const value =
				code[1] === 'x' || code[1] === 'X' ? parseInt(code.slice(2), 16) : Number(code.slice(1));
			return Number.isFinite(value) ? String.fromCodePoint(value) : entity;
		}
		return ENTITIES[code.toLowerCase()] ?? entity;
	});
}

/**
 * A tolerant parser of the HTML of the documentations, which closes the elements left open.
 * @param {string} html
 * @returns {HtmlNode[]}
 */
export function parseHtml(html) {
	/** @type {HtmlNode & { type: 'element' }} */
	const root = { type: 'element', tag: '#root', attrs: {}, children: [] };
	const stack = [root];
	const tokens =
		/<!--[\s\S]*?-->|<\/\s*([a-z][\w-]*)\s*>|<([a-z][\w-]*)((?:\s+[^\s=>/]+(?:\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]+))?)*)\s*(\/?)>/gi;
	let index = 0;
	/**
	 * Closes the element of the tag still open inside the nearest container.
	 * @param {string} tag
	 * @param {Set<string>} containers
	 */
	const closeOpen = (tag, containers) => {
		for (let at = stack.length - 1; at > 0; at--) {
			if (stack[at].tag === tag) {
				stack.length = at;
				return;
			}
			if (containers.has(stack[at].tag)) {
				return;
			}
		}
	};
	const text = (/** @type {string} */ value) => {
		if (value) {
			stack.at(-1)?.children.push({ type: 'text', text: decodeEntities(value) });
		}
	};
	for (let match = tokens.exec(html); match; match = tokens.exec(html)) {
		text(html.slice(index, match.index));
		index = tokens.lastIndex;
		if (match[0].startsWith('<!--')) {
			continue;
		}
		if (match[1]) {
			const tag = match[1].toLowerCase();
			if (tag === 'br') {
				// the </br> of the old descriptions
				stack.at(-1)?.children.push({ type: 'element', tag: 'br', attrs: {}, children: [] });
				continue;
			}
			const at = stack.map((node) => node.tag).lastIndexOf(tag);
			if (at > 0) {
				stack.length = at;
			}
			continue;
		}
		const tag = match[2].toLowerCase();
		/** @type {Record<string, string>} */
		const attrs = {};
		for (const attr of match[3].matchAll(
			/([^\s=>/]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+)))?/g
		)) {
			attrs[attr[1].toLowerCase()] = decodeEntities(attr[2] ?? attr[3] ?? attr[4] ?? '');
		}
		/** @type {HtmlNode & { type: 'element' }} */
		const node = { type: 'element', tag, attrs, children: [] };
		// a block closes the paragraph still open, an item the item, as a browser does
		if (BLOCK.has(tag) || tag === 'li') {
			closeOpen('p', CONTAINERS);
		}
		if (tag === 'li') {
			closeOpen('li', LISTS);
		}
		stack.at(-1)?.children.push(node);
		if (!VOID.has(tag) && !match[4]) {
			stack.push(node);
		}
	}
	text(html.slice(index));
	return root.children;
}

/**
 * @param {string} source
 * @returns {boolean} whether the documentation is Markdown, with no block of HTML
 */
export function isMarkdown(source) {
	return !/<(p|div|ul|ol|li|h[1-6]|pre|blockquote|table)\b/i.test(source);
}

/**
 * @param {string} source
 * @returns {string} the HTML of Markdown, the HTML it holds kept
 */
export function markdownToHtml(source) {
	return String(marked.parse(source, { gfm: true, breaks: true, async: false }));
}

/**
 * @param {HtmlNode[]} nodes
 * @param {boolean} [pre] whether the spaces are kept
 * @returns {string}
 */
function textOf(nodes, pre = false) {
	let text = '';
	for (const node of nodes) {
		if (node.type === 'text') {
			text += node.text;
		} else if (node.tag === 'br') {
			text += '\n';
		} else if (!SKIPPED.has(node.tag)) {
			text += textOf(node.children, pre);
			if (pre && (node.tag === 'p' || node.tag === 'div' || node.tag === 'li')) {
				text += '\n';
			}
		}
	}
	return pre ? text : text.replace(/[ \t\r\n\f]+/g, ' ');
}

/**
 * @param {string} href
 * @returns {string} the address of a link the view opens, empty for another scheme
 */
function safeHref(href) {
	return /^(https?:|mailto:)/i.test(href.trim()) ? href.trim() : '';
}

/**
 * @param {HtmlNode[]} nodes
 * @returns {any[]} the inline segments of nodes
 */
export function inlineSegments(nodes) {
	/** @type {any[]} */
	const segments = [];
	const push = (/** @type {any} */ segment) => {
		const last = segments.at(-1);
		if (segment.type === 'text' && last?.type === 'text') {
			last.text += segment.text;
		} else {
			segments.push(segment);
		}
	};
	for (const node of nodes) {
		if (node.type === 'text') {
			push({ type: 'text', text: node.text.replace(/[ \t\r\n\f]+/g, ' ') });
			continue;
		}
		const tag = node.tag;
		if (SKIPPED.has(tag)) continue;
		if (tag === 'br') {
			segments.push({ type: 'break' });
		} else if (tag === 'code' || tag === 'kbd' || tag === 'tt') {
			segments.push({ type: 'code', text: textOf(node.children) });
		} else if (tag === 'strong' || tag === 'b') {
			segments.push({ type: 'strong', text: textOf(node.children) });
		} else if (tag === 'em' || tag === 'i' || tag === 'cite') {
			segments.push({ type: 'emphasis', text: textOf(node.children) });
		} else if (tag === 'u' || tag === 'ins') {
			segments.push({ type: 'underline', text: textOf(node.children) });
		} else if (tag === 's' || tag === 'del' || tag === 'strike') {
			segments.push({ type: 'strike', text: textOf(node.children) });
		} else if (tag === 'a') {
			const href = safeHref(node.attrs.href ?? '');
			const text = textOf(node.children).trim() || href;
			if (href) {
				segments.push({ type: 'link', text, href });
			} else {
				push({ type: 'text', text });
			}
		} else if (tag === 'img') {
			if (node.attrs.alt) push({ type: 'text', text: node.attrs.alt });
		} else if (tag === 'input' && node.attrs.type === 'checkbox') {
			// the task lists of Markdown
			push({ type: 'text', text: 'checked' in node.attrs ? '☑ ' : '☐ ' });
		} else {
			for (const segment of inlineSegments(node.children)) push(segment);
		}
	}
	return segments;
}

/**
 * @param {any[]} segments
 * @returns {any[]} the segments without their spaces at the edges, or none when they are blank
 */
function trimSegments(segments) {
	const result = segments.map((segment) => ({ ...segment }));
	while (result[0]?.type === 'break') result.shift();
	while (result.at(-1)?.type === 'break') result.pop();
	if (result[0]?.type === 'text') result[0].text = result[0].text.replace(/^\s+/, '');
	const last = result.at(-1);
	if (last?.type === 'text') last.text = last.text.replace(/\s+$/, '');
	const kept = result.filter((segment) => segment.type === 'break' || segment.text);
	return kept.some((segment) => segment.type !== 'break' && segment.text.trim()) ? kept : [];
}

/**
 * @param {HtmlNode[]} nodes
 * @returns {any[]} the blocks of nodes
 */
export function nodesToBlocks(nodes) {
	/** @type {any[]} */
	const blocks = [];
	/** @type {HtmlNode[]} */
	let inline = [];
	const flush = () => {
		const segments = trimSegments(inlineSegments(inline));
		if (segments.length) {
			blocks.push({ type: 'paragraph', className: '', segments });
		}
		inline = [];
	};
	for (const node of nodes) {
		if (node.type === 'text' || !BLOCK.has(node.tag)) {
			inline.push(node);
			continue;
		}
		flush();
		const tag = node.tag;
		if (/^h[1-6]$/.test(tag)) {
			const segments = trimSegments(inlineSegments(node.children));
			if (segments.length) blocks.push({ type: 'heading', segments });
		} else if (tag === 'p') {
			const segments = trimSegments(inlineSegments(node.children));
			if (segments.length) {
				blocks.push({ type: 'paragraph', className: node.attrs.class ?? '', segments });
			}
		} else if (tag === 'ul' || tag === 'ol') {
			const items = listItems(node);
			if (items.length) blocks.push({ type: tag === 'ol' ? 'ordered-list' : 'list', items });
		} else if (tag === 'pre') {
			const text = textOf(node.children, true).replace(/^\n+|\s+$/g, '');
			if (text) blocks.push({ type: 'code-block', text });
		} else if (tag === 'blockquote') {
			const children = nodesToBlocks(node.children);
			if (children.length) {
				blocks.push({
					type: /\bdoc-note\b/.test(node.attrs.class ?? '') ? 'note' : 'quote',
					blocks: children
				});
			}
		} else if (tag === 'table') {
			const rows = tableRows(node);
			if (rows.length) blocks.push({ type: 'table', rows });
		} else if (tag === 'hr') {
			blocks.push({ type: 'rule' });
		} else {
			blocks.push(...nodesToBlocks(node.children));
		}
	}
	flush();
	return blocks;
}

/**
 * @param {HtmlNode & { type: 'element' }} list
 * @returns {{ segments: any[], blocks: any[] }[]}
 */
function listItems(list) {
	return list.children
		.filter((child) => child.type === 'element' && child.tag === 'li')
		.map((item) => {
			const children = /** @type {HtmlNode & { type: 'element' }} */ (item).children;
			const nested = children.filter(
				(child) => child.type === 'element' && BLOCK.has(child.tag) && child.tag !== 'p'
			);
			const own = children.filter((child) => !nested.includes(child));
			const segments = trimSegments(
				inlineSegments(
					own.flatMap((child) =>
						child.type === 'element' && child.tag === 'p'
							? [...child.children, { type: 'element', tag: 'br', attrs: {}, children: [] }]
							: [child]
					)
				)
			);
			return { segments, blocks: nodesToBlocks(nested) };
		})
		.filter((item) => item.segments.length || item.blocks.length);
}

/**
 * @param {HtmlNode & { type: 'element' }} table
 * @returns {{ header: boolean, cells: any[][] }[]}
 */
function tableRows(table) {
	/** @type {{ header: boolean, cells: any[][] }[]} */
	const rows = [];
	const visit = (/** @type {HtmlNode} */ node) => {
		if (node.type !== 'element') return;
		if (node.tag === 'tr') {
			const cells = node.children.filter(
				(cell) => cell.type === 'element' && (cell.tag === 'td' || cell.tag === 'th')
			);
			rows.push({
				header:
					cells.length > 0 && cells.every((cell) => cell.type === 'element' && cell.tag === 'th'),
				cells: cells.map((cell) =>
					trimSegments(
						inlineSegments(/** @type {HtmlNode & { type: 'element' }} */ (cell).children)
					)
				)
			});
		} else {
			node.children.forEach(visit);
		}
	};
	table.children.forEach(visit);
	return rows.filter((row) => row.cells.some((cell) => cell.length));
}

/**
 * @param {string} source the HTML of a documentation, or its Markdown
 * @returns {any[]} its blocks
 */
export function documentationBlocks(source) {
	const text = String(source ?? '')
		.replace(/\r\n?/g, '\n')
		.trim();
	if (!text) {
		return [];
	}
	return nodesToBlocks(parseHtml(isMarkdown(text) ? markdownToHtml(text) : text));
}

/**
 * @param {string} source the HTML of a short documentation, or its Markdown
 * @returns {any[]} its inline segments, its paragraphs one after the other
 */
export function documentationSegments(source) {
	const blocks = documentationBlocks(source);
	/** @type {any[]} */
	const segments = [];
	for (const block of blocks) {
		if (segments.length) segments.push({ type: 'break' });
		segments.push(...(block.segments ?? []));
	}
	return segments;
}
