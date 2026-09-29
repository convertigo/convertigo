<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { getUrl, toaster } from '$lib/utils/service';
	import { onDestroy, tick, untrack } from 'svelte';

	/**
	 * The builds of the application of a project, as the application editor of the Eclipse Studio runs
	 * them: the studio.ngxbuilder.WsBuilder socket installs its packages when asked or needed, serves it in
	 * development mode or builds it locally, streams the output and the progress, then gives the URL of
	 * the application it serves or tells the end of a local build.
	 *
	 * @type {{
	 *  projectName?: string,
	 *  active?: boolean,
	 *  onLoad?: (url: string) => void,
	 *  onBuilt?: () => void,
	 *  onServerStop?: () => void,
	 *  onFailedChange?: (failed: boolean) => void,
	 *  serveRequest?: number | { at: number, install?: string },
	 *  onServeRequestTaken?: () => void,
	 *  buildRequest?: number,
	 *  onBuildRequestTaken?: () => void
	 * }}
	 */
	let {
		projectName = '',
		active = false,
		onLoad,
		onBuilt,
		onServerStop,
		onFailedChange,
		serveRequest = 0,
		onServeRequestTaken,
		buildRequest = 0,
		onBuildRequestTaken
	} = $props();

	const LOCAL_BUILDS = [
		{ mode: 'prod', label: 'Production build' },
		{ mode: 'fast', label: 'Fast build' },
		{ mode: 'watch', label: 'Watch build' }
	];

	const MAX_LINES = 2000;

	/** @type {WebSocket | null} */
	let socket = null;
	let socketProject = '';
	/** @type {ReturnType<typeof setTimeout> | undefined} */
	let reconnectTimer;
	/** whether this panel asked the development server, whose address a new socket also tells */
	let serveAsked = false;
	/** @type {'closed' | 'connecting' | 'open'} */
	let connection = $state('closed');
	let progress = $state(-1);
	let url = $state('');
	/** the development server, idle or serving, and the local build, idle or building:<mode> */
	let devState = $state('idle');
	/** whether the builder told the state of the project since the socket opened */
	let attached = false;
	let localState = $state('idle');
	/** whether the builder writes the sources of the application while it is edited */
	let autoBuild = $state(true);
	/** an engine without Studio writes the sources at once, without auto build */
	let autoBuildSupported = $state(false);
	/** @type {{ name: string, url: string }[]} the URLs of the development server on the network */
	let networkUrls = $state([]);
	let qrOpen = $state(false);
	let qrUrl = $state('');
	/** whether the last compilation of the development server, or the last local build, failed */
	let failed = $state(false);
	let running = $derived(devState !== 'idle' || localState !== 'idle');
	let status = $derived(
		[
			devState === 'serving' && 'Serving in development mode',
			failed && 'Build failed',
			localState.startsWith('building:') &&
				`${LOCAL_BUILDS.find((build) => localState === `building:${build.mode}`)?.label ?? 'Build'} running`
		]
			.filter(Boolean)
			.join(' · ')
	);
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

	$effect(() => {
		// the Studio marks the Build view while the application does not build
		const current = failed;
		untrack(() => onFailedChange?.(current));
	});

	/**
	 * A compilation that fails is told once, until the application builds again, as the application editor
	 * of the Eclipse Studio shows it.
	 * @param {boolean} next
	 * @param {string} what
	 */
	function setFailed(next, what) {
		if (next && !failed) {
			toaster.error({
				title: `The application of ${socketProject} does not build`,
				description: `${what} failed: its errors are in the Build view.`
			});
		}
		failed = next;
	}

	/** a development server to show, asked by the Dev mode of the preview */
	let pendingServe = false;
	/** the packages to update or install again before serving, as Update packages and Execute */
	let pendingInstall = '';
	$effect(() => {
		// the request can mount the panel: it is taken, then served once the builder tells its state
		if (serveRequest) {
			pendingServe = true;
			pendingInstall = typeof serveRequest === 'object' ? (serveRequest.install ?? '') : '';
			untrack(() => {
				onServeRequestTaken?.();
				serveWhenReady();
			});
		}
	});

	/** a production build, asked before a deployment as the Eclipse Studio builds the application */
	let pendingBuild = false;
	let pendingBuildNotified = false;
	$effect(() => {
		if (buildRequest) {
			pendingBuild = true;
			pendingBuildNotified = false;
			untrack(() => {
				onBuildRequestTaken?.();
				serveWhenReady();
			});
		}
	});

	/**
	 * Shows the running development server, or starts it, once the builder tells its state, and starts the
	 * production build asked.
	 */
	function serveWhenReady() {
		if (connection !== 'open' || !attached) {
			return;
		}
		if (pendingBuild && localState === 'idle') {
			pendingBuild = false;
			send('build_local', { mode: 'prod' });
		} else if (pendingBuild && localState === 'building:watch' && !pendingBuildNotified) {
			pendingBuildNotified = true;
			void append('log', 'The production build starts once the watch build is stopped.');
		}
		if (!pendingServe) {
			return;
		}
		// a server starting or stopping keeps the request until the builder tells its next state
		if (pendingInstall) {
			// the builder replaces a running server with one whose packages are updated
			send('build_dev', { install: pendingInstall });
		} else if (devState === 'serving' && url) {
			onLoad?.(url);
		} else if (devState === 'idle') {
			send('build_dev');
		} else {
			return;
		}
		pendingServe = false;
		pendingInstall = '';
	}

	onDestroy(() => {
		clearTimeout(reconnectTimer);
		const current = socket;
		socket = null;
		current?.close();
	});

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
		clearTimeout(reconnectTimer);
		socket?.close();
		socketProject = project;
		serveAsked = false;
		lines = [];
		progress = -1;
		url = '';
		devState = 'idle';
		localState = 'idle';
		failed = false;
		attached = false;
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
				// the builder connects again once the engine is back, as after a restart
				clearTimeout(reconnectTimer);
				reconnectTimer = setTimeout(() => {
					if (socket === current && active && projectName) {
						connect(projectName);
					}
				}, 3000);
			}
		};
		current.onmessage = (event) => {
			try {
				const { type, value } = JSON.parse(event.data);
				if (type === 'progress') {
					progress = Number(value);
				} else if (type === 'state') {
					const [kind, ...rest] = String(value).split(':');
					if (kind === 'auto') {
						autoBuildSupported = rest[0] !== 'none';
						autoBuild = rest[0] !== 'false';
					} else if (kind === 'dev') {
						const wasServing = devState === 'serving';
						devState = rest.join(':');
						attached = true;
						serveWhenReady();
						if (devState === 'idle') {
							url = '';
							networkUrls = [];
							qrOpen = false;
							failed = false;
							if (wasServing) {
								onServerStop?.();
							}
						}
					} else {
						localState = rest.join(':');
						// the production build asked before a deployment starts once the running build ends
						serveWhenReady();
					}
					if (devState === 'idle' && localState === 'idle') {
						progress = -1;
					}
				} else if (type === 'network') {
					try {
						networkUrls = JSON.parse(String(value));
					} catch {
						networkUrls = [];
					}
					qrUrl = networkUrls[0]?.url ?? '';
				} else if (type === 'compiled') {
					setFailed(value === 'failed', 'The compilation of the development server');
				} else if (type === 'built') {
					progress = -1;
					setFailed(value === 'failed', 'The local build');
					// a build ending before the production build asked for a deployment does not deploy
					if (value === 'success' && !pendingBuild) {
						onBuilt?.();
					}
				} else if (type === 'load') {
					url = String(value);
					void append('log', `The application is served on ${url}`);
					// a server already running, told to a new socket, does not switch the preview
					if (serveAsked) {
						onLoad?.(url);
					}
				} else {
					void append(type, String(value));
				}
			} catch {
				void append('output', String(event.data));
			}
		};
	}

	/**
	 * @param {'attach' | 'build_dev' | 'build_local' | 'kill' | 'auto_build'} action
	 * @param {Record<string, any>} [params]
	 */
	function send(action, params = {}) {
		if (socket?.readyState !== WebSocket.OPEN) {
			return;
		}
		if (action === 'build_dev' || action === 'build_local') {
			progress = 0;
			url = '';
		}
		if (action === 'build_dev') {
			serveAsked = true;
		}
		socket.send(
			JSON.stringify({
				project: projectName,
				action,
				params: action.startsWith('build_') ? { endpoint: endpoint(), ...params } : params
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
			title="Build the application in development mode and serve it, installing its packages when missing"
			onclick={() => send('build_dev')}
		>
			<Ico icon="mdi:play" size={4} /> Build and serve
		</button>
		<select
			class="studio-builder__select input-common"
			aria-label="Packages"
			title="Install the packages of the application again, then serve it"
			disabled={connection !== 'open' || !projectName}
			value=""
			onchange={(event) => {
				const install = event.currentTarget.value;
				event.currentTarget.value = '';
				if (install) {
					send('build_dev', { install });
				}
			}}
		>
			<option value="" disabled>Packages…</option>
			<option value="update">Update packages and serve</option>
			<option value="reinstall">Re-install packages and serve</option>
		</select>
		<select
			class="studio-builder__select input-common"
			aria-label="Build locally"
			title="Build the application in the DisplayObjects/mobile folder of the project"
			disabled={connection !== 'open' || !projectName}
			value=""
			onchange={(event) => {
				const mode = event.currentTarget.value;
				event.currentTarget.value = '';
				if (mode) {
					send('build_local', { mode });
				}
			}}
		>
			<option value="" disabled>Build locally…</option>
			{#each LOCAL_BUILDS as build (build.mode)}
				<option value={build.mode}>{build.label}</option>
			{/each}
		</select>
		<button
			type="button"
			class="button-secondary"
			disabled={connection !== 'open' || !running}
			title="Stop the development server or the build"
			onclick={() => send('kill')}
		>
			<Ico icon="mdi:stop" size={4} /> Stop
		</button>
		{#if url}
			<a class="studio-builder__link" href={url} target="_blank" rel="noopener">
				<Ico icon="mdi:open-in-new-variant" size={4} /> Open
			</a>
		{/if}
		{#if networkUrls.length}
			<span class="studio-builder__qr">
				<button
					type="button"
					class="studio-builder__icon"
					title="Show the QR code of the application for a mobile device on the same network"
					aria-label="QR code"
					aria-expanded={qrOpen}
					onclick={() => (qrOpen = !qrOpen)}
				>
					<Ico icon="mdi:qrcode" size={4} />
				</button>
				{#if qrOpen}
					<div class="studio-builder__qr-popup" role="dialog" aria-label="QR code">
						<p>Flash it from a mobile device on the same network:</p>
						<select class="input-common" aria-label="Network" bind:value={qrUrl}>
							{#each networkUrls as network (network.url)}
								<option value={network.url}>{network.name} - {network.url}</option>
							{/each}
						</select>
						{#if qrUrl}
							<img src={`${endpoint()}/qrcode?d=${encodeURIComponent(qrUrl)}`} alt={qrUrl} />
						{/if}
					</div>
				{/if}
			</span>
		{/if}
		{#if autoBuildSupported}
			<label
				class="studio-builder__auto"
				title="Write the sources of the application while it is edited; when off, they are written once it is on again"
			>
				<input
					type="checkbox"
					checked={autoBuild}
					disabled={connection !== 'open' || !projectName}
					onchange={(event) => send('auto_build', { value: event.currentTarget.checked })}
				/>
				Auto build
			</label>
		{/if}
		{#if progress >= 0 && progress < 100}
			<span class="studio-builder__progress" title={`${progress}%`}>
				<span style:width={`${progress}%`}></span>
			</span>
		{/if}
		<span class={['studio-builder__status', failed && 'studio-builder__status--failed']}>
			{connection === 'open'
				? [projectName, status].filter(Boolean).join(' - ')
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

	.studio-builder__select {
		width: auto;
		height: 1.9rem;
		padding-block: 0;
		font-size: 0.75rem;
	}

	.studio-builder__auto {
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
		white-space: nowrap;
	}

	.studio-builder__qr {
		position: relative;
	}

	/* above the panel, which clips what overflows it */
	.studio-builder__qr-popup {
		position: fixed;
		z-index: 90;
		right: 1.5rem;
		bottom: 3.5rem;
		display: grid;
		width: 18rem;
		gap: 0.5rem;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 0.75rem 2rem color-mix(in oklab, black 30%, transparent);
		padding: 0.75rem;
	}

	.studio-builder__qr-popup p {
		margin: 0;
		color: var(--studio-text-idle);
	}

	.studio-builder__qr-popup select {
		height: 1.9rem;
		padding-block: 0;
		font-size: 0.72rem;
	}

	.studio-builder__qr-popup img {
		width: 100%;
		image-rendering: pixelated;
		background: white;
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

	.studio-builder__status--failed {
		color: var(--color-error-600-400);
	}
</style>
