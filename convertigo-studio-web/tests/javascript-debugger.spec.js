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
			/** @type {any} */ (window).__debuggerFixtureRuns ??= [];
			const index = /** @type {any} */ (window).__debuggerFixtureRuns.length;
			/** @type {any} */ (window).__debuggerFixtureRuns.push(
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
		async (index) => JSON.stringify(await /** @type {any} */ (window).__debuggerFixtureRuns[index]),
		index
	);
}
async function editorState(page) {
	return page.evaluate(() => {
		const monaco = /** @type {any} */ (window).monaco;
		const editor = monaco?.editor
			.getEditors()
			.find((editor) => editor.getDomNode()?.closest('.studio-editor__monaco'));
		if (!editor) return null;
		return {
			readOnly: editor.getOption(monaco.editor.EditorOption.readOnly),
			line: editor
				.getModel()
				.getAllDecorations()
				.find((decoration) => decoration.options.className === 'studio-editor-current-line')?.range
				.startLineNumber,
			text: editor.getValue()
		};
	});
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
		if (embedded)
			await page.addInitScript(() => {
				/** @type {any} */ (window).__debugFrames = [];
				/** @type {any} */ (window).java = {
					receiveFromJS: (message) =>
						/** @type {any} */ (window).__debugFrames.push(JSON.parse(message))
				};
			});
		await login(page, embedded ? 'admin/eclipse/javascript-debugger/' : 'studio/');
		if (!embedded) {
			await page
				.getByRole('button', { name: 'Show the logs, references and schema', exact: true })
				.click();
			await page.getByRole('tab', { name: 'Debug', exact: true }).click();
		}
		const panel = page.getByRole('region', { name: 'JavaScript debugger', exact: true });
		await panel.getByRole('button', { name: 'Start debugger', exact: true }).waitFor();
		if (embedded) {
			await panel.getByText('Debug Eclipse launches', { exact: true }).click();
			await expect(
				panel.getByRole('checkbox', { name: 'Debug Eclipse launches', exact: true })
			).not.toBeChecked();
		}
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
		await panel.getByRole('tab', { name: 'All scopes', exact: true }).click();
		await panel.getByRole('button', { name: 'Expand Parent 1', exact: true }).click();
		await expect(panel.getByRole('tabpanel', { name: 'All scopes', exact: true })).toContainText(
			'globalValue'
		);
		await panel.getByRole('textbox', { name: 'Watch expression', exact: true }).fill('local');
		await panel.getByRole('button', { name: 'Add watch', exact: true }).click();
		await expect(panel.locator('pre')).toHaveText(['41']);
		await panel.getByRole('button', { name: 'Script:7', exact: true }).click();
		await expect(panel.locator('pre')).toHaveText(['ReferenceError: \"local\" is not defined.']);
		if (!embedded)
			await expect.poll(() => editorState(page)).toMatchObject({ line: 7, readOnly: false });
		await panel.getByRole('button', { name: 'add:4', exact: true }).click();
		await expect(panel.locator('pre')).toHaveText(['41']);
		if (embedded) {
			const messages = await page.evaluate(() => /** @type {any} */ (window).__debugFrames);
			expect(messages.at(-1)).toMatchObject({ type: 'debugger.frame', frame: 0 });
			expect(messages.some((message) => message.frame === 1)).toBe(true);
		}
		await panel.getByRole('tab', { name: 'this', exact: true }).click();
		await expect(panel.getByLabel('Call stack')).toContainText(
			'JavascriptDebuggerTest › Probe › Script › expression'
		);
		await expect(panel.getByLabel('Source editor', { exact: true })).toHaveCount(0);
		await expect(
			panel.getByText('Run a sequence from this view (optional)', { exact: true })
		).toHaveCount(0);
		if (embedded) await page.setViewportSize({ width: 700, height: 1050 });
		else {
			await expect(
				page.locator('.studio-editor__monaco .studio-editor-current-line').first()
			).toBeVisible();
			await expect(
				page.locator('.studio-editor__monaco .studio-editor-breakpoint').first()
			).toBeVisible();
			await expect(panel.locator('.monaco-editor')).toHaveCount(0);
			await expect.poll(() => editorState(page)).toMatchObject({ line: 4, readOnly: false });
			const marker = page.locator('.studio-editor__monaco .studio-editor-breakpoint').first();
			const bounds = await marker.boundingBox();
			if (!bounds) throw new Error('The main editor breakpoint marker is not visible.');
			await marker.click();
			await expect
				.poll(
					async () =>
						(await api(page, 'breakpoints', { url: breakpoint.data.url })).data.breakpoints
				)
				.toEqual([]);
			await page.mouse.click(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
			await expect
				.poll(
					async () =>
						(await api(page, 'breakpoints', { url: breakpoint.data.url })).data.breakpoints
				)
				.toEqual([4]);
			// Resize the bottom group with its real sash, leaving the main editor visible.
			const sash = await page
				.locator('.dv-sash.dv-enabled')
				.evaluateAll((nodes) =>
					nodes
						.map((node) => node.getBoundingClientRect().toJSON())
						.find((box) => box.width > 500 && box.height < 10)
				);
			await page.mouse.move(sash.x + sash.width / 2, sash.y + sash.height / 2);
			await page.mouse.down();
			await page.mouse.move(sash.x + sash.width / 2, 450, { steps: 8 });
			await page.mouse.up();
		}
		const overflow = await panel.evaluate(
			(element) => element.scrollWidth > element.clientWidth + 1
		);
		expect(overflow).toBe(false);
		await panel.evaluate((element) => {
			element.scrollTop = 0;
		});
		if (process.env.C8O_DEBUGGER_SCREENSHOT_DIR) {
			for (const theme of ['light', 'dark']) {
				await page.evaluate((theme) => {
					document.documentElement.classList.toggle('dark', theme === 'dark');
					/** @type {any} */ (window).monaco?.editor.setTheme(theme === 'dark' ? 'vs-dark' : 'vs');
				}, theme);
				await page.screenshot({
					path: `${process.env.C8O_DEBUGGER_SCREENSHOT_DIR}/${embedded ? 'embedded' : 'web-studio'}-${theme}.png`
				});
			}
		}

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
		await expect(panel.locator('pre')).toContainText(['42', '43']);
		await panel.getByRole('button', { name: 'Step out', exact: true }).click();
		await expect(panel.locator('.debug-evaluation')).toHaveCount(0);
		await expect(panel.getByRole('button', { name: 'Step into', exact: true })).toBeEnabled();
		await api(page, 'breakpoint', {
			url: 'JavascriptDebuggerTest/scripts/helper.js',
			line: '2',
			set: 'true'
		});
		await panel.getByRole('button', { name: 'Continue', exact: true }).click();
		await panel.getByRole('button', { name: 'Script:2', exact: true }).waitFor();
		await expect(panel.getByLabel('Call stack')).toContainText(
			'JavascriptDebuggerTest › scripts/helper.js'
		);
		if (!embedded)
			await expect
				.poll(() => editorState(page))
				.toMatchObject({
					line: 2,
					readOnly: false,
					text: expect.stringContaining('includedValue += 1;')
				});
		if (!embedded && process.env.C8O_DEBUGGER_SCREENSHOT_DIR)
			await page.screenshot({
				path: `${process.env.C8O_DEBUGGER_SCREENSHOT_DIR}/included-file.png`
			});
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
		await expect(
			panel.getByRole('status').filter({ hasText: 'The paused request resumes and finishes' })
		).toBeVisible();
		expect(await completed(page, next)).toContain('42');
		await expect(
			panel.getByRole('button', { name: 'Remove watch local', exact: true })
		).toHaveCount(0);
	});
}

test('embedded variables: lazy objects, arrays, Java fields, pagination and complete long values', async ({
	page
}) => {
	await login(page, 'admin/eclipse/javascript-debugger/');
	await page.setViewportSize({ width: 700, height: 1050 });
	const panel = page.getByRole('region', { name: 'JavaScript debugger', exact: true });
	await panel.getByText('Debug Eclipse launches', { exact: true }).click();
	await expect(
		panel.getByRole('checkbox', { name: 'Debug Eclipse launches', exact: true })
	).not.toBeChecked();
	await panel.getByRole('button', { name: 'Start debugger', exact: true }).click();
	await api(page, 'breakpoint', {
		id: 'JavascriptDebuggerTest.sq:Probe.st:Script',
		property: 'expression',
		line: '4',
		set: 'true'
	});
	const run = await launch(page);
	await panel.getByRole('button', { name: 'add:4', exact: true }).waitFor();
	await panel
		.getByRole('textbox', { name: 'Evaluate an expression', exact: true })
		.fill(
			'this.tree={items:[1,{answer:42}]}; this.many=Array.from({length:230},(_,i)=>i); this.longText="x".repeat(1200); this.javaData=new java.awt.Point(2,3); this.tree'
		);
	await panel.getByRole('button', { name: 'Evaluate', exact: true }).click();
	await expect(panel.locator('.debug-evaluation')).toContainText('[Object]');
	await panel.getByRole('tab', { name: 'this', exact: true }).click();
	const variables = panel.getByRole('tabpanel', { name: 'this', exact: true });
	await expect(variables.getByRole('button', { name: 'Expand tree', exact: true })).toBeVisible();
	const propertyCalls = [];
	page.on('request', (request) => {
		if (
			request.url().endsWith('studio.debug.Debugger') &&
			request.postData()?.includes('action=properties')
		)
			propertyCalls.push(request.postData());
	});
	expect(propertyCalls).toHaveLength(0);
	await variables.getByRole('button', { name: 'Expand tree', exact: true }).click();
	await variables.getByRole('button', { name: 'Expand items', exact: true }).click();
	await variables.getByRole('button', { name: 'Expand 1', exact: true }).click();
	await expect(variables.getByRole('row').filter({ hasText: 'answer' })).toContainText('42');
	await variables.getByRole('button', { name: 'Expand javaData', exact: true }).click();
	await expect(
		variables
			.getByRole('row')
			.filter({ has: page.getByRole('button', { name: 'Show value of x', exact: true }) })
	).toContainText('2');
	await variables.getByRole('button', { name: 'Expand many', exact: true }).click();
	for (let i = 0; i < 2; i++)
		await variables
			.getByRole('button', { name: 'Load next 100 properties of many', exact: true })
			.click();
	await expect(
		variables.getByRole('button', { name: 'Show value of 229', exact: true })
	).toHaveText('229');
	await expect(
		variables.getByRole('button', { name: 'Load next 100 properties of many', exact: true })
	).toHaveCount(0);
	await variables.getByRole('button', { name: 'Collapse many', exact: true }).click();
	await variables.getByRole('button', { name: 'Show value of longText', exact: true }).click();
	await expect(
		variables.getByRole('region', { name: 'Value of longText', exact: true }).locator('pre')
	).toHaveText('x'.repeat(1200));
	await variables.getByRole('button', { name: 'Close full value', exact: true }).click();
	expect(propertyCalls.length).toBeGreaterThanOrEqual(7);
	if (process.env.C8O_DEBUGGER_SCREENSHOT_DIR)
		await page.screenshot({ path: `${process.env.C8O_DEBUGGER_SCREENSHOT_DIR}/variable-tree.png` });
	await panel.getByRole('button', { name: 'Stop debugger', exact: true }).click();
	expect(await completed(page, run)).toContain('42');
});

test('standalone request status and evaluations are cleared when a new run pauses', async ({
	page
}) => {
	await login(page, 'javascript-debugger/');
	const panel = page.getByRole('region', { name: 'JavaScript debugger', exact: true });
	await panel.getByRole('button', { name: 'Start debugger', exact: true }).click();
	await panel.getByText('Run a sequence from this view (optional)', { exact: true }).click();
	await panel.getByRole('textbox', { name: 'Project', exact: true }).fill('JavascriptDebuggerTest');
	await panel.getByRole('textbox', { name: 'Sequence', exact: true }).fill('Probe');
	await panel.getByRole('button', { name: 'Run sequence', exact: true }).click();
	await expect(panel.getByRole('status').filter({ hasText: 'Request finished.' })).toBeVisible();
	await api(page, 'breakpoint', {
		id: 'JavascriptDebuggerTest.sq:Probe.st:Script',
		property: 'expression',
		line: '4',
		set: 'true'
	});
	await panel.getByRole('button', { name: 'Run sequence', exact: true }).click();
	await panel.getByRole('button', { name: 'add:4', exact: true }).waitFor();
	await expect(panel.getByRole('status').filter({ hasText: 'Request finished.' })).toHaveCount(0);
	await panel
		.getByRole('textbox', { name: 'Evaluate an expression', exact: true })
		.fill('local + 1');
	await panel.getByRole('button', { name: 'Evaluate', exact: true }).click();
	await expect(panel.locator('.debug-evaluation')).toContainText('42');
	await panel.getByRole('button', { name: 'Continue', exact: true }).click();
	await expect(panel.getByRole('button', { name: 'Run sequence', exact: true })).toBeEnabled();
	await panel.getByRole('button', { name: 'Run sequence', exact: true }).click();
	await panel.getByRole('button', { name: 'add:4', exact: true }).waitFor();
	await expect(panel.locator('.debug-evaluation')).toHaveCount(0);
	await expect(panel.getByRole('status').filter({ hasText: 'Request finished.' })).toHaveCount(0);
	await panel.getByRole('button', { name: 'Stop debugger', exact: true }).click();
});
