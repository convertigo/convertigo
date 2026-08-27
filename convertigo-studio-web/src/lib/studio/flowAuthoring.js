export const FLOW_AUTHORING_PROTOCOL = 'convertigo.flow.authoring.v1';
export const FLOW_AUTHORING_MODES = ['browse', 'select', 'move'];

/**
 * @typedef {{
 *  nodeId: string,
 *  sourceProject?: string,
 *  sourceRelativePath: string,
 *  sourceMutationPath: string
 * }} FlowAuthoringReference
 */

/**
 * @param {unknown} value
 * @returns {value is FlowAuthoringReference}
 */
export function isFlowAuthoringReference(value) {
	const candidate = /** @type {Record<string, unknown> | null} */ (
		value && typeof value === 'object' ? value : null
	);
	return Boolean(
		candidate &&
		typeof candidate.nodeId === 'string' &&
		typeof candidate.sourceRelativePath === 'string' &&
		candidate.sourceRelativePath &&
		typeof candidate.sourceMutationPath === 'string' &&
		candidate.sourceMutationPath
	);
}

/**
 * @param {unknown} value
 * @returns {value is { protocol: string, type: string, reference?: FlowAuthoringReference, source?: FlowAuthoringReference, mode?: unknown, position?: unknown, payload?: unknown, themeContext?: unknown }}
 */
export function isFlowAuthoringMessage(value) {
	const candidate = /** @type {Record<string, unknown> | null} */ (
		value && typeof value === 'object' ? value : null
	);
	return Boolean(
		candidate &&
		candidate.protocol === FLOW_AUTHORING_PROTOCOL &&
		typeof candidate.type === 'string'
	);
}

/**
 * @param {FlowAuthoringReference | null | undefined} reference
 */
export function highlightAuthoringMessage(reference) {
	return reference
		? {
				protocol: FLOW_AUTHORING_PROTOCOL,
				type: 'authoring.highlight',
				reference: {
					nodeId: reference.nodeId,
					...(reference.sourceProject ? { sourceProject: reference.sourceProject } : {}),
					sourceRelativePath: reference.sourceRelativePath,
					sourceMutationPath: reference.sourceMutationPath
				}
			}
		: { protocol: FLOW_AUTHORING_PROTOCOL, type: 'authoring.highlight.clear' };
}

/**
 * @param {'browse' | 'select' | 'move'} mode
 */
export function authoringModeMessage(mode) {
	return {
		protocol: FLOW_AUTHORING_PROTOCOL,
		type: 'authoring.mode.set',
		mode: FLOW_AUTHORING_MODES.includes(mode) ? mode : 'browse'
	};
}

export function themeContextRequestMessage() {
	return { protocol: FLOW_AUTHORING_PROTOCOL, type: 'viewer.theme.request' };
}

/**
 * @param {unknown} message
 * @returns {{ mode: string, palette: string, tokens: { value: string, label: string, cssVariable: string, light: string, dark: string, current: string }[] } | null}
 */
export function themeContextFromMessage(message) {
	if (!isFlowAuthoringMessage(message) || message.type !== 'viewer.theme') {
		return null;
	}
	const context = /** @type {Record<string, unknown> | null} */ (
		message.themeContext && typeof message.themeContext === 'object' ? message.themeContext : null
	);
	const tokens = Array.isArray(context?.tokens)
		? context.tokens.filter(
				(token) =>
					token &&
					typeof token === 'object' &&
					typeof token.value === 'string' &&
					typeof token.light === 'string' &&
					typeof token.dark === 'string'
			)
		: [];
	if (!context || !tokens.length) {
		return null;
	}
	return {
		mode: String(context.mode ?? ''),
		palette: String(context.palette ?? ''),
		tokens: /** @type {any} */ (tokens)
	};
}

/**
 * @param {unknown} message
 * @returns {'browse' | 'select' | 'move' | null}
 */
export function authoringModeFromMessage(message) {
	if (
		!isFlowAuthoringMessage(message) ||
		message.type !== 'authoring.mode.changed' ||
		typeof message.mode !== 'string' ||
		!FLOW_AUTHORING_MODES.includes(message.mode)
	) {
		return null;
	}
	return /** @type {'browse' | 'select' | 'move'} */ (message.mode);
}

/**
 * Tree nodes backed by generated frontend AST references can be revealed in the
 * development viewer. Other Studio nodes deliberately keep the local action hidden.
 * @param {unknown} id
 */
export function isFrontendAuthoringNodeId(id) {
	const value = String(id ?? '');
	return value.includes('.FlowEngine.frontends.') && value.includes('.authoring_');
}

/**
 * @param {unknown} message
 * @returns {{ reference: FlowAuthoringReference, position: 'before' | 'inside' | 'after', payload: import('./dnd').DboDragPayload } | null}
 */
export function authoringDropRequest(message) {
	const payload = /** @type {Record<string, unknown> | null} */ (
		isFlowAuthoringMessage(message) && message.payload && typeof message.payload === 'object'
			? message.payload
			: null
	);
	if (
		!isFlowAuthoringMessage(message) ||
		message.type !== 'authoring.drop' ||
		!isFlowAuthoringReference(message.reference) ||
		!['before', 'inside', 'after'].includes(String(message.position ?? '')) ||
		!payload ||
		!['paletteData', 'treeData'].includes(String(payload.type ?? ''))
	) {
		return null;
	}
	return {
		reference: message.reference,
		position: /** @type {'before' | 'inside' | 'after'} */ (message.position),
		payload: /** @type {import('./dnd').DboDragPayload} */ (payload)
	};
}

/**
 * @param {unknown} message
 * @returns {{ source: FlowAuthoringReference, reference: FlowAuthoringReference, position: 'before' | 'inside' | 'after' } | null}
 */
export function authoringMoveRequest(message) {
	if (
		!isFlowAuthoringMessage(message) ||
		message.type !== 'authoring.move' ||
		!isFlowAuthoringReference(message.source) ||
		!isFlowAuthoringReference(message.reference) ||
		!['before', 'inside', 'after'].includes(String(message.position ?? ''))
	) {
		return null;
	}
	return {
		source: message.source,
		reference: message.reference,
		position: /** @type {'before' | 'inside' | 'after'} */ (message.position)
	};
}

/**
 * @param {unknown} message
 * @returns {FlowAuthoringReference | null}
 */
export function selectedAuthoringReference(message) {
	if (
		!isFlowAuthoringMessage(message) ||
		!['authoring.select', 'authoring.reveal'].includes(message.type) ||
		!isFlowAuthoringReference(message.reference)
	) {
		return null;
	}
	return message.reference;
}
