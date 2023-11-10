import { purgeCss } from 'vite-plugin-tailwind-purgecss';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';
import Icons from 'unplugin-icons/vite';

function determineProxy() {
	const c8oPort =
		process.argv.filter((s) => s.startsWith('--c8oPort=')).map((s) => s.substring(10))[0] ??
		'18080';
	const isWSL = process.platform !== 'win32' && process.env.WSL_DISTRO_NAME != undefined;
	let convertigoUrl = `http://localhost:${c8oPort}`;
	if (isWSL) {
		// Configuration de proxy pour WSL2
		convertigoUrl = `http://172.29.80.1:${c8oPort}`;
	}
	console.log(`Proxy for /convertigo → ${convertigoUrl}`);
	return convertigoUrl;
}

export default defineConfig({
	plugins: [
		sveltekit(),
		purgeCss(),
		Icons({
			compiler: 'svelte',
			autoInstall: true,
			defaultClass: 'ico'
		})
	],
	server: {
		proxy: {
			'/convertigo': {
				target: determineProxy(),
				ws: true
			}
		}
	}
});
