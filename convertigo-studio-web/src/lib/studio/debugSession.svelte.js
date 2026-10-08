import { call } from '#lib/utils/service.js';

/**
 * @typedef {{ url: string, line: number, function: string }} DebugFrame
 */

/**
 * @typedef {{
 *  attached: boolean,
 *  breakOnExceptions: boolean,
 *  serial: number,
 *  sources: string[],
 *  stopped: boolean,
 *  thread?: string,
 *  alert?: string,
 *  frames?: DebugFrame[]
 * }} DebugState
 */

/**
 * The state of the JavaScript debugger, shared by the debug panel and the code editor, and followed
 * while the debugger runs.
 */
export const debugSession = $state({ state: /** @type {DebugState | null} */ (null) });

/** @type {ReturnType<typeof setTimeout> | undefined} */
let timer;

/**
 * Keeps the state a call of the debugger service returns, and follows it while the debugger runs.
 * @param {DebugState | null | undefined} state
 */
export function followDebugger(state) {
	if (state) {
		debugSession.state = state;
	}
	clearTimeout(timer);
	timer = debugSession.state?.attached ? setTimeout(refreshDebugger, 1000) : undefined;
}

export async function refreshDebugger() {
	clearTimeout(timer);
	timer = undefined;
	const result = await call('studio.debug.Debugger', { action: 'state' });
	followDebugger(result?.state);
}
