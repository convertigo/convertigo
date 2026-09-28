import { describe, expect, it } from 'vitest';
import {
	decodeStudioSelectionId,
	encodeStudioSelectionId,
	studioSelectionIdFromUrl,
	studioSelectionPath,
	studioSelectionUrl
} from './routeSelection';

describe('Studio route selection helpers', () => {
	it('uses a readable route separator for qname typed segments', () => {
		expect(encodeStudioSelectionId('lib_ConvertigoMCP.sq:mcp_initialize.st:result')).toBe(
			'lib_ConvertigoMCP.sq~mcp_initialize.st~result'
		);
		expect(decodeStudioSelectionId('lib_ConvertigoMCP.sq~mcp_initialize.st~result')).toBe(
			'lib_ConvertigoMCP.sq:mcp_initialize.st:result'
		);
	});

	it('keeps non route-safe object names encoded inside the path segment', () => {
		expect(encodeStudioSelectionId('Project.sq:Sequence.st:"field 1" : ""')).toBe(
			'Project.sq~Sequence.st~%22field%201%22%20~%20%22%22'
		);
	});

	it('keeps the slashes of a file id out of the path, which Tomcat would refuse encoded', () => {
		const path = studioSelectionPath('/convertigo/studio/', 'Project//css/my file.css');
		expect(path).toBe('/convertigo/studio/Project%252F%252Fcss%252Fmy%20file.css/');
		expect(
			studioSelectionIdFromUrl('/convertigo/studio/', new URL(`https://example.test${path}`))
		).toBe('Project//css/my file.css');
		expect(decodeStudioSelectionId('Project%2F%2Fcss%2Fmy file.css')).toBe(
			'Project//css/my file.css'
		);
	});

	it('builds canonical Studio paths with and without a selection', () => {
		expect(studioSelectionPath('/convertigo/studio', '')).toBe('/convertigo/studio/');
		expect(studioSelectionPath('/convertigo/studio/', 'Project.sq:Sequence')).toBe(
			'/convertigo/studio/Project.sq~Sequence/'
		);
	});

	it('preserves the current query string when updating the selection', () => {
		const url = new URL('http://localhost/convertigo/studio/?theme=dark&tab=flow#logs');

		expect(studioSelectionUrl('/convertigo/studio/', 'Project.sq:Sequence', url)).toBe(
			'/convertigo/studio/Project.sq~Sequence/?theme=dark&tab=flow'
		);
	});

	it('reads a shallow Studio selection from the current browser URL', () => {
		expect(
			studioSelectionIdFromUrl(
				'/convertigo/studio/',
				new URL(
					'https://example.test/convertigo/studio/Project.sq~Sequence.st~Step/?profile=frontend'
				)
			)
		).toBe('Project.sq:Sequence.st:Step');
	});
});
