import { persistedState } from 'svelte-persisted-state';

export const DEFAULT_MAX_LOADED_LOG_LINES = 100000;
export const MIN_MAX_LOADED_LOG_LINES = 1000;
export const MAX_MAX_LOADED_LOG_LINES = 1000000;

export const maxLoadedLogLinesState = persistedState(
	'admin.logs.maxLoadedLines',
	DEFAULT_MAX_LOADED_LOG_LINES,
	{ syncTabs: true }
);

export function normalizeMaxLoadedLogLines(value) {
	const number = Number(value);
	if (!Number.isFinite(number)) {
		return DEFAULT_MAX_LOADED_LOG_LINES;
	}
	return Math.min(MAX_MAX_LOADED_LOG_LINES, Math.max(MIN_MAX_LOADED_LOG_LINES, Math.round(number)));
}
