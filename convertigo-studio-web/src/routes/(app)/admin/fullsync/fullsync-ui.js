const PAGE_OPTIONS_BASE = Object.freeze(['5', '10', '20', '30', '50', '100']);

export const FULLSYNC_PAGE_OPTIONS_BASE = PAGE_OPTIONS_BASE;
export const FULLSYNC_PAGE_OPTIONS_WITH_500 = Object.freeze([...PAGE_OPTIONS_BASE, '500']);
export const FULLSYNC_PAGE_OPTIONS_WITH_500_AND_NONE = Object.freeze([
	...FULLSYNC_PAGE_OPTIONS_WITH_500,
	'none'
]);
export const FULLSYNC_DB_LIST_PAGE_OPTIONS = Object.freeze(['20', '50', '100']);

export function fullSyncOptionLabel(value) {
	return value == 'none' ? 'None' : String(value ?? '');
}
