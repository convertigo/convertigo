import { describe, expect, it } from 'vitest';
import { initialSteps, moveStep, stepsError, stepsParameter } from './rebaseSteps.js';

const todo = [
	{ id: 'a1', subject: 'First', body: 'First\n\nwith details\n' },
	{ id: 'b2', subject: 'Second' },
	{ id: 'c3', subject: 'Third' }
];

describe('rebaseSteps', () => {
	it('picks each commit, its message proposed to reword it', () => {
		const steps = initialSteps(todo);
		expect(steps.map((step) => step.action)).toEqual(['pick', 'pick', 'pick']);
		expect(steps[0].message).toBe('First\n\nwith details');
		expect(steps[1].message).toBe('Second');
	});

	it('moves a commit earlier or later, not out of the list', () => {
		const steps = initialSteps(todo);
		expect(moveStep(steps, 2, -1).map((step) => step.id)).toEqual(['a1', 'c3', 'b2']);
		expect(moveStep(steps, 0, -1)).toBe(steps);
		expect(moveStep(steps, 2, 1)).toBe(steps);
	});

	it('refuses a first commit merged into nothing, a rewording without message, all dropped', () => {
		const steps = initialSteps(todo);
		expect(stepsError(steps)).toBe('');
		steps[0].action = 'drop';
		steps[1].action = 'squash';
		expect(stepsError(steps)).toContain('b2');
		steps[1].action = 'reword';
		steps[1].message = ' ';
		expect(stepsError(steps)).toContain('needs its new message');
		expect(stepsError(steps.map((step) => ({ ...step, action: 'drop' })))).toContain(
			'Keep at least'
		);
	});

	it('gives the engine the message of the commits reworded only', () => {
		const steps = initialSteps(todo);
		steps[1].action = 'reword';
		steps[1].message = 'Second, reworded ';
		steps[2].action = 'fixup';
		expect(stepsParameter(steps)).toEqual([
			{ id: 'a1', action: 'pick' },
			{ id: 'b2', action: 'reword', message: 'Second, reworded' },
			{ id: 'c3', action: 'fixup' }
		]);
	});
});
