import js from '@eslint/js';
import tsParser from '@typescript-eslint/parser';
import prettier from 'eslint-config-prettier';
import svelte from 'eslint-plugin-svelte';
import globals from 'globals';
import svelteConfig from './svelte.config.js';

function toFlatGlobals(source = {}) {
	return Object.fromEntries(
		Object.entries(source).map(([name, writable]) => [name, writable ? 'writable' : 'readonly'])
	);
}

export default [
	{
		ignores: [
			'.DS_Store',
			'node_modules/**',
			'build/**',
			'.svelte-kit/**',
			'package/**',
			'static/**',
			'src/lib/studio/**',
			'src/routes/studio/**',
			'.env',
			'.env.*',
			'pnpm-lock.yaml',
			'package-lock.json',
			'yarn.lock'
		]
	},
	js.configs.recommended,
	...svelte.configs['flat/recommended'],
	prettier,
	{
		rules: {
			// This rule is too strict for our helper-based internal routing and generic link components.
			'svelte/no-navigation-without-resolve': 'off'
		}
	},
	{
		languageOptions: {
			ecmaVersion: 'latest',
			sourceType: 'module',
			globals: {
				...toFlatGlobals(globals.browser),
				...toFlatGlobals(globals.node),
				...toFlatGlobals(globals.es2017)
			}
		}
	},
	{
		files: ['**/*.svelte', '**/*.svelte.js', '**/*.svelte.ts'],
		languageOptions: {
			parserOptions: {
				parser: tsParser,
				svelteConfig
			}
		}
	}
];
