import adapter from '@sveltejs/adapter-static';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';

// SvelteKit loads this config in multiple build workers whose argv values are
// not guaranteed to match. A base inferred from those transient arguments can
// make prerendered HTML and client chunks use different __sveltekit globals.
// Keep the default relative deployment deterministic and expose one explicit
// environment variable for installations that need a fixed base.
const base = process.env.C8O_STUDIO_BASE ?? '';

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
			pages: '../eclipse-plugin-studio/tomcat/webapps/convertigo/tmp',
			strict: false
		}),
		paths: {
			base
		},
		prerender: {
			handleHttpError: 'ignore',
			handleMissingId: 'ignore',
			handleEntryGeneratorMismatch: 'ignore',
			entries: ['*', '/dashboard/_/frontend', '/dashboard/_/platforms', '/studio/_']
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
