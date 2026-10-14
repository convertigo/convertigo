import { expect, test } from '@playwright/test';

const path = '/convertigo/dashboard/Demo/backend/?view=json#response';
const notice = (page) => page.locator('#convertigo-chunk-recovery');

test.use({ baseURL: 'http://127.0.0.1:4174' });

test.beforeEach(async ({ request }) => {
	await request.get('/__test/reset');
});

test('an old lazy import offers a reload without losing input; confirmed reload loads the new version', async ({
	page,
	request
}) => {
	await page.goto(path);
	await page.getByLabel('Unsaved value').fill('keep this draft');
	await request.post('/__test/deploy');
	await page.getByRole('button', { name: 'Load lazy view' }).click();
	await expect(notice(page)).toBeVisible();
	await expect(page.getByLabel('Unsaved value')).toHaveValue('keep this draft');
	await expect(page).toHaveURL(/view=json#response$/);
	page.once('dialog', (dialog) => dialog.dismiss());
	await notice(page).getByRole('button', { name: 'Reload', exact: true }).click();
	await expect(page.getByLabel('Unsaved value')).toHaveValue('keep this draft');
	page.once('dialog', (dialog) => dialog.accept());
	await Promise.all([
		page.waitForEvent('load'),
		notice(page).getByRole('button', { name: 'Reload', exact: true }).click()
	]);
	await expect(page).toHaveURL(/view=json#response$/);
	await page.getByRole('button', { name: 'Load lazy view' }).click();
	await expect(page.locator('#result')).toHaveText('B');
	await expect(notice(page)).toHaveCount(0);
});

test('Monaco AMD 404 is recovered without replaying the backend execution', async ({
	page,
	request
}) => {
	await page.goto(path);
	await request.post('/__test/deploy');
	await page.getByRole('button', { name: 'Execute', exact: true }).click();
	await expect(notice(page)).toBeVisible();
	expect(await (await request.get('/__test/count')).text()).toBe('1');
	page.once('dialog', (dialog) => dialog.accept());
	await Promise.all([
		page.waitForEvent('load'),
		notice(page).getByRole('button', { name: 'Reload', exact: true }).click()
	]);
	expect(await (await request.get('/__test/count')).text()).toBe('1');
	await page.getByRole('button', { name: 'Load Monaco entry point' }).click();
	await expect(page.locator('#result')).toHaveText('B');
});

test('a persistent missing resource cannot trigger a reload loop', async ({ page, request }) => {
	await page.goto(path);
	await request.post('/__test/deploy?broken');
	await page.getByRole('button', { name: 'Load lazy view' }).click();
	await expect(notice(page)).toBeVisible();
	page.once('dialog', (dialog) => dialog.accept());
	await Promise.all([
		page.waitForEvent('load'),
		notice(page).getByRole('button', { name: 'Reload', exact: true }).click()
	]);
	await page.getByRole('button', { name: 'Load lazy view' }).click();
	await expect(notice(page).getByRole('button', { name: 'Reload', exact: true })).toBeDisabled();
	await expect(notice(page)).toContainText('still unavailable');
	await notice(page).getByRole('button', { name: 'Later' }).click();
	await expect(notice(page)).toBeHidden();
});

test('offline and server failures do not offer the update recovery', async ({ page }) => {
	await page.route('**/convertigo/_app/immutable/**', (route) =>
		route.fulfill({ status: 503, body: 'unavailable' })
	);
	await page.goto(path);
	await page.getByRole('button', { name: 'Load lazy view' }).click();
	await expect
		.poll(() =>
			page.evaluate(
				() =>
					performance
						.getEntriesByType('resource')
						.filter((entry) => entry.name.includes('/_app/immutable/')).length
			)
		)
		.toBeGreaterThan(0);
	await expect(notice(page)).toHaveCount(0);
});
