import { describe, expect, it } from 'vitest';
import { changesInZones, findZones, generatedRuns, zonePurpose } from './editableZones.js';

const PAGE = [
	'import { Component } from "@angular/core";',
	'/*Begin_c8o_PageImport*/',
	'/*End_c8o_PageImport*/',
	'export class Page {',
	'	/*Begin_c8o_PageDeclaration*/',
	'	public count = 0;',
	'	/*End_c8o_PageDeclaration*/',
	'	constructor() {',
	'		super();',
	'		this.events = null;',
	'		/*Begin_c8o_PageConstructor*/',
	'		/*End_c8o_PageConstructor*/',
	'	}',
	'',
	'	ngOnInit() {',
	'		/*Begin_c8o_PageInitialization*/',
	'		/*End_c8o_PageInitialization*/',
	'	}',
	'}'
].join('\n');

describe('editableZones', () => {
	it('finds the zones between their comments', () => {
		const zones = findZones(PAGE);
		expect(zones.map((zone) => [zone.name, zone.beginLine, zone.endLine, zone.empty])).toEqual([
			['PageImport', 2, 3, true],
			['PageDeclaration', 5, 7, false],
			['PageConstructor', 11, 12, true],
			['PageInitialization', 16, 17, true]
		]);
		const declaration = zones[1];
		expect(PAGE.slice(declaration.contentStart, declaration.contentEnd).trim()).toBe(
			'public count = 0;'
		);
		expect(PAGE.slice(declaration.beginOffset, declaration.contentStart)).toBe(
			'/*Begin_c8o_PageDeclaration*/'
		);
	});

	it('leaves a zone whose End comment is missing', () => {
		expect(
			findZones('/*Begin_c8o_A*/ x /*Begin_c8o_B*/ y /*End_c8o_B*/').map((z) => z.name)
		).toEqual(['B']);
	});

	it('names the zones of the templates and of the actions', () => {
		expect(zonePurpose('PageInitialization').label).toBe('On init');
		expect(zonePurpose('NgDeclarations').label).toBe('Declarations');
		expect(zonePurpose('CompDeclaration').hint).toBe('the fields and properties of the class');
		expect(zonePurpose('function:CTS1790').label).toBe('CTS1790');
		expect(zonePurpose('SomethingNew').label).toBe('Something New');
	});

	it('gives the runs of generated lines between the zones', () => {
		const runs = generatedRuns(findZones(PAGE), PAGE.split('\n'));
		expect(runs.map((run) => [run.key, run.startLine, run.endLine, run.summary])).toEqual([
			['PageDeclaration>PageConstructor', 8, 10, 'constructor() {'],
			['PageConstructor>PageInitialization', 13, 15, 'ngOnInit() {']
		]);
		expect(generatedRuns(findZones(PAGE), PAGE.split('\n'), 1).map((run) => run.key)).toEqual([
			'start>PageImport',
			'PageImport>PageDeclaration',
			'PageDeclaration>PageConstructor',
			'PageConstructor>PageInitialization',
			'PageInitialization>end'
		]);
	});

	it('accepts the changes of the code of the zones only', () => {
		const zones = findZones(PAGE);
		const declaration = zones[1];
		expect(changesInZones(zones, [{ rangeOffset: declaration.contentStart, rangeLength: 0 }])).toBe(
			true
		);
		expect(
			changesInZones(zones, [
				{ rangeOffset: declaration.contentStart + 2, rangeLength: 5 },
				{ rangeOffset: zones[2].contentEnd, rangeLength: 0 }
			])
		).toBe(true);
		// a Backspace at the start of the zone would remove the end of its Begin comment
		expect(
			changesInZones(zones, [{ rangeOffset: declaration.contentStart - 1, rangeLength: 1 }])
		).toBe(false);
		expect(changesInZones(zones, [{ rangeOffset: 0, rangeLength: 3 }])).toBe(false);
	});
});
