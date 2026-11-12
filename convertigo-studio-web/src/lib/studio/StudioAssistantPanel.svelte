<script>
	import Light from '#lib/common/Light.svelte.js';
	import Projects from '#lib/common/Projects.svelte.js';
	import { call, getFrontendUrl } from '#lib/utils/service.js';
	import { browser } from '$app/env';
	import { onMount, untrack } from 'svelte';
	import {
		activationProjects,
		chooseAssistantUrl,
		externalUrl,
		remoteAgentContext,
		stackChanges
	} from './assistantHost.js';
	import { saveStudioPreferences, studioPreferences } from './studioPreferences.svelte.js';

	/**
	 * The Convertigo Assistant, as the Assistant view of the Eclipse Studio: the local Assistant of the workspace
	 * when its agent stack is installed, else the Assistant hosted on assistant.convertigo.com, which offers to
	 * activate the local agent; the Studio then imports the stack and opens the local Assistant. A bar installs
	 * or updates the stack from the built releases of the published manifest (stable or preview channel).
	 *
	 * @type {{ projectName?: string, agentProfile?: string }}
	 */
	let { projectName = '', agentProfile = 'generalist' } = $props();

	const assistantProject = 'lib_ConvertigoAssistant';
	const assistantContextType = `${assistantProject}.context`;
	const assistantContextRequestType = `${assistantContextType}.request`;
	const activateType = `${assistantProject}.activateLocalAgent`;
	const activationStatusType = `${activateType}.status`;
	const openExternalType = `${assistantProject}.openExternal`;
	const assistantBaseUrl = getFrontendUrl(assistantProject).replace(/index\.html$/, '');
	const assistantAgentUrl = `${assistantBaseUrl}path-to-xfirst/:threadid`;
	let iframe = $state();
	let iframeReady = $state(false);
	/** the local agent stack, as studio.assistant.Stack tells it */
	let stack = $state(/** @type {any} */ (null));
	/** the page of the panel: chosen once, then only by an activation, never by a selection or a profile */
	let assistant = $state(/** @type {{ url: string, local: boolean } | null} */ (null));
	let activating = $state(false);
	/** the built releases of the stack the manifest of the channel lists */
	let manifest = $state(/** @type {any} */ (null));
	let manifestError = $state('');
	let channel = $state(
		/** @type {'stable' | 'preview'} */ (
			studioPreferences.assistantChannel === 'preview' ? 'preview' : 'stable'
		)
	);
	/** what the installation of the stack is doing, or what stopped it */
	let progress = $state('');
	let installError = $state('');
	/** a new page of the same Assistant, after an update */
	let frameSerial = $state(0);
	let changes = $derived(stack && manifest ? stackChanges(stack, manifest) : []);
	let showBar = $derived(
		!String(studioPreferences.assistantUrl ?? '').trim() &&
			stack !== null &&
			(stack.state !== 'ready' || changes.length > 0 || activating || Boolean(installError))
	);
	let barText = $derived.by(() => {
		if (activating) {
			return progress;
		}
		if (installError) {
			return installError;
		}
		if (manifestError) {
			return manifestError;
		}
		const assistantRelease = manifest?.projects?.find(
			(/** @type {any} */ project) => project.name === assistantProject
		);
		if (stack?.state !== 'ready') {
			return `The local Assistant is not installed${assistantRelease ? `: ${channel} release ${assistantRelease.version}` : ''}.`;
		}
		const installed = stack?.projects?.[assistantProject]?.version ?? '';
		return `Local Assistant ${installed}: newer ${channel} releases are available (${changes
			.map((change) => `${change.name.replace(/^lib_Convertigo/, '')} ${change.to}`)
			.join(', ')}).`;
	});

	/** the local Assistant, with the agent of the server, as the Studio opened it before */
	function localAssistantUrl() {
		const query = new URLSearchParams({
			agentBridge: '1',
			serverAgent: '1',
			assistantMode: 'agent',
			assistantSurface: 'studio',
			assistantContext: 'studio',
			userId: 'studio',
			'dark-theme': String(untrack(() => Light.dark))
		});
		return `${assistantAgentUrl}?${query}`;
	}

	function absolute(/** @type {string} */ url) {
		return browser ? new URL(url, window.location.href).toString() : url;
	}

	/** the engine of the Studio, as the localConvertigoUrl of the Eclipse Studio */
	function localConvertigoUrl() {
		return absolute(assistantBaseUrl).replace(/\/projects\/.*$/, '');
	}

	async function loadStack() {
		const result = await call('studio.assistant.Stack', {}, { silentError: () => true });
		stack = result?.projects ? result : null;
	}

	function chooseAssistant() {
		assistant = chooseAssistantUrl({
			preference: String(studioPreferences.assistantUrl ?? ''),
			// an engine that does not tell its stack (an older one) keeps the local Assistant, as before
			stackReady: stack === null || stack?.state === 'ready',
			localUrl: localAssistantUrl(),
			dark: untrack(() => Light.dark)
		});
	}

	async function loadManifest() {
		manifestError = '';
		const wanted = channel;
		const result = await call(
			'studio.assistant.Stack',
			{ action: 'manifest', channel: wanted },
			{ silentError: () => true }
		);
		if (wanted !== channel) {
			return;
		}
		if (result?.manifest) {
			manifest = result.manifest;
		} else {
			manifest = null;
			manifestError = `The ${wanted} manifest of the Assistant cannot be read.`;
		}
	}

	onMount(() => {
		void loadStack().then(() => {
			chooseAssistant();
			if (stack !== null) {
				void loadManifest();
			}
		});
	});

	$effect(() => {
		// the channel chosen in the Studio preferences, as in the bar: the releases it offers are read again
		const preferred = studioPreferences.assistantChannel === 'preview' ? 'preview' : 'stable';
		if (preferred !== untrack(() => channel)) {
			channel = preferred;
			installError = '';
			if (untrack(() => stack) !== null) {
				void loadManifest();
			}
		}
	});

	/**
	 * @param {string} next
	 */
	function changeChannel(next) {
		channel = next === 'preview' ? 'preview' : 'stable';
		saveStudioPreferences({ assistantChannel: channel });
		installError = '';
		void loadManifest();
	}

	let assistantContext = $derived.by(() => {
		const common = {
			assistantSurface: 'studio',
			assistantContext: 'studio',
			agentProfile,
			skillProfile: agentProfile,
			userId: 'studio',
			darkTheme: Light.dark,
			theme: Light.dark ? 'dark' : 'light',
			projectContext: projectName,
			defaultProject: projectName,
			projectScope: projectName ? 'selected' : '',
			currentUrl: browser ? window.location.href : '',
			currentRoute: browser ? window.location.pathname : ''
		};
		if (assistant?.local) {
			return {
				...common,
				assistantRuntime: 'server',
				agentBridgeAvailable: true,
				localAgentBridgeAvailable: false
			};
		}
		return {
			...common,
			...remoteAgentContext(stack, {
				assistantUrl: assistant?.url ?? '',
				localConvertigoUrl: localConvertigoUrl(),
				localAssistantUrl: absolute(localAssistantUrl())
			})
		};
	});

	function assistantOrigin() {
		return assistant ? new URL(assistant.url, window.location.href).origin : '';
	}

	/**
	 * @param {Record<string, unknown>} message
	 */
	function postAssistantMessage(message) {
		if (!browser || !iframe?.contentWindow || !assistant) {
			return;
		}
		iframe.contentWindow.postMessage(message, assistantOrigin());
	}

	function sendAssistantContext() {
		const context = assistantContext;
		postAssistantMessage({ type: assistantContextType, payload: context });
		postAssistantMessage({
			type: 'select',
			projectName,
			...context
		});
	}

	function onAssistantLoad() {
		iframeReady = true;
		postAssistantMessage({ type: 'init' });
		sendAssistantContext();
	}

	/**
	 * @param {string} status running, success or error
	 * @param {string} message
	 * @param {string} name
	 * @param {boolean} done
	 */
	function postActivationStatus(status, message, name, done) {
		postAssistantMessage({
			type: activationStatusType,
			payload: {
				status,
				message,
				projectName: name,
				done,
				localAssistantUrl: absolute(localAssistantUrl())
			}
		});
	}

	/**
	 * Installs or updates projects of the stack, then opens the local Assistant (again), as the activation of the
	 * Eclipse Studio.
	 * @param {{ name: string, importUrl: string, forceUpdate: boolean }[]} projects
	 * @param {(status: string, message: string, name: string, done: boolean) => void} report
	 */
	async function installStack(projects, report) {
		if (activating) {
			return;
		}
		activating = true;
		installError = '';
		const tell = (
			/** @type {string} */ status,
			/** @type {string} */ message,
			name = '',
			done = false
		) => {
			progress = message;
			if (status === 'error') {
				installError = message;
			}
			report(status, message, name, done);
		};
		try {
			tell('running', 'Activating the local Assistant…');
			for (const [index, project] of projects.entries()) {
				const installed = stack?.projects?.[project.name]?.installed === true;
				if (installed && !project.forceUpdate) {
					tell('running', `${project.name} is already installed.`, project.name);
					continue;
				}
				tell(
					'running',
					`${installed ? 'Updating' : 'Importing'} ${project.name} (${index + 1}/${projects.length})…`,
					project.name
				);
				const result = await call(
					'studio.assistant.Stack',
					{
						action: 'install',
						name: project.name,
						importUrl: project.importUrl,
						forceUpdate: String(project.forceUpdate)
					},
					{ silentError: () => true }
				);
				if (result?.isError || result?.error || !result?.projects) {
					const reason = String(result?.error?.message ?? result?.error ?? 'unknown error');
					tell(
						'error',
						`The installation of ${project.name} failed: ${reason}`,
						project.name,
						true
					);
					return;
				}
				stack = result;
			}
			await loadStack();
			// the projects of the stack show in the tree
			void Projects.refresh();
			if (stack?.state !== 'ready') {
				tell(
					'error',
					'The agent stack is not ready yet: open the Assistant again in a moment.',
					'',
					true
				);
				return;
			}
			tell('success', 'Local Assistant activated.', '', true);
			// the local Assistant replaces the hosted one, or opens again in its new version
			iframeReady = false;
			frameSerial += 1;
			chooseAssistant();
		} finally {
			activating = false;
		}
	}

	/**
	 * Activates the local agent the remote Assistant asks for, with the projects it gives.
	 * @param {any} payload
	 */
	function activateLocalAgent(payload) {
		const projects = activationProjects(payload);
		if (!projects.length) {
			postActivationStatus(
				'error',
				'No project of the agent stack to import was given by the Assistant.',
				'',
				true
			);
			return;
		}
		void installStack(projects, postActivationStatus);
	}

	/** installs or updates the stack from the built releases of the manifest of the channel */
	function installFromManifest() {
		void installStack(changes, () => {});
	}

	/**
	 * @param {HTMLIFrameElement} node
	 */
	function assistantFrame(node) {
		iframe = node;
		return () => {
			if (iframe === node) {
				iframe = undefined;
				iframeReady = false;
			}
		};
	}

	/**
	 * @param {Record<string, any>} context
	 * @param {boolean} ready
	 * @param {string} url
	 * @returns {import('svelte/attachments').Attachment<HTMLIFrameElement>}
	 */
	function synchronizeAssistant(context, ready, url) {
		return (node) => {
			if (!browser || !ready || !node.contentWindow || !url) {
				return;
			}
			const origin = new URL(url, window.location.href).origin;
			node.contentWindow.postMessage({ type: assistantContextType, payload: context }, origin);
			// An empty selection is an update too: clear the previous ambient project.
			node.contentWindow.postMessage(
				{ type: 'select', projectName: context.defaultProject, ...context },
				origin
			);
		};
	}

	/**
	 * @param {MessageEvent} event
	 */
	function onAssistantMessage(event) {
		if (!browser || !assistant || event.source !== iframe?.contentWindow) {
			return;
		}
		if (event.origin !== assistantOrigin()) {
			return;
		}
		const type = event.data?.type;
		if (type === assistantContextRequestType) {
			sendAssistantContext();
		} else if (type === activateType) {
			activateLocalAgent(event.data?.payload);
		} else if (type === openExternalType) {
			const url = externalUrl(event.data);
			if (url) {
				window.open(url, '_blank', 'noopener,noreferrer');
			}
		}
	}
</script>

<svelte:window onmessage={onAssistantMessage} />

<div class="studio-assistant-panel">
	{#if showBar}
		<div class="studio-assistant-bar" role="region" aria-label="Local Assistant">
			<span class="studio-assistant-bar__text" role="status">{barText}</span>
			<select
				class="input-common"
				aria-label="Release channel"
				value={channel}
				disabled={activating}
				onchange={(event) => changeChannel(event.currentTarget.value)}
			>
				<option value="stable">Stable</option>
				<option value="preview">Preview</option>
			</select>
			<button
				type="button"
				class="button-primary"
				disabled={activating || !changes.length}
				onclick={installFromManifest}
			>
				{stack?.state === 'ready' ? 'Update' : 'Install'}
			</button>
		</div>
	{/if}
	{#if assistant}
		{#key `${assistant.url}#${frameSerial}`}
			<iframe
				{@attach assistantFrame}
				{@attach synchronizeAssistant(assistantContext, iframeReady, assistant.url)}
				src={assistant.url}
				title="Convertigo Assistant"
				class="studio-assistant"
				referrerpolicy="strict-origin-when-cross-origin"
				onload={onAssistantLoad}
			></iframe>
		{/key}
	{:else}
		<div class="studio-assistant studio-assistant--waiting" aria-busy="true">
			<span>Opening the Convertigo Assistant…</span>
		</div>
	{/if}
</div>

<style>
	.studio-assistant-panel {
		display: flex;
		height: 100%;
		min-height: 0;
		flex-direction: column;
	}

	.studio-assistant-panel > .studio-assistant {
		flex: 1 1 0;
		height: auto;
	}

	.studio-assistant-bar {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 0.45rem;
		border-bottom: 1px solid var(--studio-line);
		background: var(--studio-chrome-bg);
		padding: 0.4rem 0.6rem;
		font-size: 0.75rem;
	}

	.studio-assistant-bar__text {
		flex: 1 1 12rem;
		min-width: 0;
		color: var(--studio-text);
	}

	.studio-assistant-bar select {
		flex: none;
		width: auto;
		height: 1.8rem;
		padding-block: 0;
		padding-inline: 0.5rem;
		font-size: 0.75rem;
	}

	.studio-assistant-bar button {
		min-height: 1.8rem;
		padding-block: 0;
		font-size: 0.75rem;
	}

	.studio-assistant {
		display: block;
		width: 100%;
		height: 100%;
		min-height: 0;
		border: 0;
		background: var(--color-surface-50-950);
	}

	.studio-assistant--waiting {
		display: grid;
		place-content: center;
		gap: 0.4rem;
		color: var(--studio-text-idle);
		font-size: 0.8rem;
		text-align: center;
	}
</style>
