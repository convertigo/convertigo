import { describe, expect, it, vi } from 'vitest';
import { convertProjectYaml } from './projectYaml.js';

describe('project YAML conversion', () => {
	it('warns about compatibility and does nothing when cancelled', async () => {
		const confirm = vi.fn(() => false);
		const request = vi.fn();
		expect(await convertProjectYaml('Demo', confirm, request)).toBe(false);
		expect(confirm.mock.calls[0][0]).toContain('minimum engine version to 8.5');
		expect(confirm.mock.calls[0][0]).toContain('Older engines');
		expect(request).not.toHaveBeenCalled();
	});
	it('converts the requested project after confirmation', async () => {
		const request = vi.fn().mockResolvedValue({ admin: { success: { message: 'Converted' } } });
		expect(await convertProjectYaml('Demo', () => true, request)).toBe(true);
		expect(request).toHaveBeenCalledWith('projects.ConvertYaml', { projectName: 'Demo' });
	});
	it.each([{ isError: true }, {}, { admin: { error: 'Cannot save' } }])('does not report a failed conversion as saved (%j)', async (result) => {
		expect(await convertProjectYaml('Demo', () => true, vi.fn().mockResolvedValue(result))).toBe(false);
	});
});
