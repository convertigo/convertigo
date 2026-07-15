const { test, expect } = require('@playwright/test');

const built = 'http://127.0.0.1:19080/convertigo/projects/sample_RetailStoreFlowRun4/DisplayObjects/mobile';
const store = `${built}/store`;

async function openNamedCard(page, name, buttonName) {
  const label = page.getByText(name, { exact: true }).first();
  await expect(label).toBeVisible({ timeout: 120_000 });
  const card = label.locator(`xpath=ancestor::*[.//button[normalize-space()="${buttonName}"]][1]`);
  await card.getByRole('button', { name: buttonName, exact: true }).click();
}

async function assertNoHorizontalOverflow(page) {
  const metrics = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth
  }));
  expect(metrics.scrollWidth).toBeLessThanOrEqual(metrics.clientWidth);
}

async function assertVisibleImagesLoaded(page, minimum) {
  const images = await page.locator('img').evaluateAll(nodes => nodes
    .filter(node => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0 && rect.height > 0;
    })
    .map(node => ({
      src: node.currentSrc || node.src,
      complete: node.complete,
      naturalWidth: node.naturalWidth
    })));
  expect(images.length).toBeGreaterThanOrEqual(minimum);
  expect(images.filter(image => !image.complete || image.naturalWidth === 0)).toEqual([]);
}

test('Run4 production catalog, detail, history, desktop and mobile', async ({ browser }) => {
  test.setTimeout(180_000);
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await context.newPage();
  const consoleErrors = [];
  const pageErrors = [];
  const notFoundUrls = [];
  page.on('console', msg => {
    if (msg.type() === 'error' && !msg.text().includes('status of 404')) consoleErrors.push(msg.text());
  });
  page.on('pageerror', error => pageErrors.push(error.message));
  page.on('response', response => {
    if (response.status() === 404) notFoundUrls.push(response.url());
  });

  await page.goto(store, { waitUntil: 'domcontentloaded', timeout: 120_000 });
  await expect(page.getByText('EPICERIE SUCREE', { exact: true }).first()).toBeVisible({ timeout: 120_000 });
  await assertNoHorizontalOverflow(page);
  await assertVisibleImagesLoaded(page, 14);
  await page.screenshot({ path: 'run4-desktop-root.png', fullPage: true });

  const rootChoices = await page.getByRole('button', { name: 'OUVRIR', exact: true }).count();
  expect(rootChoices).toBe(14);

  await openNamedCard(page, 'EPICERIE SUCREE', 'OUVRIR');
  await openNamedCard(page, 'GOUTERS & BISCUITS', 'OUVRIR');
  await openNamedCard(page, 'BISCUITS AU CHOCOLAT', 'OUVRIR');
  await expect(page.getByText('Biscuits Z\'animo chocolat lait Cadbury', { exact: true })).toBeVisible({ timeout: 30_000 });
  await assertVisibleImagesLoaded(page, 1);
  await page.screenshot({ path: 'run4-desktop-products.png', fullPage: true });

  await openNamedCard(page, 'Biscuits Z\'animo chocolat lait Cadbury', 'VOIR LE PRODUIT');
  await expect(page).toHaveURL(/\/detail\/?$/);
  await expect(page.getByText('PRODUCT', { exact: true })).toBeVisible();
  await expect(page.getByText('Biscuits Z\'animo chocolat lait Cadbury', { exact: true })).toBeVisible();
  await expect(page.getByText('la boite de 200 g', { exact: true })).toBeVisible();
  await expect(page.getByText('QUANTITÉ', { exact: true })).toBeVisible();
  await expect(page.getByText('1', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'AJOUTER AU PANIER', exact: true })).toBeVisible();
  await assertVisibleImagesLoaded(page, 1);
  await page.screenshot({ path: 'run4-desktop-detail.png', fullPage: true });

  await page.goBack();
  await expect(page.getByText('Biscuits Z\'animo chocolat lait Cadbury', { exact: true })).toBeVisible();
  await page.goForward();
  await expect(page.getByText('PRODUCT', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'RAYONS', exact: true }).click();
  await expect(page.getByText('Biscuits Z\'animo chocolat lait Cadbury', { exact: true })).toBeVisible();

  // Load the application shell online, then prove local FullSync views/gets with
  // networking disabled and without reloading the HTML document.
  await page.goto(store, { waitUntil: 'domcontentloaded', timeout: 120_000 });
  await expect(page.getByText('EPICERIE SUCREE', { exact: true }).first()).toBeVisible({ timeout: 120_000 });
  const offlineFetches = [];
  let captureOffline = true;
  page.on('request', request => {
    if (captureOffline && ['fetch', 'xhr'].includes(request.resourceType())) offlineFetches.push(request.url());
  });
  await context.setOffline(true);
  await openNamedCard(page, 'EPICERIE SUCREE', 'OUVRIR');
  await openNamedCard(page, 'GOUTERS & BISCUITS', 'OUVRIR');
  await openNamedCard(page, 'BISCUITS AU CHOCOLAT', 'OUVRIR');
  await expect(page.getByText('Biscuits Z\'animo chocolat lait Cadbury', { exact: true })).toBeVisible({ timeout: 30_000 });
  await assertVisibleImagesLoaded(page, 1);
  expect(offlineFetches).toEqual([]);
  await openNamedCard(page, 'Biscuits Z\'animo chocolat lait Cadbury', 'VOIR LE PRODUIT');
  await expect(page).toHaveURL(/\/detail\/?$/);
  await expect(page.getByText('Biscuits Z\'animo chocolat lait Cadbury', { exact: true })).toBeVisible();
  await assertVisibleImagesLoaded(page, 1);
  await page.screenshot({ path: 'run4-offline-detail.png', fullPage: true });
  await page.goBack();
  await expect(page.getByText('Biscuits Z\'animo chocolat lait Cadbury', { exact: true })).toBeVisible();
  captureOffline = false;
  await context.setOffline(false);

  await page.setViewportSize({ width: 390, height: 844 });
  await page.reload({ waitUntil: 'domcontentloaded', timeout: 120_000 });
  await expect(page.getByText('EPICERIE SUCREE', { exact: true }).first()).toBeVisible({ timeout: 120_000 });
  await assertNoHorizontalOverflow(page);
  await assertVisibleImagesLoaded(page, 14);
  await page.screenshot({ path: 'run4-mobile.png', fullPage: true });

  const unexpected404s = notFoundUrls.filter(url => !/\/convertigo\/fullsync\/retailstore\/_local\//.test(url));
  expect(unexpected404s).toEqual([]);
  expect(offlineFetches).toEqual([]);
  expect(consoleErrors).toEqual([]);
  expect(pageErrors).toEqual([]);
  await context.close();
});
