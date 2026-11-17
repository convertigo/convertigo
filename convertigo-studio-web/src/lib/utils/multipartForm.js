/**
 * Separate files from ordinary form fields without mutating a live FormData
 * iterator. Several files may share the same field name.
 * @param {FormData} data
 */
export function splitMultipartForm(data) {
	const files = new FormData();
	const fields = new URLSearchParams();
	for (const [key, value] of data.entries()) {
		if (value instanceof File) files.append(key, value);
		else fields.append(key, value);
	}
	return { files, fields };
}
