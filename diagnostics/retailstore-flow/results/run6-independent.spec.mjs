import { test, expect, chromium } from '@playwright/test';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const appUrl = process.env.RETAILSTORE_URL
  ?? 'http://127.0.0.1:19080/convertigo/projects/sample_RetailStoreFlowRun6/DisplayObjects/mobile/';
const profileDir = path.join(os.tmpdir(), 'retailstore-flow-run6-independent-profile');
const evidenceDir = process.env.RUN6_EVIDENCE_DIR ?? path.resolve(process.cwd(), '../results');
const branch = ['EPICERIE SUCREE', 'GOUTERS & BISCUITS', 'BISCUITS AU CHOCOLAT'];

const catalogButtons = (page) => page.locator('button:visible').filter({
  hasNotText: /^(RAYONS|EPICERIE SUCREE|GOUTERS & BISCUITS|BISCUITS AU CHOCOLAT|\+|−)$/
});

async function assertNoHorizontalOverflow(page) {
  expect(await page.evaluate(() => (
    document.documentElement.scrollWidth <= document.documentElement.clientWidth
  ))).toBeTruthy();
}

test('independent strict RetailStore Flow acceptance', async () => {
  test.setTimeout(240_000);
  fs.rmSync(profileDir, { recursive: true, force: true });

  const context = await chromium.launchPersistentContext(profileDir, {
    headless: true,
    viewport: { width: 390, height: 844 }
  });
  const page = context.pages()[0] ?? await context.newPage();
  const browserErrors = [];
  const offlineRequests = [];
  page.on('pageerror', error => browserErrors.push(String(error)));

  await page.goto(appUrl, { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('button', { name: 'RAYONS', exact: true }))
    .toBeVisible({ timeout: 120_000 });
  await expect.poll(async () => page.getByRole('button', { name: branch[0], exact: true }).count(), {
    timeout: 60_000
  }).toBeGreaterThan(0);
  await expect.poll(async () => page.locator('img[alt="Category"]').first()
    .evaluate(img => img.naturalWidth)).toBeGreaterThan(0);
  await assertNoHorizontalOverflow(page);
  await page.screenshot({
    path: path.join(evidenceDir, 'run6-root-mobile.png'),
    fullPage: true
  });
  const rootCategoryCount = await page.locator('img[alt="Category"]:visible').count();
  const duplicateRootCount = await page.getByRole('button', { name: branch[0], exact: true }).count();
  expect.soft(duplicateRootCount).toBe(1);
  expect.soft(rootCategoryCount).toBe(14);
  await expect.soft(page.getByText('PRODUCT', { exact: true })).toBeHidden();

  for (const name of branch) {
    await page.getByRole('button', { name, exact: true }).first().click();
    await page.waitForTimeout(350);
  }
  await expect.poll(async () => catalogButtons(page).count(), { timeout: 15_000 })
    .toBeGreaterThan(0);
  const strictBranchProductCount = await catalogButtons(page).count();
  expect.soft(strictBranchProductCount).toBe(22);
  for (const name of branch) {
    await expect(page.getByRole('button', { name, exact: true }).first()).toBeVisible();
  }
  await assertNoHorizontalOverflow(page);
  await page.screenshot({
    path: path.join(evidenceDir, 'run6-products-mobile.png'),
    fullPage: true
  });

  const productName = (await catalogButtons(page).first().innerText()).trim();
  await catalogButtons(page).first().click();
  const detail = page.getByText('PRODUCT', { exact: true }).locator('..');
  await expect(detail.getByText(productName, { exact: true })).toBeVisible();
  await expect(detail.locator('p').nth(2)).toContainText('€');
  await expect.poll(async () => detail.locator('img').evaluate(img => img.naturalWidth))
    .toBeGreaterThan(0);
  const totalAtOne = await detail.locator('p').nth(4).innerText();
  await detail.getByRole('button', { name: '+', exact: true }).click();
  await expect(detail.locator('p').nth(3)).toContainText('2');
  await expect.poll(async () => detail.locator('p').nth(4).innerText()).not.toBe(totalAtOne);
  await detail.getByRole('button', { name: '−', exact: true }).click();
  await detail.getByRole('button', { name: '−', exact: true }).click();
  await expect(detail.locator('p').nth(3)).toContainText('1');
  await page.screenshot({
    path: path.join(evidenceDir, 'run6-detail-mobile.png'),
    fullPage: true
  });

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
  await expect(catalogButtons(page)).toHaveCount(22);
  await catalogButtons(page).first().click();
  await expect(page.getByText('PRODUCT', { exact: true })).toBeVisible();
  await expect(page.getByText('PRODUCT', { exact: true }).locator('..')
    .getByText(productName, { exact: true })).toBeVisible();
  expect(offlineRequests).toEqual([]);

  await context.setOffline(false);
  await page.setViewportSize({ width: 1280, height: 900 });
  await assertNoHorizontalOverflow(page);
  await page.screenshot({
    path: path.join(evidenceDir, 'run6-detail-desktop.png'),
    fullPage: true
  });
  expect(browserErrors).toEqual([]);
  console.log(`RUN6_INDEPENDENT_STATE=${JSON.stringify({
    rootCategoryCount,
    duplicateRootCount,
    strictBranchProductCount,
    offlineFetchOrXhrCount: offlineRequests.length,
    offlineRequests
  })}`);
  await context.close();
});
