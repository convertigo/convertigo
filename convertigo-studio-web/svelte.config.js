import adapter from '@sveltejs/adapter-static';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';

let base = '';
try {
	base = process.argv
		.filter((s) => s.startsWith('--base='))
		.map((s) => s.substring(7))
		.join();
} catch (e) {
	console.error('dynamic base failed', e);
}

/** @type {import('@sveltejs/kit').Config} */
const config = {
	extensions: ['.svelte'],
	// Consult https://kit.svelte.dev/docs/integrations#preprocessors
	// for more information about preprocessors
	preprocess: [vitePreprocess()],

	vitePlugin: {
		inspector: true
	},
	kit: {
		// adapter-auto only supports some environments, see https://kit.svelte.dev/docs/adapter-auto for a list.
		// If your environment is not supported or you settled on a specific environment, switch out the adapter.
		// See https://kit.svelte.dev/docs/adapters for more information about adapters.
		adapter: adapter({
			pages: '../eclipse-plugin-studio/tomcat/webapps/convertigo/studio',
			strict: false
		}),
		paths: {
			base
		},
		prerender: {
			handleHttpError: ({ path }) => {
				if (path.startsWith('/convertigo/')) {
					return;
				}
			},
			handleMissingId: 'warn',
			handleEntryGeneratorMismatch: 'warn',
			entries: ['*', '/dashboard/_/frontend', '/dashboard/_/platforms']
		}
	},

	onwarn: (warning, handler) => {
		if (warning.code.startsWith('a11y_') || warning.code.startsWith('css_')) {
			return;
		}
		handler(warning);
	}
};
export default config;
