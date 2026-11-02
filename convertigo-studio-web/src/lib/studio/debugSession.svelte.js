import { call } from '#lib/utils/service.js';
import { setJavascriptDebugSession } from './javascriptDebugRequest.js';

/** @typedef {{ url: string, line: number, function: string, location?: {label: string, id?: string, property?: string, offset?: number} }} DebugFrame */
/** @typedef {{ notice?: string, pauseTimeoutMillis?: number, enabled?: boolean, attached: boolean, sessionId?: string, pauseId?: number, running?: boolean, breakOnExceptions: boolean, breakOnEnter?: boolean, breakOnReturn?: boolean, studioRequests?: boolean, serial: number, sources: string[], sourceLocations?: Record<string, {label: string}>, stopped: boolean, frames?: DebugFrame[], events?: {type: string, serial: number}[] }} DebugState */
export const debugSession = $state({
	state: /** @type {DebugState | null} */ (null),
	frameIndex: 0
});
/** @type {ReturnType<typeof setTimeout> | undefined} */
let timer;
let followers = 0;
let generation = 0;
const listeners = new Set();
/** @type {Map<string, Promise<any>>} */
const inspections = new Map();

/** @param {DebugState | null | undefined} state */
export function followDebugger(state) {
	if (state) {
		const previous = debugSession.state;
		if (
			state.sessionId !== previous?.sessionId ||
			state.pauseId !== previous?.pauseId ||
			!state.stopped
		)
			inspections.clear();
		// A slow poll must not overwrite a newer command response.
		if (state.sessionId === previous?.sessionId && state.serial < (previous?.serial ?? -1)) return;
		debugSession.state = state;
		for (const listener of listeners) listener(state);
		setJavascriptDebugSession(state.attached ? state.sessionId : '');
	}
	clearTimeout(timer);
	timer = followers && debugSession.state?.attached ? setTimeout(refreshDebugger, 1000) : undefined;
}
export async function refreshDebugger() {
	clearTimeout(timer);
	timer = undefined;
	const expected = generation;
	const result = await call('studio.debug.Debugger', { action: 'state' });
	if (expected === generation && followers) followDebugger(result?.state);
}
/** Follow only while a debugger panel is mounted; abandonment expires on the server. */
export function watchDebugger(onState = (_state) => {}) {
	listeners.add(onState);
	if (debugSession.state) onState(debugSession.state);
	followers++;
	void refreshDebugger();
	return () => {
		listeners.delete(onState);
		if (--followers === 0) {
			generation++;
			clearTimeout(timer);
			timer = undefined;
			inspections.clear();
			setJavascriptDebugSession('');
		}
	};
}
/** @param {string} action @param {Record<string, any>} [parameters] */
export async function debugCommand(action, parameters = {}) {
	if (action === 'start' || action === 'stop') generation++;
	const expected = generation;
	const result = await call('studio.debug.Debugger', {
		action,
		pauseId: String(debugSession.state?.pauseId ?? -1),
		...parameters
	});
	if (expected === generation) followDebugger(result?.state);
	return result;
}
/** @param {number} frame */
export function inspectFrame(frame) {
	const state = debugSession.state;
	if (!state?.stopped) return Promise.resolve(null);
	const key = `${state.sessionId}:${state.pauseId}:${frame}`;
	if (!inspections.has(key))
		inspections.set(
			key,
			call('studio.debug.Debugger', {
				action: 'variables',
				pauseId: String(state.pauseId),
				frame: String(frame)
			}).then((result) => result?.variables ?? null)
		);
	return inspections.get(key);
}
export function invalidateInspection() {
	inspections.clear();
}
/** Safe identifier previews, without evaluating getters or arbitrary hover expressions. */
export function variablePreviews(variables) {
	/** @type {Record<string, string>} */
	const values = Object.create(null);
	for (const scope of variables?.scopes ?? []) {
		for (const variable of scope.variables ?? []) {
			if (!Object.hasOwn(values, variable.name)) values[variable.name] = variable.value;
		}
	}
	return values;
}
