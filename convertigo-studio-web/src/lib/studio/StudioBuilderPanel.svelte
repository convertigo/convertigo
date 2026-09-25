<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { getUrl } from '$lib/utils/service';
	import { onDestroy, tick } from 'svelte';

	/**
	 * The development build of the application of a project, as the application editor of the Eclipse
	 * Studio runs it: the studio.ngxbuilder.WsBuilder socket runs `npm run ionic:serve`, streams its output
	 * and its progress, then gives the URL of the application it serves.
	 *
	 * @type {{
	 *  projectName?: string,
	 *  active?: boolean,
	 *  onLoad?: (url: string) => void
	 * }}
	 */
	let { projectName = '', active = false, onLoad } = $props();

	const MAX_LINES = 2000;

	/** @type {WebSocket | null} */
	let socket = null;
	let socketProject = '';
	/** @type {'closed' | 'connecting' | 'open'} */
	let connection = $state('closed');
	let progress = $state(-1);
	let url = $state('');
	/** @type {{ kind: string, text: string }[]} */
	let lines = $state([]);
	/** @type {HTMLDivElement | undefined} */
	let output = $state();

	$effect(() => {
		const project = projectName;
		if (active && project && project !== socketProject) {
			connect(project);
		}
	});

	onDestroy(() => socket?.close());

	/**
	 * @param {string} kind
	 * @param {string} text
	 */
	async function append(kind, text) {
		const stick = output ? output.scrollTop + output.clientHeight >= output.scrollHeight - 8 : true;
		lines = [...lines.slice(-(MAX_LINES - 1)), { kind, text }];
		if (stick) {
			await tick();
			output?.scrollTo({ top: output.scrollHeight });
		}
	}

	/**
	 * @returns {string} the URL of the Convertigo endpoint, which the built application calls
	 */
	function endpoint() {
		return new URL('../../', new URL(getUrl(), location.href)).href.replace(/\/$/, '');
	}

	/**
	 * @param {string} project
	 */
	function connect(project) {
		socket?.close();
		socketProject = project;
		lines = [];
		progress = -1;
		url = '';
		const address = new URL(`${getUrl()}studio.ngxbuilder.WsBuilder`, location.href);
		address.protocol = address.protocol === 'https:' ? 'wss:' : 'ws:';
		// a browser sends no header with a websocket handshake: the XSRF token of the session goes as a parameter
		address.searchParams.set('__xsrfToken', localStorage.getItem('x-xsrf-token') ?? '');
		connection = 'connecting';
		const current = new WebSocket(address.href);
		socket = current;
		current.onopen = () => {
			connection = 'open';
			send('attach');
		};
		current.onclose = () => {
			if (socket === current) {
				connection = 'closed';
				socketProject = '';
			}
		};
		current.onmessage = (event) => {
			try {
				const { type, value } = JSON.parse(event.data);
				if (type === 'progress') {
					progress = Number(value);
				} else if (type === 'load') {
					url = String(value);
					void append('log', `The application is served on ${url}`);
					onLoad?.(url);
				} else {
					void append(type, String(value));
				}
			} catch {
				void append('output', String(event.data));
			}
		};
	}

	/**
	 * @param {'attach' | 'build_dev' | 'kill'} action
	 */
	function send(action) {
		if (socket?.readyState !== WebSocket.OPEN) {
			return;
		}
		socket.send(
			JSON.stringify({
				project: projectName,
				action,
				params: action === 'build_dev' ? { endpoint: endpoint() } : {}
			})
		);
	}
</script>

<div class="studio-builder">
	<div class="studio-builder__bar">
		<button
			type="button"
			class="button-primary"
			disabled={connection !== 'open' || !projectName}
			title="Build the application in development mode and serve it"
			onclick={() => {
				progress = 0;
				send('build_dev');
			}}
		>
			<Ico icon="mdi:play" size={4} /> Build and serve
		</button>
		<button
			type="button"
			class="button-secondary"
			disabled={connection !== 'open'}
			title="Stop the development server"
			onclick={() => send('kill')}
		>
			<Ico icon="mdi:stop" size={4} /> Stop
		</button>
		{#if url}
			<a class="studio-builder__link" href={url} target="_blank" rel="noopener">
				<Ico icon="mdi:open-in-new-variant" size={4} /> Open
			</a>
		{/if}
		{#if progress >= 0 && progress < 100}
			<span class="studio-builder__progress" title={`${progress}%`}>
				<span style:width={`${progress}%`}></span>
			</span>
		{/if}
		<span class="studio-builder__status">
			{connection === 'open'
				? projectName
				: connection === 'connecting'
					? 'Connecting to the builder…'
					: 'Builder not connected'}
		</span>
		<button
			type="button"
			class="studio-builder__icon"
			title="Clear the output"
			onclick={() => (lines = [])}
		>
			<Ico icon="mdi:broom" size={4} />
		</button>
	</div>
	<div class="studio-builder__output" bind:this={output}>
		{#each lines as line, index (index)}
			<div class={['studio-builder__line', `studio-builder__line--${line.kind}`]}>{line.text}</div>
		{:else}
			<div class="studio-builder__line studio-builder__line--log">
				Build and serve the application of {projectName || 'the selected project'} to follow its output
				here.
			</div>
		{/each}
	</div>
</div>

<style>
	.studio-builder {
		display: grid;
		height: 100%;
		min-height: 0;
		grid-template-rows: auto minmax(0, 1fr);
	}

	.studio-builder__bar {
		display: flex;
		min-width: 0;
		align-items: center;
		gap: 0.5rem;
		padding: 0.35rem 0.85rem;
		font-size: 0.75rem;
	}

	.studio-builder__bar :is(button, a) {
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
	}

	.studio-builder__link {
		color: var(--color-primary-500);
	}

	.studio-builder__progress {
		position: relative;
		width: 8rem;
		height: 0.3rem;
		overflow: hidden;
		border-radius: 999px;
		background: var(--studio-selection-bg);
	}

	.studio-builder__progress span {
		position: absolute;
		inset: 0 auto 0 0;
		background: var(--color-primary-500);
		transition: width 0.2s ease;
	}

	.studio-builder__status {
		min-width: 0;
		margin-left: auto;
		overflow: hidden;
		color: var(--studio-text-idle);
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-builder__icon {
		display: grid;
		width: 1.6rem;
		height: 1.6rem;
		place-items: center;
		border: 0;
		border-radius: 0.3rem;
		background: transparent;
		color: var(--studio-text-idle);
	}

	.studio-builder__icon:hover {
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

	.studio-builder__output {
		min-height: 0;
		overflow: auto;
		padding: 0.25rem 0.85rem 0.5rem;
		font-family: var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
		font-size: 0.72rem;
		line-height: 1.45;
	}

	.studio-builder__line {
		white-space: pre-wrap;
		word-break: break-word;
	}

	.studio-builder__line--log {
		color: var(--studio-text-idle);
	}

	.studio-builder__line--error {
		color: var(--color-error-600-400);
	}
</style>
