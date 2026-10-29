/** The picker pages a session has received, by revision: an unchanged one is not sent again. */
const EDITOR_PAGE_LIMIT = 4;
const editorPages = new Map();

/** @returns {string} the revisions of the pages this session holds, for the service to omit */
export function knownEditorRevisions() {
	return [...editorPages.keys()].join(',');
}

/**
 * Remembers the page of a response and returns it, or the page of its revision
 * when the service omitted an unchanged one.
 *
 * @param {{ html?: string, htmlRevision?: string } | null | undefined} response
 * @returns {string}
 */
export function editorPage(response) {
	const revision = String(response?.htmlRevision ?? '');
	const html = typeof response?.html === 'string' ? response.html : '';
	if (html) {
		if (revision) {
			editorPages.delete(revision);
			editorPages.set(revision, html);
			while (editorPages.size > EDITOR_PAGE_LIMIT) {
				editorPages.delete(editorPages.keys().next().value);
			}
		}
		return html;
	}
	const known = revision ? editorPages.get(revision) : undefined;
	if (known) {
		editorPages.delete(revision);
		editorPages.set(revision, known);
	}
	return known ?? '';
}

export function forgetEditorPages() {
	editorPages.clear();
}

/**
 * Answers a request an editor of the picker sends asynchronously: the paths of
 * a binding source the picker listed without them (pathsDeferred).
 *
 * @param {{ id?: string, propertyName?: string } | null | undefined} target
 * @param {any} request
 * @param {(service: string, data: Record<string, string>) => Promise<any>} callService
 * @returns {Promise<{ ok: boolean, bindingSources?: any[], error?: string }>}
 */
export async function pickerRequest(target, request, callService) {
	const name = String(request?.name ?? '');
	if (name !== 'bindingSourceDetail') {
		return { ok: false, error: `Unsupported Flow picker request: ${name}` };
	}
	const source = request?.payload?.source;
	if (!target?.id || !target?.propertyName || !source || typeof source !== 'object') {
		return { ok: false, error: 'The binding source request is incomplete' };
	}
	const response = await callService('studio.flowpicker.Get', {
		id: target.id,
		propertyName: String(request?.payload?.property || target.propertyName),
		bindingSource: JSON.stringify(source)
	});
	if (!Array.isArray(response?.bindingSources)) {
		return {
			ok: false,
			error: String(
				response?.error?.message ||
					response?.error ||
					response?.message ||
					'The binding source is not available'
			)
		};
	}
	return { ok: true, bindingSources: response.bindingSources };
}
