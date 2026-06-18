import { call } from '$lib/utils/service';

export const SOURCE_PICKER_DND_TYPE = 'sourcePickerData';
export const SOURCE_PICKER_DATA_TRANSFER = 'sourcepickerdata';

/**
 * @typedef {{
 *  type: typeof SOURCE_PICKER_DND_TYPE,
 *  data: {
 *   ownerId?: string,
 *   sourceId?: string,
 *   sourceName?: string,
 *   sourcePriority: string,
 *   xpath: string,
 *   displayXpath?: string
 *  }
 * }} SourcePickerDragPayload
 */

/**
 * @param {any} payload
 * @returns {payload is SourcePickerDragPayload}
 */
export function isSourcePickerPayload(payload) {
	return Boolean(
		payload?.type === SOURCE_PICKER_DND_TYPE &&
		payload?.data?.sourcePriority &&
		typeof payload?.data?.xpath === 'string'
	);
}

/**
 * @param {DragEvent} event
 * @param {any} fallback
 * @returns {SourcePickerDragPayload | undefined}
 */
export function getSourcePickerDragPayload(event, fallback) {
	if (isSourcePickerPayload(fallback)) {
		return fallback;
	}
	const raw =
		event.dataTransfer?.getData(SOURCE_PICKER_DATA_TRANSFER) ||
		event.dataTransfer?.getData('text/plain');
	if (!raw) {
		return undefined;
	}
	try {
		const payload = JSON.parse(raw);
		return isSourcePickerPayload(payload) ? payload : undefined;
	} catch {
		return undefined;
	}
}

/**
 * @param {DragEvent} event
 * @param {SourcePickerDragPayload} payload
 */
export function setSourcePickerDragData(event, payload) {
	const serialized = JSON.stringify(payload);
	event.dataTransfer?.setData(SOURCE_PICKER_DATA_TRANSFER, serialized);
	event.dataTransfer?.setData('text/plain', serialized);
	if (event.dataTransfer) {
		event.dataTransfer.effectAllowed = 'copy';
		event.dataTransfer.dropEffect = 'copy';
	}
}

/**
 * @param {SourcePickerDragPayload} payload
 * @returns {string[]}
 */
export function sourceDefinitionFromPayload(payload) {
	return [String(payload?.data?.sourcePriority ?? ''), String(payload?.data?.xpath ?? '.')];
}

/**
 * @param {string} targetId
 * @param {SourcePickerDragPayload} payload
 * @param {string=} propertyName
 * @returns {Promise<any>}
 */
export function applySourcePickerDrop(targetId, payload, propertyName = '') {
	return call('studio.sourcepicker.Apply', {
		id: targetId,
		sourcePriority: payload?.data?.sourcePriority ?? '',
		xpath: payload?.data?.xpath ?? '.',
		propertyName
	});
}
