const { test, expect, chromium } = require('@playwright/test');
const { mkdtemp } = require('node:fs/promises');
const { tmpdir } = require('node:os');
const { join } = require('node:path');

const base = process.env.RUN5_BASE
  || 'http://127.0.0.1:19080/convertigo/projects/sample_RetailStoreFlowRun5/DisplayObjects/mobile';

test('two starts complete with the same persistent FullSync profile', async () => {
  test.setTimeout(240_000);
  const profile = await mkdtemp(join(tmpdir(), 'run5-persistent-'));
  const context = await chromium.launchPersistentContext(profile, {
    viewport: { width: 390, height: 844 }
  });
  const page = context.pages()[0] || await context.newPage();
  const observed = [];

  try {
    for (let pass = 1; pass <= 2; pass += 1) {
      await page.goto(`${base}/?pass=${pass}`, { waitUntil: 'domcontentloaded' });
      const deadline = Date.now() + 120_000;
      while (!/\/store\/?$/.test(new URL(page.url()).pathname) && Date.now() < deadline) {
        const state = await page.evaluate(() => ({
          images: [...document.images].map((image) => image.getAttribute('src')),
          text: document.body.innerText
        }));
        observed.push({ pass, ...state });
        await page.waitForTimeout(50);
      }
      await expect(page).toHaveURL(/\/store\/?$/, { timeout: 5_000 });
      await expect(page.getByRole('button', { name: 'EPICERIE SUCREE', exact: true })).toBeVisible({ timeout: 30_000 });
    }

    const sources = observed.flatMap((entry) => entry.images).filter(Boolean);
    expect(sources.some((source) => source.endsWith('/init.gif'))).toBeTruthy();
    expect(sources.some((source) => source.endsWith('/sync.gif'))).toBeTruthy();
    expect(sources.some((source) => source.endsWith('/optimize.gif'))).toBeTruthy();
  } finally {
    await context.close();
  }
});
