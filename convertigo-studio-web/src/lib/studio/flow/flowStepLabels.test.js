import { describe, expect, it } from 'vitest';
import {
	branchLabel,
	branchLabelForStep,
	branchTone,
	dropLabel,
	dropZoneLabel,
	dropZoneTone,
	isIfStep,
	isSimpleIfStep,
	isThenElseStep
} from './flowStepLabels';

describe('Studio flow step labels', () => {
	it('normalizes boolean branch labels to studio terms', () => {
		expect(branchLabel('true')).toBe('Then');
		expect(branchLabel('false')).toBe('Else');
		expect(branchLabel('yes')).toBe('Then');
		expect(branchLabel('no')).toBe('Else');
		expect(branchLabel('suite')).toBe('Next');
	});

	it('labels simple IfStep false outputs as continuation paths', () => {
		expect(branchLabelForStep('false', { type: 'IfStep' })).toBe('Next');
		expect(branchLabelForStep('false', { type: 'IfExistStep' })).toBe('Next');
		expect(branchLabelForStep('false', { type: 'IfStep', hasElseBranch: true })).toBe('Else');
		expect(branchLabelForStep('false', { type: 'IfExistStep', hasElseBranch: true })).toBe('Else');
		expect(branchLabelForStep('false', { type: 'IfThenElseStep' })).toBe('Else');
	});

	it('normalizes branch labels to visual tones', () => {
		expect(branchTone('true')).toBe('then');
		expect(branchTone('false')).toBe('else');
		expect(branchTone('suite')).toBe('next');
		expect(branchTone('loop')).toBe('loop');
		expect(branchTone('done')).toBe('done');
		expect(branchTone('custom')).toBe('');
	});

	it('labels simple if drops as branch or continuation targets', () => {
		expect(dropLabel({ position: 'inside', step: { type: 'IfStep' } })).toBe('Drop in THEN branch');
		expect(dropLabel({ position: 'after', step: { type: 'IfStep' } })).toBe('Drop on NEXT path');
		expect(dropLabel({ position: 'after', step: { type: 'IfThenElseStep' } })).toBe(
			'Drop in ELSE branch'
		);
	});

	it('keeps nested lane context in before and after labels', () => {
		expect(dropLabel({ position: 'before', branch: 'true', host: 'if(??)' })).toBe(
			'Before in Then branch of if(??)'
		);
		expect(dropLabel({ position: 'after', branch: '', host: 'object' })).toBe('After in object');
		expect(dropLabel({ position: 'inside', host: 'object' })).toBe('Inside object');
		expect(dropLabel({ position: 'inside', branch: 'false', host: 'if(??)' })).toBe(
			'Else branch of if(??)'
		);
	});

	it('uses compact branch labels inside node drop zones', () => {
		expect(dropZoneLabel({ zone: 'inside', step: { type: 'IfStep' } })).toBe('THEN');
		expect(dropZoneLabel({ zone: 'after', step: { type: 'IfStep' } })).toBe('NEXT');
		expect(dropZoneLabel({ zone: 'after', step: { type: 'IfThenElseStep' } })).toBe('ELSE');
		expect(dropZoneLabel({ zone: 'inside', branch: 'false' })).toBe('ELSE');
		expect(dropZoneLabel({ zone: 'inside', branch: 'false', step: { type: 'IfStep' } })).toBe(
			'NEXT'
		);
		expect(dropZoneLabel({ zone: 'before', branch: 'true' })).toBe('BEFORE');
	});

	it('uses semantic tones for active drop zones', () => {
		expect(dropZoneTone({ zone: 'inside', step: { type: 'IfStep' } })).toBe('then');
		expect(dropZoneTone({ zone: 'after', step: { type: 'IfStep' } })).toBe('next');
		expect(dropZoneTone({ zone: 'after', step: { type: 'IfThenElseStep' } })).toBe('else');
		expect(dropZoneTone({ zone: 'inside', branch: 'false' })).toBe('else');
		expect(dropZoneTone({ zone: 'after', branch: 'loop' })).toBe('loop');
		expect(dropZoneTone({ zone: 'before' })).toBe('');
	});

	it('detects if steps from type or classname metadata', () => {
		expect(isIfStep({ type: 'IfStep' })).toBe(true);
		expect(isIfStep({ type: 'IfExistStep' })).toBe(true);
		expect(isIfStep({ type: 'IfThenElseStep' })).toBe(true);
		expect(isIfStep({ classname: 'com.twinsoft.convertigo.beans.steps.IfStep' })).toBe(true);
		expect(isIfStep({ classname: 'com.twinsoft.convertigo.beans.steps.IfExistStep' })).toBe(true);
		expect(isIfStep({ type: 'SimpleStep' })).toBe(false);
		expect(isThenElseStep({ type: 'IfThenElseStep' })).toBe(true);
		expect(isSimpleIfStep({ type: 'IfStep' })).toBe(true);
		expect(isSimpleIfStep({ type: 'IfExistStep' })).toBe(true);
		expect(isSimpleIfStep({ type: 'IfThenElseStep' })).toBe(false);
	});
});
