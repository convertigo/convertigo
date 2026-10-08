import adapter from '@sveltejs/adapter-static';
import { sveltekit } from '@sveltejs/kit/vite';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';
import tailwindcss from '@tailwindcss/vite';
import Icons from 'unplugin-icons/vite';
import { defineConfig } from 'vite';
import convertigo from './src/convertigo.plugin.js';

function determineProxy() {
	const c8oPort =
		process.env.C8O_PORT ??
		process.env.npm_config_c8oPort ??
		process.argv.filter((s) => s.startsWith('--c8oPort=')).map((s) => s.substring(10))[0] ??
		'18080';
	let convertigoUrl = `http://localhost:${c8oPort}`;
	// if (isWSL) {
	// 	// Configuration de proxy pour WSL2
	// 	convertigoUrl = `http://172.29.80.1:${c8oPort}`;
	// }
	console.log(`Proxy for /convertigo → ${convertigoUrl}`);
	return convertigoUrl;
}

// SvelteKit loads this config in multiple build workers whose argv values are
// not guaranteed to match. A base inferred from those transient arguments can
// make prerendered HTML and client chunks use different __sveltekit globals.
// Keep the default relative deployment deterministic and expose one explicit
// environment variable for installations that need a fixed base.
const base = /** @type {'' | `/${string}`} */ (process.env.C8O_STUDIO_BASE ?? '');

export default defineConfig(({ command }) => {
	const conf = {
		plugins: [
			convertigo(),
			tailwindcss(),
			sveltekit({
				extensions: ['.svelte'],
				// Consult https://kit.svelte.dev/docs/integrations#preprocessors
				// for more information about preprocessors
				preprocess: [vitePreprocess()],
				inspector: true,
				onwarn: (warning, handler) => {
					if (warning.code.startsWith('a11y_') || warning.code.startsWith('css_')) {
						return;
					}
					handler(warning);
				},

				// adapter-auto only supports some environments, see https://kit.svelte.dev/docs/adapter-auto for a list.
				// If your environment is not supported or you settled on a specific environment, switch out the adapter.
				// See https://kit.svelte.dev/docs/adapters for more information about adapters.
				adapter: adapter({
					pages: '../eclipse-plugin-studio/tomcat/webapps/convertigo/tmp',
					strict: false
				}),
				paths: { base },
				prerender: {
					handleHttpError: 'ignore',
					handleMissingId: 'ignore',
					handleEntryGeneratorMismatch: 'ignore',
					entries: ['*', '/dashboard/_/frontend', '/dashboard/_/platforms', '/studio/_']
				}
			}),
			Icons({ compiler: 'svelte', autoInstall: true, defaultClass: 'ico' })
		],
		build: {
			rollupOptions: {
				output: {
					manualChunks(id) {
						if (id.includes('@xyflow/svelte')) {
							return 'xyflow';
						}
					}
				}
			}
		}
	};
	if (command === 'serve') {
		conf.server = {
			proxy: {
				'/convertigo': {
					target: determineProxy(),
					ws: true
				}
			}
		};
	}
	return conf;
});
