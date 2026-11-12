/**
 * The Convertigo Assistant in the web Studio, as the Assistant view of the Eclipse Studio hosts it: the local
 * Assistant of the workspace (lib_ConvertigoAssistant) when its agent stack is installed, else the Assistant
 * hosted on assistant.convertigo.com, which offers to activate the local agent by importing the stack.
 */

export const HOSTED_ASSISTANT_URL = 'https://assistant.convertigo.com/';
export const AGENT_DOWNLOAD_URL = 'https://www.convertigo.com/developers/download-low-code-studio';
/** the onboarding of the local agent the Assistant expects from its host, as the Eclipse Studio */
export const AGENT_ONBOARDING_FEATURE_VERSION = '2026-07-02.agent-onboarding-v1';
/** the projects of the local agent stack, the only ones an activation imports */
export const AGENT_STACK_PROJECTS = [
	'lib_ConvertigoAssistant',
	'lib_ConvertigoMCP',
	'lib_ConvertigoAgentBridge'
];

/**
 * The page the Assistant panel opens: the URL of the preference, else the local Assistant once its stack is
 * ready, else the hosted Assistant.
 * @param {{ preference?: string, stackReady?: boolean, localUrl: string, dark?: boolean }} options
 * @returns {{ url: string, local: boolean }}
 */
export function chooseAssistantUrl({
	preference = '',
	stackReady = false,
	localUrl,
	dark = false
}) {
	const custom = preference.trim();
	if (custom) {
		return { url: custom, local: false };
	}
	if (stackReady) {
		return { url: localUrl, local: true };
	}
	const hosted = new URL(HOSTED_ASSISTANT_URL);
	hosted.searchParams.set('dark-theme', String(dark));
	return { url: hosted.toString(), local: false };
}

/**
 * What the Studio tells a remote Assistant about the local agent stack, as the context of the Eclipse Studio.
 * @param {{ state?: string, updateAllowed?: boolean, projects?: Record<string, { installed?: boolean, version?: string }> } | null} stack
 * @param {{ assistantUrl: string, localConvertigoUrl: string, localAssistantUrl: string, studioVersion?: string }} urls
 */
export function remoteAgentContext(stack, urls) {
	const projects = stack?.projects ?? {};
	const installed = (/** @type {string} */ name) => projects[name]?.installed === true;
	const version = (/** @type {string} */ name) => projects[name]?.version ?? '';
	const available = stack?.state === 'ready';
	return {
		assistantRuntime: 'remote',
		assistantUrl: urls.assistantUrl,
		localConvertigoUrl: urls.localConvertigoUrl,
		localAssistantUrl: urls.localAssistantUrl,
		downloadUrl: AGENT_DOWNLOAD_URL,
		agentOnboardingFeatureVersion: AGENT_ONBOARDING_FEATURE_VERSION,
		studioVersion: urls.studioVersion ?? '',
		canActivateLocalAgent: true,
		localAssistantInstalled: installed('lib_ConvertigoAssistant'),
		localMcpInstalled: installed('lib_ConvertigoMCP'),
		localAgentBridgeInstalled: installed('lib_ConvertigoAgentBridge'),
		localAssistantVersion: version('lib_ConvertigoAssistant'),
		localMcpVersion: version('lib_ConvertigoMCP'),
		localAgentBridgeVersion: version('lib_ConvertigoAgentBridge'),
		localAgentStackUpdateAllowed: stack?.updateAllowed !== false,
		localAgentStackAvailable: available,
		localAgentStackState: stack?.state ?? 'missing',
		localAgentStackLoading: stack?.state === 'loading',
		// the remote Assistant does not reach the agent of the Studio: it offers to activate it
		localAgentBridgeAvailable: false,
		agentBridgeAvailable: false
	};
}

/**
 * The projects an activation of the Assistant asks to import, the ones of the agent stack with an import URL.
 * @param {any} payload
 * @returns {{ name: string, importUrl: string, forceUpdate: boolean }[]}
 */
export function activationProjects(payload) {
	const projects = Array.isArray(payload?.projects) ? payload.projects : [];
	return projects
		.filter(
			(/** @type {any} */ project) =>
				AGENT_STACK_PROJECTS.includes(project?.name) &&
				typeof project?.importUrl === 'string' &&
				project.importUrl.trim()
		)
		.map((/** @type {any} */ project) => ({
			name: project.name,
			importUrl: project.importUrl.trim(),
			forceUpdate: payload?.forceUpdate === true || project.forceUpdate === true
		}));
}

/**
 * The URL of an external link the Assistant asks to open, only http and https.
 * @param {any} message
 * @returns {string}
 */
export function externalUrl(message) {
	const url = String(message?.url ?? message?.payload?.url ?? '').trim();
	return /^https?:\/\//i.test(url) ? url : '';
}
