import { describe, expect, it } from 'vitest';
import { splitMultipartForm } from './multipartForm';

describe('multipart form files and fields', () => {
	it('keeps repeated files, later files and repeated text fields in their original order', () => {
		const source = new FormData();
		source.append('file', new File(['one'], 'One.car'));
		source.append('file', new File(['two'], 'Two.zip'));
		source.append('other', new File(['three'], 'Three.car'));
		source.append('option', 'first');
		source.append('option', 'second');
		const { files, fields } = splitMultipartForm(source);
		expect([...files.values()].map((file) => file.name)).toEqual([
			'One.car',
			'Two.zip',
			'Three.car'
		]);
		expect(fields.getAll('option')).toEqual(['first', 'second']);
		expect(source.getAll('file')).toHaveLength(2);
		expect([...source.entries()]).toHaveLength(5);
	});
	it('preserves the single-file and text-only forms', () => {
		const source = new FormData();
		source.append('bAssembleXsl', 'true');
		expect(splitMultipartForm(source).fields.get('bAssembleXsl')).toBe('true');
		expect([...splitMultipartForm(source).files]).toEqual([]);
		source.append('file', new File(['car'], 'One.car'));
		const { files, fields } = splitMultipartForm(source);
		expect(files.get('file').name).toBe('One.car');
		expect(fields.get('bAssembleXsl')).toBe('true');
	});
});
