<script>
	import Light from '#lib/common/Light.svelte.js';
	import { call, getFrontendUrl } from '#lib/utils/service.js';
	import { browser } from '$app/env';
	import { onMount, untrack } from 'svelte';
	import {
		activationProjects,
		chooseAssistantUrl,
		externalUrl,
		remoteAgentContext
	} from './assistantHost.js';
	import { studioPreferences } from './studioPreferences.svelte.js';

	/**
	 * The Convertigo Assistant, as the Assistant view of the Eclipse Studio: the local Assistant of the workspace
	 * when its agent stack is installed, else the Assistant hosted on assistant.convertigo.com, which offers to
	 * activate the local agent; the Studio then imports the stack and opens the local Assistant.
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
	let activating = false;

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

	onMount(() => {
		void loadStack().then(chooseAssistant);
	});

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
	 * Activates the local agent the remote Assistant asks for: imports the projects of the stack, then opens
	 * the local Assistant, as the Eclipse Studio.
	 * @param {any} payload
	 */
	async function activateLocalAgent(payload) {
		if (activating) {
			return;
		}
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
		activating = true;
		try {
			postActivationStatus('running', 'Activating the local Assistant…', '', false);
			for (const project of projects) {
				const installed = stack?.projects?.[project.name]?.installed === true;
				if (installed && !project.forceUpdate) {
					postActivationStatus(
						'running',
						`${project.name} is already installed.`,
						project.name,
						false
					);
					continue;
				}
				postActivationStatus(
					'running',
					`${installed ? 'Updating' : 'Importing'} ${project.name}…`,
					project.name,
					false
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
					postActivationStatus('error', `Activation failed: ${reason}`, project.name, true);
					return;
				}
				stack = result;
			}
			await loadStack();
			if (stack?.state !== 'ready') {
				postActivationStatus(
					'error',
					'The agent stack is not ready yet: open the Assistant again in a moment.',
					'',
					true
				);
				return;
			}
			postActivationStatus('success', 'Local Assistant activated.', '', true);
			// the local Assistant replaces the hosted one, unless a page is set in the preferences
			iframeReady = false;
			chooseAssistant();
		} finally {
			activating = false;
		}
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
			void activateLocalAgent(event.data?.payload);
		} else if (type === openExternalType) {
			const url = externalUrl(event.data);
			if (url) {
				window.open(url, '_blank', 'noopener,noreferrer');
			}
		}
	}
</script>

<svelte:window onmessage={onAssistantMessage} />

{#if assistant}
	{#key assistant.url}
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

<style>
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
