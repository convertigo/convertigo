import { render } from 'svelte/server';
import { describe, expect, it } from 'vitest';
import MaskedPropertyInput from './MaskedPropertyInput.svelte';

describe('MaskedPropertyInput', () => {
	it('renders a native password input and an explicit clear action for a stored value', () => {
		const { body } = render(MaskedPropertyInput, {
			props: { row: { displayName: 'SMTP password', hasMaskedValue: true } }
		});

		expect(body).toContain('type="password"');
		expect(body).toContain('autocomplete="new-password"');
		expect(body).toContain('placeholder="Value set"');
		expect(body).toContain('aria-label="Clear SMTP password"');
	});

	it('disables editing and omits the clear action for a read-only property', () => {
		const { body } = render(MaskedPropertyInput, {
			props: { row: { displayName: 'SMTP password', hasMaskedValue: true, isDisabled: true } }
		});

		expect(body).toMatch(/<input\b[^>]*\bdisabled\b/);
		expect(body).not.toContain('aria-label="Clear SMTP password"');
	});
});
