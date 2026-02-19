import { toaster } from '$lib/utils/service';

export function fullSyncErrorMessage(error) {
	if (typeof error == 'string') return error;
	if (error?.message) return error.message;
	return 'Unknown FullSync error';
}

export function showFullSyncError(error, setLastError) {
	const message = fullSyncErrorMessage(error);
	setLastError?.(message);
	toaster.error({
		description: message,
		duration: 4200
	});
	return message;
}

export function showFullSyncSuccess(message) {
	toaster.success({
		description: message,
		duration: 2400
	});
}

/**
 * @param {(message: string) => void} setLastError
 */
export function createFullSyncFeedback(setLastError) {
	return {
		showError(error) {
			return showFullSyncError(error, setLastError);
		},
		showSuccess(message) {
			showFullSyncSuccess(message);
		}
	};
}
