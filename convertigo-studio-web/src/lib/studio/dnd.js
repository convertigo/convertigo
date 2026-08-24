import { acceptDbo, addDbo, moveDbo } from '$lib/utils/service';

const SILENT_ERROR_OPTIONS = {
	silentError: () => true
};
const FOLDER_TYPE_IDS = new Set(['sq', 'cn', 'tr', 'st', 'vr', 'tc', 'ref', 'url', 'app', 'mob']);

/**
 * @typedef {'copy' | 'move' | 'none'} DropAction
 * @typedef {'inside' | 'first' | 'before' | 'after'} DropPosition
 * @typedef {{ type?: string, data?: { id?: string, classname?: string, type?: string, [key: string]: any }, options?: Record<string, unknown> }} DboDragPayload
 * @typedef {Object} DboDropResult
 * @property {boolean=} done
 * @property {boolean=} accept
 * @property {boolean=} refused
 * @property {boolean=} retried
 * @property {string=} id
 * @property {string=} selectedId
 * @property {string=} selectionId
 * @property {string=} target
 * @property {string=} parentId
 * @property {string=} previousParentId
 * @property {string=} selectionSourcePath
 * @property {string=} projectedSourcePath
 * @property {DropPosition=} position
 * @property {string=} source
 * @property {DboDragPayload=} payload
 * @property {string[]=} affectedParentIds
 */

/**
 * @param {DragEvent} event
 * @param {DboDragPayload | undefined} fallback
 * @returns {DboDragPayload | undefined}
 */
function getDboDragPayload(event, fallback) {
	if (fallback?.type) {
		return fallback;
	}
	const raw =
		event.dataTransfer?.getData('palettedata') ||
		event.dataTransfer?.getData('treedata') ||
		event.dataTransfer?.getData('text/plain');
	if (!raw) {
		return undefined;
	}
	try {
		const payload = JSON.parse(raw);
		return payload?.type ? payload : undefined;
	} catch {
		return undefined;
	}
}

/**
 * @param {DragEvent} event
 * @param {DboDragPayload | undefined} payload
 * @returns {DropAction}
 */
function getDboDropAction(event, payload) {
	if (!event.dataTransfer || !payload) {
		return 'none';
	}
	if (payload.type === 'paletteData' || event.dataTransfer.effectAllowed === 'copy') {
		event.dataTransfer.dropEffect = 'copy';
		return 'copy';
	}
	event.dataTransfer.dropEffect = event.ctrlKey ? 'copy' : 'move';
	const dropEffect = event.dataTransfer.dropEffect;
	if (dropEffect === 'copy' || dropEffect === 'move') {
		return dropEffect;
	}
	return event.ctrlKey ? 'copy' : 'move';
}

/**
 * @param {DboDragPayload} payload
 * @param {DropAction} dropAction
 * @returns {'copy' | 'move'}
 */
function getAcceptAction(payload, dropAction) {
	return payload.type === 'paletteData' || dropAction === 'copy' ? 'copy' : 'move';
}

/**
 * @param {DboDragPayload | undefined} payload
 * @param {string} target
 * @returns {boolean}
 */
function isSelfDrop(payload, target) {
	return Boolean(
		payload?.type === 'treeData' &&
		payload?.data?.id &&
		areEquivalentDboObjectIds(payload.data.id, target)
	);
}

/**
 * @param {DboDragPayload | undefined} payload
 * @param {string} target
 * @param {DropAction} dropAction
 * @returns {boolean}
 */
function isInvalidMoveDrop(payload, target, dropAction) {
	if (dropAction !== 'move' || payload?.type !== 'treeData' || !payload.data?.id || !target) {
		return false;
	}
	const source = payload.data.id;
	return areEquivalentDboObjectIds(source, target) || isDescendantObjectId(target, source);
}

/**
 * @param {DboDragPayload | undefined} payload
 * @param {string} target
 * @param {DropPosition} position
 * @param {DropAction} dropAction
 * @returns {boolean}
 */
function isSameParentReorder(payload, target, position, dropAction) {
	if (
		position === 'inside' ||
		dropAction !== 'move' ||
		payload?.type !== 'treeData' ||
		!payload.data?.id ||
		!target
	) {
		return false;
	}
	const sourceParent = parentObjectId(payload.data.id);
	const targetParent = parentObjectId(target);
	return Boolean(sourceParent && sourceParent === targetParent);
}

/**
 * @param {{
 *  payload?: DboDragPayload,
 *  target?: string,
 *  position?: DropPosition,
 *  sourceSiblingIds?: string[]
 * }} move
 * @returns {boolean}
 */
function isNoopSiblingMove(move) {
	const sourceId = move.payload?.type === 'treeData' ? (move.payload.data?.id ?? '') : '';
	const target = move.target ?? '';
	const position = move.position ?? 'inside';
	const siblings = move.sourceSiblingIds ?? [];
	if (!sourceId || !target || !siblings.length || position === 'inside') {
		return false;
	}
	const equivalentSourceIds = equivalentDboObjectIds(sourceId);
	const sourceIndex = siblings.findIndex((id) => equivalentSourceIds.includes(id));
	if (sourceIndex < 0) {
		return false;
	}
	if (position === 'first') {
		return sourceIndex === 0;
	}
	if (areEquivalentDboObjectIds(target, sourceId)) {
		return true;
	}
	if (position === 'before') {
		return areEquivalentDboObjectIds(siblings[sourceIndex + 1], target);
	}
	return areEquivalentDboObjectIds(siblings[sourceIndex - 1], target);
}

/**
 * @param {string} id
 * @returns {string}
 */
function parentObjectId(id) {
	if (!id) {
		return '';
	}
	const folderParent = id.match(/^(.*\.[a-z]{2,4}:[^:.]+):([a-z]{2,4})$/);
	if (folderParent?.[1] && FOLDER_TYPE_IDS.has(folderParent[2])) {
		return folderParent[1];
	}
	const topTypedObject = id.match(/^([^.:]+)\.([a-z]{2,4}):[^.]+$/);
	if (topTypedObject?.[1] && topTypedObject?.[2]) {
		return `${topTypedObject[1]}:${topTypedObject[2]}`;
	}
	const dotIndex = id.lastIndexOf('.');
	if (dotIndex > 0) {
		return id.slice(0, dotIndex);
	}
	return '';
}

/**
 * @param {string} candidateId
 * @param {string} parentId
 * @returns {boolean}
 */
function isDescendantObjectId(candidateId, parentId) {
	if (!candidateId || !parentId) {
		return false;
	}
	for (const candidate of equivalentDboObjectIds(candidateId)) {
		for (const parent of equivalentDboObjectIds(parentId)) {
			if (
				candidate !== parent &&
				(candidate.startsWith(`${parent}.`) ||
					candidate.startsWith(`${parent}:`) ||
					candidate.startsWith(`${parent}/`))
			) {
				return true;
			}
		}
	}
	return false;
}

/**
 * Structured JSON/XML step children can be echoed either as regular step qnames
 * (`parent.st:name`) or source-container child qnames (`parent.name`).
 * @param {string | undefined} id
 * @returns {string[]}
 */
function equivalentDboObjectIds(id) {
	if (!id) {
		return [];
	}
	const ids = [id];
	const regularStepChild = id.match(/^(.*\.st:[^.]+)\.st:([^/]+)$/);
	if (regularStepChild?.[1] && regularStepChild?.[2]) {
		pushUnique(ids, `${regularStepChild[1]}.${regularStepChild[2]}`);
	}
	const structuredChild = id.match(/^(.*\.st:[^.]+)\.([^/]+)$/);
	if (!regularStepChild && structuredChild?.[1] && structuredChild?.[2]) {
		pushUnique(ids, `${structuredChild[1]}.st:${structuredChild[2]}`);
	}
	return ids;
}

/**
 * @param {string | undefined} left
 * @param {string | undefined} right
 * @returns {boolean}
 */
function areEquivalentDboObjectIds(left, right) {
	if (!left || !right) {
		return false;
	}
	return equivalentDboObjectIds(left).some((id) => equivalentDboObjectIds(right).includes(id));
}

/**
 * @param {{ payload?: DboDragPayload, target?: string, position?: DropPosition, dropAction?: DropAction }} drop
 * @returns {Promise<boolean>}
 */
async function canDropDbo(drop) {
	const payload = drop.payload;
	const target = drop.target ?? '';
	const position = drop.position ?? 'inside';
	const dropAction = drop.dropAction ?? 'none';
	if (
		!payload ||
		!target ||
		dropAction === 'none' ||
		isSelfDrop(payload, target) ||
		isInvalidMoveDrop(payload, target, dropAction)
	) {
		return false;
	}
	if (isSameParentReorder(payload, target, position, dropAction)) {
		return true;
	}
	try {
		const result = await acceptDbo(getAcceptAction(payload, dropAction), target, position, payload);
		return Boolean(result?.accept);
	} catch {
		return false;
	}
}

/**
 * @param {{ payload: DboDragPayload, target: string, position: DropPosition, dropAction: DropAction, silent?: boolean }} drop
 * @returns {Promise<DboDropResult>}
 */
async function tryDboDrop(drop) {
	const accepted = await canDropDbo(drop);
	if (!accepted) {
		return {
			done: false,
			refused: true,
			target: drop.target,
			position: drop.position,
			payload: drop.payload
		};
	}
	const action =
		drop.payload.type === 'paletteData' || drop.dropAction === 'copy' ? 'copy' : 'move';
	const options = drop.silent ? SILENT_ERROR_OPTIONS : {};
	const result =
		action === 'copy'
			? await addDbo(drop.target, drop.position, drop.payload, options)
			: await moveDbo(drop.target, drop.position, drop.payload, options);
	return normalizeDropResult(result, drop);
}

/**
 * @param {any} result
 * @param {{ payload: DboDragPayload, target: string, position: DropPosition, dropAction?: DropAction }} drop
 * @returns {DboDropResult}
 */
function normalizeDropResult(result, drop) {
	const selectedId = inferDropSelectedId(result, drop);
	const parentId = result?.parentId || inferDropParentId(drop);
	const previousParentId = result?.previousParentId || inferDropPreviousParentId(drop);
	return {
		...(result ?? {}),
		selectedId,
		target: drop.target,
		parentId,
		previousParentId,
		position: drop.position,
		payload: drop.payload,
		affectedParentIds: affectedDboParentIds({
			...(result ?? {}),
			selectedId,
			target: drop.target,
			parentId,
			previousParentId,
			position: drop.position,
			payload: drop.payload
		})
	};
}

/**
 * @param {any} result
 * @param {{ payload: DboDragPayload, target: string, position: DropPosition }} drop
 * @returns {string | undefined}
 */
function inferDropSelectedId(result, drop) {
	if (typeof result?.id === 'string' && result.id) {
		return result.id;
	}
	if (drop.payload.type !== 'treeData') {
		return undefined;
	}
	return inferMovedObjectId(drop) || drop.payload.data?.id;
}

/**
 * @param {{ payload: DboDragPayload, target: string, position: DropPosition }} drop
 * @returns {string}
 */
function inferMovedObjectId(drop) {
	const sourceId = drop.payload.data?.id ?? '';
	const sourceName = objectNameFromId(sourceId);
	if (!sourceId || !sourceName) {
		return sourceId;
	}
	if (drop.position === 'inside') {
		return joinChildObjectId(drop.target, sourceName, sourceId, drop.payload.data?.classname ?? '');
	}
	const targetParentId = parentObjectId(drop.target);
	if (isStructuredStepChild(sourceId, targetParentId, drop.payload.data?.classname ?? '')) {
		return `${targetParentId}.${sourceName}`;
	}
	const separatorIndex = qnameSeparatorIndex(drop.target);
	return separatorIndex >= 0
		? `${drop.target.slice(0, separatorIndex + 1)}${sourceName}`
		: sourceId;
}

/**
 * @param {{ target: string, position: DropPosition }} drop
 * @returns {string}
 */
function inferDropParentId(drop) {
	return drop.position === 'inside' ? drop.target : parentObjectId(drop.target);
}

/**
 * @param {{ payload: DboDragPayload, dropAction?: DropAction }} drop
 * @returns {string | undefined}
 */
function inferDropPreviousParentId(drop) {
	if (drop.payload.type !== 'treeData' || drop.dropAction === 'copy') {
		return undefined;
	}
	return parentObjectId(drop.payload.data?.id ?? '') || undefined;
}

/**
 * Source-backed Flow widgets expose their value through properties/pickers;
 * their stable source id is not a regular DBO name to edit inline.
 * @param {DboDropResult | undefined} mutation
 * @returns {boolean}
 */
function shouldStartInlineRename(mutation) {
	return Boolean(
		mutation?.payload?.type === 'paletteData' &&
		mutation.source !== 'flow' &&
		(mutation.selectedId || mutation.id) &&
		!mutation.selectionSourcePath &&
		!mutation.projectedSourcePath
	);
}

/**
 * @param {string} parentId
 * @param {string} childName
 * @param {string} sourceId
 * @param {string=} sourceClassname
 * @returns {string}
 */
function joinChildObjectId(parentId, childName, sourceId = '', sourceClassname = '') {
	const folderPrefix = folderTargetPrefix(parentId);
	if (folderPrefix) {
		return `${folderPrefix}${childName}`;
	}
	if (isStructuredStepChild(sourceId, parentId, sourceClassname)) {
		return `${parentId}.${childName}`;
	}
	if (sourceId.includes('.st:')) {
		return `${parentId}.st:${childName}`;
	}
	return `${parentId}.${childName}`;
}

/**
 * JSON/XML field-like steps are rendered as sequence steps at the top level,
 * but when nested below their source container their qname uses `parent.child`
 * instead of `parent.st:child`.
 * @param {string} sourceId
 * @param {string} parentId
 * @param {string=} sourceClassname
 * @returns {boolean}
 */
function isStructuredStepChild(sourceId, parentId, sourceClassname = '') {
	if (!sourceId || !parentId || !parentId.includes('.st:')) {
		return false;
	}
	if (/\.(json|xml).+step$/i.test(sourceClassname)) {
		return true;
	}
	const sourceName = objectNameFromId(sourceId).toLowerCase();
	return sourceName.startsWith('"') || sourceName.startsWith('<');
}

/**
 * @param {string | undefined} id
 * @returns {string}
 */
function objectNameFromId(id) {
	if (!id) {
		return '';
	}
	const separatorIndex = qnameSeparatorIndex(id);
	return separatorIndex >= 0 ? id.slice(separatorIndex + 1) : id;
}

/**
 * @param {string | undefined} id
 * @param {string} name
 * @returns {string}
 */
function renameObjectId(id, name) {
	if (!id) {
		return name;
	}
	const separatorIndex = qnameSeparatorIndex(id);
	return separatorIndex >= 0 ? `${id.slice(0, separatorIndex + 1)}${name}` : name;
}

/**
 * @param {string} id
 * @returns {number}
 */
function qnameSeparatorIndex(id) {
	const typedSegment = /\.([a-z]{2,4}):/g;
	let typedSeparatorIndex = -1;
	let match;
	while ((match = typedSegment.exec(id))) {
		typedSeparatorIndex = match.index + match[0].length - 1;
	}
	if (typedSeparatorIndex >= 0) {
		const structuredChildIndex = id.lastIndexOf('.');
		return structuredChildIndex > typedSeparatorIndex ? structuredChildIndex : typedSeparatorIndex;
	}
	return Math.max(id.lastIndexOf(':'), id.lastIndexOf('.'));
}

/**
 * Tree folders use ids like `Project:sq` or `Project.sq:Seq:st`, while database
 * objects below them use qnames like `Project.sq:Seq.st:Step`.
 * @param {string} nodeId
 * @returns {string}
 */
function folderTargetPrefix(nodeId) {
	const match = nodeId.match(/^(.*):([^:.]+)$/);
	if (!match?.[1] || !match?.[2] || !FOLDER_TYPE_IDS.has(match[2])) {
		return '';
	}
	return `${match[1]}.${match[2]}:`;
}

/**
 * @param {{
 *  payload?: DboDragPayload,
 *  target?: string,
 *  position?: DropPosition,
 *  dropAction?: DropAction,
 *  fallbackTarget?: string,
 *  fallbackPosition?: DropPosition
 * }} drop
 * @returns {Promise<DboDropResult>}
 */
async function performDboDrop(drop) {
	const payload = drop.payload;
	const target = drop.target ?? '';
	const position = drop.position ?? 'inside';
	const dropAction = drop.dropAction ?? 'none';
	if (
		!payload ||
		!target ||
		dropAction === 'none' ||
		isSelfDrop(payload, target) ||
		isInvalidMoveDrop(payload, target, dropAction)
	) {
		return { done: false, refused: true, target, position, payload };
	}
	if (shouldComposePreciseDrop(drop)) {
		const preciseResult = await performComposedPreciseDrop({
			payload,
			target,
			position,
			dropAction,
			fallbackTarget: drop.fallbackTarget ?? '',
			fallbackPosition: drop.fallbackPosition ?? 'inside'
		});
		return preciseResult.done
			? { ...preciseResult, retried: true }
			: retargetFailedPreciseDropResult(preciseResult, {
					payload,
					target,
					position
				});
	}
	if (shouldRetryEquivalentPreciseTarget(drop)) {
		return await tryEquivalentPreciseTargetDrop({ payload, target, position, dropAction });
	}
	const canRetry = shouldRetryDrop(drop);
	const firstResult = await tryDboDrop({
		payload,
		target,
		position,
		dropAction,
		silent: canRetry
	});
	if (firstResult.done || !canRetry) {
		return firstResult;
	}
	const fallbackResult = await tryDboDrop({
		payload,
		target: drop.fallbackTarget ?? target,
		position: drop.fallbackPosition ?? 'after',
		dropAction
	});
	return fallbackResult.done ? { ...fallbackResult, retried: true } : firstResult;
}

/**
 * Returns whether the visual drop preview may rely on the fallback path that
 * performDboDrop will actually execute.
 * @param {{
 *  position?: DropPosition,
 *  payload?: DboDragPayload,
 *  target?: string,
 *  dropAction?: DropAction,
 *  fallbackTarget?: string,
 *  fallbackPosition?: DropPosition
 * }} drop
 * @returns {boolean}
 */
function canUseDboDropFallback(drop) {
	return shouldRetryDrop(drop) || shouldComposePreciseDrop(drop);
}

/**
 * Fallbacks are only for dropping on a node that cannot receive the child:
 * retrying an exact before/after placement as "inside parent" silently appends
 * to the end, which is worse than refusing the intended precise move.
 * @param {{
 *  position?: DropPosition,
 *  fallbackTarget?: string,
 *  fallbackPosition?: DropPosition
 * }} drop
 * @returns {boolean}
 */
function shouldRetryDrop(drop) {
	return Boolean(drop.position === 'inside' && drop.fallbackTarget && drop.fallbackPosition);
}

/**
 * Some structured JSON/XML children can be targeted either as `parent.child`
 * or `parent.st:child`. Try both qname forms for direct precise sibling drops,
 * including same-parent reorders that intentionally bypass Accept.
 * @param {{
 *  target?: string,
 *  position?: DropPosition
 * }} drop
 * @returns {boolean}
 */
function shouldRetryEquivalentPreciseTarget(drop) {
	return Boolean(drop.position !== 'inside' && equivalentDboObjectIds(drop.target).length > 1);
}

/**
 * Some engine versions accept exact cross-parent "first/after" drops but still
 * append the object. In that case we insert in the parent, then move the
 * created/moved object to the requested sibling position.
 * @param {{
 *  payload?: DboDragPayload,
 *  target?: string,
 *  position?: DropPosition,
 *  dropAction?: DropAction,
 *  fallbackTarget?: string,
 *  fallbackPosition?: DropPosition
 * }} drop
 * @returns {boolean}
 */
function shouldComposePreciseDrop(drop) {
	return Boolean(
		drop.position !== 'inside' &&
		drop.fallbackTarget &&
		drop.fallbackPosition === 'inside' &&
		!supportsAtomicPreciseDrop(drop) &&
		!isSameParentReorder(
			drop.payload,
			drop.target ?? '',
			drop.position ?? 'inside',
			drop.dropAction ?? 'none'
		)
	);
}

/**
 * Flow frontend source mutations carry their requested insertion index all the
 * way to the canonical AST. Sending an initial parent append would create an
 * observable intermediate document and a second generation cycle.
 *
 * @param {{ payload?: DboDragPayload, target?: string }} drop
 * @returns {boolean}
 */
function supportsAtomicPreciseDrop(drop) {
	const payload = drop.payload;
	if (payload?.type === 'paletteData' && payload.data?.type === 'FrontendBlock') {
		return true;
	}
	return Boolean(
		payload?.type === 'treeData' &&
		payload.data?.id?.includes('.frontends.') &&
		drop.target?.includes('.frontends.')
	);
}

/**
 * @param {{
 *  payload: DboDragPayload,
 *  target: string,
 *  position: DropPosition,
 *  dropAction: DropAction,
 *  fallbackTarget: string,
 *  fallbackPosition: DropPosition
 * }} drop
 * @returns {Promise<DboDropResult>}
 */
async function performComposedPreciseDrop(drop) {
	const parentResult = await tryDboDrop({
		payload: drop.payload,
		target: drop.fallbackTarget,
		position: drop.fallbackPosition,
		dropAction: drop.dropAction
	});
	const insertedId = normalizeComposedInsertedId(parentResult.selectedId || parentResult.id, drop);
	if (!parentResult.done || !insertedId) {
		return parentResult;
	}
	const movePayload = {
		type: 'treeData',
		data: {
			id: insertedId,
			classname: drop.payload.data?.classname ?? ''
		},
		options: drop.payload.options ?? {}
	};
	const preciseResult = await tryEquivalentPreciseDrop(drop, movePayload);
	return preciseResult.done
		? mergeComposedDropResult(parentResult, {
				...preciseResult,
				target: drop.target,
				parentId: preciseResult.parentId || inferDropParentId(drop)
			})
		: parentResult;
}

/**
 * @param {{
 *  target: string,
 *  position: DropPosition
 * }} drop
 * @param {DboDragPayload} movePayload
 * @returns {Promise<DboDropResult>}
 */
async function tryEquivalentPreciseDrop(drop, movePayload) {
	let lastResult = /** @type {DboDropResult} */ ({ done: false, refused: true });
	const targetIds = equivalentDboObjectIds(drop.target);
	for (let index = 0; index < targetIds.length; index += 1) {
		const target = targetIds[index];
		const result = await tryDboDrop({
			payload: movePayload,
			target,
			position: drop.position,
			dropAction: 'move',
			silent: index < targetIds.length - 1
		});
		lastResult = result;
		if (result.done) {
			return result;
		}
	}
	return lastResult;
}

/**
 * @param {{
 *  payload: DboDragPayload,
 *  target: string,
 *  position: DropPosition,
 *  dropAction: DropAction
 * }} drop
 * @returns {Promise<DboDropResult>}
 */
async function tryEquivalentPreciseTargetDrop(drop) {
	let lastResult = /** @type {DboDropResult} */ ({ done: false, refused: true });
	const targetIds = equivalentDboObjectIds(drop.target);
	for (let index = 0; index < targetIds.length; index += 1) {
		const target = targetIds[index];
		const result = await tryDboDrop({
			payload: drop.payload,
			target,
			position: drop.position,
			dropAction: drop.dropAction,
			silent: index < targetIds.length - 1
		});
		lastResult = result;
		if (result.done) {
			return normalizeEquivalentTargetDropResult(result, drop);
		}
	}
	return retargetFailedPreciseDropResult(lastResult, drop);
}

/**
 * Some engine responses echo JSON/XML field children with the regular step
 * folder qname (`parent.st:name`) even though nested structured children are
 * addressed as `parent.name`. Normalize only that equivalent shape before the
 * second move of a composed precise drop.
 * @param {string | undefined} receivedId
 * @param {{
 *  payload: DboDragPayload,
 *  fallbackTarget: string,
 *  fallbackPosition: DropPosition
 * }} drop
 * @returns {string | undefined}
 */
function normalizeComposedInsertedId(receivedId, drop) {
	const inferredId = inferMovedObjectId({
		payload: drop.payload,
		target: drop.fallbackTarget,
		position: drop.fallbackPosition
	});
	if (!receivedId || !inferredId || receivedId === inferredId) {
		return receivedId || inferredId;
	}
	if (
		objectNameFromId(receivedId) === objectNameFromId(inferredId) &&
		isStructuredStepChild(receivedId, drop.fallbackTarget, drop.payload.data?.classname ?? '')
	) {
		return inferredId;
	}
	return receivedId;
}

/**
 * @param {DboDropResult} parentResult
 * @param {DboDropResult} preciseResult
 * @returns {DboDropResult}
 */
function mergeComposedDropResult(parentResult, preciseResult) {
	const affectedParentIds = [];
	for (const id of affectedDboParentIds(parentResult)) {
		pushUnique(affectedParentIds, id);
	}
	for (const id of affectedDboParentIds(preciseResult)) {
		pushUnique(affectedParentIds, id);
	}
	return {
		...preciseResult,
		payload: parentResult.payload || preciseResult.payload,
		previousParentId: parentResult.previousParentId || preciseResult.previousParentId,
		affectedParentIds
	};
}

/**
 * Keep the UI anchored to the qname shape selected by the user while allowing
 * the service call to use an equivalent qname that the engine can resolve.
 * @param {DboDropResult} result
 * @param {{ payload: DboDragPayload, target: string, position: DropPosition }} drop
 * @returns {DboDropResult}
 */
function normalizeEquivalentTargetDropResult(result, drop) {
	const parentId = result.parentId || inferDropParentId(drop);
	const nextResult = {
		...result,
		target: drop.target,
		parentId,
		position: drop.position,
		payload: drop.payload
	};
	return {
		...nextResult,
		affectedParentIds: affectedDboParentIds(nextResult)
	};
}

/**
 * Keep refused precise drops attached to the user's intended target. The
 * fallback parent is an implementation detail and should not leak into the
 * visual state when no mutation happened.
 * @param {DboDropResult} result
 * @param {{ payload: DboDragPayload, target: string, position: DropPosition }} drop
 * @returns {DboDropResult}
 */
function retargetFailedPreciseDropResult(result, drop) {
	const parentId = inferDropParentId(drop);
	const nextResult = {
		...result,
		target: drop.target,
		parentId,
		position: drop.position,
		payload: drop.payload
	};
	return {
		...nextResult,
		affectedParentIds: affectedDboParentIds(nextResult)
	};
}

/**
 * @param {DboDropResult | undefined} mutation
 * @returns {string[]}
 */
function affectedDboParentIds(mutation) {
	const ids = [];
	const targetIsMutatedSubject = isMutationSubjectPayload(mutation);
	for (const id of mutation?.affectedParentIds ?? []) {
		pushRelatedTreeContainerIds(ids, id);
	}
	pushRelatedTreeContainerIds(ids, mutation?.parentId);
	pushRelatedTreeContainerIds(ids, mutation?.previousParentId);
	if (mutation?.position === 'inside' && !targetIsMutatedSubject) {
		pushRelatedTreeContainerIds(ids, mutation?.target);
	} else {
		pushTreeFolderIds(ids, mutation?.target);
		pushRelatedTreeContainerIds(ids, parentObjectId(mutation?.target ?? ''));
	}
	pushTreeFolderIds(ids, mutation?.selectedId ?? mutation?.id);
	pushRelatedTreeContainerIds(ids, parentObjectId(mutation?.selectedId ?? mutation?.id ?? ''));
	pushTreeFolderIds(ids, mutation?.payload?.data?.id);
	pushRelatedTreeContainerIds(ids, parentObjectId(mutation?.payload?.data?.id ?? ''));
	for (const id of equivalentDboObjectIds(mutation?.selectedId ?? mutation?.id)) {
		pushTreeFolderIds(ids, id);
		pushRelatedTreeContainerIds(ids, parentObjectId(id));
	}
	for (const id of equivalentDboObjectIds(mutation?.payload?.data?.id)) {
		pushTreeFolderIds(ids, id);
		pushRelatedTreeContainerIds(ids, parentObjectId(id));
	}
	return ids.filter(Boolean);
}

/**
 * Returns every visible tree/flow container that may need to stay expanded or
 * be refreshed after a DBO mutation. Keep this broader than
 * `affectedDboParentIds`: selection, source, target and parent context all
 * matter for cross-view refreshes.
 * @param {DboDropResult | undefined} mutation
 * @returns {string[]}
 */
function mutationDboContextIds(mutation) {
	const ids = [];
	for (const id of affectedDboParentIds(mutation)) {
		pushUnique(ids, id);
	}
	for (const id of [
		mutation?.selectedId,
		mutation?.id,
		mutation?.target,
		mutation?.parentId,
		mutation?.previousParentId,
		mutation?.payload?.data?.id
	]) {
		for (const equivalentId of equivalentDboObjectIds(id)) {
			for (const ancestorId of expandableDboAncestorIds(equivalentId)) {
				pushUnique(ids, ancestorId);
			}
		}
	}
	if (mutation?.position === 'inside' && !isMutationSubjectPayload(mutation)) {
		pushRelatedTreeContainerIds(ids, mutation.target);
	}
	return ids.filter(Boolean);
}

/**
 * Source-backed frontend mutations already replace their server-side projected
 * route. Refresh only the concrete old/new parents in the visible tree: asking
 * for every ancestor again can replace the route while a deeper refresh still
 * targets its previous object graph, collapsing the branch until it is reopened.
 * Other DBOs keep the broader compatibility context used by the legacy tree.
 * @param {DboDropResult | undefined} mutation
 * @returns {string[]}
 */
function mutationDboRefreshIds(mutation) {
	if (!mutation?.selectionSourcePath && !mutation?.projectedSourcePath) {
		return mutationDboContextIds(mutation);
	}
	const ids = [];
	pushUnique(ids, mutation.parentId);
	pushUnique(ids, mutation.previousParentId);
	if (mutation.position === 'inside' && !isMutationSubjectPayload(mutation)) {
		pushUnique(ids, mutation.target);
	}
	return ids.filter(Boolean);
}

/**
 * Rename/delete mutations use `target` for the object that changed, whereas
 * drop mutations use it for a destination container/sibling.
 * @param {DboDropResult | undefined} mutation
 * @returns {boolean}
 */
function isMutationSubjectPayload(mutation) {
	const type = mutation?.payload?.type;
	return type === 'renameData' || type === 'deleteData';
}

/**
 * Returns the expandable tree ancestors for a qname-like database object id.
 * Folder ids such as `Project:sq` are included because tree folders are not
 * database objects but must still remain open around selected objects.
 * @param {string | undefined} id
 * @returns {string[]}
 */
function expandableDboAncestorIds(id) {
	const ids = [];
	if (!id) {
		return ids;
	}
	const projectName = id.split(/[.:]/)[0] ?? '';
	pushUnique(ids, projectName);
	for (const folderId of dboTreeFolderIds(id)) {
		pushUnique(ids, folderId);
	}
	let parentId = parentObjectId(id);
	while (parentId) {
		pushUnique(ids, parentId);
		for (const folderId of dboTreeFolderIds(parentId)) {
			pushUnique(ids, folderId);
		}
		parentId = parentObjectId(parentId);
	}
	return ids;
}

/**
 * Tree folders such as `Project:sq` and `Project.sq:Sequence:st` are not real
 * database objects, but they are the visible containers that must be refreshed
 * and kept expanded after a DBO mutation.
 * @param {string | undefined} id
 * @returns {string[]}
 */
function dboTreeFolderIds(id) {
	const qname = id ?? '';
	if (!qname) {
		return [];
	}
	const ids = [];
	const typedSegment = /\.([a-z]{2,4}):/g;
	let match;
	while ((match = typedSegment.exec(qname))) {
		const type = match[1];
		if (!FOLDER_TYPE_IDS.has(type)) {
			continue;
		}
		const parentPrefix = qname.slice(0, match.index);
		if (parentPrefix) {
			pushUnique(ids, `${parentPrefix}:${type}`);
		}
	}
	return ids;
}

/**
 * @param {string[]} ids
 * @param {string | undefined} id
 */
function pushRelatedTreeContainerIds(ids, id) {
	pushUnique(ids, id);
	pushTreeFolderIds(ids, id);
}

/**
 * @param {string[]} ids
 * @param {string | undefined} id
 */
function pushTreeFolderIds(ids, id) {
	for (const folderId of dboTreeFolderIds(id)) {
		pushUnique(ids, folderId);
	}
}

/**
 * @param {string[]} ids
 * @param {string | undefined} id
 */
function pushUnique(ids, id) {
	if (id && !ids.includes(id)) {
		ids.push(id);
	}
}

export {
	affectedDboParentIds,
	areEquivalentDboObjectIds,
	canDropDbo,
	canUseDboDropFallback,
	dboTreeFolderIds,
	equivalentDboObjectIds,
	expandableDboAncestorIds,
	getDboDragPayload,
	getDboDropAction,
	inferMovedObjectId,
	isDescendantObjectId,
	isNoopSiblingMove,
	mutationDboContextIds,
	mutationDboRefreshIds,
	objectNameFromId,
	parentObjectId,
	performDboDrop,
	renameObjectId,
	shouldStartInlineRename
};
