import { readFile } from 'node:fs/promises';
import { createServer } from 'node:http';

let version = 'A';
let permanentlyMissing = false;
let executeCount = 0;
const helper = await readFile(new URL('../static/chunk-recovery.js', import.meta.url));
const loader = await readFile(
	new URL('../node_modules/monaco-editor/min/vs/loader.js', import.meta.url)
);

createServer(async (request, response) => {
	const url = new URL(request.url, 'http://127.0.0.1:4174');
	const send = (status, type, body) => {
		response.writeHead(status, { 'Content-Type': type, 'Cache-Control': 'no-store' });
		response.end(request.method === 'HEAD' ? undefined : body);
	};
	if (url.pathname === '/__test/reset') {
		version = 'A';
		permanentlyMissing = false;
		executeCount = 0;
		return send(200, 'text/plain', 'reset');
	}
	if (url.pathname === '/__test/deploy') {
		version = 'B';
		permanentlyMissing = url.searchParams.has('broken');
		return send(200, 'text/plain', 'deployed');
	}
	if (url.pathname === '/__test/count') return send(200, 'text/plain', String(executeCount));
	if (url.pathname === '/convertigo/execute') {
		executeCount++;
		return send(200, 'application/json', '{}');
	}
	if (url.pathname === '/convertigo/chunk-recovery.js') return send(200, 'text/javascript', helper);
	if (url.pathname === '/convertigo/monaco/vs/loader.js')
		return send(200, 'text/javascript', loader);
	if (url.pathname === '/convertigo/monaco/vs/editor/editor.main.js') {
		return send(
			200,
			'text/javascript',
			`define('vs/editor/editor.main', ['../editor-${version === 'A' ? 'KLE6jdfb' : 'BdtEMBbM'}'], () => {});`
		);
	}
	if (url.pathname.includes('/_app/immutable/') || url.pathname.includes('/monaco/vs/')) {
		const isCurrent =
			url.pathname.endsWith(version === 'A' ? 'page.ABC12345.js' : 'page.DEF67890.js') ||
			url.pathname.endsWith(version === 'A' ? 'editor-KLE6jdfb.js' : 'editor-BdtEMBbM.js');
		if (permanentlyMissing || !isCurrent)
			return send(404, 'text/html', '<html>missing old chunk</html>');
		const body = url.pathname.includes('/monaco/')
			? `define('vs/editor-${version === 'A' ? 'KLE6jdfb' : 'BdtEMBbM'}', [], () => { document.querySelector('#result').textContent = '${version}'; });`
			: `export const version = '${version}';`;
		return send(200, 'text/javascript', body);
	}
	if (url.pathname === '/convertigo/dashboard/Demo/backend/') {
		return send(
			200,
			'text/html',
			`<!doctype html><html><head>
			<script src='/convertigo/chunk-recovery.js'></script></head><body>
			<input aria-label='Unsaved value'><button id='load'>Load lazy view</button><button id='execute'>Execute</button>
			<button id='monaco'>Load Monaco entry point</button><p id='result'></p>
			<script src='/convertigo/monaco/vs/loader.js'></script>
			<script>
				const current = '${version}';
				window.require.config({ paths: { vs: '/convertigo/monaco/vs' } });
				document.querySelector('#monaco').onclick = () => window.require(['vs/editor/editor.main'], () => {}, () => {});
				document.querySelector('#execute').onclick = async () => {
					await fetch('/convertigo/execute', { method: 'POST' });
					window.require(['vs/editor-KLE6jdfb'], () => {}, () => {});
				};
				document.querySelector('#load').onclick = async () => {
					try {
						const module = await import('/convertigo/_app/immutable/nodes/page.' + (current === 'A' ? 'ABC12345' : 'DEF67890') + '.js');
						document.querySelector('#result').textContent = module.version;
					} catch (error) {
						const event = new Event('vite:preloadError'); event.payload = error; window.dispatchEvent(event);
					}
				};
			</script></body></html>`
		);
	}
	return send(404, 'text/plain', 'not found');
}).listen(4174, '127.0.0.1');
