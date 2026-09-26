import { describe, expect, it } from 'vitest';
import { PROJECT_TEMPLATES, projectNameError, projectTemplateSettings } from './projectTemplates';

describe('projectTemplates', () => {
	it('gives each template an importable URL and a unique id', () => {
		const ids = new Set(PROJECT_TEMPLATES.map((template) => template.id));
		expect(ids.size).toBe(PROJECT_TEMPLATES.length);
		for (const template of PROJECT_TEMPLATES) {
			expect(template.url).toMatch(/^template_\w+=https:\/\/github\.com\/convertigo\/.+\.zip$/);
		}
	});

	it('types the connector settings as the service reads them', () => {
		const rest = PROJECT_TEMPLATES.find((template) => template.id === 'rest');
		if (!rest) {
			throw new Error('missing REST template');
		}
		expect(
			projectTemplateSettings(rest, {
				connectorName: '  ',
				server: ' api.example.com ',
				port: '8443',
				https: 1
			})
		).toEqual({ server: 'api.example.com', port: 8443, https: true });
	});

	it('gives a screen template its emulator with its fields', () => {
		const screen = PROJECT_TEMPLATES.find((template) => template.id === '5250screen');
		if (!screen) {
			throw new Error('missing 5250 screen template');
		}
		expect(
			projectTemplateSettings(screen, {
				host: ' as400.example.com ',
				port: '23',
				connectionParameter: ''
			})
		).toEqual({ emulator: 'IBM5250', host: 'as400.example.com', port: 23 });
	});

	it('refuses the names a project cannot take', () => {
		expect(projectNameError('')).not.toBe('');
		expect(projectNameError('My Project')).not.toBe('');
		expect(projectNameError('1project')).not.toBe('');
		expect(projectNameError('My_Project2')).toBe('');
	});
});
