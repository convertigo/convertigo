import { describe, expect, it } from 'vitest';
import {
	activationProjects,
	chooseAssistantUrl,
	externalUrl,
	HOSTED_ASSISTANT_URL,
	localAgentContext,
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

	it('tells the local Assistant of a desktop Studio its version and stack, as the Eclipse Studio', () => {
		const installed = { installed: true, version: '1.6.0' };
		const urls = { assistantUrl: 'a', localConvertigoUrl: 'b', localAssistantUrl: 'c' };
		const ready = localAgentContext(
			{
				state: 'ready',
				studioVersion: '8.5.0.m001',
				projects: {
					lib_ConvertigoAssistant: installed,
					lib_ConvertigoMCP: installed,
					lib_ConvertigoAgentBridge: installed
				}
			},
			urls
		);
		// the Assistant offers its agent to a local Studio from 8.4.4 with its stack available
		expect(ready.assistantRuntime).toBe('local');
		expect(ready.studioVersion).toBe('8.5.0.m001');
		expect(ready.localAgentStackAvailable).toBe(true);
		expect(ready.localAgentBridgeAvailable).toBe(true);
		expect(ready.agentBridgeAvailable).toBe(true);
		const loading = localAgentContext({ state: 'loading', projects: {} }, urls);
		expect(loading.localAgentStackAvailable).toBe(false);
		expect(loading.agentBridgeAvailable).toBe(false);
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

describe('assistantHost releases', () => {
	it('orders the versions of the stack', async () => {
		const { compareVersions } = await import('./assistantHost.js');
		expect(compareVersions('1.5.9', '1.6.0-beta.5')).toBeLessThan(0);
		expect(compareVersions('1.6.0', '1.6.0-beta.5')).toBeGreaterThan(0);
		expect(compareVersions('1.6.0-beta.10', '1.6.0-beta.5')).toBeGreaterThan(0);
		expect(compareVersions('v0.2.29', '0.2.29')).toBe(0);
	});

	it('installs the missing projects and updates the older releases', async () => {
		const { stackChanges } = await import('./assistantHost.js');
		const manifest = {
			projects: [
				{ name: 'lib_ConvertigoMCP', version: '0.3.0-beta.1', importUrl: 'm' },
				{ name: 'lib_ConvertigoAgentBridge', version: '0.4.31', importUrl: 'b' },
				{ name: 'lib_ConvertigoAssistant', version: '1.6.0-beta.5', importUrl: 'a' }
			]
		};
		const stack = {
			updateAllowed: true,
			projects: {
				lib_ConvertigoMCP: { installed: true, version: '0.2.29' },
				lib_ConvertigoAgentBridge: { installed: true, version: '0.5.0-beta.3' }
			}
		};
		expect(
			stackChanges(stack, manifest).map((change) => [change.name, change.forceUpdate])
		).toEqual([
			['lib_ConvertigoMCP', true],
			['lib_ConvertigoAssistant', false]
		]);
		expect(
			stackChanges({ ...stack, updateAllowed: false }, manifest).map((change) => change.name)
		).toEqual(['lib_ConvertigoAssistant']);
	});
});
