import { describe, expect, it } from 'vitest';
import { serviceErrorMessage } from './serviceError.js';

describe('serviceErrorMessage', () => {
	it('reads the message of the error of the engine', () => {
		expect(serviceErrorMessage({ message: 'Invalid name', exception: 'EngineException' })).toBe(
			'Invalid name'
		);
		expect(serviceErrorMessage({ exception: 'NullPointerException' })).toBe('NullPointerException');
	});

	it('keeps a text and falls back when there is no error', () => {
		expect(serviceErrorMessage('Not found')).toBe('Not found');
		expect(serviceErrorMessage(undefined, 'Unable to load')).toBe('Unable to load');
		expect(serviceErrorMessage({ code: 42 })).toBe('{"code":42}');
	});
});
