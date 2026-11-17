import { machine } from '@zag-js/file-upload';
import { describe, expect, it } from 'vitest';
import { fileUploadRejectionText, prepareFileUploadPicker } from './fileUploadSelection';

// Exercise the upload dependency's real acceptance/duplicate/rejection actions,
// rather than replacing its validation with a mock that would accept every file.
function uploadSelection(maxFiles = 1) {
	/** @type {Map<string, any[]>} */
	const values = new Map([
		['acceptedFiles', []],
		['rejectedFiles', []]
	]);
	const context = {
		get: (key) => values.get(key) ?? [],
		set: (key, value) =>
			values.set(key, typeof value === 'function' ? value(values.get(key)) : value)
	};
	const createProps = machine.props;
	const actions = machine.implementations?.actions;
	if (!createProps || !actions?.clearFiles || !actions?.setEventFiles) {
		throw new Error('Upload machine actions are unavailable');
	}
	// These actions only need the props and context supplied by this fixture.
	const props = createProps(/** @type {any} */ ({ props: { id: 'upload-test', maxFiles } }));
	/** @type {any} */
	const parameters = {
		context,
		prop: (key) => props[key],
		computed: (key) => (key === 'multiple' ? maxFiles > 1 : 'application/zip,.car,.zip')
	};
	return {
		get accepted() {
			return context.get('acceptedFiles');
		},
		get rejected() {
			return context.get('rejectedFiles');
		},
		clearFiles: () => actions.clearFiles(parameters),
		select: (files) => actions.setEventFiles({ ...parameters, event: { type: 'FILES.SET', files } })
	};
}

const archive = (name = 'Example.car', type = 'application/octet-stream') =>
	new File(['synthetic archive'], name, { type, lastModified: 1 });

describe('single-file picker selection', () => {
	it('clears the displayed file when opening the picker, including a cancelled opening', () => {
		const upload = uploadSelection();
		upload.select([archive()]);
		expect(upload.accepted).toHaveLength(1);
		prepareFileUploadPicker(upload, 1);
		// No selection follows a cancelled chooser: the old row must stay cleared.
		expect(upload.accepted).toEqual([]);
		expect(upload.rejected).toEqual([]);
	});

	it('accepts the same archive after reopening instead of rejecting it as a duplicate', () => {
		const upload = uploadSelection();
		const file = archive();
		upload.select([file]);
		prepareFileUploadPicker(upload, 1);
		upload.select([file]);
		expect(upload.accepted).toEqual([file]);
		expect(upload.rejected).toEqual([]);
	});

	it.each([
		['Another.car', ''],
		['Another.CAR', 'application/octet-stream'],
		['Another.zip', 'application/zip']
	])('accepts a replacement %s without relying on the browser MIME type', (name, type) => {
		const upload = uploadSelection();
		upload.select([archive()]);
		prepareFileUploadPicker(upload, 1);
		const next = archive(name, type);
		upload.select([next]);
		expect(upload.accepted).toEqual([next]);
		expect(upload.rejected).toEqual([]);
	});

	it('keeps type validation and clears its previous rejection on the next opening', () => {
		const upload = uploadSelection();
		upload.select([archive('invalid.txt', 'text/plain')]);
		expect(upload.accepted).toEqual([]);
		expect(upload.rejected[0].errors).toContain('FILE_INVALID_TYPE');
		prepareFileUploadPicker(upload, 1);
		expect(upload.rejected).toEqual([]);
		const valid = archive();
		upload.select([valid]);
		expect(upload.accepted).toEqual([valid]);
	});

	it('preserves multi-file accumulation and duplicate validation', () => {
		const upload = uploadSelection(3);
		const first = archive();
		const second = archive('Another.car');
		upload.select([first]);
		prepareFileUploadPicker(upload, 3);
		upload.select([second]);
		expect(upload.accepted).toEqual([first, second]);
		upload.select([first]);
		expect(upload.rejected[0].errors).toContain('FILE_EXISTS');
	});
});

describe('multiple-file rejection messages', () => {
	it('describes a repeated accepted file without reporting an unsupported type', () => {
		const upload = uploadSelection(3);
		const first = archive();
		upload.select([first]);
		upload.select([first]);
		expect(fileUploadRejectionText(upload.rejected, 3, 'Unsupported file type')).toBe(
			'This file is already selected.'
		);
		expect(upload.accepted).toEqual([first]);
	});
	it('reports the file-count limit from the upload machine', () => {
		const upload = uploadSelection(2);
		upload.select([archive('A.car'), archive('B.car'), archive('C.car')]);
		expect(fileUploadRejectionText(upload.rejected, 2, 'Unsupported file type')).toBe(
			'Choose at most 2 files.'
		);
	});
	it('retains the allowed-type message for a file of another type', () => {
		const upload = uploadSelection(3);
		upload.select([archive('invalid.txt', 'text/plain')]);
		expect(fileUploadRejectionText(upload.rejected, 3, 'Allowed: .car, .zip')).toBe(
			'Allowed: .car, .zip'
		);
	});
});
