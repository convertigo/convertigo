import { describe, expect, it } from 'vitest';
import { formatProjectReference, parseProjectReference } from './projectReference';

describe('parseProjectReference', () => {
	it('reads a project of a Git repository with its options', () => {
		expect(
			parseProjectReference(
				'lib_OAuth=https://github.com/convertigo/c8oprj-lib-oauth.git:path=lib:branch=8.4:autoPull=true'
			)
		).toEqual({
			projectName: 'lib_OAuth',
			url: 'https://github.com/convertigo/c8oprj-lib-oauth.git',
			path: 'lib',
			branch: '8.4',
			autoPull: true
		});
	});

	it('reads a project of an archive, named by its file when no name is given', () => {
		expect(parseProjectReference('https://example.com/files/MyLib.car')).toMatchObject({
			projectName: 'MyLib',
			url: 'https://example.com/files/MyLib.car'
		});
		expect(
			parseProjectReference(
				'mobilebuilder_tpl_8_4_0_ngx=https://github.com/convertigo/c8oprj-mobilebuilder-tpl/archive/mobilebuilder_tpl_8_4_0_ngx.zip'
			)
		).toMatchObject({
			projectName: 'mobilebuilder_tpl_8_4_0_ngx',
			url: 'https://github.com/convertigo/c8oprj-mobilebuilder-tpl/archive/mobilebuilder_tpl_8_4_0_ngx.zip',
			branch: ''
		});
		expect(parseProjectReference('Other=http://example.com/a/b.ZIP')).toMatchObject({
			projectName: 'Other',
			url: 'http://example.com/a/b.ZIP'
		});
	});

	it('reads a project of the workspace', () => {
		expect(parseProjectReference('TestBackEnd')).toEqual({
			projectName: 'TestBackEnd',
			url: '',
			path: '',
			branch: '',
			autoPull: false
		});
	});
});

describe('formatProjectReference', () => {
	it('writes back what it reads', () => {
		for (const value of [
			'lib_OAuth=https://github.com/convertigo/c8oprj-lib-oauth.git:branch=main',
			'RefLib=/tmp/refrepo/reflib.git:path=sub:autoPull=true',
			'TestBackEnd'
		]) {
			expect(formatProjectReference(parseProjectReference(value))).toBe(value);
		}
	});

	it('keeps only the name without an URL', () => {
		expect(
			formatProjectReference({ projectName: 'A', url: ' ', path: 'p', branch: 'b', autoPull: true })
		).toBe('A');
	});
});
