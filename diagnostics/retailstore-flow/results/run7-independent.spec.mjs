import { test, expect, chromium } from '@playwright/test';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const appUrl = process.env.RETAILSTORE_URL
  ?? 'http://127.0.0.1:19080/convertigo/projects/sample_RetailStoreFlowRun7/DisplayObjects/mobile/';
const profileDir = path.join(os.tmpdir(), 'retailstore-flow-run7-independent-profile');
const evidenceDir = process.env.RUN7_EVIDENCE_DIR ?? path.resolve(process.cwd(), 'diagnostics/retailstore-flow/results');
const branch = ['EPICERIE SUCREE', 'GOUTERS & BISCUITS', 'BISCUITS AU CHOCOLAT'];

const catalogButtons = (page) => page.locator('button:visible').filter({
  hasNotText: /^(RAYONS|EPICERIE SUCREE|GOUTERS & BISCUITS|BISCUITS AU CHOCOLAT|Back|View product|Open category|\+|−)$/
});

async function assertNoHorizontalOverflow(page) {
  expect(await page.evaluate(() => (
    document.documentElement.scrollWidth <= document.documentElement.clientWidth
  ))).toBeTruthy();
}

async function assertVisibleImagesLoaded(page) {
  const images = page.locator('img:visible');
  expect(await images.count()).toBeGreaterThan(0);
  expect(await images.evaluateAll(nodes => nodes.every(image => image.naturalWidth > 0))).toBeTruthy();
}

test('independent strict RetailStore Flow Run7 acceptance', async () => {
  test.setTimeout(300_000);
  fs.rmSync(profileDir, { recursive: true, force: true });

  const startedAt = Date.now();
  const context = await chromium.launchPersistentContext(profileDir, {
    headless: true,
    viewport: { width: 390, height: 844 }
  });
  const page = context.pages()[0] ?? await context.newPage();
  const browserErrors = [];
  const offlineRequests = [];
  page.on('pageerror', error => browserErrors.push(String(error)));

  await page.goto(appUrl, { waitUntil: 'domcontentloaded' });
  const loadingImage = page.locator('.loading-image:visible').first();
  await expect(loadingImage).toBeVisible({ timeout: 30_000 });
  await expect.poll(async () => loadingImage.evaluate(image => image.naturalWidth), {
    timeout: 15_000
  }).toBeGreaterThan(0);
  const progressAnimationSrc = await loadingImage.getAttribute('src');
  const progressMessage = (await page.locator('.loading-state:visible .callout').first().innerText()).trim();
  await expect(page.getByRole('button', { name: 'RAYONS', exact: true }))
    .toBeVisible({ timeout: 120_000 });
  await expect.poll(async () => page.getByRole('button', { name: branch[0], exact: true }).count(), {
    timeout: 60_000
  }).toBeGreaterThan(0);
  const firstUsefulRenderMs = Date.now() - startedAt;
  await expect(loadingImage).toBeHidden();
  await expect(page.locator('.fb-page-shell > .fb-card')).toHaveCount(2);
  await assertNoHorizontalOverflow(page);
  await assertVisibleImagesLoaded(page);
  await page.screenshot({ path: path.join(evidenceDir, 'run7-root-mobile.png'), fullPage: true });

  const rootNames = await page.locator('button:visible').evaluateAll(buttons =>
    buttons.map(button => button.textContent?.trim()).filter(Boolean));
  const duplicateRootCount = rootNames.filter(name => name === branch[0]).length;
  expect.soft(duplicateRootCount).toBe(1);
  expect.soft(rootNames.filter(name => name !== 'RAYONS').length).toBeGreaterThanOrEqual(14);
  await expect.soft(page.getByText('PRODUCT', { exact: true })).toBeHidden();

  for (const name of branch) {
    await page.getByRole('button', { name, exact: true }).first().click();
    await page.waitForTimeout(350);
  }
  await expect.poll(async () => catalogButtons(page).count(), { timeout: 15_000 }).toBe(22);
  const strictBranchProductCount = await catalogButtons(page).count();
  for (const name of branch) {
    await expect(page.getByRole('button', { name, exact: true }).first()).toBeVisible();
  }
  await assertNoHorizontalOverflow(page);
  await assertVisibleImagesLoaded(page);
  await page.screenshot({ path: path.join(evidenceDir, 'run7-products-mobile.png'), fullPage: true });

  const productName = (await catalogButtons(page).first().innerText()).trim();
  await catalogButtons(page).first().click();
  await expect(page.getByText('PRODUCT', { exact: true })).toBeVisible();
  await expect(page.getByText(productName, { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Back', exact: true })).toBeVisible();
  await assertVisibleImagesLoaded(page);
  const quantity = page.getByText('1', { exact: true }).last();
  await expect(quantity).toBeVisible();
  await page.getByRole('button', { name: '+', exact: true }).click();
  await expect(page.getByText('2', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: '−', exact: true }).click();
  await page.getByRole('button', { name: '−', exact: true }).click();
  await expect(page.getByText('1', { exact: true }).last()).toBeVisible();
  await page.screenshot({ path: path.join(evidenceDir, 'run7-detail-mobile.png'), fullPage: true });

	await page.getByRole('button', { name: 'Back', exact: true }).click();
	await expect(page.getByRole('button', { name: productName, exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'RAYONS', exact: true }).click();
  await expect(page.getByRole('button', { name: branch[0], exact: true })).toBeVisible();

  page.on('request', request => {
    if (['fetch', 'xhr'].includes(request.resourceType())) offlineRequests.push(request.url());
  });
  await context.setOffline(true);
  for (const name of branch) {
    await page.getByRole('button', { name, exact: true }).first().click();
    await page.waitForTimeout(350);
  }
  await expect.poll(async () => catalogButtons(page).count(), { timeout: 15_000 }).toBe(22);
  await catalogButtons(page).first().click();
  await expect(page.getByText('PRODUCT', { exact: true })).toBeVisible();
  await expect(page.getByText(productName, { exact: true })).toBeVisible();
  await assertVisibleImagesLoaded(page);
  expect(offlineRequests).toEqual([]);

  await context.setOffline(false);
  await page.setViewportSize({ width: 1280, height: 900 });
  await assertNoHorizontalOverflow(page);
  await page.screenshot({ path: path.join(evidenceDir, 'run7-detail-desktop.png'), fullPage: true });
  expect(browserErrors).toEqual([]);

  console.log(`RUN7_INDEPENDENT_STATE=${JSON.stringify({
    firstUsefulRenderMs,
    duplicateRootCount,
    strictBranchProductCount,
    productName,
    progressAnimationSrc,
    progressMessage,
    offlineFetchOrXhrCount: offlineRequests.length
  })}`);
  await context.close();
});
