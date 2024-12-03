import { join } from 'path';
import * as themes from '@skeletonlabs/skeleton/themes';
import newTheme from './src/themes/newTheme';
import forms from '@tailwindcss/forms';
import typography from '@tailwindcss/typography';
import { skeleton } from '@skeletonlabs/skeleton/plugin';
import { convertigoPlugin } from './src/convertigo.plugin';
/** @type {import('tailwindcss').Config} */
export default {
	darkMode: 'class',
	content: [
		'./src/**/*.{html,js,svelte,ts}',
		join(require.resolve('@skeletonlabs/skeleton-svelte'), '../**/*.{html,js,svelte,ts}')
	],
	safelist: [
		{
			pattern: /(size|w|h)-.+/
		}
	],
	theme: {
		extend: {
			colors: {
				'pale-violet': '#d6b5d6',
				'pale-blue': '#b5c7d6',
				'pale-green': '#b5d6c7',
				'pale-pink': '#d6b5b5'
			}
		}
	},
	plugins: [
		forms,
		typography,
		skeleton({
			themes: [newTheme]
		}),
		convertigoPlugin
	]
};
