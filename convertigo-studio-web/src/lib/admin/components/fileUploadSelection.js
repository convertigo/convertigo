/**
 * The picker clears its native input before opening, so a single-file field must
 * also clear the displayed selection and any prior rejection.
 * @param {{ clearFiles: () => void }} upload
 * @param {number} maxFiles
 */
export function prepareFileUploadPicker(upload, maxFiles) {
	if (maxFiles === 1) upload.clearFiles();
}
