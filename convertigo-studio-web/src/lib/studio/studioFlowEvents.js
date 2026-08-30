/**
 * @param {ReturnType<typeof import('$lib/admin/adminEvents').parseAdminEvent>} event
 * @param {string} selectedProject
 * @returns {{ projectName: string, url: string, mode: 'development' | 'production' } | null}
 */
export function flowBrowserPreview(event, selectedProject) {
	if (event?.topic !== 'flow.browser.open') return null;
	const projectName = String(event.payload.project ?? '');
	const url = String(event.payload.url ?? '');
	if (!projectName || projectName !== selectedProject || !url) return null;
	const mode = /** @type {'development' | 'production'} */ (
		String(event.payload.kind ?? '') === 'frontbuilder.svelte.dev' ? 'development' : 'production'
	);
	return { projectName, url, mode };
}

/**
 * @param {ReturnType<typeof import('$lib/admin/adminEvents').parseAdminEvent>} event
 * @param {string} selectedProject
 */
export function flowSourceReveal(event, selectedProject) {
	if (event?.topic !== 'flow.source.changed' || event.payload.reveal !== true) return '';
	const projectName = String(event.payload.project ?? '');
	if (!projectName || projectName !== selectedProject) return '';
	return String(event.payload.sourcePath ?? '').trim();
}
