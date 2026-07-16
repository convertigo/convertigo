const { test, expect } = require('@playwright/test');

const base = process.env.RUN5_BASE
  || 'http://127.0.0.1:19080/convertigo/projects/sample_RetailStoreFlowRun5/DisplayObjects/mobile';

test('Run5 RetailStore production online and offline', async ({ page, context }) => {
  test.setTimeout(240_000);
  page.on('console', msg => console.log('browser', msg.type(), msg.text()));
  page.on('pageerror', error => console.log('pageerror', error.message));
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(base + '/', { waitUntil: 'domcontentloaded' });
  await expect(page).toHaveURL(/\/store\/?$/, { timeout: 30_000 });
  await page.screenshot({ path: 'run5-sync-mobile.png', fullPage: true });

  const sweet = page.getByRole('button', { name: 'EPICERIE SUCREE', exact: true });
  await expect(sweet).toBeVisible({ timeout: 180_000 });
  await page.screenshot({ path: 'run5-root-mobile.png', fullPage: true });

  const rootButtons = page.locator('button').filter({ hasNotText: /^RAYONS$/ });
  expect(await rootButtons.count()).toBe(14);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy();

  await sweet.click();
  await page.getByRole('button', { name: 'GOUTERS & BISCUITS', exact: true }).click();
  await page.getByRole('button', { name: 'BISCUITS AU CHOCOLAT', exact: true }).click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: 'run5-22-products-mobile.png', fullPage: true });

  const productButtons = page.locator('button').filter({ hasNotText: /^(RAYONS|EPICERIE SUCREE|GOUTERS & BISCUITS|BISCUITS AU CHOCOLAT)$/ });
  console.log('leaf buttons', await page.locator('button').allInnerTexts());
  expect(await productButtons.count()).toBe(22);
  const cachedImagesOnline = await page.locator('img').evaluateAll(imgs => imgs.filter(i => i.complete && i.naturalWidth > 0).length);
  console.log('image sources', await page.locator('img').evaluateAll(imgs => imgs.slice(0, 2).map(i => ({ src: i.getAttribute('src'), complete: i.complete, naturalWidth: i.naturalWidth }))));
  expect(cachedImagesOnline).toBeGreaterThan(0);

  const productName = (await productButtons.first().innerText()).trim();
  await productButtons.first().click();
  await expect(page).toHaveURL(/\/detail\/?$/);
  await expect(page.getByText('PRODUCT', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Back', exact: true })).toBeVisible();
  await expect(page.getByText(productName, { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'AJOUTER AU PANIER', exact: true })).toBeVisible();

  const plus = page.getByRole('button', { name: '+', exact: true });
  const minus = page.getByRole('button', { name: '−', exact: true });
  const before = await page.locator('text=1').count();
  expect(before).toBeGreaterThan(0);
  await plus.click();
  await expect(page.getByText('2', { exact: true })).toBeVisible();
  await minus.click();
  await minus.click();
  await expect(page.getByText('1', { exact: true })).toBeVisible();
  await page.screenshot({ path: 'run5-detail-mobile.png', fullPage: true });

  await page.goBack();
  await expect(page).toHaveURL(/\/store\/?$/);
  await expect(page.getByRole('button', { name: productName, exact: true })).toBeVisible({ timeout: 30_000 });
  await page.getByRole('button', { name: productName, exact: true }).click();
  await expect(page).toHaveURL(/\/detail\/?$/);
  await page.getByRole('button', { name: 'Back', exact: true }).click();
  await expect(page.getByRole('button', { name: productName, exact: true })).toBeVisible();

  const network = [];
  page.on('request', req => {
    if (['fetch', 'xhr'].includes(req.resourceType())) network.push(req.url());
  });
  await context.setOffline(true);
  await page.getByRole('button', { name: 'RAYONS', exact: true }).click();
  await page.getByRole('button', { name: 'EPICERIE SUCREE', exact: true }).click();
  await page.getByRole('button', { name: 'GOUTERS & BISCUITS', exact: true }).click();
  await page.getByRole('button', { name: 'BISCUITS AU CHOCOLAT', exact: true }).click();
  await expect(page.getByRole('button', { name: productName, exact: true })).toBeVisible();
  expect(await productButtons.count()).toBe(22);
  await productButtons.first().click();
  await expect(page).toHaveURL(/\/detail\/?$/);
  await page.getByRole('button', { name: 'Back', exact: true }).click();
  await expect(page.getByRole('button', { name: productName, exact: true })).toBeVisible();
  expect(network).toEqual([]);
  const cachedImagesOffline = await page.locator('img').evaluateAll(imgs => imgs.filter(i => i.complete && i.naturalWidth > 0).length);
  expect(cachedImagesOffline).toBeGreaterThan(0);

  await context.setOffline(false);
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.screenshot({ path: 'run5-store-desktop.png', fullPage: true });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy();

  const direct = await context.newPage();
  await direct.goto(base + '/detail/', { waitUntil: 'domcontentloaded' });
  await expect(direct.getByText('PRODUCT', { exact: true })).toBeVisible();
  await direct.getByRole('button', { name: 'Back', exact: true }).click();
  await expect(direct).toHaveURL(/\/store\/?$/);
  await direct.close();
});
