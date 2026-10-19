// Runs against the disposable topology in compose.yaml, never against a customer cluster.
const assert = require('node:assert/strict');
const { execFileSync } = require('node:child_process');
const fs = require('node:fs');
const { createRequire } = require('node:module');
const path = require('node:path');
const root = path.resolve(__dirname, '../../..');
const { chromium } = createRequire(path.join(root, 'convertigo-studio-web/package.json'))('@playwright/test');
const output = process.env.C8O_PROBE_OUTPUT || '/tmp/c8o-1231-ttl';
fs.mkdirSync(output, { recursive: true });
const base = 'http://127.0.0.1:29141/convertigo';
const targetContainer = 'c8o-1231-ttl-engine-b-1';
const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

(async () => {
	const browser = await chromium.launch({ headless: true });
	const page = await browser.newPage({ viewport: { width: 1600, height: 1000 } });
	const summary = {};
	const trace = [];
	let stopped = false;
	page.on('response', (response) => {
		if (!response.url().includes('/admin/services/')) return;
		const headers = response.headers();
		trace.push({ at: Date.now(), url: response.url(), status: response.status(),
			target: headers['x-c8o-instance'], unreachable: headers['x-c8o-instance-unreachable'] });
	});
	try {
		await page.goto(`${base}/login/`);
		await page.locator('[name="authUserName"]').fill('admin');
		await page.locator('[name="authPassword"]').fill('issue1231-test-only');
		await page.getByRole('button', { name: 'Login', exact: true }).click();
		await page.waitForURL('**/admin/**');
		const selector = page.locator('header select');
		await selector.waitFor();
		const local = await selector.inputValue();
		const xsrf = await page.evaluate(() => localStorage.getItem('x-xsrf-token'));
		const service = (name, headers = {}) => page.request.get(`${base}/admin/services/${name}`, {
			headers: { 'x-xsrf-token': xsrf || 'Fetch', ...headers }, timeout: 15000
		});
		const listed = await service('engine.ListInstances');
		assert.match(listed.headers()['content-type'], /json|javascript/, await listed.text());
		const instances = await listed.json();
		assert.equal(instances.storeMode, 'redis');
		const target = instances.instances.find((instance) => instance.instanceId !== local)?.instanceId;
		assert.ok(target, 'both instances must be registered before starting');
		await selector.selectOption(target);
		const targeted = () => service('engine.GetStatus', { 'X-C8O-Instance': target });
		const healthy = await targeted();
		assert.equal(healthy.status(), 200);
		assert.equal(healthy.headers()['x-c8o-instance'], target);
		summary.healthyForward = true;

		// An abrupt crash leaves discovery to expire naturally; a graceful shutdown may unregister it.
		execFileSync('docker', ['kill', targetContainer], { stdio: 'pipe' });
		stopped = true;
		const stoppedAt = Date.now();
		await page.getByRole('button', { name: 'Refresh instances', exact: true }).click();
		const banner = page.getByText(`Instance ${target} unreachable`, { exact: true });
		await banner.waitFor({ timeout: 20000 });
		assert.equal(await selector.isVisible(), true);
		assert.equal(await selector.inputValue(), target);
		await page.screenshot({ path: path.join(output, 'before-ttl.png') });
		summary.failedRefreshKeepsSelector = true;

		while (Date.now() - stoppedAt < 45000) {
			await delay(5000);
			assert.equal(await selector.inputValue(), target);
			assert.equal(await banner.isVisible(), true);
			console.log(`Target selection retained ${Math.floor((Date.now() - stoppedAt) / 1000)}s after crash`);
		}
		const discovery = await service('engine.ListInstances', { 'X-C8O-Instance': local });
		assert.equal((await discovery.json()).instances.some((instance) => instance.instanceId === target), false,
			'target discovery entry must actually be expired');
		for (let i = 0; i < 3; i++) {
			const failed = await targeted();
			assert.equal(failed.status(), 502);
			assert.equal(failed.headers()['x-c8o-instance'], target);
			assert.equal(failed.headers()['x-c8o-instance-unreachable'], target);
			assert.equal(failed.headers()['x-c8o-instance-local'], local);
			assert.match(failed.headers()['cache-control'], /no-store/);
			assert.match((await failed.json()).cause, /not registered in Redis/);
			await page.getByRole('button', { name: 'Refresh instances', exact: true }).click();
			await delay(500);
			assert.equal(await selector.isVisible(), true);
			assert.equal(await selector.inputValue(), target);
			assert.equal(await banner.isVisible(), true);
		}
		summary.expiredDiscoveryEntryConfirmed = true;
		summary.noFallbackAfterTtl = true;
		summary.elapsedAfterCrashSeconds = Math.floor((Date.now() - stoppedAt) / 1000);
		await page.screenshot({ path: path.join(output, 'after-ttl.png') });

		await page.getByRole('button', { name: `Use ${local}`, exact: true }).click();
		await delay(750);
		assert.equal(await selector.inputValue(), local);
		assert.equal(await banner.count(), 0);
		summary.explicitLocalRecovery = true;
		await page.screenshot({ path: path.join(output, 'explicit-local.png') });
		console.log(JSON.stringify(summary, null, 2));
	} finally {
		fs.writeFileSync(path.join(output, 'summary.json'), JSON.stringify(summary, null, 2));
		fs.writeFileSync(path.join(output, 'trace.json'), JSON.stringify(trace, null, 2));
		if (stopped) execFileSync('docker', ['start', targetContainer], { stdio: 'pipe' });
		await browser.close();
	}
})().catch((error) => { console.error(error); process.exitCode = 1; });
