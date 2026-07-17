import { test, expect, chromium } from '@playwright/test';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const appUrl = process.env.RETAILSTORE_URL ?? 'http://localhost:19080/convertigo/projects/sample_RetailStoreFlowRun6/DisplayObjects/mobile/index.html';
const profileDir = path.join(os.tmpdir(), 'retailstore-flow-run6-profile');

test('online seed/pull followed by offline catalog and persistent relaunch', async () => {
  test.setTimeout(240_000);
  fs.rmSync(profileDir, { recursive: true, force: true });
  const metrics = {};
  const navigationStart = Date.now();
  let context = await chromium.launchPersistentContext(profileDir, { headless: true });
  let page = context.pages()[0] ?? await context.newPage();
  const browserErrors = [];
  page.on('pageerror', error => browserErrors.push(String(error)));

  await page.goto(appUrl, { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('button', { name: 'RAYONS', exact: true })).toBeVisible({ timeout: 120_000 });
  metrics.synchronizationCompletionMs = Date.now() - navigationStart;
  const catalogButtons = () => page.locator('button:visible').filter({ hasNotText: /^(RAYONS|\+|−)$/ });
  await expect.poll(async () => catalogButtons().count(), { timeout: 60_000 }).toBeGreaterThan(0);
  metrics.firstLocalViewCompletionMs = Date.now() - navigationStart;
  metrics.firstUsefulRootRenderMs = Date.now() - navigationStart;
  await expect.poll(async () => page.locator('img[alt="Category"]').first().evaluate(img => img.naturalWidth)).toBeGreaterThan(0);
  expect(await page.locator('body').innerText()).not.toMatch(/100\s*\/\s*100/);

  await context.setOffline(true);
  const detail = page.getByText('PRODUCT', { exact: true }).locator('..');
  let detailReady = false;
  const trail = [];
  for (let depth = 0; depth < 12 && !detailReady; depth += 1) {
    const candidates = catalogButtons();
    await expect.poll(() => candidates.count(), { timeout: 15_000 }).toBeGreaterThan(0);
    trail.push((await candidates.last().innerText()).trim());
    await candidates.last().click();
    await page.waitForTimeout(350);
    const name = (await detail.locator('p').nth(1).innerText()).trim();
    detailReady = name.length > 0 && name !== 'Text' && name !== 'Select a product';
  }
  expect(detailReady).toBeTruthy();
  await expect(detail.locator('p').nth(2)).toContainText('€');
  await expect.poll(async () => detail.locator('img').evaluate(img => img.naturalWidth)).toBeGreaterThan(0);
  metrics.firstSuccessfulLocalGetMs = Date.now() - navigationStart;

  const totalAtOne = await detail.locator('p').nth(4).innerText();
  await detail.getByRole('button', { name: '+', exact: true }).click();
  await expect(detail.locator('p').nth(3)).toContainText('2');
  await expect.poll(async () => detail.locator('p').nth(4).innerText()).not.toBe(totalAtOne);
  await detail.getByRole('button', { name: '−', exact: true }).click();
  await detail.getByRole('button', { name: '−', exact: true }).click();
  await expect(detail.locator('p').nth(3)).toContainText('1');

  for (const ancestor of trail.slice(0, -1)) {
    await expect(page.getByRole('button', { name: ancestor, exact: true })).toBeVisible();
  }
  const historyLength = await page.evaluate(() => history.length);
  await page.getByRole('button', { name: 'RAYONS', exact: true }).click();
  await expect.poll(async () => catalogButtons().count(), { timeout: 15_000 }).toBeGreaterThan(0);
  expect(await page.evaluate(() => history.length)).toBe(historyLength);

  expect(browserErrors).toEqual([]);
  await context.setOffline(false);
  await context.close();

  const secondLaunchStart = Date.now();
  context = await chromium.launchPersistentContext(profileDir, { headless: true });
  page = context.pages()[0] ?? await context.newPage();
  await page.goto(appUrl, { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('button', { name: 'RAYONS', exact: true })).toBeVisible({ timeout: 120_000 });
  await expect.poll(async () => page.locator('button:visible').filter({ hasNotText: /^(RAYONS|\+|−)$/ }).count(), { timeout: 60_000 }).toBeGreaterThan(0);
  expect(await page.locator('body').innerText()).not.toMatch(/100\s*\/\s*100/);
  metrics.secondPersistentLaunchCompletionMs = Date.now() - secondLaunchStart;
  await context.close();

  console.log(`CAMPAIGN_BROWSER_METRICS=${JSON.stringify(metrics)}`);
});
