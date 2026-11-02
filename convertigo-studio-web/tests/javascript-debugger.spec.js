import { expect, test } from '@playwright/test';

// Opt-in live-engine recipe. Install engine/test/javascript-debugger/project as JavascriptDebuggerTest
// in a private runtime with web_studio=enabled and javascript.debugger=true, then set this URL.
const runtime = process.env.C8O_DEBUGGER_TEST_URL;
test.skip(!runtime, 'Requires the private JavaScript debugger test runtime');
test.setTimeout(60_000);

async function api(page, action, parameters = {}) {
	return page.evaluate(
		async ({ runtime, action, parameters }) => {
			const response = await fetch(`${runtime}/admin/services/studio.debug.Debugger`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/x-www-form-urlencoded',
					'x-xsrf-token': localStorage.getItem('x-xsrf-token') ?? 'Fetch'
				},
				body: new URLSearchParams({ action, ...parameters })
			});
			return { status: response.status, data: await response.json() };
		},
		{ runtime, action, parameters }
	);
}
async function launch(page, selected = true) {
	const { data } = await api(page, 'state');
	return page.evaluate(
		({ runtime, marker }) => {
			window.__debuggerFixtureRuns ??= [];
			const index = window.__debuggerFixtureRuns.length;
			window.__debuggerFixtureRuns.push(
				fetch(`${runtime}/projects/JavascriptDebuggerTest/.json`, {
					method: 'POST',
					headers: {
						'Content-Type': 'application/x-www-form-urlencoded',
						'x-xsrf-token': localStorage.getItem('x-xsrf-token') ?? 'Fetch'
					},
					body: new URLSearchParams({ __sequence: 'Probe', __context: '*', ...marker })
				}).then((response) => response.json())
			);
			return index;
		},
		{ runtime, marker: selected ? { __javascriptDebug: data.state.sessionId } : {} }
	);
}
async function completed(page, index) {
	return page.evaluate(
		async (index) => JSON.stringify(await window.__debuggerFixtureRuns[index]),
		index
	);
}
async function login(page, destination) {
	const response = await page.request.post(`${runtime}/admin/services/engine.Authenticate`, {
		form: { authType: 'login', authUserName: 'admin', authPassword: 'admin' },
		headers: { 'x-xsrf-token': 'Fetch' }
	});
	expect(response.status()).toBe(200);
	const token = response.headers()['x-xsrf-token'];
	if (token)
		await page.addInitScript((token) => localStorage.setItem('x-xsrf-token', token), token);
	await page.goto(`${runtime}/${destination}`);
	await api(page, 'stop');
}

// Release only this browser's own business session, so repeated runs fit the local edition's session limit.
test.afterEach(async ({ page }) => {
	if (!runtime || page.isClosed()) return;
	await api(page, 'stop').catch(() => {});
	const cookie = (await page.context().cookies(runtime)).find(
		(cookie) => cookie.name === 'JSESSIONID'
	);
	if (cookie)
		await page.evaluate(
			async ({ runtime, sessionId }) => {
				await fetch(`${runtime}/admin/services/connections.Delete`, {
					method: 'POST',
					headers: {
						'Content-Type': 'application/x-www-form-urlencoded',
						'x-xsrf-token': localStorage.getItem('x-xsrf-token') ?? 'Fetch'
					},
					body: new URLSearchParams({ sessionId })
				});
			},
			{ runtime, sessionId: cookie.value }
		);
});

test('server refuses Eclipse attachment and unauthenticated administration', async ({
	page,
	playwright
}) => {
	await login(page, 'admin/eclipse/javascript-debugger/');
	await api(page, 'start');
	const refusal = await api(page, 'studioRequests', { value: 'true' });
	expect(refusal.status).toBe(400);
	expect(refusal.data.error.message).toContain('requires Studio mode');
	await api(page, 'stop');
	const anonymous = await playwright.request.newContext();
	try {
		const response = await anonymous.post(`${runtime}/admin/services/studio.debug.Debugger`, {
			form: { action: 'start' },
			headers: { 'x-xsrf-token': 'Fetch' }
		});
		expect(response.status()).not.toBe(200);
	} finally {
		await anonymous.dispose();
	}
});

for (const embedded of [true, false]) {
	test(`${embedded ? 'embedded page' : 'Web Studio panel'}: breakpoint, scopes, watches, steps and detach`, async ({
		page
	}) => {
		await login(page, embedded ? 'admin/eclipse/javascript-debugger/' : 'studio/');
		if (!embedded) {
			await page
				.getByRole('button', { name: 'Show the logs, references and schema', exact: true })
				.click();
			await page.getByRole('tab', { name: 'Debug', exact: true }).click();
			await page
				.getByRole('region', { name: 'Debug', exact: true })
				.getByRole('button', { name: 'Maximize the view', exact: true })
				.click();
		}
		const panel = page.getByRole('region', { name: 'JavaScript debugger', exact: true });
		await panel.getByRole('button', { name: 'Start debugger', exact: true }).waitFor();
		if (embedded)
			await panel.getByRole('checkbox', { name: 'Debug Eclipse launches', exact: true }).uncheck();
		await panel.getByRole('button', { name: 'Start debugger', exact: true }).click();
		await panel.getByRole('button', { name: 'Stop debugger', exact: true }).waitFor();
		const breakpoint = await api(page, 'breakpoint', {
			id: 'JavascriptDebuggerTest.sq:Probe.st:Script',
			property: 'expression',
			line: '4',
			set: 'true'
		});
		expect(breakpoint.data.accepted).toBe(true);
		const first = await launch(page);
		await panel
			.getByRole('button', { name: 'add:4', exact: true })
			.waitFor({ timeout: 10000 })
			.catch(async () => {
				const reply = JSON.parse(await completed(page, first));
				throw new Error(
					`Fixture did not pause: ${reply.document?.error?.message ?? 'Request finished without debugger source'}`
				);
			});
		await expect(panel.getByRole('tabpanel', { name: 'Locals', exact: true })).toContainText('41');
		await panel.getByRole('tab', { name: 'this', exact: true }).click();
		await expect(panel.getByRole('tabpanel', { name: 'this', exact: true })).toContainText('probe');
		await panel.getByRole('tab', { name: 'Alls', exact: true }).click();
		await expect(panel.getByRole('tabpanel', { name: 'Alls', exact: true })).toContainText(
			'globalValue'
		);
		await panel.getByRole('textbox', { name: 'Watch expression', exact: true }).fill('local');
		await panel.getByRole('button', { name: 'Add watch', exact: true }).click();
		await expect(panel.locator('pre')).toHaveText(['41']);
		await panel.getByRole('button', { name: `${breakpoint.data.url}:7`, exact: true }).click();
		await expect(panel.locator('pre')).toHaveText(['Evaluation failed: EcmaError']);
		await panel.getByRole('button', { name: 'add:4', exact: true }).click();
		await expect(panel.locator('pre')).toHaveText(['41']);
		await panel.getByRole('tab', { name: 'this', exact: true }).click();
		if (process.env.C8O_DEBUGGER_SCREENSHOT_DIR)
			await panel.screenshot({
				path: `${process.env.C8O_DEBUGGER_SCREENSHOT_DIR}/${embedded ? 'embedded' : 'web-studio'}.png`
			});

		const ordinary = await launch(page, false);
		expect(await completed(page, ordinary)).toContain('42');
		const second = await launch(page);
		expect(await completed(page, second)).toContain('already has a running request');
		await expect(
			panel.getByRole('status').filter({ hasText: 'Another Eclipse or web request was refused' })
		).toBeVisible();
		await panel.getByRole('button', { name: 'Step over', exact: true }).click();
		await panel.getByRole('button', { name: 'add:5', exact: true }).waitFor();
		await expect(panel.locator('pre')).toHaveText(['42']);
		await panel
			.getByRole('textbox', { name: 'Evaluate an expression', exact: true })
			.fill('local + 1');
		await panel.getByRole('button', { name: 'Evaluate', exact: true }).click();
		await expect(panel.locator('pre')).toContainText(['43', '42']);
		await panel.getByRole('button', { name: 'Step out', exact: true }).click();
		await expect(panel.getByRole('button', { name: 'Step into', exact: true })).toBeEnabled();
		await panel.getByRole('button', { name: 'Continue', exact: true }).click();
		expect(await completed(page, first)).toContain('42');
		await expect(panel.getByRole('button', { name: 'Pause next', exact: true })).toBeEnabled();
		await panel.getByRole('button', { name: 'Pause next', exact: true }).click();
		const next = await launch(page);
		await expect(panel.getByRole('button', { name: 'Step into', exact: true })).toBeEnabled();
		await panel.getByRole('button', { name: 'Step into', exact: true }).click();
		await expect(panel.getByRole('button', { name: 'Continue', exact: true })).toBeEnabled();
		await panel.getByRole('button', { name: 'Stop debugger', exact: true }).click();
		await expect(panel.getByRole('button', { name: 'Start debugger', exact: true })).toBeVisible();
		expect(await completed(page, next)).toContain('42');
		await expect(
			panel.getByRole('button', { name: 'Remove watch local', exact: true })
		).toHaveCount(0);
	});
}
