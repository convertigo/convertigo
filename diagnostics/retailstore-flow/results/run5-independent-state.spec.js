const { test, expect } = require('@playwright/test');

const base = 'http://127.0.0.1:19080/convertigo/projects/sample_RetailStoreFlowRun5/DisplayObjects/mobile';
const path = ['EPICERIE SUCREE', 'GOUTERS & BISCUITS', 'BISCUITS AU CHOCOLAT'];

test('records independent navigation state', async ({ page, context }) => {
  test.setTimeout(240_000);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(base + '/', { waitUntil: 'domcontentloaded' });
  await expect(page).toHaveURL(/\/store\/?$/, { timeout: 30_000 });
  await expect(page.getByRole('button', { name: path[0], exact: true })).toBeVisible({ timeout: 180_000 });

  const snapshots = [];
  const record = async (step) => {
    snapshots.push(await page.evaluate((name) => {
      const buttons = [...document.querySelectorAll('button')].map((node) => ({
        text: (node.textContent || '').trim(),
        top: Math.round(node.getBoundingClientRect().top + window.scrollY)
      }));
      const rayons = buttons.find((button) => button.text === 'RAYONS');
      return {
        step: name,
        buttons: buttons.map((button) => button.text),
        rayonsTop: rayons?.top ?? null,
        pageHeight: document.documentElement.scrollHeight
      };
    }, step));
  };

  await record('root');
  for (const name of path) {
    await page.getByRole('button', { name, exact: true }).click();
    await page.waitForTimeout(250);
    await record(name);
  }

  await context.setOffline(true);
  await page.getByRole('button', { name: 'RAYONS', exact: true }).click();
  await page.waitForTimeout(250);
  await record('offline-root');
  for (const name of path) {
    await page.getByRole('button', { name, exact: true }).click();
    await page.waitForTimeout(250);
    await record('offline-' + name);
  }
  await context.setOffline(false);
  await page.waitForTimeout(1000);
  await record('reconnected');

  console.log('STATE_SNAPSHOTS=' + JSON.stringify(snapshots));
  const leaf = snapshots[3].buttons;
  expect(leaf).not.toContain('EPICERIE SUCREE');
  expect(leaf.filter((name) => !['RAYONS', ...path].includes(name))).toHaveLength(22);
  expect(snapshots.at(-1).buttons).toEqual(leaf);
  expect(snapshots[3].rayonsTop).toBeLessThan(300);
});
