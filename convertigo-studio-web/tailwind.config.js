import { join } from 'path';

import forms from '@tailwindcss/forms';
import typography from '@tailwindcss/typography';
import { skeleton } from '@skeletonlabs/tw-plugin';
import { developperTheme } from './src/themes/developper';
import { convertigoTheme } from './src/themes/convertigoTheme';
/** @type {import('tailwindcss').Config} */
export default {
	darkMode: 'class',
	content: [
		'./src/**/*.{html,js,svelte,ts}',
		join(require.resolve('@skeletonlabs/skeleton'), '../**/*.{html,js,svelte,ts}')
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
			themes: {
				custom: [convertigoTheme, developperTheme]
			}
		})
	]
};
