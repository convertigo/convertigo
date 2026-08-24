import { describe, expect, it } from 'vitest';
import { findDeepKeys } from './service';

describe('service response traversal', () => {
	it('ignores null branches while finding nested state messages', () => {
		expect(
			findDeepKeys(
				{
					result: null,
					items: [null, { details: { error: 'Nested failure' } }]
				},
				['error', 'errorMessage']
			)
		).toBe('Nested failure');
	});

	it('accepts an empty response', () => {
		expect(findDeepKeys(null, ['error'])).toBeNull();
	});
});
