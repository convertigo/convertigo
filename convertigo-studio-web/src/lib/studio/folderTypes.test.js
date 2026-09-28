import { describe, expect, it } from 'vitest';
import { isFolderId } from './folderTypes.js';

describe('Studio tree folder ids', () => {
	it('tells the folders from the objects named as a folder type', () => {
		expect(isFolderId('Project:sq')).toBe(true);
		expect(isFolderId('Project.cn:Http:tr')).toBe(true);
		expect(isFolderId('Project.sq:Sequence:vr')).toBe(true);
		expect(isFolderId('Project.sq:Sequence.vr:id')).toBe(false);
		expect(isFolderId('Project.sq:sq')).toBe(false);
		expect(isFolderId('Project')).toBe(false);
		expect(isFolderId('Project:zz')).toBe(false);
	});
});
