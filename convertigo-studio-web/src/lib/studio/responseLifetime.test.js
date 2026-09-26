import { describe, expect, it } from 'vitest';
import { formatLifetime, parseLifetime } from './responseLifetime';

describe('parseLifetime', () => {
	it('reads the lifetimes the Eclipse Studio generates', () => {
		expect(parseLifetime('absolute,3600')).toEqual({
			mode: 'absolute',
			seconds: 3600,
			time: '00:00:00',
			day: 1
		});
		expect(parseLifetime('daily,23:05:00')).toMatchObject({ mode: 'daily', time: '23:05:00' });
		expect(parseLifetime('weekly,08:00:30,2')).toMatchObject({
			mode: 'weekly',
			time: '08:00:30',
			day: 2
		});
		expect(parseLifetime('monthly,00:00:00,31')).toMatchObject({ mode: 'monthly', day: 31 });
	});

	it('gives nothing for another value', () => {
		expect(parseLifetime('')).toBeNull();
		expect(parseLifetime('always')).toBeNull();
		expect(parseLifetime('daily,24:00:00')).toBeNull();
		expect(parseLifetime('weekly,08:00:00,8')).toBeNull();
	});
});

describe('formatLifetime', () => {
	it('writes the lifetime as the engine reads it', () => {
		expect(formatLifetime({ mode: 'absolute', seconds: 90.7, time: '', day: 1 })).toBe(
			'absolute,90'
		);
		expect(formatLifetime({ mode: 'daily', seconds: 0, time: '7:5:0', day: 1 })).toBe(
			'daily,07:05:00'
		);
		expect(formatLifetime({ mode: 'weekly', seconds: 0, time: '08:00:00', day: 9 })).toBe(
			'weekly,08:00:00,7'
		);
		expect(formatLifetime({ mode: 'monthly', seconds: 0, time: '23:59:59', day: 3 })).toBe(
			'monthly,23:59:59,03'
		);
	});

	it('reads back what it writes', () => {
		for (const value of [
			'absolute,60',
			'daily,12:30:00',
			'weekly,00:00:00,1',
			'monthly,06:00:00,15'
		]) {
			const lifetime = parseLifetime(value);
			expect(lifetime && formatLifetime(lifetime)).toBe(value);
		}
	});
});
