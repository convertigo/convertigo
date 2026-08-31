/**
 * @param {ReturnType<typeof import('$lib/admin/adminEvents').parseAdminEvent>} event
 * @returns {{ projectName: string, url: string, mode: 'development' | 'production' } | null}
 */
export function flowBrowserPreview(event) {
	if (event?.topic !== 'flow.browser.open') return null;
	const projectName = String(event.payload.project ?? '');
	const url = String(event.payload.url ?? '');
	if (!projectName || !url) return null;
	const mode = /** @type {'development' | 'production'} */ (
		String(event.payload.kind ?? '') === 'frontbuilder.svelte.dev' ? 'development' : 'production'
	);
	return { projectName, url, mode };
}

/**
 * @param {ReturnType<typeof import('$lib/admin/adminEvents').parseAdminEvent>} event
 * @returns {{ projectName: string, sourcePath: string } | null}
 */
export function flowSourceReveal(event) {
	if (event?.topic !== 'flow.source.changed' || event.payload.reveal !== true) return null;
	const projectName = String(event.payload.project ?? '');
	const sourcePath = String(event.payload.sourcePath ?? '').trim();
	if (!projectName || !sourcePath) return null;
	return { projectName, sourcePath };
}
