/**
 * The picker clears its native input before opening, so a single-file field must
 * also clear the displayed selection and any prior rejection.
 * @param {{ clearFiles: () => void }} upload
 * @param {number} maxFiles
 */
export function prepareFileUploadPicker(upload, maxFiles) {
	if (maxFiles === 1) upload.clearFiles();
}

/**
 * A multiple-file selection lives in the upload machine. Restore that complete
 * selection after native input/cancel events, including a repeated file choice.
 * @param {{ acceptedFiles: File[] }} upload
 * @param {HTMLInputElement} input
 * @param {number} maxFiles
 */
export function syncMultipleFileUploadInput(upload, input, maxFiles) {
	if (maxFiles <= 1) return;
	const selection = new DataTransfer();
	for (const file of upload.acceptedFiles) selection.items.add(file);
	input.files = selection.files;
}

/** @param {{errors?: string[]}[]} rejected @param {number} maxFiles @param {string} fallback */
export function fileUploadRejectionText(rejected, maxFiles, fallback) {
	if (rejected.some((file) => file.errors?.includes('TOO_MANY_FILES'))) {
		return `Choose at most ${maxFiles} files.`;
	}
	if (rejected.length && rejected.every((file) => file.errors?.includes('FILE_EXISTS'))) {
		return 'This file is already selected.';
	}
	return fallback;
}
