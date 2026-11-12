import { describe, expect, it } from 'vitest';
import {
	activationProjects,
	chooseAssistantUrl,
	externalUrl,
	HOSTED_ASSISTANT_URL,
	remoteAgentContext
} from './assistantHost.js';

describe('assistantHost', () => {
	it('opens the local Assistant once its stack is ready, else the hosted one, unless a URL is chosen', () => {
		const localUrl =
			'http://localhost:28080/convertigo/projects/lib_ConvertigoAssistant/DisplayObjects/mobile/x';
		expect(chooseAssistantUrl({ stackReady: true, localUrl })).toEqual({
			url: localUrl,
			local: true
		});
		expect(chooseAssistantUrl({ stackReady: false, localUrl, dark: true })).toEqual({
			url: `${HOSTED_ASSISTANT_URL}?dark-theme=true`,
			local: false
		});
		expect(
			chooseAssistantUrl({ preference: ' https://example.com/a ', stackReady: true, localUrl })
		).toEqual({
			url: 'https://example.com/a',
			local: false
		});
	});

	it('tells a remote Assistant what the local agent stack has', () => {
		const context = remoteAgentContext(
			{
				state: 'missing',
				updateAllowed: true,
				projects: { lib_ConvertigoAssistant: { installed: true, version: '1.2' } }
			},
			{ assistantUrl: 'a', localConvertigoUrl: 'b', localAssistantUrl: 'c' }
		);
		expect(context.assistantRuntime).toBe('remote');
		expect(context.canActivateLocalAgent).toBe(true);
		expect(context.localAssistantInstalled).toBe(true);
		expect(context.localAssistantVersion).toBe('1.2');
		expect(context.localMcpInstalled).toBe(false);
		expect(context.localAgentStackAvailable).toBe(false);
		expect(context.agentBridgeAvailable).toBe(false);
	});

	it('imports only the projects of the agent stack', () => {
		expect(
			activationProjects({
				forceUpdate: true,
				projects: [
					{
						name: 'lib_ConvertigoMCP',
						importUrl: ' lib_ConvertigoMCP=https://github.com/x/y.git '
					},
					{ name: 'evil', importUrl: 'evil=https://example.com' },
					{ name: 'lib_ConvertigoAgentBridge' }
				]
			})
		).toEqual([
			{
				name: 'lib_ConvertigoMCP',
				importUrl: 'lib_ConvertigoMCP=https://github.com/x/y.git',
				forceUpdate: true
			}
		]);
		expect(activationProjects(null)).toEqual([]);
	});

	it('opens only http and https links outside', () => {
		expect(externalUrl({ url: 'https://convertigo.com' })).toBe('https://convertigo.com');
		expect(externalUrl({ payload: { url: 'http://a.b' } })).toBe('http://a.b');
		expect(externalUrl({ url: 'javascript:alert(1)' })).toBe('');
	});
});
