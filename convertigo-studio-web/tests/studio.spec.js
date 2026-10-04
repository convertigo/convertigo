import { expect, test } from '@playwright/test';

/**
 * @typedef {Window & typeof globalThis & {
 *   __studioFlowDragContext?: {
 *     sourceNodeId: string,
 *     targetNodeId: string,
 *     xRatio: number,
 *     yRatio: number,
 *     dataTransfer: DataTransfer
 *   }
 * }} StudioFlowTestWindow
 */

const projectName = 'StudioProject';
const sequenceName = 'TestSequence';
const sequenceId = `${projectName}.sq:${sequenceName}`;
const initStepId = `${sequenceId}.st:Init`;
const flowEngineId = `${projectName}.Engine`;
const frontendBuilderId = `${flowEngineId}.frontends.svelte`;
const frontendStructureId = `${frontendBuilderId}.routes.home.structure`;

test('studio project menu previews a reference tag and creates its memberships in one command', async ({
	page
}) => {
	await mockStudioServices(page);
	const members = [projectName, 'Library', 'Leaf'];
	const tags = await mockTagServices(page, {
		projects: [...members, 'Unrelated'],
		referenceTargets: members
	});
	await page.goto('/studio/');
	await openTreeMenu(page, projectName);
	await page.getByRole('menuitem', { name: 'Create tag from references…', exact: true }).click();
	const dialog = page.getByRole('dialog');
	await expect(dialog.getByLabel('Label', { exact: true })).toHaveValue(projectName);
	for (const name of members) {
		const checkbox = dialog.getByRole('checkbox', { name: `Include ${name}`, exact: true });
		await expect(checkbox).toBeChecked();
		await expect(checkbox).toBeDisabled();
	}
	await expect(
		dialog.getByRole('checkbox', { name: 'Include Unrelated', exact: true })
	).not.toBeChecked();
	expect(tags.commands).toHaveLength(0);
	await dialog.getByLabel('Label', { exact: true }).fill('Application stack');
	await dialog.getByRole('button', { name: 'Create tag', exact: true }).click();
	await expect.poll(() => tags.commands.length).toBe(1);
	expect(tags.commands[0]).toMatchObject({
		action: 'createFromReferences',
		input: { project: projectName, definition: { label: 'Application stack' } }
	});
	await expect(dialog.getByRole('button', { name: 'Update tag', exact: true })).toBeVisible();
	const leaf = dialog.getByRole('checkbox', { name: 'Include Leaf', exact: true });
	await expect(leaf).toBeEnabled();
	await leaf.uncheck();
	await expect.poll(() => tags.commands.length).toBe(2);
	expect(tags.commands[1]).toMatchObject({ action: 'remove', input: { targets: ['Leaf'] } });
	await dialog.getByRole('button', { name: 'Close', exact: true }).click();
	await expandTreeNode(page, projectName);
	await expandTreeNode(page, projectName + ':sq');
	await openTreeMenu(page, sequenceId);
	await expect(
		page.getByRole('menuitem', { name: 'Create tag from references…', exact: true })
	).toHaveCount(0);
});

test('studio project membership checks preserve the scrolled list and its DOM rows', async ({
	page
}) => {
	await mockStudioServices(page);
	const names = Array.from({ length: 60 }, (_, i) => `Project${String(i).padStart(2, '0')}`);
	const tags = await mockTagServices(page, { projects: [projectName, ...names] });
	await page.goto('/studio/');
	await openTreeMenu(page, projectName);
	await page.getByRole('menuitem', { name: 'Project tags…', exact: true }).click();
	const dialog = page.getByRole('dialog');
	const list = dialog.locator('.studio-tags__member-list');
	const checkbox = dialog.getByRole('checkbox', { name: 'Include Project40', exact: true });
	await checkbox.scrollIntoViewIfNeeded();
	const before = await list.evaluate((element) => element.scrollTop);
	expect(before).toBeGreaterThan(0);
	const row = await checkbox.elementHandle();
	await checkbox.check();
	await expect.poll(() => tags.commands.length).toBe(1);
	await expect(checkbox).toBeEnabled();
	await expect.poll(() => list.evaluate((element) => element.scrollTop)).toBe(before);
	expect(await row.evaluate((element) => element.isConnected)).toBe(true);
	await checkbox.uncheck();
	await expect.poll(() => tags.commands.length).toBe(2);
	await expect(checkbox).toBeEnabled();
	await expect.poll(() => list.evaluate((element) => element.scrollTop)).toBe(before);
});

test('studio reference tag creation requires a single project selection', async ({ page }) => {
	const projects = [projectName, 'Library'];
	await mockStudioServices(page, { projects });
	const tags = await mockTagServices(page, { projects });
	await page.goto('/studio/');
	await selectTreeNode(page, projectName);
	await page
		.locator('button.studio-tree-node__content[data-node-id="Library"]')
		.click({ modifiers: ['ControlOrMeta'] });
	await openTreeMenu(page, projectName);
	await expect(page.getByRole('menuitem', { name: 'Project tags…', exact: true })).toBeEnabled();
	await expect(
		page.getByRole('menuitem', { name: 'Create tag from references…', exact: true })
	).toBeDisabled();
	expect(tags.commands).toHaveLength(0);
});

test('studio reference tag creation is disabled when its preview cannot be read', async ({
	page
}) => {
	await mockStudioServices(page);
	const tags = await mockTagServices(page, { referenceError: true });
	await page.goto('/studio/');
	await openTreeMenu(page, projectName);
	await page.getByRole('menuitem', { name: 'Create tag from references…', exact: true }).click();
	const dialog = page.getByRole('dialog');
	await expect(dialog.getByRole('alert')).toContainText('Unable to read references of Library');
	await expect(dialog.getByRole('alert')).toContainText('Unreadable fixture');
	await expect(page.getByRole('dialog')).toHaveCount(1);
	await expect(dialog.getByRole('button', { name: 'Create tag', exact: true })).toBeDisabled();
	expect(tags.commands).toHaveLength(0);
});

test('studio reference tags report unavailable libraries without disabling creation', async ({
	page
}) => {
	await mockStudioServices(page);
	const tags = await mockTagServices(page, {
		projects: [projectName, 'Library'],
		referenceTargets: [projectName, 'Library'],
		referenceWarnings: [
			'Library references NotInstalled, which is not in this workspace and will not be included.'
		]
	});
	await page.goto('/studio/');
	await openTreeMenu(page, projectName);
	await page.getByRole('menuitem', { name: 'Create tag from references…', exact: true }).click();
	const dialog = page.getByRole('dialog');
	await expect(dialog.getByRole('alert')).toContainText('NotInstalled');
	await expect(dialog.getByLabel('Label', { exact: true })).toBeEnabled();
	await expect(
		dialog.getByRole('checkbox', { name: 'Include Library', exact: true })
	).toBeChecked();
	await expect(dialog.getByRole('button', { name: 'Create tag', exact: true })).toBeEnabled();
	await dialog.getByRole('button', { name: 'Create tag', exact: true }).click();
	await expect(dialog.getByRole('button', { name: 'Update tag', exact: true })).toBeEnabled();
	expect(tags.commands).toHaveLength(1);
});

test('studio reference tag creation also opens from a grouped project occurrence', async ({
	page
}) => {
	await page.addInitScript(() => localStorage.setItem('studio-tags-grouped', 'true'));
	await mockStudioServices(page);
	const tags = await mockTagServices(page, { referenceTargets: [projectName] });
	await page.goto('/studio/');
	await ensureTreeNodeExpanded(page, 'tag-row:fixture-stack');
	await openTreeMenu(page, projectName);
	await page.getByRole('menuitem', { name: 'Create tag from references…', exact: true }).click();
	const dialog = page.getByRole('dialog');
	await expect(dialog.getByLabel('Label', { exact: true })).toHaveValue(projectName);
	await expect(dialog.getByRole('button', { name: 'Create tag', exact: true })).toBeEnabled();
	expect(tags.commands).toHaveLength(0);
});

test('studio project action menus remain inside a short viewport', async ({ page }) => {
	await page.setViewportSize({ width: 1280, height: 720 });
	const projects = [...Array.from({ length: 8 }, (_, index) => `Library${index}`), projectName];
	await mockStudioServices(page, { projects });
	await mockTagServices(page, { projects });
	await page.goto('/studio/');
	await openTreeMenu(page, projectName);
	const menu = page.getByRole('menu', { name: `Actions for ${projectName}`, exact: true });
	await expect
		.poll(async () => {
			const bounds = await menu.boundingBox();
			return Boolean(bounds && bounds.y >= 0 && bounds.y + bounds.height <= 720);
		})
		.toBe(true);
	await page.getByRole('menuitem', { name: 'Create tag from references…', exact: true }).click();
	await expect(page.getByRole('dialog').getByLabel('Label', { exact: true })).toHaveValue(
		projectName
	);
});

for (const theme of ['light', 'dark']) {
	test(`studio tags have an opaque elevated dialog in ${theme} mode`, async ({ page }) => {
		await page.addInitScript((value) => localStorage.setItem('theme', value), theme);
		await mockStudioServices(page);
		await mockTagServices(page);
		await page.goto('/studio/');
		await openTreeMenu(page, projectName);
		await page.getByRole('menuitem', { name: 'Project tags…', exact: true }).click();
		const dialog = page.getByRole('dialog');
		await expect(dialog).toBeVisible();
		const appearance = await dialog.evaluate((element) => {
			const style = getComputedStyle(element);
			return {
				background: style.backgroundColor,
				shadow: style.boxShadow,
				border: parseFloat(style.borderTopWidth)
			};
		});
		expect(appearance.background).not.toBe('rgba(0, 0, 0, 0)');
		expect(appearance.background).not.toBe('transparent');
		expect(appearance.shadow).not.toBe('none');
		expect(appearance.border).toBeGreaterThanOrEqual(1);
		await expect(page.locator('html')).toHaveClass(theme === 'dark' ? /dark/ : /^(?!.*\bdark\b)/);
		await page.screenshot({ path: test.info().outputPath('tags-dialog.png') });
	});
}

test('studio tags edit typed contributions and immediately refresh badges', async ({ page }) => {
	await mockStudioServices(page);
	const tags = await mockTagServices(page);
	await page.goto('/studio/');
	await expandTreeNode(page, projectName);
	await expandTreeNode(page, projectName + ':sq');
	await selectTreeNode(page, sequenceId);
	await openTreeMenu(page, sequenceId);
	await page.getByRole('menuitem', { name: 'Sequence tags…', exact: true }).click();
	const dialog = page.getByRole('dialog');
	await dialog.getByRole('button', { name: 'CRM', exact: true }).click();
	await dialog.getByLabel('Label', { exact: true }).fill('Client <script>');
	await dialog.getByText('Advanced', { exact: true }).click();
	await dialog.getByLabel('Count', { exact: true }).fill('7');
	await dialog.getByLabel('Enabled', { exact: true }).check();
	await dialog.getByRole('combobox', { name: 'Level', exact: true }).selectOption('high');
	await expect(dialog.getByText('retired — extension unavailable (read only)')).toBeVisible();
	await dialog.getByRole('button', { name: 'Update tag', exact: true }).click();
	await expect.poll(() => tags.commands.length).toBe(1);
	expect(tags.commands[0].input.definition.metadata).toEqual({
		neutral: { count: 7, enabled: true, level: 'high' },
		retired: { preserved: ['ordered', 'values'] }
	});
	await dialog.getByRole('button', { name: 'Close', exact: true }).click();
	await expect(
		page.locator('button.studio-tree-node__content[data-node-id="' + sequenceId + '"]')
	).toContainText('Client <script>');
	await expect(page.getByRole('button', { name: 'Save project - unsaved changes' })).toBeEnabled();
});

test('studio tags edit ordered references through the generic descriptor without raw JSON', async ({
	page
}) => {
	await mockStudioServices(page);
	const tags = await mockTagServices(page, { references: true });
	await page.goto('/studio/');
	await expandTreeNode(page, projectName);
	await expandTreeNode(page, projectName + ':sq');
	await selectTreeNode(page, sequenceId);
	await openTreeMenu(page, sequenceId);
	await page.getByRole('menuitem', { name: 'Sequence tags…', exact: true }).click();
	const dialog = page.getByRole('dialog');
	await dialog.getByRole('button', { name: 'CRM', exact: true }).click();
	await dialog.getByText('Advanced', { exact: true }).click();
	const resources = dialog.getByRole('group', { name: 'Resources', exact: true });
	await expect(resources.getByRole('alert')).toContainText('retired');
	await resources.getByRole('button', { name: 'Remove retired', exact: true }).click();
	await resources
		.getByRole('combobox', { name: 'Choose a reference', exact: true })
		.selectOption('Mail');
	await resources.getByRole('button', { name: 'Add', exact: true }).click();
	await resources
		.getByRole('combobox', { name: 'Choose a reference', exact: true })
		.selectOption('B2');
	await resources.getByRole('button', { name: 'Add', exact: true }).click();
	await resources.getByRole('button', { name: 'Move B2 up', exact: true }).click();
	await resources.getByRole('button', { name: 'Move B2 up', exact: true }).click();
	await expect(resources.getByRole('list')).toContainText('1. B2');
	await expect(resources.getByRole('combobox')).not.toContainText('B1');
	await expect(resources.getByRole('alert')).toHaveCount(0);
	await dialog.getByRole('button', { name: 'Update tag', exact: true }).click();
	await expect.poll(() => tags.commands.length).toBe(1);
	expect(tags.commands[0].input.definition.metadata.neutral.resources).toEqual([
		'B2',
		'B1',
		'Mail'
	]);
	await expect(resources.getByRole('list')).toContainText('1. B2');
});

test('studio tags reorder memberships without changing the tag being edited', async ({ page }) => {
	await mockStudioServices(page);
	const tags = await mockTagServices(page);
	await page.goto('/studio/');
	await expandTreeNode(page, projectName);
	await expandTreeNode(page, projectName + ':sq');
	await selectTreeNode(page, sequenceId);
	await openTreeMenu(page, sequenceId);
	await page.getByRole('menuitem', { name: 'Sequence tags…', exact: true }).click();
	const dialog = page.getByRole('dialog');
	await dialog.getByRole('button', { name: 'CRM', exact: true }).click();
	await dialog.getByLabel('Label', { exact: true }).fill('Pending label');
	await expect(
		dialog.getByRole('list', { name: 'Ordered values' }).getByRole('listitem').first()
	).toContainText('1. CRM');
	await dialog.getByRole('button', { name: 'Move CRM down', exact: true }).click();
	await expect.poll(() => tags.commands.length).toBe(1);
	expect(tags.commands[0]).toEqual({
		action: 'reorder',
		input: {
			targets: [sequenceId],
			tagIds: ['41a604a2-4900-437b-9cb8-7209f4152e3a', '5e1012bd-4ab1-452a-8e23-4228586256a2']
		}
	});
	await expect(
		dialog.getByRole('list', { name: 'Ordered values' }).getByRole('listitem').first()
	).toContainText('1. Audit');
	await expect(dialog.getByLabel('Label', { exact: true })).toHaveValue('Pending label');
	await expect(dialog.getByRole('button', { name: 'Move Audit up', exact: true })).toBeDisabled();
	await dialog.getByRole('button', { name: 'Close', exact: true }).click();
	await openTreeMenu(page, sequenceId);
	await page.getByRole('menuitem', { name: 'Sequence tags…', exact: true }).click();
	await expect(
		dialog.getByRole('list', { name: 'Ordered values' }).getByRole('listitem').first()
	).toContainText('1. Audit');
});

test('studio tags enable sequence actions after a direct URL loads its lazy tree', async ({
	page
}) => {
	await page.addInitScript(() => localStorage.setItem('studio-tags-grouped', 'true'));
	await mockStudioServices(page);
	await mockTagServices(page);
	const url = `/studio/${sequenceId.replaceAll(':', '~')}/`;
	// Vite preview lacks the engine's dynamic Studio URL fallback.
	await page.route('**' + url, async (route) => {
		const response = await page.request.get('/studio/_/');
		await route.fulfill({ response });
	});
	await page.goto(url);
	await openTreeMenu(page, sequenceId);
	await expect(page.getByRole('menuitem', { name: 'Sequence tags…', exact: true })).toBeEnabled();
	await page.keyboard.press('Escape');
	await openTreeMenu(page, sequenceId);
	await page.getByRole('menuitem', { name: 'Sequence tags…', exact: true }).click();
	await expect(
		page.getByRole('dialog').getByRole('checkbox', { name: 'Include TestSequence', exact: true })
	).toBeChecked();
});

test('studio tags preserve loaded workspace occurrences after a tag update', async ({ page }) => {
	await page.addInitScript(() => localStorage.setItem('studio-tags-grouped', 'true'));
	await mockStudioServices(page);
	await mockTagServices(page);
	await page.goto('/studio/');
	const tree = page.getByRole('tree', { name: 'Projects' });
	await expect(
		tree.getByRole('button', { name: 'Stack (1)', exact: true }).locator('..').locator('..')
	).toHaveAttribute('aria-expanded', 'true');
	await expandTreeNode(page, projectName);
	await expandTreeNode(page, projectName + ':sq');
	await tree.getByRole('button', { name: 'CRM (1)', exact: true }).click();
	await expandTreeNode(page, sequenceId);
	await expandTreeNode(page, sequenceId + ':st');
	await selectTreeNode(page, sequenceId);
	await openTreeMenu(page, sequenceId);
	await page.getByRole('menuitem', { name: 'Sequence tags…', exact: true }).click();
	const dialog = page.getByRole('dialog');
	await dialog.getByRole('button', { name: 'CRM', exact: true }).click();
	await dialog.getByLabel('Label', { exact: true }).fill('Clients');
	await dialog.getByRole('button', { name: 'Update tag', exact: true }).click();
	await expect(dialog.getByText('Modified — save the project to keep these tags.')).toBeVisible();
	await dialog.getByRole('button', { name: 'Close', exact: true }).click();
	await expect(tree.getByRole('button', { name: 'Clients (1)', exact: true })).toBeVisible();
	await expect(
		tree.locator('button.studio-tree-node__content[data-node-id="' + initStepId + '"]')
	).toBeVisible();
	await selectTreeNode(page, initStepId);
	await openTreeMenu(page, initStepId);
	await expect(page.getByRole('menuitem', { name: 'Sequence tags…', exact: true })).toHaveCount(0);
	await page.keyboard.press('Escape');
});

test('studio tags exclude steps from assignment actions and grouped collections', async ({
	page
}) => {
	await mockStudioServices(page);
	const tags = await mockTagServices(page);
	await page.goto('/studio/');
	await expandTreeNode(page, projectName);
	await expandTreeNode(page, projectName + ':sq');
	await selectTreeNode(page, sequenceId);
	await openTreeMenu(page, sequenceId);
	await expect(page.getByRole('menuitem', { name: 'Sequence tags…', exact: true })).toBeEnabled();
	await page.keyboard.press('Escape');
	await openTreeMenu(page, sequenceId);
	await page.getByRole('menuitem', { name: 'Group by tags', exact: true }).click();
	const tree = page.getByRole('tree', { name: 'Projects' });
	await tree.getByRole('button', { name: 'CRM (1)', exact: true }).click();
	const occurrence = tree.locator(
		'button.studio-tree-node__content[data-node-id="' + sequenceId + '"]'
	);
	await occurrence
		.locator('..')
		.locator('..')
		.locator(':scope > div > span > button[aria-label="Expand"]')
		.click();
	await expandTreeNode(page, sequenceId + ':st');
	await selectTreeNode(page, initStepId);
	await openTreeMenu(page, initStepId);
	await expect(page.getByRole('menuitem', { name: 'Sequence tags…', exact: true })).toHaveCount(0);
	await page.keyboard.press('Escape');
	const steps = tree
		.locator('button.studio-tree-node__content[data-node-id="' + sequenceId + ':st"]')
		.locator('..')
		.locator('..');
	await expect(steps.locator('.studio-tree-tag')).toHaveCount(0);
	await expect(
		steps.getByRole('button', { name: /^(Untagged|CRM|Audit)( \(\d+\))?$/ })
	).toHaveCount(0);
	await tree
		.locator('button.studio-tree-node__content[data-node-id="' + sequenceId + '"]')
		.first()
		.click({ modifiers: ['ControlOrMeta'] });
	await openTreeMenu(page, sequenceId);
	await expect(page.getByRole('menuitem', { name: 'Sequence tags…', exact: true })).toBeDisabled();
	await page.keyboard.press('Escape');
	expect(tags.commands).toHaveLength(0);
});

test('studio tags keep independent occurrence expansion and deduplicate object commands', async ({
	page
}) => {
	await mockStudioServices(page);
	const tags = await mockTagServices(page);
	await page.goto('/studio/');
	await expandTreeNode(page, projectName);
	await expandTreeNode(page, projectName + ':sq');
	await openTreeMenu(page, projectName);
	await page.getByRole('menuitem', { name: 'Group by tags', exact: true }).click();
	const tree = page.getByRole('tree', { name: 'Projects' });
	await expect(tree.getByRole('button', { name: 'Stack (1)', exact: true })).toBeVisible();
	await ensureTreeNodeExpanded(page, projectName);
	await ensureTreeNodeExpanded(page, projectName + ':sq');
	for (const label of ['Audit', 'CRM']) {
		const content = tree.getByRole('button', { name: `${label} (1)`, exact: true });
		await content
			.locator('..')
			.locator('..')
			.locator(':scope > div > span > button[aria-label="Expand"]')
			.click();
	}
	const occurrences = tree.locator(
		'button.studio-tree-node__content[data-node-id="' + sequenceId + '"]'
	);
	await expect(occurrences).toHaveCount(2);
	await expect(occurrences.nth(0).locator('.studio-tree-tag')).toHaveCount(1);
	await expect(occurrences.nth(0)).toContainText('CRM');
	await expect(occurrences.nth(1).locator('.studio-tree-tag')).toHaveCount(1);
	await expect(occurrences.nth(1)).toContainText('Audit');
	const rows = occurrences.locator('..').locator('..');
	await rows.nth(0).locator(':scope > div > span > button[aria-label="Expand"]').click();
	await expect(rows.nth(0)).toHaveAttribute('aria-expanded', 'true');
	await expect(rows.nth(1)).toHaveAttribute('aria-expanded', 'false');
	await occurrences.nth(0).click();
	await occurrences.nth(1).click({ modifiers: ['ControlOrMeta'] });
	await page.keyboard.press('ControlOrMeta+c');
	await expect.poll(() => tags.copies.length).toBe(1);
	expect(JSON.parse(tags.copies[0].get('ids'))).toEqual([sequenceId]);
	await tree.getByRole('button', { name: 'Audit (1)', exact: true }).click({ button: 'right' });
	await page.getByRole('menuitem', { name: 'Sequence tags…', exact: true }).click();
	const dialog = page.getByRole('dialog');
	await expect(dialog.getByLabel('Label', { exact: true })).toHaveValue('Audit');
	await expect(
		dialog.getByRole('checkbox', { name: 'Include TestSequence', exact: true })
	).toBeChecked();
	await dialog.getByRole('button', { name: 'Close', exact: true }).click();
	await tree.getByRole('button', { name: 'Audit (1)', exact: true }).click({ button: 'right' });
	await page.getByRole('menuitem', { name: 'Group by tags', exact: true }).click();
	await expect(tree.getByRole('button', { name: 'Audit (1)', exact: true })).toHaveCount(0);
	await expect(
		tree
			.locator('button.studio-tree-node__content[data-node-id="' + sequenceId + '"]')
			.locator('.studio-tree-tag')
	).toHaveCount(2);
});

test('studio tags immediately refresh a retained group counter after membership removal', async ({
	page
}) => {
	await page.addInitScript(() => localStorage.setItem('studio-tags-grouped', 'true'));
	await mockStudioServices(page);
	await mockTagServices(page, { secondSequence: true });
	await page.goto('/studio/');
	await expandTreeNode(page, projectName);
	await expandTreeNode(page, projectName + ':sq');
	const tree = page.getByRole('tree', { name: 'Projects' });
	await tree.getByRole('button', { name: 'Audit (2)', exact: true }).click();
	await selectTreeNode(page, sequenceId);
	await openTreeMenu(page, sequenceId);
	await page.getByRole('menuitem', { name: 'Sequence tags…', exact: true }).click();
	const dialog = page.getByRole('dialog');
	await dialog.getByRole('button', { name: 'Audit', exact: true }).click();
	await dialog.getByRole('checkbox', { name: 'Include TestSequence', exact: true }).uncheck();
	await expect(dialog.getByText('Modified — save the project to keep these tags.')).toBeVisible();
	await dialog.getByRole('button', { name: 'Close', exact: true }).click();
	await expect(tree.getByRole('button', { name: 'Audit (1)', exact: true })).toBeVisible();
	await expect(tree.getByRole('button', { name: 'Audit (2)', exact: true })).toHaveCount(0);
});

for (const trigger of ['menu', 'F2']) {
	test(`studio tags rename one occurrence with ${trigger} and update both memberships`, async ({
		page
	}) => {
		await page.addInitScript(() => localStorage.setItem('studio-tags-grouped', 'true'));
		await mockStudioServices(page);
		const tags = await mockTagServices(page);
		await page.goto('/studio/');
		await expandTreeNode(page, projectName);
		await expandTreeNode(page, projectName + ':sq');
		const tree = page.getByRole('tree', { name: 'Projects' });
		for (const label of ['Audit', 'CRM'])
			await tree.getByRole('button', { name: `${label} (1)`, exact: true }).click();
		const occurrences = tree.locator(
			`button.studio-tree-node__content[data-node-id="${sequenceId}"]`
		);
		await expect(occurrences).toHaveCount(2);
		const occurrence = occurrences.nth(trigger === 'menu' ? 0 : 1);
		if (trigger === 'menu') {
			await occurrence.click({ button: 'right' });
			await page.getByRole('menuitem', { name: /^Rename\b/ }).click();
		} else {
			await occurrence.click();
			await occurrence.press('F2');
		}
		const input = tree.getByRole('textbox', { name: 'Rename object', exact: true });
		await expect(input).toHaveCount(1);
		await expect(input).toBeFocused();
		await input.fill('RenamedSequence');
		await input.press('Enter');
		await expect(input).toHaveCount(0);
		await expect.poll(() => tags.renames.length).toBe(1);
		expect(tags.renames[0].get('id')).toBe(sequenceId);
		expect(tags.renames[0].get('name')).toBe('RenamedSequence');
		const renamedId = `${projectName}.sq:RenamedSequence`;
		await expect(
			tree.locator(`button.studio-tree-node__content[data-node-id="${renamedId}"]`)
		).toHaveCount(2);
		await expect(occurrences).toHaveCount(0);
		await expect(tree.getByRole('button', { name: 'Audit (1)', exact: true })).toBeVisible();
		await expect(tree.getByRole('button', { name: 'CRM (1)', exact: true })).toBeVisible();
	});
}

test('studio tags retain project capabilities in normal view after workspace refresh', async ({
	page
}) => {
	const projects = [projectName];
	await mockStudioServices(page, { projects });
	await mockTagServices(page, { projects });
	await page.goto('/studio/');
	await expandTreeNode(page, projectName);
	await expandTreeNode(page, projectName + ':sq');
	await openTreeMenu(page, projectName);
	await expect(page.getByRole('menuitem', { name: 'Project tags…', exact: true })).toBeEnabled();
	await page.keyboard.press('Escape');
	projects.push('NewProject');
	await selectTreeNode(page, projectName);
	await page.keyboard.press('F5');
	await openTreeMenu(page, 'NewProject');
	await expect(page.getByRole('menuitem', { name: 'Project tags…', exact: true })).toBeEnabled();
	await page.keyboard.press('Escape');
	await openTreeMenu(page, projectName);
	await expect(page.getByRole('menuitem', { name: 'Project tags…', exact: true })).toBeEnabled();
	await page.keyboard.press('Escape');
	await expect(
		page.locator(`button.studio-tree-node__content[data-node-id="${sequenceId}"]`)
	).toBeVisible();
});

test('studio tags finish membership changes before starting a new tag', async ({ page }) => {
	let release = () => {};
	const pending = new Promise((resolve) => {
		release = () => resolve(undefined);
	});
	await mockStudioServices(page);
	const tags = await mockTagServices(page, { beforeApply: () => pending });
	await page.goto('/studio/');
	await expandTreeNode(page, projectName);
	await expandTreeNode(page, projectName + ':sq');
	await selectTreeNode(page, sequenceId);
	await openTreeMenu(page, sequenceId);
	await page.getByRole('menuitem', { name: 'Sequence tags…', exact: true }).click();
	const dialog = page.getByRole('dialog');
	await dialog.getByRole('button', { name: 'CRM', exact: true }).click();
	await dialog.getByRole('checkbox', { name: 'Include TestSequence', exact: true }).uncheck();
	await expect.poll(() => tags.commands.length).toBe(1);
	await expect(dialog.getByRole('button', { name: 'New tag', exact: true })).toBeDisabled();
	release();
	await expect(dialog.getByRole('button', { name: 'New tag', exact: true })).toBeEnabled();
	await dialog.getByRole('button', { name: 'New tag', exact: true }).click();
	await expect(dialog.getByLabel('Label', { exact: true })).toHaveValue('');
	await expect(dialog.getByRole('button', { name: 'Create tag', exact: true })).toBeVisible();
});

async function openTreeMenu(page, nodeId) {
	await page
		.locator('button.studio-tree-node__content[data-node-id="' + nodeId + '"]')
		.first()
		.click({ button: 'right' });
	await expect(page.getByRole('menuitem', { name: 'Group by tags', exact: true })).toBeVisible();
}

async function mockTagServices(page, options = {}) {
	const ids = ['5e1012bd-4ab1-452a-8e23-4228586256a2', '41a604a2-4900-437b-9cb8-7209f4152e3a'];
	let target = sequenceId;
	const other = `${projectName}.sq:OtherSequence`;
	const assignments = {
		[target]: [...ids],
		...(options.secondSequence ? { [other]: [ids[1]] } : {})
	};
	const tags = {
		[ids[0]]: {
			label: 'CRM',
			metadata: {
				neutral: {
					count: 1,
					enabled: false,
					level: 'low',
					...(options.references ? { resources: ['retired', 'B1'] } : {})
				},
				retired: { preserved: ['ordered', 'values'] }
			}
		},
		[ids[1]]: { label: 'Audit', metadata: {} }
	};
	const commands = [],
		copies = [],
		renames = [];
	let revision = 1,
		dirty = false;
	const snapshot = (scope = 'projectObjects') => ({
		scope,
		project: projectName,
		revision: String(revision),
		dirty,
		readOnly: false,
		tags,
		assignments,
		targets: [target, ...(options.secondSequence ? [other] : [])],
		projects: options.projects ?? [projectName],
		diagnostics: [],
		contributions: {
			neutral: {
				label: 'Neutral test',
				fields: {
					count: { type: 'integer', label: 'Count' },
					enabled: { type: 'boolean', label: 'Enabled' },
					level: { type: 'string', label: 'Level', enum: ['low', 'high'] },
					...(options.references
						? {
								resources: {
									type: 'array',
									label: 'Resources',
									uniqueItems: true,
									items: { type: 'string', enum: ['B1', 'B2', 'Mail'] }
								}
							}
						: {})
				}
			}
		}
	});
	await page.route('**/admin/services/studio.tags.*', async (route) => {
		const params = new URLSearchParams(route.request().postData() ?? '');
		const scope = params.get('scope') ?? 'projectObjects';
		if (
			serviceName(route.request().url()) === 'studio.tags.Get' &&
			params.get('referenceProject') &&
			options.referenceError
		)
			return route.fulfill({
				status: 500,
				contentType: 'text/xml',
				body: '<error><message>Unable to read references of Library: Unreadable fixture</message><exception>java.io.IOException</exception></error>'
			});
		if (serviceName(route.request().url()) === 'studio.tags.Get')
			return route.fulfill({
				json: {
					...snapshot(scope),
					...(params.get('referenceProject')
						? {
								referenceTargets: options.referenceTargets ?? [params.get('referenceProject')],
								diagnostics: options.referenceWarnings ?? []
							}
						: {})
				}
			});
		const input = JSON.parse(params.get('input') ?? '{}');
		const action = params.get('action');
		commands.push({ action, input });
		await options.beforeApply?.(action);
		if (action === 'createFromReferences') {
			input.id = '3be52fce-0274-4e89-9c55-7c1f4f435170';
			tags[input.id] = { ...input.definition, shared: false };
			for (const name of options.referenceTargets ?? [input.project])
				assignments[name] = [input.id];
		} else if (action === 'remove') {
			for (const id of input.targets)
				assignments[id] = assignments[id].filter((tag) => !input.tagIds.includes(tag));
		} else if (action === 'assign') {
			for (const id of input.targets)
				assignments[id] = [...new Set([...(assignments[id] ?? []), ...input.tagIds])];
		} else if (action === 'reorder') {
			for (const id of input.targets) assignments[id] = [...input.tagIds];
		} else tags[input.id] = input.definition;
		revision++;
		dirty = scope === 'projectObjects';
		await route.fulfill({
			json: {
				...snapshot(scope),
				id: input.id,
				done: true,
				dirtyProjects: dirty ? [projectName] : [],
				affectedTargets: [target],
				affectedContainers: scope === 'workspaceProjects' ? [''] : [projectName + ':sq']
			}
		});
	});
	await page.route('**/admin/services/studio.dbo.Copy', async (route) => {
		copies.push(new URLSearchParams(route.request().postData() ?? ''));
		await route.fulfill({ json: { done: true, xml: '<convertigo-clipboard/>' } });
	});
	await page.route('**/admin/services/studio.dbo.Rename', async (route) => {
		const params = new URLSearchParams(route.request().postData() ?? '');
		if (params.get('id') !== target) return route.fallback();
		renames.push(params);
		const previous = target;
		target = projectName + '.sq:' + params.get('name');
		assignments[target] = assignments[previous];
		delete assignments[previous];
		await route.fulfill({
			json: { done: true, id: target, affectedContainers: [projectName + ':sq'] }
		});
	});
	await page.route('**/admin/services/studio.treeview.Get', async (route) => {
		const params = new URLSearchParams(route.request().postData() ?? '');
		const grouped = params.get('tagsGrouped') === 'true';
		const children = (id) => {
			const nodes = id
				? treeviewChildren(id.replace(target, sequenceId), false, createStudioState()).map(
						(node) => ({
							...node,
							id: node.id.replace(sequenceId, target),
							...(node.id === sequenceId
								? { name: target.split('.sq:')[1], label: target.split('.sq:')[1] }
								: {})
						})
					)
				: (options.projects ?? [projectName]).map((name) =>
						treeNode(name, name, 'Project', {
							children: true,
							taggable: true,
							tagScope: 'workspaceProjects'
						})
					);
			if (options.secondSequence && id === projectName + ':sq')
				nodes.push(
					treeNode(other, 'OtherSequence', 'GenericSequence', {
						taggable: true,
						tagScope: 'projectObjects'
					})
				);
			const decorated = nodes.map((node) => ({
				...node,
				...(assignments[node.id]
					? { tags: assignments[node.id].map((id) => ({ ...tags[id], id })) }
					: {})
			}));
			if (grouped && !id)
				return [
					{
						id: 'tag-row:fixture-stack',
						rowId: 'tag-row:fixture-stack',
						tagGroup: true,
						tagId: 'e6d4e02c-d29d-4bf9-8c68-b0be926eeb86',
						scope: 'workspaceProjects',
						project: '',
						collectionId: 'workspace',
						label: 'Stack',
						count: decorated.length,
						children: decorated.map((node) => ({
							...node,
							rowId: 'tag-row:fixture-stack/' + node.id
						}))
					}
				];
			if (!grouped || !decorated.some((node) => node.id === target)) return decorated;
			const groups = ids
				.slice()
				.sort((a, b) => tags[a].label.localeCompare(tags[b].label))
				.map((tagId) => {
					const rowId = 'tag-row:' + Buffer.from(id + '/' + tagId).toString('base64url');
					return {
						id: rowId,
						rowId,
						tagGroup: true,
						tagId,
						scope: 'projectObjects',
						project: projectName,
						collectionId: id,
						label: tags[tagId].label,
						count: decorated.filter((node) => assignments[node.id]?.includes(tagId)).length,
						children: decorated
							.filter((node) => assignments[node.id]?.includes(tagId))
							.map((node) => ({ ...node, rowId: rowId + '/' + node.id }))
					};
				});
			return [...groups, ...decorated.filter((node) => !assignments[node.id])];
		};
		const idsParam = params.get('ids');
		await route.fulfill({
			json: idsParam
				? Object.fromEntries(JSON.parse(idsParam).map((id) => [id, children(id)]))
				: { children: children(params.get('id')) }
		});
	});
	return { commands, copies, renames };
}

test('studio object search supports each filter without text, combines filters and opens results', async ({
	page
}) => {
	await mockStudioServices(page);
	const requests = [];
	await page.route('**/admin/services/studio.treeview.Search', async (route) => {
		requests.push(new URLSearchParams(route.request().postData() ?? ''));
		await route.fulfill({
			json: {
				results: [
					{
						id: initStepId,
						name: 'Init',
						type: 'Step',
						project: projectName,
						path: [projectName, sequenceName],
						icon: ''
					}
				],
				truncated: false
			}
		});
	});
	await page.goto('/studio/');
	await page.getByRole('button', { name: 'Search', exact: true }).click();
	const panel = page.locator('.studio-search');
	const submit = panel.getByRole('button', { name: 'Search', exact: true });
	await expect(submit).toBeDisabled();
	const filters = [
		['Broken sources', 'brokenSources'],
		['Inactive objects', 'inactive'],
		['Uses symbols', 'symbols'],
		['Unknown symbols', 'unknownSymbols']
	];
	for (const [label, key] of filters) {
		await panel.getByRole('checkbox', { name: label }).check();
		await expect(submit).toBeEnabled();
		await submit.click();
		await expect(panel.getByText('1 object in 1 project')).toBeVisible();
		const params = requests.at(-1);
		expect(params.get('text')).toBe('');
		for (const [, filterKey] of filters) {
			expect(params.get(filterKey)).toBe(String(filterKey === key));
		}
		await panel.getByRole('checkbox', { name: label }).uncheck();
		await expect(submit).toBeDisabled();
	}
	for (const [label] of filters) await panel.getByRole('checkbox', { name: label }).check();
	await submit.click();
	await expect.poll(() => requests.length).toBe(5);
	for (const [, key] of filters) expect(requests.at(-1).get(key)).toBe('true');
	const group = panel.getByRole('button', { name: `${projectName} 1`, exact: true });
	await group.click();
	await expect(panel.getByRole('button', { name: /Init/ })).toHaveCount(0);
	await group.click();
	await panel.getByRole('button', { name: /Init/ }).click();
	await expect(page).toHaveURL(
		new RegExp(`/studio/${projectName}\\.sq~${sequenceName}\\.st~Init/$`)
	);
});

test('studio object type-only search keeps file search independent of object filters', async ({
	page
}) => {
	await mockStudioServices(page);
	const requests = [];
	await page.route('**/admin/services/studio.*.Search', async (route) => {
		requests.push({
			service: serviceName(route.request().url()),
			params: new URLSearchParams(route.request().postData() ?? '')
		});
		await route.fulfill({ json: { results: [], truncated: false } });
	});
	await page.goto('/studio/');
	await page.getByRole('button', { name: 'Search', exact: true }).click();
	const panel = page.locator('.studio-search');
	const submit = panel.getByRole('button', { name: 'Search', exact: true });
	await panel.getByRole('combobox', { name: 'Object type' }).selectOption('Step');
	await expect(submit).toBeEnabled();
	await submit.click();
	await expect(panel.getByText('No object found.')).toBeVisible();
	expect(requests[0].params.get('text')).toBe('');
	expect(requests[0].params.get('type')).toBe('Step');
	await panel.getByRole('checkbox', { name: 'Broken sources' }).check();
	await panel.getByRole('radio', { name: 'Files', exact: true }).click();
	await expect(panel.getByRole('group', { name: 'Object filters (AND)' })).toHaveCount(0);
	await expect(submit).toBeDisabled();
	await panel.getByRole('textbox', { name: 'Search in the files' }).fill('hello');
	await submit.click();
	await expect(panel.getByText('No line found.')).toBeVisible();
	expect(requests[1].service).toBe('studio.source.Search');
	for (const key of ['type', 'brokenSources', 'inactive', 'symbols', 'unknownSymbols']) {
		expect(requests[1].params.has(key)).toBe(false);
	}
	await panel.getByRole('textbox', { name: 'Search in the files' }).fill('');
	await panel.getByRole('radio', { name: 'Objects', exact: true }).click();
	await expect(panel.getByRole('checkbox', { name: 'Broken sources' })).toBeChecked();
	await expect(submit).toBeEnabled();
});

test('studio object search combines text options and project scope with filters and reports errors', async ({
	page
}) => {
	await mockStudioServices(page);
	const requests = [];
	await page.route('**/admin/services/studio.treeview.Search', async (route) => {
		requests.push(new URLSearchParams(route.request().postData() ?? ''));
		await route.fulfill({ json: { error: { message: 'The regular expression is not valid.' } } });
	});
	await page.goto('/studio/');
	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('button', { name: 'Search', exact: true }).click();
	const panel = page.locator('.studio-search');
	await panel.getByRole('combobox', { name: 'Scope' }).selectOption('project');
	await panel.getByRole('checkbox', { name: 'Unknown symbols' }).check();
	await panel.getByRole('textbox', { name: 'Search in the objects' }).fill('[');
	await panel.getByTitle('Match case', { exact: true }).click();
	await panel.getByTitle('Use a regular expression', { exact: true }).click();
	await panel.getByRole('button', { name: 'Search', exact: true }).click();
	await expect(panel.getByText('The regular expression is not valid.')).toBeVisible();
	expect(Object.fromEntries(requests[0])).toMatchObject({
		text: '[',
		matchCase: 'true',
		regExp: 'true',
		scope: projectName,
		unknownSymbols: 'true'
	});
	await expect(panel.getByRole('button', { name: 'Search', exact: true })).toBeEnabled();
});

test('studio opens a selected backend object with tree, execution and flow synchronized', async ({
	page
}) => {
	await mockStudioServices(page);
	await page.goto('/studio/#flow');

	await expect(page.locator('strong').filter({ hasText: 'Convertigo Studio' })).toBeVisible();
	await expect(page.getByRole('radio', { name: 'Backend' })).toHaveAttribute(
		'aria-checked',
		'true'
	);
	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);

	await page.getByRole('tab', { name: 'Execution', exact: true }).click();
	await expect(page.getByText('Variables (1)')).toBeVisible();
	await page.getByRole('button', { name: 'Execute' }).click();
	await expect(responseEditor(page)).toBeVisible();
	await page.getByRole('button', { name: 'Clear' }).click();
	await expect(responseEditor(page)).toHaveCount(0);

	await page.getByRole('tab', { name: 'Flow', exact: true }).click();
	await expect(page.getByRole('tab', { name: 'Flow', exact: true })).toHaveAttribute(
		'aria-selected',
		'true'
	);
	await expect(flowNodeName(page, 'request')).toBeVisible();
	await expect(flowNodeName(page, 'response')).toBeVisible();
	await expect(flowNodeName(page, 'Init')).toBeVisible();

	await expandTreeNode(page, sequenceId);
	await expandTreeNode(page, `${sequenceId}:st`);
	await selectTreeNode(page, initStepId);

	await expect(page.locator('[role="treeitem"][aria-selected="true"]')).toContainText('Init');
	await expect(flowNodeName(page, 'request')).toBeVisible();
	await expect(flowNodeName(page, 'response')).toBeVisible();
	await expect(flowNodeName(page, 'Init')).toBeVisible();
	await expect(page).toHaveURL(
		new RegExp(`/studio/${projectName}\\.sq~${sequenceName}\\.st~Init/$`)
	);
});

test('studio applies and executes requestable test cases from the execution panel', async ({
	page
}) => {
	const executionRequests = [];
	const state = createStudioState({
		variables: [
			{
				name: 'input',
				value: 'initial',
				comment: 'Execution variable'
			}
		],
		testcases: [
			{
				name: 'PresetInput',
				variable: [{ name: 'input', value: 'from-testcase' }]
			}
		]
	});
	await mockStudioServices(page, { state, executionRequests });
	await page.goto('/studio/#flow');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('tab', { name: 'Execution', exact: true }).click();

	await expect(page.getByText('Variables (1)')).toBeVisible();
	await ensureButtonExpanded(page, /Variables \(1\)/);
	await expect(page.locator('input[name="input"]')).toHaveValue('initial');
	await ensureButtonExpanded(page, /Test cases \(1\)/);
	await page.getByRole('button', { name: 'Edit' }).click();
	await expect(page.locator('input[name="input"]')).toHaveValue('from-testcase');

	await page
		.locator('.requestable-execution__actions button')
		.filter({ hasText: 'Execute' })
		.click();
	await expect.poll(() => executionRequests.length).toBe(1);
	expect(executionRequests[0]).toContain('__sequence=TestSequence');
	expect(executionRequests[0]).toContain('__nocache=true');
	// each run has a context of its own, which Stop aborts
	expect(executionRequests[0]).toMatch(/__context=studio-web-execution-[0-9a-f-]{36}&/);
	expect(executionRequests[0]).toContain('__removeContext=true');
	expect(executionRequests[0]).toContain('input=from-testcase');

	await page.locator('.requestable-testcases__grid button').filter({ hasText: 'Execute' }).click();
	await expect.poll(() => executionRequests.length).toBe(2);
	expect(executionRequests[1]).toContain('__sequence=TestSequence');
	expect(executionRequests[1]).toContain('__nocache=true');
	// each run has a context of its own, which Stop aborts
	expect(executionRequests[1]).toMatch(/__context=studio-web-execution-[0-9a-f-]{36}&/);
	expect(executionRequests[1]).toContain('__removeContext=true');
	expect(executionRequests[1]).toContain('__testcase=PresetInput');
	expect(executionRequests[1]).not.toContain('input=');
});

test('studio redirects authenticated users without the web admin role', async ({ page }) => {
	await mockStudioServices(page, { roles: ['TEST_PLATFORM'] });

	await page.goto('/studio/');

	await expect(page).toHaveURL(/\/dashboard\/$/);
});

test('studio frontend profile exposes preview devices from a navigation drawer', async ({
	page
}) => {
	await mockStudioServices(page);
	await page.goto('/studio/');

	await selectTreeNode(page, projectName);
	await page.getByRole('radio', { name: 'Frontend' }).click();

	await expect(page.getByRole('tab', { name: 'Palette' })).toBeVisible();
	await expect(page.getByRole('tab', { name: 'Properties', exact: true })).toBeVisible();
	await expect(page.getByRole('tab', { name: 'Frontend', exact: true })).toBeVisible();
	await expect(page.getByRole('tab', { name: 'Doc', exact: true })).toBeVisible();
	await expect(page.getByRole('tab', { name: /Devices/ })).toHaveCount(0);
	await page.getByRole('button', { name: 'Choose preview device' }).click();
	await expect(page.getByText('Current device')).toBeVisible();
	await expect(page.getByRole('button', { name: 'Select device Responsive' })).toBeVisible();

	await page.getByRole('button', { name: /Apple iPhone/ }).click();
	await page.getByRole('button', { name: 'Select device iPhone 17 Pro', exact: true }).click();

	await expect(page.getByText('iPhone 17 Pro').first()).toBeVisible();
	await page.getByRole('button', { name: 'Choose preview device' }).click();
	await expect(page.getByRole('button', { name: 'Landscape orientation' })).toBeEnabled();

	await page.getByRole('button', { name: 'Landscape orientation' }).click();
	await expect(page.getByText('iPhone 17 Pro · 874×402')).toBeVisible();
});

test('studio follows a Flow bootstrap to the new project, exact source and dev viewer', async ({
	page
}) => {
	const createdProject = 'BootstrappedFlowApp';
	const sourcePath =
		'libs/flow/frontbuilder/svelte/model/BootstrappedFlowApp/src/routes/+page.flow.svelte';
	const sourceId = `${createdProject}.Engine.frontends.svelte.routes.home.structure.welcomeTitle`;
	await mockStudioServices(page, {
		projects: [projectName, createdProject],
		authoringTargets: { [`${createdProject}:${sourcePath}`]: sourceId },
		adminEvents: [
			{
				id: 'bootstrap-source',
				topic: 'flow.source.changed',
				timestamp: 2,
				instance: 'studio-test',
				payload: { project: createdProject, sourcePath, reveal: true }
			},
			{
				id: 'bootstrap-viewer',
				topic: 'flow.browser.open',
				timestamp: 3,
				instance: 'studio-test',
				payload: {
					project: createdProject,
					url: '/convertigo/gw/studio-test-ticket/',
					kind: 'frontbuilder.svelte.dev'
				}
			}
		]
	});
	await page.goto('/studio/');

	await expect(page).toHaveURL(/BootstrappedFlowApp\.Engine\.frontends\.svelte/);
	await expect(page.getByRole('radio', { name: 'Frontend' })).toHaveAttribute(
		'aria-checked',
		'true'
	);
	await expect(page.locator('iframe[title="BootstrappedFlowApp frontend"]')).toHaveAttribute(
		'src',
		/\/convertigo\/gw\/studio-test-ticket\/$/
	);
	await expect(
		page.frameLocator('iframe[title="BootstrappedFlowApp frontend"]').getByText('Dev preview')
	).toBeVisible();
});

test('studio frontend profile documents Flow properties from their authoring contract', async ({
	page
}) => {
	await mockStudioServices(page, { flowPicker: true });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, flowEngineId);
	await selectTreeNode(page, frontendBuilderId);
	await page.getByRole('radio', { name: 'Frontend' }).click();
	await page.getByRole('tab', { name: 'Doc', exact: true }).click();

	// the Doc view, laid out by the dock among the other views of the Studio
	await expect(
		page.locator('[data-dock-view="doc"]').getByRole('heading', { name: 'Svelte builder' })
	).toBeVisible();
	await expect(page.getByText('Visible text rendered by the component.')).toBeVisible();
});

test('studio inserts and reorders source-backed frontend blocks at the requested tree position', async ({
	page
}) => {
	const state = createStudioState();
	await mockStudioServices(page, { state, frontendRefreshDelayMs: 4_000 });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, flowEngineId);
	await expandTreeNode(page, frontendBuilderId);
	await expandTreeNode(page, frontendStructureId);
	await selectTreeNode(page, frontendStructureId);
	await page.getByRole('radio', { name: 'Frontend' }).click();
	await page.getByRole('tab', { name: 'Palette' }).click();
	await expect(paletteItem(page, 'Text')).toBeVisible();
	await expectFrontendOrder(page, ['firstText', 'secondText']);

	await dragPaletteItemToTreeNode(page, 'Text', `${frontendStructureId}.secondText`, 0.08);

	await expect(
		page.locator(`button.studio-tree-node__content[data-node-id="${frontendStructureId}.text1"]`)
	).toBeVisible({ timeout: 2_500 });
	await expectFrontendOrder(page, ['firstText', 'text1', 'secondText']);
	await expect(page.locator('[role="treeitem"][aria-selected="true"]')).toContainText('New text');
	await expect(page.getByRole('textbox', { name: 'Rename step' })).toHaveCount(0);
	await expect(page.getByRole('tab', { name: 'Properties', exact: true })).toHaveAttribute(
		'aria-selected',
		'true'
	);

	await dragTreeNodeToTreeNode(
		page,
		`${frontendStructureId}.text1`,
		`${frontendStructureId}.secondText`,
		{ yRatio: 0.9 }
	);

	await expectFrontendOrder(page, ['firstText', 'secondText', 'text1']);
	await expect(page.locator('[role="treeitem"][aria-selected="true"]')).toContainText('New text');
});

test('studio drops a frontend block on a closed container without leaving its placeholder', async ({
	page
}) => {
	const state = createStudioState();
	await mockStudioServices(page, { state });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, flowEngineId);
	await expandTreeNode(page, frontendBuilderId);
	await selectTreeNode(page, frontendStructureId);
	await page.getByRole('radio', { name: 'Frontend' }).click();
	await page.getByRole('tab', { name: 'Palette' }).click();
	await expect(paletteItem(page, 'Text')).toBeVisible();

	// the structure is closed, its children are not loaded: the placeholder goes after it
	await dragPaletteItemToTreeNode(page, 'Text', frontendStructureId, 0.5);

	await expect(
		page.locator(`button.studio-tree-node__content[data-node-id="${frontendStructureId}.text1"]`)
	).toBeVisible();
	await expect(
		page.locator('button.studio-tree-node__content[data-node-id*="__pending_"]')
	).toHaveCount(0);
});

test('studio drops a block from the preview after a node that takes nothing inside', async ({
	page
}) => {
	const state = createStudioState();
	const addRequests = [];
	await mockStudioServices(page, {
		state,
		addRequests,
		authoringReferences: { secondText: `${frontendStructureId}.secondText` }
	});
	// the Dev viewer drops a palette block in the middle of the second text
	await page.route('**/convertigo/gw/studio-test-ticket/**', async (route) => {
		await route.fulfill({
			status: 200,
			contentType: 'text/html',
			body: `<!doctype html><html><body><button type="button">Drop on the second text</button><script>
				document.querySelector('button').addEventListener('click', () => parent.postMessage({
					protocol: 'convertigo.flow.authoring.v1',
					type: 'authoring.drop',
					reference: {
						nodeId: 'secondText',
						sourceRelativePath: 'model/StudioProject/src/routes/+page.flow.svelte',
						sourceMutationPath: 'frontAst.nodes[1]'
					},
					position: 'inside',
					payload: { type: 'paletteData', data: { type: 'FrontendBlock', id: 'FrontendBlock:svelte.text', block: 'svelte.text', name: 'Text' } }
				}, location.origin));
			</script></body></html>`
		});
	});
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, flowEngineId);
	await expandTreeNode(page, frontendBuilderId);
	await expandTreeNode(page, frontendStructureId);
	await selectTreeNode(page, frontendStructureId);
	await page.getByRole('radio', { name: 'Frontend' }).click();
	await page.getByRole('radio', { name: 'Dev', exact: true }).click();
	await expectFrontendOrder(page, ['firstText', 'secondText']);

	await page
		.frameLocator('iframe[title="StudioProject frontend"]')
		.getByRole('button', { name: 'Drop on the second text' })
		.click();

	// a text takes no child: the block goes after it, as in the tree, not at the end of the page
	await expect
		.poll(() => addRequests.map((params) => `${params.get('target')}:${params.get('position')}`))
		.toEqual([`${frontendStructureId}.secondText:after`]);
	await expectFrontendOrder(page, ['firstText', 'secondText', 'text1']);
});

test('studio runs the actions of the chip of the preview as its tree does', async ({ page }) => {
	const state = createStudioState();
	const contextActions = [];
	const removeRequests = [];
	await mockStudioServices(page, {
		state,
		contextActions,
		removeRequests,
		authoringReferences: { secondText: `${frontendStructureId}.secondText` }
	});
	// the chip of a Dev viewer asks for the actions of the second text, then runs two of them
	await page.route('**/convertigo/gw/studio-test-ticket/**', async (route) => {
		await route.fulfill({
			status: 200,
			contentType: 'text/html',
			body: `<!doctype html><html><body>
				<output id="actions"></output><output id="done"></output>
				<button type="button" id="ask">Ask the actions</button>
				<button type="button" id="disable">Disable</button>
				<button type="button" id="delete">Delete</button>
				<script>
					const reference = { nodeId: 'secondText', sourceMutationPath: 'frontAst.nodes[1]',
						sourceRelativePath: 'model/StudioProject/src/routes/+page.flow.svelte' };
					const send = (message) => parent.postMessage(Object.assign(
						{ protocol: 'convertigo.flow.authoring.v1', reference: reference }, message), location.origin);
					addEventListener('message', (event) => {
						if (event.data && event.data.type === 'authoring.actions') {
							document.getElementById('actions').textContent =
								event.data.actions.map((action) => action.id).join(',');
						}
						if (event.data && event.data.type === 'authoring.action.done') {
							document.getElementById('done').textContent += event.data.action + ';';
						}
					});
					document.getElementById('ask').onclick = () => send({ type: 'authoring.actions.request' });
					document.getElementById('disable').onclick = () =>
						send({ type: 'authoring.action', action: 'flow.node.disable' });
					document.getElementById('delete').onclick = () =>
						send({ type: 'authoring.action', action: 'object.delete' });
				</script></body></html>`
		});
	});
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, flowEngineId);
	await expandTreeNode(page, frontendBuilderId);
	await expandTreeNode(page, frontendStructureId);
	await selectTreeNode(page, frontendStructureId);
	await page.getByRole('radio', { name: 'Frontend' }).click();
	await page.getByRole('radio', { name: 'Dev', exact: true }).click();
	await expectFrontendOrder(page, ['firstText', 'secondText']);
	const viewer = page.frameLocator('iframe[title="StudioProject frontend"]');

	// the actions of the tree on the object itself, not those of the whole frontend
	await viewer.getByRole('button', { name: 'Ask the actions' }).click();
	await expect(viewer.locator('#actions')).toHaveText('flow.node.disable,object.delete');
	await viewer.getByRole('button', { name: 'Disable' }).click();
	await expect.poll(() => contextActions.at(-1)).toBe('flow.node.disable');
	await expect(viewer.locator('#done')).toHaveText('flow.node.disable;');
	// Delete asks the same confirmation as the tree
	page.once('dialog', (dialog) => dialog.accept());
	await viewer.getByRole('button', { name: 'Delete' }).click();
	await expect
		.poll(() => removeRequests.map((params) => params.get('id')))
		.toEqual([`${frontendStructureId}.secondText`]);
	await expect(viewer.locator('#done')).toHaveText('flow.node.disable;object.delete;');
	await expectFrontendOrder(page, ['firstText']);
});

test('studio tells the preview when its move is over and highlights the moved node where it is', async ({
	page
}) => {
	const state = createStudioState();
	await mockStudioServices(page, {
		state,
		authoringReferences: {
			firstText: `${frontendStructureId}.firstText`,
			secondText: `${frontendStructureId}.secondText`
		}
	});
	// a Dev viewer moves the second text before the first one, and shows what the Studio tells it
	await page.route('**/convertigo/gw/studio-test-ticket/**', async (route) => {
		await route.fulfill({
			status: 200,
			contentType: 'text/html',
			body: `<!doctype html><html><body>
				<output id="highlight"></output><output id="done"></output>
				<button type="button" id="move">Move before the first text</button>
				<script>
					const path = 'model/StudioProject/src/routes/+page.flow.svelte';
					addEventListener('message', (event) => {
						const message = event.data || {};
						if (message.type === 'authoring.highlight') {
							document.getElementById('highlight').textContent =
								message.reference.nodeId + '@' + message.reference.sourceMutationPath;
						}
						if (message.type === 'authoring.move.done') {
							document.getElementById('done').textContent = message.reference.nodeId;
						}
					});
					parent.postMessage({ protocol: 'convertigo.flow.authoring.v1', type: 'viewer.ready' }, location.origin);
					document.getElementById('move').onclick = () => parent.postMessage({
						protocol: 'convertigo.flow.authoring.v1', type: 'authoring.move', position: 'before',
						source: { nodeId: 'secondText', sourceRelativePath: path, sourceMutationPath: 'frontAst.nodes[1]' },
						reference: { nodeId: 'firstText', sourceRelativePath: path, sourceMutationPath: 'frontAst.nodes[0]' }
					}, location.origin);
				</script></body></html>`
		});
	});
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, flowEngineId);
	await expandTreeNode(page, frontendBuilderId);
	await expandTreeNode(page, frontendStructureId);
	await selectTreeNode(page, `${frontendStructureId}.secondText`);
	await page.getByRole('radio', { name: 'Frontend' }).click();
	await page.getByRole('radio', { name: 'Dev', exact: true }).click();
	const viewer = page.frameLocator('iframe[title="StudioProject frontend"]');
	await expect(viewer.locator('#highlight')).toHaveText('secondText@frontAst.nodes[1]');

	await viewer.getByRole('button', { name: 'Move before the first text' }).click();

	// the moved text keeps its id: the viewer is told the move is over, and where the text is now
	await expect(viewer.locator('#done')).toHaveText('secondText');
	await expect(viewer.locator('#highlight')).toHaveText('secondText@frontAst.nodes[0]');
	await expectFrontendOrder(page, ['secondText', 'firstText']);
});

test('studio vibe profile gives the Assistant the selected project and keeps the live frontend beside it', async ({
	page
}) => {
	const state = createStudioState();
	const contextActions = [];
	await page.setViewportSize({ width: 1440, height: 900 });
	await mockStudioServices(page, {
		state,
		contextActions,
		assistant: true,
		projects: [projectName, 'AnotherFlowProject']
	});
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, flowEngineId);
	await selectTreeNode(page, frontendBuilderId);
	await page.getByRole('radio', { name: 'Vibe' }).click();

	const assistantFrame = page.frameLocator('iframe[title="Convertigo Assistant"]');
	await expect(page.locator('iframe[title="Convertigo Assistant"]')).toHaveAttribute(
		'src',
		/DisplayObjects\/mobile\/path-to-xfirst\/:threadid\?.*serverAgent=1/
	);
	await expect
		.poll(async () => {
			const src = await page.locator('iframe[title="Convertigo Assistant"]').getAttribute('src');
			return new URL(src ?? '', page.url()).searchParams.get('targetProject');
		})
		.toBeNull();
	await expect
		.poll(async () => {
			const src = await page.locator('iframe[title="Convertigo Assistant"]').getAttribute('src');
			return new URL(src ?? '', page.url()).searchParams.get('agentProfile');
		})
		.toBeNull();
	await expect(assistantFrame.getByTestId('assistant-context')).toHaveText(
		`${projectName} · studio`
	);
	await expect(assistantFrame.getByTestId('assistant-context')).toHaveAttribute(
		'data-server-agent',
		'1'
	);
	await expect(assistantFrame.getByTestId('assistant-context')).toHaveAttribute(
		'data-assistant-runtime',
		'server'
	);
	await expect(assistantFrame.getByTestId('assistant-context')).toHaveAttribute(
		'data-agent-bridge-available',
		'true'
	);
	await expect(assistantFrame.getByTestId('assistant-context')).toHaveAttribute(
		'data-query-project',
		'No project'
	);
	await expect(assistantFrame.getByTestId('assistant-context')).toHaveAttribute(
		'data-agent-profile',
		'flow'
	);
	const assistantElement = page.locator('iframe[title="Convertigo Assistant"]');
	const originalAssistantUrl = await assistantElement.getAttribute('src');
	await assistantFrame.getByTestId('assistant-context').evaluate((node) => {
		node.dataset.conversationProof = 'keep-this-conversation';
	});
	await expect(page.getByRole('tab', { name: 'Frontend', exact: true })).toHaveAttribute(
		'aria-selected',
		'true'
	);

	const actions = page.getByRole('button', { name: 'Actions for Svelte frontend' });
	await actions.click();
	await page.getByRole('menuitem', { name: /Start dev mode/ }).click();
	await expect.poll(() => contextActions).toEqual(['frontbuilder.svelte.dev.start']);
	await expect(page.locator('iframe[title="StudioProject frontend"]')).toHaveAttribute(
		'src',
		/\/convertigo\/gw\/studio-test-ticket\/$/
	);
	await expect(page.getByText('Dev', { exact: true })).toBeVisible();

	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('tab', { name: 'Execution', exact: true }).click();
	await expect(page.getByText('Variables (1)')).toBeVisible();
	await selectTreeNode(page, 'AnotherFlowProject');
	await expect(assistantFrame.getByTestId('assistant-context')).toHaveText(
		'AnotherFlowProject · studio'
	);
	await expect(assistantElement).toHaveAttribute('src', originalAssistantUrl);
	await expect(assistantFrame.getByTestId('assistant-context')).toHaveAttribute(
		'data-conversation-proof',
		'keep-this-conversation'
	);
});

test('studio vibe profile opens the Agent route without requiring a selected project', async ({
	page
}) => {
	await mockStudioServices(page, { assistant: true, noProjects: true });
	await page.goto('/studio/');
	await page.getByRole('radio', { name: 'Vibe' }).click();

	const assistant = page.locator('iframe[title="Convertigo Assistant"]');
	await expect(assistant).toHaveAttribute(
		'src',
		/DisplayObjects\/mobile\/path-to-xfirst\/:threadid\?.*serverAgent=1/
	);
	const assistantContext = page
		.frameLocator('iframe[title="Convertigo Assistant"]')
		.getByTestId('assistant-context');
	await expect(assistantContext).toHaveText('No project · studio');
	await expect(assistantContext).toHaveAttribute('data-initial-project', 'No project');
	await expect(assistantContext).toHaveAttribute('data-server-agent', '1');
	await expect(assistantContext).toHaveAttribute('data-query-project', 'No project');
});

test('studio vibe profile remains usable on touch with the optional tree hidden', async ({
	page
}) => {
	await page.setViewportSize({ width: 390, height: 844 });
	await mockStudioServices(page, { assistant: true });
	await page.goto('/studio/');
	await selectTreeNode(page, projectName);
	await page.getByRole('radio', { name: 'Vibe' }).click();

	await page.getByRole('button', { name: 'Hide projects' }).click();
	await expect(page.getByRole('tree', { name: 'Projects' })).toBeHidden();
	const assistant = page.locator('iframe[title="Convertigo Assistant"]');
	const frontend = page.locator('iframe[title="StudioProject frontend"]');
	await expect(assistant).toBeVisible();
	await expect(frontend).toBeVisible();
	await expect
		.poll(async () => {
			const assistantBox = await assistant.boundingBox();
			const frontendBox = await frontend.boundingBox();
			return Boolean(assistantBox && frontendBox && assistantBox.y < frontendBox.y);
		})
		.toBe(true);
});

test('studio exposes Flow actions through a touch menu and switches the iframe to dev mode', async ({
	page
}) => {
	const state = createStudioState();
	const contextActions = [];
	await page.setViewportSize({ width: 390, height: 844 });
	await mockStudioServices(page, { state, contextActions });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, flowEngineId);
	await selectTreeNode(page, frontendBuilderId);
	await page.getByRole('radio', { name: /Frontend|Tree, preview/ }).click();

	const actions = page.getByRole('button', { name: 'Actions for Svelte frontend' });
	await expect(actions).toBeVisible();
	await actions.click();
	await expect(page.getByRole('menuitem', { name: /Start dev mode/ })).toBeEnabled();
	await expect(page.getByRole('menuitem', { name: /Stop dev mode/ })).toBeDisabled();
	await expect(page.getByRole('menuitem', { name: /Build prod/ })).toBeEnabled();

	await page.getByRole('menuitem', { name: /Start dev mode/ }).click();
	await expect.poll(() => contextActions).toEqual(['frontbuilder.svelte.dev.start']);
	const devIframe = page.locator('iframe[title="StudioProject frontend"]');
	await expect(devIframe).toHaveAttribute('src', /\/convertigo\/gw\/studio-test-ticket\/$/);
	await expect
		.poll(async () => new URL((await devIframe.getAttribute('src')) ?? '', page.url()).origin)
		.toBe(new URL(page.url()).origin);
	await expect(page.getByText('Dev', { exact: true })).toBeVisible();
	await expect(
		page.frameLocator('iframe[title="StudioProject frontend"]').getByText('Dev preview')
	).toBeVisible();

	await actions.click();
	await expect(page.getByRole('menuitem', { name: /Start dev mode/ })).toBeDisabled();
	await expect(page.getByRole('menuitem', { name: /Stop dev mode/ })).toBeEnabled();
	await page.getByRole('menuitem', { name: /Stop dev mode/ }).click();
	await expect
		.poll(() => contextActions)
		.toEqual(['frontbuilder.svelte.dev.start', 'frontbuilder.svelte.dev.stop']);
	await expect(page.locator('iframe[title="StudioProject frontend"]')).toHaveAttribute(
		'src',
		/DisplayObjects\/mobile\/index\.html/
	);
	await expect(page.getByText('Prod', { exact: true })).toBeVisible();
	await expect(
		page.frameLocator('iframe[title="StudioProject frontend"]').getByText('Prod preview')
	).toBeVisible();
	await actions.click();
	await expect(page.getByRole('menuitem', { name: /Start dev mode/ })).toBeEnabled();
	await expect(page.getByRole('menuitem', { name: /Stop dev mode/ })).toBeDisabled();
});

test('studio switches Prod and Dev directly from the frontend viewer toolbar', async ({ page }) => {
	const state = createStudioState();
	const contextActions = [];
	await mockStudioServices(page, { state, contextActions });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, flowEngineId);
	await selectTreeNode(page, frontendBuilderId);
	await page.getByRole('radio', { name: /Frontend|Tree, preview/ }).click();

	await page.getByRole('radio', { name: 'Dev', exact: true }).click();
	await expect.poll(() => contextActions).toEqual(['frontbuilder.svelte.dev.start']);
	await expect(page.locator('iframe[title="StudioProject frontend"]')).toHaveAttribute(
		'src',
		/\/convertigo\/gw\/studio-test-ticket\/$/
	);

	await page.getByRole('radio', { name: 'Prod', exact: true }).click();
	await expect(page.locator('iframe[title="StudioProject frontend"]')).toHaveAttribute(
		'src',
		/DisplayObjects\/mobile\/index\.html/
	);
	await expect.poll(() => contextActions).toEqual(['frontbuilder.svelte.dev.start']);
});

test('studio restores an already running Dev viewer when returning to the frontend', async ({
	page
}) => {
	const state = createStudioState();
	state.devRunning = true;
	state.contextMenuDevRunning = true;
	const contextActions = [];
	await mockStudioServices(page, { state, contextActions });
	await page.goto('/studio/');

	await selectTreeNode(page, projectName);
	await page.getByRole('radio', { name: 'Vibe' }).click();

	await expect.poll(() => contextActions).toEqual(['frontbuilder.svelte.dev.open']);
	await expect(page.locator('iframe[title="StudioProject frontend"]')).toHaveAttribute(
		'src',
		/\/convertigo\/gw\/studio-test-ticket\/$/
	);
	await expect(page.getByRole('radio', { name: 'Dev', exact: true })).toBeChecked();
});

test('studio hosts the Flow binding web component and applies its value on touch', async ({
	page
}) => {
	const propertyUpdates = [];
	const flowPickerRequests = [];
	await page.addInitScript(() => localStorage.setItem('theme', 'dark'));
	await page.setViewportSize({ width: 390, height: 844 });
	await mockStudioServices(page, { propertyUpdates, flowPickerRequests, flowPicker: true });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, flowEngineId);
	await selectTreeNode(page, frontendBuilderId);
	await page.getByRole('tab', { name: 'Properties', exact: true }).click();
	await page.getByRole('button', { name: 'Choose a source or compose Text' }).click();
	await expect(page.getByRole('tab', { name: 'Picker', exact: true })).toHaveCount(0);

	const picker = page.frameLocator('iframe[title="Flow picker for Text"]');
	await expect(picker.locator('flow-binding-editor')).toBeVisible();
	await expect(picker.locator('html')).toHaveAttribute('data-flow-theme', 'dark');
	const pickerFrame = page.locator('iframe[title="Flow picker for Text"]');
	const sourceDocument = await pickerFrame.getAttribute('srcdoc');
	await page.getByLabel('Toggle light mode').click();
	await expect(picker.locator('html')).toHaveAttribute('data-flow-theme', 'light');
	expect(await pickerFrame.getAttribute('srcdoc')).toBe(sourceDocument);
	await picker.getByRole('button', { name: 'Source' }).click();
	await picker.getByRole('combobox', { name: 'Source' }).selectOption('local.points');
	// the value of the picker is a change of the Properties, which their Apply sets without saving
	await picker.getByRole('button', { name: 'Apply' }).click();
	expect(await pickerFrame.getAttribute('srcdoc')).toBe(sourceDocument);
	await expect(picker.locator('html')).toHaveAttribute('data-flow-theme', 'light');
	expect(propertyUpdates).toHaveLength(0);
	await page.getByRole('button', { name: 'Apply', exact: true }).click();

	await expect.poll(() => propertyUpdates.length).toBe(1);
	expect(flowPickerRequests).toHaveLength(1);
	await expect(page.getByText('Loading Flow picker', { exact: true })).toHaveCount(0);
	const update = propertyUpdates[0];
	expect(update.get('id')).toBe(frontendBuilderId);
	expect(update.get('save')).toBe('false');
	const props = JSON.parse(update.get('props') ?? '[]');
	expect(props).toEqual([
		expect.objectContaining({
			name: 'text',
			originalValue: JSON.stringify({
				mode: 'literal',
				value: 'Fresh Flow chart benchmark'
			}),
			value: JSON.stringify({
				mode: 'source',
				source: { category: 'local', name: 'points' },
				path: [{ kind: 'property', name: 'value' }]
			})
		})
	]);
	// the change applied closes the picker of the property
	await expect(page.locator('iframe[title="Flow picker for Text"]')).toHaveCount(0);
	expect(flowPickerRequests).toHaveLength(1);
});

test('studio edits a literal Flow binding directly without loading the picker', async ({
	page
}) => {
	const propertyUpdates = [];
	const flowPickerRequests = [];
	await mockStudioServices(page, { propertyUpdates, flowPickerRequests, flowPicker: true });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, flowEngineId);
	await selectTreeNode(page, frontendBuilderId);
	await page.getByRole('tab', { name: 'Properties', exact: true }).click();
	const textProperty = page.locator('.studio-properties__field').filter({ hasText: 'TEXT' });
	await textProperty.getByRole('textbox').fill('Edited directly');
	await page.getByRole('button', { name: 'Apply', exact: true }).click();

	await expect.poll(() => propertyUpdates.length).toBe(1);
	expect(flowPickerRequests).toHaveLength(0);
	const props = JSON.parse(propertyUpdates[0].get('props') ?? '[]');
	expect(JSON.parse(props[0]?.value ?? '{}')).toEqual({
		mode: 'literal',
		value: 'Edited directly'
	});
});

test('studio chooses the schema type of a step among the types of the project', async ({
	page
}) => {
	const propertyUpdates = [];
	await mockStudioServices(page, { propertyUpdates, qnameProperty: true });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await expandTreeNode(page, sequenceId);
	await expandTreeNode(page, `${sequenceId}:st`);
	await selectTreeNode(page, initStepId);
	await page.getByRole('tab', { name: 'Properties', exact: true }).click();
	const property = page.locator('.studio-properties__field').filter({ hasText: 'Schema type' });
	await expect(property.locator('code')).toHaveText('none');
	await property.getByRole('button', { name: 'Choose Schema type' }).click();

	const dialog = page.getByRole('dialog', { name: 'Schema type' });
	await expect(dialog.getByRole('option')).toHaveCount(2);
	await dialog.getByRole('searchbox').fill('person');
	await expect(dialog.getByRole('option')).toHaveCount(1);
	await dialog.getByRole('option', { name: /personType/ }).click();
	await expect(dialog.getByRole('status')).toHaveText('Use the dynamic type personType.');
	await dialog.getByRole('textbox', { name: 'Local name' }).fill('addressType');
	await expect(dialog.getByRole('status')).toHaveText(
		'Create the dynamic type addressType in http://studio/project.'
	);
	await dialog.getByRole('button', { name: 'Apply' }).click();
	await expect(property.locator('code')).toHaveText('addressType (http://studio/project)');

	await page.getByRole('button', { name: 'Apply', exact: true }).click();
	await expect.poll(() => propertyUpdates.length).toBe(1);
	const props = JSON.parse(propertyUpdates[0].get('props') ?? '[]');
	expect(props[0]).toMatchObject({
		name: 'xmlComplexTypeAffectation',
		value: '{http://studio/project}addressType'
	});
});

test('studio chooses the font of an application among the fonts of the catalog', async ({
	page
}) => {
	const propertyUpdates = [];
	await mockStudioServices(page, { propertyUpdates, fontProperty: true });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await expandTreeNode(page, sequenceId);
	await expandTreeNode(page, `${sequenceId}:st`);
	await selectTreeNode(page, initStepId);
	await page.getByRole('tab', { name: 'Properties', exact: true }).click();
	const property = page.locator('.studio-properties__field').filter({ hasText: 'Definition' });
	await expect(property.locator('code')).toHaveText('inherits');
	await property.getByRole('button', { name: 'Choose Definition' }).click();

	const dialog = page.getByRole('dialog', { name: 'Definition' });
	await expect(dialog.getByRole('option')).toHaveCount(2);
	await dialog.getByRole('searchbox').fill('plex');
	await dialog.getByRole('option', { name: /IBM Plex Sans/ }).click();
	await expect(dialog.getByRole('combobox', { name: 'Weight' })).toHaveValue('400');
	await dialog.getByRole('combobox', { name: 'Weight' }).selectOption('700');
	await dialog.getByRole('button', { name: 'Apply' }).click();
	await expect(property.locator('code')).toHaveText('IBM Plex Sans (700 normal latin)');

	await page.getByRole('button', { name: 'Apply', exact: true }).click();
	await expect.poll(() => propertyUpdates.length).toBe(1);
	const props = JSON.parse(propertyUpdates[0].get('props') ?? '[]');
	expect(props[0]?.name).toBe('fontSource');
	expect(JSON.parse(props[0]?.value ?? '{}')).toEqual({
		fontId: 'ibm-plex-sans',
		fontFamily: 'IBM Plex Sans',
		fontWeight: '700',
		fontStyle: 'normal',
		fontSubset: 'latin'
	});
});

test('studio chooses the requestable an operation targets among the requestables', async ({
	page
}) => {
	const propertyUpdates = [];
	await mockStudioServices(page, { propertyUpdates, namedSourceProperty: true });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('tab', { name: 'Properties', exact: true }).click();
	await page.getByRole('button', { name: 'Choose Target requestable' }).click();

	const dialog = page.getByRole('dialog', { name: 'Target requestable' });
	await expect(dialog.getByRole('option')).toHaveCount(3);
	await expect(dialog.getByRole('option', { selected: true })).toContainText('TestSequence');
	await dialog.getByRole('searchbox').fill('book');
	await dialog.getByRole('option', { name: /getBook/ }).click();
	await expect(dialog.getByRole('status')).toHaveText('StudioProject.Books.getBook');
	await dialog.getByRole('button', { name: 'Apply' }).click();

	await page.getByRole('button', { name: 'Apply', exact: true }).click();
	await expect.poll(() => propertyUpdates.length).toBe(1);
	const props = JSON.parse(propertyUpdates[0].get('props') ?? '[]');
	expect(props[0]).toMatchObject({
		name: 'targetRequestable',
		value: 'StudioProject.Books.getBook'
	});
});

test('studio edits a project reference by its parts', async ({ page }) => {
	const propertyUpdates = [];
	await mockStudioServices(page, { propertyUpdates, referenceProperty: true });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('tab', { name: 'Properties', exact: true }).click();
	await page.getByRole('button', { name: 'Edit the project reference' }).click();

	const dialog = page.getByRole('dialog', { name: 'Project name and remote URL' });
	await expect(dialog.getByRole('combobox', { name: 'Project name' })).toHaveValue('lib_OAuth');
	await expect(dialog.getByRole('textbox', { name: 'Git branch' })).toBeDisabled();
	await dialog
		.getByRole('textbox', { name: 'Git or http URL' })
		.fill('https://github.com/convertigo/c8oprj-lib-oauth.git');
	await dialog.getByRole('textbox', { name: 'Git branch' }).fill('8.4');
	await expect(dialog.getByRole('textbox', { name: 'Project remote URL' })).toHaveValue(
		'lib_OAuth=https://github.com/convertigo/c8oprj-lib-oauth.git:branch=8.4'
	);
	await dialog.getByRole('button', { name: 'Apply' }).click();

	await page.getByRole('button', { name: 'Apply', exact: true }).click();
	await expect.poll(() => propertyUpdates.length).toBe(1);
	const props = JSON.parse(propertyUpdates[0].get('props') ?? '[]');
	expect(props[0]).toMatchObject({
		name: 'projectName',
		value: 'lib_OAuth=https://github.com/convertigo/c8oprj-lib-oauth.git:branch=8.4'
	});
});

test('studio builds the response lifetime of a requestable', async ({ page }) => {
	const propertyUpdates = [];
	await mockStudioServices(page, { propertyUpdates, lifetimeProperty: true });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('tab', { name: 'Properties', exact: true }).click();
	await page.getByRole('button', { name: 'Build the response lifetime' }).click();

	const dialog = page.getByRole('dialog', { name: 'Response lifetime' });
	await expect(dialog.getByRole('spinbutton', { name: 'Seconds' })).toHaveValue('3600');
	await dialog.getByRole('combobox', { name: 'Expires' }).selectOption('weekly');
	await dialog.getByRole('combobox', { name: 'Day' }).selectOption('Monday');
	await dialog.getByLabel('Time', { exact: true }).fill('08:30:00');
	await expect(dialog.getByRole('status')).toHaveText('weekly,08:30:00,2');
	await dialog.getByRole('button', { name: 'Apply' }).click();

	await page.getByRole('button', { name: 'Apply', exact: true }).click();
	await expect.poll(() => propertyUpdates.length).toBe(1);
	const props = JSON.parse(propertyUpdates[0].get('props') ?? '[]');
	expect(props[0]).toMatchObject({ name: 'responseExpiryDate', value: 'weekly,08:30:00,2' });
});

test('studio sets the visibility of a variable with its masks', async ({ page }) => {
	const propertyUpdates = [];
	await mockStudioServices(page, { propertyUpdates, flagsProperty: true });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('tab', { name: 'Properties', exact: true }).click();
	const flags = page.getByRole('group', { name: 'Visibility' });
	await expect(flags.getByRole('checkbox', { name: 'Mask in the log files' })).toBeChecked();
	await expect(flags.getByRole('checkbox', { name: 'Mask in the Studio' })).not.toBeChecked();
	// a mask applies at once, as the property sheet of the Eclipse Studio sets a checked value: the masks
	// of the log files and of the Studio together
	await flags.getByRole('checkbox', { name: 'Mask in the Studio' }).check();
	await expect.poll(() => propertyUpdates.length).toBe(1);
	const props = JSON.parse(propertyUpdates[0].get('props') ?? '[]');
	expect(props[0]).toMatchObject({ name: 'visibility', value: '3' });
});

test('studio treats an empty legacy Flow binding as an editable literal', async ({ page }) => {
	const propertyUpdates = [];
	const flowPickerRequests = [];
	await mockStudioServices(page, { propertyUpdates, flowPickerRequests, flowPicker: true });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, flowEngineId);
	await selectTreeNode(page, frontendBuilderId);
	await page.getByRole('tab', { name: 'Properties', exact: true }).click();
	const classesProperty = page.locator('.studio-properties__field').filter({ hasText: 'CLASSES' });
	await classesProperty.getByRole('textbox').fill('layout-centered');
	await page.getByRole('button', { name: 'Apply', exact: true }).click();

	await expect.poll(() => propertyUpdates.length).toBe(1);
	expect(flowPickerRequests).toHaveLength(0);
	const props = JSON.parse(propertyUpdates[0].get('props') ?? '[]');
	expect(JSON.parse(props[0]?.value ?? '{}')).toEqual({
		mode: 'literal',
		value: 'layout-centered'
	});
});

test('studio retries a transient Flow picker failure without closing the editor', async ({
	page
}) => {
	const flowPickerProbe = { remaining: 1, requests: 0 };
	await mockStudioServices(page, { flowPicker: true, flowPickerProbe });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, flowEngineId);
	await selectTreeNode(page, frontendBuilderId);
	await page.getByRole('tab', { name: 'Properties', exact: true }).click();
	await page.getByRole('button', { name: 'Choose a source or compose Text' }).click();

	const picker = page.frameLocator('iframe[title="Flow picker for Text"]');
	await expect(picker.locator('flow-binding-editor')).toBeVisible();
	await expect.poll(() => flowPickerProbe.requests).toBe(2);
	await expect(page.getByRole('button', { name: 'Retry' })).toHaveCount(0);
});

test('studio keeps tree, flow and url synchronized after a flow rename mutation', async ({
	page
}) => {
	const state = createStudioState();
	await mockStudioServices(page, { state });
	await page.goto('/studio/#flow');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('tab', { name: 'Flow', exact: true }).click();

	await flowNodeName(page, 'Init').click();
	await page.getByRole('button', { name: 'Rename step' }).click();
	const input = page.getByRole('textbox', { name: 'Rename step' });
	await expect(input).toBeFocused();
	await input.fill('RenamedInit');
	await input.press('Enter');

	await expect(flowNodeName(page, 'RenamedInit')).toBeVisible();
	await ensureTreeNodeExpanded(page, sequenceId);
	await ensureTreeNodeExpanded(page, `${sequenceId}:st`);
	await expect(page.locator('[role="treeitem"][aria-selected="true"]')).toContainText(
		'RenamedInit'
	);
	await expect(page).toHaveURL(
		new RegExp(`/studio/${projectName}\\.sq~${sequenceName}\\.st~RenamedInit/$`)
	);
});

test('studio keeps flow and url synchronized after a tree rename mutation', async ({ page }) => {
	const state = createStudioState();
	await mockStudioServices(page, { state });
	await page.goto('/studio/#flow');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('tab', { name: 'Flow', exact: true }).click();
	await ensureTreeNodeExpanded(page, sequenceId);
	await ensureTreeNodeExpanded(page, `${sequenceId}:st`);
	await selectTreeNode(page, initStepId);

	await page.getByRole('button', { name: 'Actions for Init' }).click();
	await page.getByRole('menuitem', { name: 'Rename', exact: true }).click();
	const input = page.getByRole('textbox', { name: 'Rename object' });
	await expect(input).toBeFocused();
	await input.fill('TreeRenamedInit');
	await input.press('Enter');

	await expect(page.locator('[role="treeitem"][aria-selected="true"]')).toContainText(
		'TreeRenamedInit'
	);
	await expect(flowNodeName(page, 'TreeRenamedInit')).toBeVisible();
	await expect(page).toHaveURL(
		new RegExp(`/studio/${projectName}\\.sq~${sequenceName}\\.st~TreeRenamedInit/$`)
	);
});

test('studio keeps tree, flow and url synchronized after a flow delete mutation', async ({
	page
}) => {
	const state = createStudioState();
	await mockStudioServices(page, { state });
	await page.goto('/studio/#flow');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('tab', { name: 'Flow', exact: true }).click();
	await ensureTreeNodeExpanded(page, sequenceId);
	await ensureTreeNodeExpanded(page, `${sequenceId}:st`);
	await expect(
		page.locator(`button.studio-tree-node__content[data-node-id="${initStepId}"]`)
	).toBeVisible();

	await flowNodeName(page, 'Init').click();
	page.once('dialog', async (dialog) => {
		expect(dialog.message()).toContain('Init');
		await dialog.accept();
	});
	await page.getByRole('button', { name: 'Delete step' }).click();

	await expect(flowNodeName(page, 'Init')).toHaveCount(0);
	await expect(flowNodeName(page, 'return')).toBeVisible();
	await expect(
		page.locator(`button.studio-tree-node__content[data-node-id="${initStepId}"]`)
	).toHaveCount(0);
	await expect(page.locator('[role="treeitem"][aria-selected="true"]')).toContainText(sequenceName);
	await expect(page).toHaveURL(new RegExp(`/studio/${projectName}\\.sq~${sequenceName}/$`));
});

test('studio selects several tree objects and deletes them together', async ({ page }) => {
	const state = createStudioState();
	await mockStudioServices(page, { state });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await expandTreeNode(page, sequenceId);
	await expandTreeNode(page, `${sequenceId}:st`);
	const init = page.locator(`button.studio-tree-node__content[data-node-id="${initStepId}"]`);
	const returnStep = page.locator(
		`button.studio-tree-node__content[data-node-id="${sequenceId}.st:return"]`
	);
	await init.click();
	await returnStep.click({ modifiers: ['ControlOrMeta'] });
	await expect(page.locator('.studio-tree-node__row--multi')).toHaveCount(2);

	// a plain click selects one object again
	await returnStep.click();
	await expect(page.locator('.studio-tree-node__row--multi')).toHaveCount(0);
	await init.click();
	await returnStep.click({ modifiers: ['Shift'] });
	await expect(page.locator('.studio-tree-node__row--multi')).toHaveCount(2);

	page.once('dialog', async (dialog) => {
		expect(dialog.message()).toContain('Delete these 2 objects?');
		await dialog.accept();
	});
	await init.press('Delete');
	await expect(init).toHaveCount(0);
	await expect(returnStep).toHaveCount(0);
});

test('studio opens the menu of a tree object on a right click', async ({ page }) => {
	const state = createStudioState();
	await mockStudioServices(page, { state });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	const sequence = page.locator(`button.studio-tree-node__content[data-node-id="${sequenceId}"]`);
	const box = await sequence.boundingBox();
	await sequence.click({ button: 'right', position: { x: 24, y: 6 } });
	// the object is selected and its menu opens where it was clicked, as in the tree of Eclipse
	await expect(sequence.locator('xpath=..')).toHaveClass(/studio-tree-node__row--selected/);
	const copy = page.getByRole('menuitem', { name: 'Copy', exact: true });
	await expect(copy).toBeVisible();
	const menu = await copy.locator('xpath=ancestor::*[@data-part="content"]').boundingBox();
	expect(Math.abs((menu?.x ?? 0) - ((box?.x ?? 0) + 24))).toBeLessThan(16);
});

test('studio creates, renames and deletes the files of a project', async ({ page }) => {
	const state = createStudioState();
	await mockStudioServices(page, { state });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}/`);
	await selectTreeNode(page, `${projectName}/`);
	await page.getByRole('button', { name: 'Actions for Files' }).click();
	await page.getByRole('menuitem', { name: 'New file…' }).click();
	// the Studio asks the name in its own dialog, which the desktop Studio needs instead of window.prompt
	await answerStudioPrompt(page, 'notes.txt');
	const notes = page.locator(
		`button.studio-tree-node__content[data-node-id="${projectName}//notes.txt"]`
	);
	await expect(notes).toBeVisible();
	expect(state.files).toEqual(['notes.txt', 'readme.md']);

	// a file is not an object: F2 and Del rename and delete the file
	await notes.click();
	await notes.press('F2');
	await answerStudioPrompt(page, 'todo.txt');
	const todo = page.locator(
		`button.studio-tree-node__content[data-node-id="${projectName}//todo.txt"]`
	);
	await expect(todo).toBeVisible();
	page.once('dialog', async (dialog) => {
		expect(dialog.message()).toContain('Delete "todo.txt"?');
		await dialog.accept();
	});
	await todo.press('Delete');
	await expect(todo).toHaveCount(0);
	expect(state.files).toEqual(['readme.md']);
});

test('studio adds a palette step from the flow and opens inline rename', async ({ page }) => {
	const state = createStudioState();
	await mockStudioServices(page, { state });
	await page.goto('/studio/#flow');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('tab', { name: 'Flow', exact: true }).click();
	await ensureTreeNodeExpanded(page, sequenceId);
	await ensureTreeNodeExpanded(page, `${sequenceId}:st`);
	await page.getByRole('tab', { name: 'Palette' }).click();
	await expect(paletteItem(page, 'Simple step')).toBeVisible();
	await expect(flowNodeName(page, 'Init')).toBeVisible();

	await dragPaletteItemToFlowNode(page, 'Simple step', initStepId, { xRatio: 0.94 });

	const renameInput = page.getByRole('textbox', { name: 'Rename step' });
	await expect(renameInput).toBeFocused();
	await renameInput.fill('DroppedFromPalette');
	await renameInput.press('Enter');

	const droppedId = `${sequenceId}.st:DroppedFromPalette`;
	await expect(flowNodeName(page, 'DroppedFromPalette')).toBeVisible();
	await expect(
		page.locator(`button.studio-tree-node__content[data-node-id="${droppedId}"]`)
	).toBeVisible();
	await expect(page.locator('[role="treeitem"][aria-selected="true"]')).toContainText(
		'DroppedFromPalette'
	);
	await expect(page).toHaveURL(
		new RegExp(`/studio/${projectName}\\.sq~${sequenceName}\\.st~DroppedFromPalette/$`)
	);
});

test('studio keeps the favorite objects of the palette', async ({ page }) => {
	await mockStudioServices(page);
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('tab', { name: 'Palette' }).click();
	await expect(paletteItem(page, 'JSON field')).toBeVisible();
	await expect(page.getByRole('heading', { name: /Favorites/ })).toHaveCount(0);

	await paletteItem(page, 'JSON field').click();
	await page.getByRole('button', { name: 'Add to the favorites' }).click();
	await expect(page.getByRole('button', { name: 'Remove from the favorites' })).toBeVisible();
	await expect(paletteItem(page, 'JSON field')).toHaveCount(2);
	await expect(paletteItem(page, 'JSON field').first().getByLabel('Favorite')).toBeVisible();

	// the favorites stay in the browser
	await page.goto('/studio/');
	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('tab', { name: 'Palette' }).click();
	await expect(paletteItem(page, 'JSON field')).toHaveCount(2);
	await page.locator('#studio-palette-search').fill('json field');
	await expect(paletteItem(page, 'JSON field')).toHaveCount(1);

	// the built-in objects hide, as in the palette of Eclipse
	await page.locator('#studio-palette-search').fill('');
	await page.getByRole('button', { name: 'Built-in objects' }).click();
	await expect(paletteItem(page, 'JSON field')).toHaveCount(0);
	await expect(page.getByRole('button', { name: 'Built-in objects' })).toHaveAttribute(
		'aria-pressed',
		'false'
	);
	await page.getByRole('button', { name: 'Built-in objects' }).click();
	await expect(paletteItem(page, 'JSON field')).toHaveCount(2);
});

test('studio palette renders resolved descriptor icons instead of treating names as files', async ({
	page
}) => {
	await mockStudioServices(page);
	const iconPath = '/cache/flow-icons-v2/studio/iconify/mdi/equal.svg';
	const imageRequests = [];
	await page.route('**/admin/services/studio.palette.Get', async (route) => {
		await route.fulfill({
			json: {
				categories: [
					{
						name: 'Compare',
						items: [
							{
								id: 'compare.equal',
								name: 'Equal',
								icon: 'mdi:equal',
								iconify: 'mdi:equal',
								iconSvg: iconPath
							}
						]
					}
				]
			}
		});
	});
	await page.route('**/admin/services/studio.dbo.GetIcon?*', async (route) => {
		const path = new URL(route.request().url()).searchParams.get('iconPath');
		imageRequests.push(path);
		if (path !== iconPath) return route.fallback();
		await route.fulfill({
			contentType: 'image/svg+xml',
			body: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path d="M4 8h16v3H4zm0 5h16v3H4z"/></svg>'
		});
	});
	await page.goto('/studio/');
	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('tab', { name: 'Palette' }).click();
	await expect(paletteItem(page, 'Equal').locator('svg path')).toHaveAttribute(
		'd',
		'M4 8h16v3H4zm0 5h16v3H4z'
	);
	expect(imageRequests).toContain(iconPath);
	expect(imageRequests).not.toContain('mdi:equal');
});

test('studio retries a transient palette failure without changing focus', async ({ page }) => {
	const state = createStudioState();
	const paletteProbe = { remaining: 1, requests: 0 };
	const transitionWarnings = [];
	page.on('console', (message) => {
		if (message.text().includes('NaNpx')) {
			transitionWarnings.push(message.text());
		}
	});
	await mockStudioServices(page, { state, paletteProbe });
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('tab', { name: 'Palette' }).click();

	await expect(paletteItem(page, 'Simple step')).toBeVisible();
	await expect.poll(() => paletteProbe.requests).toBe(2);
	const category = page.getByRole('button', { name: /Steps/ });
	await category.click();
	await category.click();
	await expect.poll(() => transitionWarnings).toEqual([]);
});

test('studio moves a tree step and keeps flow and url synchronized', async ({ page }) => {
	const state = createStudioState();
	state.steps.splice(1, 0, {
		name: 'Second',
		classname: 'com.twinsoft.convertigo.beans.steps.SimpleStep'
	});
	const returnStepId = `${sequenceId}.st:return`;
	await mockStudioServices(page, { state });
	await page.goto('/studio/#flow');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('tab', { name: 'Flow', exact: true }).click();
	await ensureTreeNodeExpanded(page, sequenceId);
	await ensureTreeNodeExpanded(page, `${sequenceId}:st`);
	await expectTreeStepOrder(page, ['Init', 'Second', 'return']);

	await dragTreeNodeToTreeNode(page, initStepId, returnStepId, {
		yRatio: 0.9,
		beforeDrop: async () => {
			await expect(page.getByText('Move after return')).toBeVisible();
		}
	});

	expect(state.steps.map((step) => step.name)).toEqual(['Second', 'return', 'Init']);
	await expectTreeStepOrder(page, ['Second', 'return', 'Init']);
	await expect(flowNodeName(page, 'Init')).toBeVisible();
	await expect(page.locator('[role="treeitem"][aria-selected="true"]')).toContainText('Init');
	await expect(
		page.locator(`button.studio-tree-node__content[data-node-id="${initStepId}"]`)
	).toBeVisible();
	await expect(page).toHaveURL(
		new RegExp(`/studio/${projectName}\\.sq~${sequenceName}\\.st~Init/$`)
	);
});

test('studio reorders structured child steps without collapsing their parent', async ({ page }) => {
	const state = createStudioState({
		steps: [
			{
				name: 'object',
				classname: 'com.twinsoft.convertigo.beans.steps.JsonObjectStep',
				isSourceContainer: true,
				children: [
					{
						name: 'field1',
						classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
					},
					{
						name: 'field2',
						classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
					}
				]
			},
			{ name: 'return', classname: 'com.twinsoft.convertigo.beans.steps.ReturnStep' }
		]
	});
	const objectId = `${sequenceId}.st:object`;
	const field1Id = `${objectId}.st:field1`;
	const field2Id = `${objectId}.st:field2`;
	await mockStudioServices(page, { state });
	await page.goto('/studio/#flow');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('tab', { name: 'Flow', exact: true }).click();
	await ensureTreeNodeExpanded(page, sequenceId);
	await ensureTreeNodeExpanded(page, `${sequenceId}:st`);
	await ensureTreeNodeExpanded(page, objectId);
	await expectTreeChildOrder(page, objectId, ['field1', 'field2']);

	await dragTreeNodeToTreeNode(page, field2Id, field1Id, {
		yRatio: 0.1,
		beforeDrop: async () => {
			await expect(page.getByText('Move before field1')).toBeVisible();
		}
	});

	await expect
		.poll(() => state.steps[0].children?.map((step) => step.name))
		.toEqual(['field2', 'field1']);
	await expectTreeChildOrder(page, objectId, ['field2', 'field1']);
	await expectFlowChildOrder(page, objectId, ['field2', 'field1']);
	await expectTreeNodeExpanded(page, objectId);
	await expect(flowNodeName(page, 'field2')).toBeVisible();
	await expect(page.locator('[role="treeitem"][aria-selected="true"]')).toContainText('field2');
	await expect(page).toHaveURL(
		new RegExp(`/studio/${projectName}\\.sq~${sequenceName}\\.st~object\\.st~field2/$`)
	);
});

test('studio moves a structured flow child before a sibling and keeps tree and url synchronized', async ({
	page
}) => {
	const state = createStudioState({
		steps: [
			{
				name: 'object',
				classname: 'com.twinsoft.convertigo.beans.steps.JsonObjectStep',
				isSourceContainer: true,
				children: [
					{
						name: 'field1',
						classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
					},
					{
						name: 'field2',
						classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
					}
				]
			},
			{ name: 'return', classname: 'com.twinsoft.convertigo.beans.steps.ReturnStep' }
		]
	});
	const objectId = `${sequenceId}.st:object`;
	const field1Id = `${objectId}.st:field1`;
	const field2Id = `${objectId}.st:field2`;
	await mockStudioServices(page, { state });
	await page.goto('/studio/#flow');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('tab', { name: 'Flow', exact: true }).click();
	await ensureTreeNodeExpanded(page, sequenceId);
	await ensureTreeNodeExpanded(page, `${sequenceId}:st`);
	await ensureTreeNodeExpanded(page, objectId);
	await page.getByRole('button', { name: 'Expand substeps (2)' }).click();
	await expect(flowNodeName(page, 'field1')).toBeVisible();
	await expect(flowNodeName(page, 'field2')).toBeVisible();

	await selectFlowNode(page, field2Id);
	await dragFlowNodeToFlowNode(page, field2Id, field1Id, {
		xRatio: 0.08,
		beforeDrop: async () => {
			await expect(page.getByText('Before in object')).toBeVisible();
		}
	});

	await expect
		.poll(() => state.steps[0].children?.map((step) => step.name))
		.toEqual(['field2', 'field1']);
	await expectTreeChildOrder(page, objectId, ['field2', 'field1']);
	await expectTreeNodeExpanded(page, objectId);
	await expect(flowNodeName(page, 'field2')).toBeVisible();
	await expect(page.locator('[role="treeitem"][aria-selected="true"]')).toContainText('field2');
	await expect(page).toHaveURL(
		new RegExp(`/studio/${projectName}\\.sq~${sequenceName}\\.st~object\\.st~field2/$`)
	);
});

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} nodeId
 */
test('studio closes a project and opens it again', async ({ page }) => {
	const state = createStudioState({ closedProjects: ['Archive'] });
	/** @type {URLSearchParams[]} */
	const closeRequests = [];
	await mockStudioServices(page, {
		state,
		projects: [projectName, 'Archive'],
		closeRequests
	});
	await page.goto('/studio/');

	// a closed project shows without children, and only opens or is deleted
	const archiveRow = page.locator('.studio-tree-node__row', {
		has: page.locator('button.studio-tree-node__content[data-node-id="Archive"]')
	});
	await expect(archiveRow).toHaveClass(/studio-tree-node__row--closed/);
	await expect(
		page.locator('button.studio-tree-node__toggle-button[data-node-id="Archive"]')
	).toHaveCount(0);
	await selectTreeNode(page, 'Archive');
	await page.getByRole('button', { name: 'Actions for Archive' }).click();
	await expect(page.getByRole('menuitem', { name: 'Open' })).toBeVisible();
	await expect(page.getByRole('menuitem', { name: 'Delete project' })).toBeVisible();
	await expect(page.getByRole('menuitem', { name: 'Save' })).toHaveCount(0);
	await page.keyboard.press('Escape');

	// a double click opens it, as in the Eclipse Studio
	await page.locator('button.studio-tree-node__content[data-node-id="Archive"]').dblclick();
	await expect(archiveRow).not.toHaveClass(/studio-tree-node__row--closed/);
	expect(closeRequests.map((params) => [params.get('projects'), params.get('open')])).toEqual([
		['["Archive"]', 'true']
	]);

	await selectTreeNode(page, projectName);
	await page.getByRole('button', { name: `Actions for ${projectName}` }).click();
	await page.getByRole('menuitem', { name: 'Close' }).click();
	const projectRow = page.locator('.studio-tree-node__row', {
		has: page.locator(`button.studio-tree-node__content[data-node-id="${projectName}"]`)
	});
	await expect(projectRow).toHaveClass(/studio-tree-node__row--closed/);
	expect(closeRequests.at(-1)?.get('open')).toBe('false');
	expect(state.closedProjects).toEqual([projectName]);
});

test('studio copies objects to the system clipboard and pastes the ones of another Studio', async ({
	page
}) => {
	const state = createStudioState();
	/** @type {URLSearchParams[]} */
	const pasteRequests = [];
	await mockStudioServices(page, { state, pasteRequests });
	// an in-memory clipboard, which leaves the clipboard of the system as it is
	await page.addInitScript(() => {
		const clipboard = {
			text: '',
			async readText() {
				return clipboard.text;
			},
			async writeText(/** @type {string} */ text) {
				clipboard.text = text;
			},
			async write(/** @type {any[]} */ items) {
				const blob = await items[0].getType('text/plain');
				clipboard.text = await blob.text();
			}
		};
		Object.defineProperty(navigator, 'clipboard', { value: clipboard, configurable: true });
		/** @type {any} */ (window).testClipboard = clipboard;
	});
	await page.goto('/studio/');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('button', { name: `Actions for ${sequenceName}` }).click();
	await page.getByRole('menuitem', { name: 'Copy', exact: true }).click();
	await expect
		.poll(() => page.evaluate(() => /** @type {any} */ (window).testClipboard.text))
		.toContain('<convertigo clipboard="copy">');

	// the objects another Studio copied, pasted with Ctrl or ⌘ V from the paste event
	const eclipseCopy =
		'<?xml version="1.0" encoding="ISO-8859-1"?>\n<convertigo-clipboard><sequence name="Copied"/></convertigo-clipboard>';
	await page.evaluate((text) => {
		const target = /** @type {HTMLElement} */ (document.activeElement);
		const mod = /Mac/.test(navigator.platform) ? { metaKey: true } : { ctrlKey: true };
		target.dispatchEvent(new KeyboardEvent('keydown', { key: 'v', bubbles: true, ...mod }));
		const clipboardData = new DataTransfer();
		clipboardData.setData('text/plain', text);
		target.dispatchEvent(new ClipboardEvent('paste', { clipboardData, bubbles: true }));
	}, eclipseCopy);
	await expect.poll(() => pasteRequests.length).toBe(1);
	expect(pasteRequests[0].get('xml')).toBe(eclipseCopy);
	expect(pasteRequests[0].get('target')).toBe(sequenceId);

	// the menu reads the system clipboard, and other text leaves the copy of this Studio
	await page.evaluate(() => {
		/** @type {any} */ (window).testClipboard.text = 'some text';
	});
	await page.getByRole('button', { name: `Actions for ${sequenceName}` }).click();
	await page.getByRole('menuitem', { name: 'Paste' }).click();
	await expect.poll(() => pasteRequests.length).toBe(2);
	expect(pasteRequests[1].get('xml')).toContain('<convertigo clipboard="copy">');
});

async function expandTreeNode(page, nodeId) {
	const toggle = page.locator(`button.studio-tree-node__toggle-button[data-node-id="${nodeId}"]`);
	await expect(toggle).toBeVisible();
	await toggle.click();
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} nodeId
 */
async function ensureTreeNodeExpanded(page, nodeId) {
	const toggle = page.locator(`button.studio-tree-node__toggle-button[data-node-id="${nodeId}"]`);
	await expect(toggle).toBeVisible();
	if (
		!(await toggle.evaluate((node) =>
			node.classList.contains('studio-tree-node__toggle-button--open')
		))
	) {
		await toggle.click();
	}
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string | RegExp} name
 */
async function ensureButtonExpanded(page, name) {
	const button = page.getByRole('button', { name }).first();
	await expect(button).toBeVisible();
	if ((await button.getAttribute('aria-expanded')) !== 'true') {
		await button.click();
	}
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} nodeId
 */
async function expectTreeNodeExpanded(page, nodeId) {
	const toggle = page.locator(`button.studio-tree-node__toggle-button[data-node-id="${nodeId}"]`);
	await expect(toggle).toBeVisible();
	await expect(toggle).toHaveClass(/studio-tree-node__toggle-button--open/);
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} nodeId
 */
async function selectTreeNode(page, nodeId) {
	const content = page.locator(`button.studio-tree-node__content[data-node-id="${nodeId}"]`);
	await expect(content).toBeVisible();
	await content.click();
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} name
 */
function flowNodeName(page, name) {
	return page.locator('.flow-step-node__name').filter({ hasText: new RegExp(`^${name}$`) });
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} name
 */
function paletteItem(page, name) {
	return page.locator('.studio-palette__item').filter({ hasText: name });
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} nodeId
 */
async function selectFlowNode(page, nodeId) {
	await page.evaluate((id) => {
		const node = Array.from(document.querySelectorAll('.svelte-flow__node')).find(
			(item) => item.getAttribute('data-id') === id
		);
		if (!(node instanceof HTMLElement)) {
			throw new Error(`Missing flow node "${id}"`);
		}
		node.dispatchEvent(
			new PointerEvent('pointerdown', {
				bubbles: true,
				cancelable: true,
				composed: true
			})
		);
		node.dispatchEvent(
			new MouseEvent('click', {
				bubbles: true,
				cancelable: true,
				composed: true
			})
		);
	}, nodeId);
	await expect(page.locator(`.svelte-flow__node[data-id="${nodeId}"]`)).toHaveClass(/selected/);
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} itemName
 * @param {string} targetNodeId
 * @param {{ xRatio?: number, yRatio?: number }} [options]
 */
async function dragPaletteItemToFlowNode(page, itemName, targetNodeId, options = {}) {
	await page.evaluate(
		({ itemName, targetNodeId, xRatio, yRatio }) => {
			const source = Array.from(document.querySelectorAll('.studio-palette__item')).find((node) =>
				node.textContent?.includes(itemName)
			);
			const target = Array.from(document.querySelectorAll('.svelte-flow__node')).find(
				(node) => node.getAttribute('data-id') === targetNodeId
			);
			if (!(source instanceof HTMLElement) || !(target instanceof HTMLElement)) {
				throw new Error(`Missing DnD source "${itemName}" or target "${targetNodeId}"`);
			}
			const rect = target.getBoundingClientRect();
			const dataTransfer = new DataTransfer();
			const classname = source.getAttribute('title') ?? '';
			const payload = {
				type: 'paletteData',
				data: {
					type: 'Dbo',
					id: classname,
					name: itemName,
					classname
				},
				options: {}
			};
			dataTransfer.setData('text/plain', JSON.stringify(payload));
			dataTransfer.setData('palettedata', JSON.stringify(payload));
			dataTransfer.effectAllowed = 'copy';
			dataTransfer.dropEffect = 'copy';
			const eventOptions = {
				bubbles: true,
				cancelable: true,
				dataTransfer,
				clientX: rect.left + rect.width * xRatio,
				clientY: rect.top + rect.height * yRatio
			};
			source.dispatchEvent(new DragEvent('dragstart', eventOptions));
			target.dispatchEvent(new DragEvent('dragenter', eventOptions));
			target.dispatchEvent(new DragEvent('dragover', eventOptions));
			target.dispatchEvent(new DragEvent('drop', eventOptions));
			source.dispatchEvent(new DragEvent('dragend', eventOptions));
		},
		{
			itemName,
			targetNodeId,
			xRatio: options.xRatio ?? 0.5,
			yRatio: options.yRatio ?? 0.5
		}
	);
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} itemName
 * @param {string} targetNodeId
 * @param {number} yRatio
 */
async function dragPaletteItemToTreeNode(page, itemName, targetNodeId, yRatio) {
	await page.evaluate(
		({ itemName, targetNodeId, yRatio }) => {
			const source = Array.from(document.querySelectorAll('.studio-palette__item')).find((node) =>
				node.textContent?.includes(itemName)
			);
			const content = Array.from(
				document.querySelectorAll('button.studio-tree-node__content')
			).find((node) => node.getAttribute('data-node-id') === targetNodeId);
			const target = content?.closest('.studio-tree-node__row');
			if (!(source instanceof HTMLElement) || !(target instanceof HTMLElement)) {
				throw new Error(`Missing palette source "${itemName}" or tree target "${targetNodeId}"`);
			}
			const rect = target.getBoundingClientRect();
			const dataTransfer = new DataTransfer();
			const eventOptions = {
				bubbles: true,
				cancelable: true,
				dataTransfer,
				clientX: rect.left + rect.width / 2,
				clientY: rect.top + rect.height * yRatio
			};
			source.dispatchEvent(new DragEvent('dragstart', eventOptions));
			target.dispatchEvent(new DragEvent('dragenter', eventOptions));
			target.dispatchEvent(new DragEvent('dragover', eventOptions));
			target.dispatchEvent(new DragEvent('drop', eventOptions));
			source.dispatchEvent(new DragEvent('dragend', eventOptions));
		},
		{ itemName, targetNodeId, yRatio }
	);
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} sourceNodeId
 * @param {string} targetNodeId
 * @param {{ xRatio?: number, yRatio?: number, beforeDrop?: () => Promise<void> }} [options]
 */
async function dragFlowNodeToFlowNode(page, sourceNodeId, targetNodeId, options = {}) {
	await page.evaluate(
		({ sourceNodeId, targetNodeId, xRatio, yRatio }) => {
			const sourceNode = Array.from(document.querySelectorAll('.svelte-flow__node')).find(
				(node) => node.getAttribute('data-id') === sourceNodeId
			);
			const targetNode = Array.from(document.querySelectorAll('.svelte-flow__node')).find(
				(node) => node.getAttribute('data-id') === targetNodeId
			);
			const source = sourceNode?.querySelector('.flow-step-node__drag-handle') ?? sourceNode;
			if (
				!(sourceNode instanceof HTMLElement) ||
				!(targetNode instanceof HTMLElement) ||
				!(source instanceof HTMLElement)
			) {
				throw new Error(`Missing flow DnD source "${sourceNodeId}" or target "${targetNodeId}"`);
			}
			const sourceRect = source.getBoundingClientRect();
			const targetRect = targetNode.getBoundingClientRect();
			const dataTransfer = new DataTransfer();
			const classname = sourceNode.querySelector('.flow-step-node')?.getAttribute('title') ?? '';
			const payload = {
				type: 'treeData',
				data: {
					id: sourceNodeId,
					classname
				},
				options: {}
			};
			dataTransfer.setData('text/plain', JSON.stringify(payload));
			dataTransfer.setData('treedata', JSON.stringify(payload));
			dataTransfer.effectAllowed = 'move';
			dataTransfer.dropEffect = 'move';
			const eventOptions = {
				bubbles: true,
				cancelable: true,
				dataTransfer,
				clientX: targetRect.left + targetRect.width * xRatio,
				clientY: targetRect.top + targetRect.height * yRatio
			};
			source.dispatchEvent(
				new DragEvent('dragstart', {
					...eventOptions,
					clientX: sourceRect.left + sourceRect.width / 2,
					clientY: sourceRect.top + sourceRect.height / 2
				})
			);
			targetNode.dispatchEvent(new DragEvent('dragenter', eventOptions));
			targetNode.dispatchEvent(new DragEvent('dragover', eventOptions));
			const studioWindow = /** @type {StudioFlowTestWindow} */ (window);
			studioWindow.__studioFlowDragContext = {
				sourceNodeId,
				targetNodeId,
				xRatio,
				yRatio,
				dataTransfer
			};
		},
		{
			sourceNodeId,
			targetNodeId,
			xRatio: options.xRatio ?? 0.5,
			yRatio: options.yRatio ?? 0.5
		}
	);
	await options.beforeDrop?.();
	await page.evaluate(() => {
		const studioWindow = /** @type {StudioFlowTestWindow} */ (window);
		const context = studioWindow.__studioFlowDragContext;
		if (!context) {
			throw new Error('Missing flow DnD context');
		}
		const sourceNode = Array.from(document.querySelectorAll('.svelte-flow__node')).find(
			(node) => node.getAttribute('data-id') === context.sourceNodeId
		);
		const targetNode = Array.from(document.querySelectorAll('.svelte-flow__node')).find(
			(node) => node.getAttribute('data-id') === context.targetNodeId
		);
		const source = sourceNode?.querySelector('.flow-step-node__drag-handle') ?? sourceNode;
		if (
			!(sourceNode instanceof HTMLElement) ||
			!(targetNode instanceof HTMLElement) ||
			!(source instanceof HTMLElement)
		) {
			throw new Error(
				`Missing flow DnD source "${context.sourceNodeId}" or target "${context.targetNodeId}"`
			);
		}
		const targetRect = targetNode.getBoundingClientRect();
		const eventOptions = {
			bubbles: true,
			cancelable: true,
			dataTransfer: context.dataTransfer,
			clientX: targetRect.left + targetRect.width * context.xRatio,
			clientY: targetRect.top + targetRect.height * context.yRatio
		};
		targetNode.dispatchEvent(new DragEvent('drop', eventOptions));
		source.dispatchEvent(new DragEvent('dragend', eventOptions));
		delete studioWindow.__studioFlowDragContext;
	});
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} sourceNodeId
 * @param {string} targetNodeId
 * @param {{ yRatio?: number, beforeDrop?: () => Promise<void> }} [options]
 */
async function dragTreeNodeToTreeNode(page, sourceNodeId, targetNodeId, options = {}) {
	// a row is taken by its label, as a user does, away from its action button
	const sourceBox = await treeLabelBox(page, sourceNodeId);
	const targetBox = await treeRowBox(page, targetNodeId);
	if (!sourceBox || !targetBox) {
		throw new Error(`Missing tree DnD source "${sourceNodeId}" or target "${targetNodeId}"`);
	}
	await page.mouse.move(sourceBox.x + sourceBox.width / 2, sourceBox.y + sourceBox.height / 2);
	await page.mouse.down();
	await page.mouse.move(
		targetBox.x + targetBox.width / 2,
		targetBox.y + targetBox.height * (options.yRatio ?? 0.5),
		{ steps: 12 }
	);
	await options.beforeDrop?.();
	await page.mouse.up();
}

/**
 * Answers the question of the Studio prompt dialog.
 * @param {import('@playwright/test').Page} page
 * @param {string} value
 */
async function answerStudioPrompt(page, value) {
	const dialog = page.locator('[aria-labelledby="studio-prompt-title"]');
	await expect(dialog).toBeVisible();
	await dialog.getByRole('textbox').fill(value);
	await dialog.getByRole('button', { name: 'OK', exact: true }).click();
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} nodeId
 */
async function treeLabelBox(page, nodeId) {
	const box = await page
		.locator(`button.studio-tree-node__content[data-node-id="${nodeId}"]`)
		.boundingBox();
	return box ? { x: box.x, y: box.y, width: box.width, height: box.height } : null;
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} nodeId
 */
async function treeRowBox(page, nodeId) {
	return page.evaluate((id) => {
		const content = Array.from(document.querySelectorAll('button.studio-tree-node__content')).find(
			(node) => node.getAttribute('data-node-id') === id
		);
		const row = content?.closest('.studio-tree-node__row');
		if (!(row instanceof HTMLElement)) {
			return null;
		}
		const rect = row.getBoundingClientRect();
		return { x: rect.left, y: rect.top, width: rect.width, height: rect.height };
	}, nodeId);
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string[]} names
 */
async function expectTreeStepOrder(page, names) {
	await expect
		.poll(async () =>
			page.evaluate((currentSequenceId) => {
				return Array.from(document.querySelectorAll('button.studio-tree-node__content'))
					.map((node) => node.getAttribute('data-node-id') ?? '')
					.filter((id) => id.startsWith(`${currentSequenceId}.st:`))
					.map((id) => id.split('.st:').at(-1) ?? id);
			}, sequenceId)
		)
		.toEqual(names);
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string[]} ids
 */
async function expectFrontendOrder(page, ids) {
	await expect
		.poll(() =>
			page.evaluate((parentId) => {
				const prefix = `${parentId}.`;
				return Array.from(document.querySelectorAll('button.studio-tree-node__content'))
					.map((node) => node.getAttribute('data-node-id') ?? '')
					.filter((id) => id.startsWith(prefix) && !id.slice(prefix.length).includes('.'))
					.map((id) => id.slice(prefix.length));
			}, frontendStructureId)
		)
		.toEqual(ids);
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} parentId
 * @param {string[]} names
 */
async function expectTreeChildOrder(page, parentId, names) {
	await expect
		.poll(async () =>
			page.evaluate((currentParentId) => {
				const prefix = `${currentParentId}.st:`;
				return Array.from(document.querySelectorAll('button.studio-tree-node__content'))
					.map((node) => node.getAttribute('data-node-id') ?? '')
					.filter((id) => id.startsWith(prefix) && !id.slice(prefix.length).includes('.st:'))
					.map((id) => id.slice(prefix.length));
			}, parentId)
		)
		.toEqual(names);
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} parentId
 * @param {string[]} names
 */
async function expectFlowChildOrder(page, parentId, names) {
	await expect
		.poll(async () =>
			page.evaluate((currentParentId) => {
				/**
				 * @param {string} id
				 */
				function localName(id) {
					return id.split('.').at(-1)?.replace(/^st:/, '') ?? id;
				}

				/**
				 * @param {string} id
				 */
				function isDirectChildId(id) {
					if (!id.startsWith(`${currentParentId}.`)) {
						return false;
					}
					const childPath = id.slice(currentParentId.length + 1);
					if (!childPath) {
						return false;
					}
					if (childPath.startsWith('st:')) {
						return !childPath.slice(3).includes('.st:');
					}
					return !childPath.includes('.');
				}

				return Array.from(document.querySelectorAll('.svelte-flow__node'))
					.map((node) => {
						const id = node.getAttribute('data-id') ?? '';
						const rect = node.getBoundingClientRect();
						return { id, x: rect.x, y: rect.y };
					})
					.filter(({ id }) => isDirectChildId(id))
					.sort((left, right) => left.y - right.y || left.x - right.x)
					.map(({ id }) => localName(id));
			}, parentId)
		)
		.toEqual(names);
}

/**
 * @param {import('@playwright/test').Page} page
 */
function responseEditor(page) {
	return page.locator('.monaco-editor').filter({ hasText: 'studioSmoke' });
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {{
 *  roles?: string[],
 *  state?: ReturnType<typeof createStudioState>,
 *  executionRequests?: string[],
 *  contextActions?: string[],
 *  propertyUpdates?: URLSearchParams[],
 *  flowPickerRequests?: URLSearchParams[],
 *  flowPicker?: boolean,
 *  qnameProperty?: boolean,
 *  fontProperty?: boolean,
 *  flagsProperty?: boolean,
 *  namedSourceProperty?: boolean,
 *  lifetimeProperty?: boolean,
 *  referenceProperty?: boolean,
 *  flowPickerProbe?: { remaining: number, requests: number },
 *  assistant?: boolean,
 *  paletteProbe?: { remaining: number, requests: number },
 *  frontendRefreshDelayMs?: number,
 *  noProjects?: boolean,
 *  projects?: string[],
 *  closeRequests?: URLSearchParams[],
 *  pasteRequests?: URLSearchParams[],
 *  authoringTargets?: Record<string, string>,
 *  authoringReferences?: Record<string, string>,
 *  addRequests?: URLSearchParams[],
 *  removeRequests?: URLSearchParams[],
 *  adminEvents?: Array<{id: string, topic: string, timestamp: number, instance: string, payload: Record<string, unknown>}>
 * }} [options]
 */
async function mockStudioServices(page, options = {}) {
	await page.route('**/admin/services/**', async (route) => {
		const request = route.request();
		const service = serviceName(request.url());
		if (service === 'events.Subscribe') {
			await route.fulfill({
				status: 200,
				headers: {
					'cache-control': 'no-cache',
					'content-type': 'text/event-stream;charset=UTF-8',
					'x-xsrf-token': 'studio-test-token'
				},
				body: [
					{
						id: 'studio-test-ready',
						topic: 'admin.ready',
						timestamp: 1,
						instance: 'studio-test',
						payload: { topics: ['projects.changed', 'admin.resync.required'] }
					},
					...(options.adminEvents ?? [])
				]
					.map((event) => `id: ${event.id}\ndata: ${JSON.stringify(event)}\n\n`)
					.join('')
			});
			return;
		}
		const params = new URLSearchParams(request.postData() ?? '');
		if (service === 'studio.palette.Get' && options.paletteProbe) {
			options.paletteProbe.requests += 1;
			if (options.paletteProbe.remaining > 0) {
				options.paletteProbe.remaining -= 1;
				await route.fulfill({
					status: 503,
					headers: {
						'content-type': 'application/json',
						'x-xsrf-token': 'studio-test-token'
					},
					body: JSON.stringify({ isError: true, error: 'Temporary palette failure' })
				});
				return;
			}
		}
		if (service === 'studio.flowpicker.Get' && options.flowPickerProbe) {
			options.flowPickerProbe.requests += 1;
			if (options.flowPickerProbe.remaining > 0) {
				options.flowPickerProbe.remaining -= 1;
				await route.fulfill({
					status: 503,
					headers: {
						'content-type': 'application/json',
						'x-xsrf-token': 'studio-test-token'
					},
					body: JSON.stringify({ isError: true, error: 'Temporary Flow picker failure' })
				});
				return;
			}
		}
		const body = responseForService(service, params, options);
		if (
			service === 'studio.treeview.Get' &&
			options.frontendRefreshDelayMs &&
			params.get('id') === frontendStructureId &&
			options.state?.frontendNodes.some((node) => node.id === 'text1')
		) {
			await new Promise((resolve) => setTimeout(resolve, options.frontendRefreshDelayMs));
		}

		await route.fulfill({
			status: 200,
			headers: {
				'content-type': 'application/json',
				'x-xsrf-token': 'studio-test-token'
			},
			body: JSON.stringify(body)
		});
	});

	await page.route(`**/projects/${projectName}/.json`, async (route) => {
		options.executionRequests?.push(route.request().postData() ?? '');
		await route.fulfill({
			status: 200,
			headers: {
				'content-type': 'application/json',
				'x-xsrf-token': 'studio-test-token'
			},
			body: JSON.stringify({ studioSmoke: 'executed' })
		});
	});

	await page.route(`**/projects/${projectName}/DisplayObjects/mobile/index.html`, async (route) => {
		await route.fulfill({
			status: 200,
			contentType: 'text/html',
			body: '<!doctype html><html><body><main>Prod preview</main></body></html>'
		});
	});

	if (options.assistant) {
		await page.route(
			'**/projects/lib_ConvertigoAssistant/DisplayObjects/mobile/**',
			async (route) => {
				await route.fulfill({
					status: 200,
					contentType: 'text/html',
					body: `<!doctype html>
					<html><body>
						<main data-testid="assistant-context">Waiting for Studio context</main>
						<script>
							const output = document.querySelector('[data-testid="assistant-context"]');
							const queryProject = new URLSearchParams(location.search).get('targetProject') || '';
							output.dataset.queryProject = queryProject || 'No project';
							let studioContext = { projectContext: queryProject };
							window.addEventListener('message', (event) => {
								const message = event.data || {};
								if (message.type === 'lib_ConvertigoAssistant.context') {
									const context = message.payload || {};
									studioContext = context;
									output.dataset.assistantRuntime = context.assistantRuntime || '';
									output.dataset.agentBridgeAvailable = String(context.agentBridgeAvailable ?? '');
									output.dataset.agentProfile = context.agentProfile || '';
									output.textContent = (context.projectContext || 'No project') + ' · ' + context.assistantSurface;
								}
								if (message.type === 'select') {
									studioContext = message;
									output.textContent = (message.projectName || 'No project') + ' · ' + message.assistantSurface;
								}
								if (message.type === 'init') {
									output.dataset.initialProject = studioContext.projectContext || 'No project';
									output.dataset.serverAgent = new URLSearchParams(location.search).get('serverAgent') || '';
								}
							});
							window.parent.postMessage({ type: 'lib_ConvertigoAssistant.context.request' }, location.origin);
						</script>
					</body></html>`
				});
			}
		);
	}

	await page.route('**/convertigo/gw/studio-test-ticket/**', async (route) => {
		await route.fulfill({
			status: 200,
			contentType: 'text/html',
			body: '<!doctype html><html><body><main>Dev preview</main></body></html>'
		});
	});
}

/**
 * @param {string} url
 */
function serviceName(url) {
	const { pathname } = new URL(url);
	return pathname.split('/admin/services/').at(-1) ?? '';
}

/**
 * @param {string} service
 * @param {URLSearchParams} params
 * @param {{
 *  roles?: string[],
 *  state?: ReturnType<typeof createStudioState>,
 *  executionRequests?: string[],
 *  contextActions?: string[],
 *  propertyUpdates?: URLSearchParams[],
 *  flowPickerRequests?: URLSearchParams[],
 *  flowPicker?: boolean,
 *  qnameProperty?: boolean,
 *  fontProperty?: boolean,
 *  flagsProperty?: boolean,
 *  namedSourceProperty?: boolean,
 *  lifetimeProperty?: boolean,
 *  referenceProperty?: boolean,
 *  flowPickerProbe?: { remaining: number, requests: number },
 *  assistant?: boolean,
 *  paletteProbe?: { remaining: number, requests: number },
 *  frontendRefreshDelayMs?: number,
 *  noProjects?: boolean,
 *  projects?: string[],
 *  closeRequests?: URLSearchParams[],
 *  pasteRequests?: URLSearchParams[],
 *  authoringTargets?: Record<string, string>,
 *  authoringReferences?: Record<string, string>,
 *  addRequests?: URLSearchParams[],
 *  removeRequests?: URLSearchParams[]
 * }} [options]
 */
function responseForService(service, params, options = {}) {
	const state = options.state ?? createStudioState();
	switch (service) {
		case 'engine.CheckAuthentication':
			return {
				admin: {
					authenticated: true,
					user: 'admin',
					roles: { role: (options.roles ?? ['WEB_ADMIN']).map((name) => ({ name })) },
					ts: Date.now(),
					tz: 'Europe/Paris'
				}
			};
		case 'projects.List':
			return {
				admin: {
					projects: {
						project: options.noProjects
							? []
							: (options.projects ?? [projectName])
									.filter((name) => !state.closedProjects?.includes(name))
									.map((name) => ({
										name,
										comment: '',
										ref: ['lib_flow_engine']
									}))
					}
				}
			};
		case 'projects.GetTestPlatform':
			return testPlatformResponse(state);
		case 'studio.treeview.Get':
			if (state.closedProjects && !params.has('id') && !params.has('ids')) {
				// the projects of the workspace, closed or not
				const closed = state.closedProjects;
				return {
					children: (options.projects ?? [projectName]).map((name) => ({
						id: name,
						children: !closed.includes(name),
						...(closed.includes(name) ? { closed: true } : {})
					}))
				};
			}
			return options.noProjects ? { children: [] } : treeviewResponse(params, state);
		case 'studio.project.Close': {
			options.closeRequests?.push(params);
			const names = JSON.parse(params.get('projects') ?? '[]');
			const open = params.get('open') === 'true';
			const closed = state.closedProjects ?? [];
			state.closedProjects = open
				? closed.filter((name) => !names.includes(name))
				: [...new Set([...closed, ...names])];
			return { done: true, projects: names };
		}
		case 'studio.dbo.Copy':
			return {
				done: true,
				xml: '<?xml version="1.0" encoding="UTF-8"?>\n<convertigo clipboard="copy"><sequence/></convertigo>',
				text: '<?xml version="1.0" encoding="UTF-8"?>\n<convertigo clipboard="copy"><sequence/></convertigo>'
			};
		case 'studio.dbo.Paste':
			options.pasteRequests?.push(params);
			return { done: false, ids: [], error: 'Pasted in the test' };
		case 'studio.source.Files':
			return filesResponse(params, state);
		case 'studio.treeview.Authoring': {
			// the address of a text of the page follows its place, its id does not
			const selected = params.get('id') ?? '';
			if (selected.startsWith(`${frontendStructureId}.`)) {
				const nodeId = selected.slice(frontendStructureId.length + 1);
				const index = state.frontendNodes.findIndex((node) => node.id === nodeId);
				return index < 0
					? {}
					: {
							id: selected,
							reference: {
								nodeId,
								sourceRelativePath: 'model/StudioProject/src/routes/+page.flow.svelte',
								sourceMutationPath: `frontAst.nodes[${index}]`
							}
						};
			}
			const project = params.get('project') ?? '';
			const sourcePath = params.get('sourcePath') ?? '';
			const reference = params.has('reference') ? parseServiceJson(params.get('reference')) : null;
			const id = reference
				? options.authoringReferences?.[reference.nodeId]
				: options.authoringTargets?.[`${project}:${sourcePath}`];
			return id ? { id } : {};
		}
		case 'studio.treeview.ContextMenu':
			return contextMenuResponse(params, state);
		case 'studio.treeview.ContextAction':
			return contextActionResponse(params, state, options.contextActions);
		case 'studio.dbo.Accept':
			return acceptDboResponse(params);
		case 'studio.dbo.Add':
			options.addRequests?.push(params);
			return addDboResponse(params, state);
		case 'studio.dbo.Move':
			return moveDboResponse(params, state);
		case 'studio.dbo.Rename':
			return renameDboResponse(params, state);
		case 'studio.dbo.Remove':
			options.removeRequests?.push(params);
			return removeDboResponse(params, state);
		case 'studio.palette.Get':
			return paletteResponse(params);
		case 'studio.properties.Get':
			if (options.flowPicker && params.get('id') === frontendBuilderId) {
				return flowPropertiesResponse();
			}
			return {
				properties: {
					comment: {
						displayName: 'Comment',
						value: '',
						originalValue: '',
						type: 'java.lang.String',
						isMultiline: true
					},
					...(options.referenceProperty
						? {
								'Project name and remote URL': {
									name: 'projectName',
									displayName: 'Project name and remote URL',
									category: 'Base properties',
									class: 'java.lang.String',
									kind: 'dbo',
									projectReference: true,
									value: 'lib_OAuth'
								}
							}
						: {}),
					...(options.lifetimeProperty
						? {
								'Response lifetime': {
									name: 'responseExpiryDate',
									displayName: 'Response lifetime',
									category: 'Base properties',
									class: 'java.lang.String',
									kind: 'dbo',
									value: 'absolute,3600'
								}
							}
						: {}),
					...(options.namedSourceProperty
						? {
								'Target requestable': {
									name: 'targetRequestable',
									displayName: 'Target requestable',
									category: 'Base properties',
									class: 'java.lang.String',
									kind: 'dbo',
									namedSource: true,
									value: 'StudioProject.TestSequence'
								}
							}
						: {}),
					...(options.flagsProperty
						? {
								Visibility: {
									name: 'visibility',
									displayName: 'Visibility',
									category: 'Base properties',
									class: 'java.lang.Integer',
									kind: 'dbo',
									value: '1',
									flags: [
										{ label: 'Mask in the log files', mask: 1 },
										{ label: 'Mask in the Studio', mask: 2 }
									]
								}
							}
						: {}),
					...(options.fontProperty
						? {
								Definition: {
									name: 'fontSource',
									displayName: 'Definition',
									category: 'Base properties',
									class: 'xmlizable',
									kind: 'dbo',
									font: true,
									value: '{}'
								}
							}
						: {}),
					...(options.qnameProperty
						? {
								'Schema type': {
									name: 'xmlComplexTypeAffectation',
									displayName: 'Schema type',
									category: 'Base properties',
									class: 'xmlizable',
									kind: 'dbo',
									qname: 'complexType',
									value: ''
								}
							}
						: {})
				}
			};
		case 'studio.properties.NamedSources':
			return {
				items: [
					{
						name: 'StudioProject.TestSequence',
						label: 'TestSequence',
						type: 'GenericSequence',
						project: 'StudioProject'
					},
					{
						name: 'StudioProject.Books.getBook',
						label: 'getBook',
						type: 'JsonHttpTransaction',
						project: 'StudioProject'
					},
					{ name: 'lib_OAuth.Login', label: 'Login', type: 'GenericSequence', project: 'lib_OAuth' }
				]
			};
		case 'studio.ngxbuilder.Fonts':
			if (params.get('font')) {
				return { font: { id: params.get('font'), variants: {}, unicodeRange: {} } };
			}
			return {
				fonts: [
					{
						id: 'abel',
						family: 'Abel',
						category: 'sans-serif',
						weights: [400],
						styles: ['normal'],
						subsets: ['latin'],
						defSubset: 'latin'
					},
					{
						id: 'ibm-plex-sans',
						family: 'IBM Plex Sans',
						category: 'sans-serif',
						weights: [100, 400, 700],
						styles: ['italic', 'normal'],
						subsets: ['cyrillic', 'latin'],
						defSubset: 'latin'
					}
				]
			};
		case 'studio.properties.QNames':
			return {
				kind: 'complexType',
				namespace: 'http://studio/project',
				items: [
					{
						qname: '{http://studio/project}personType',
						namespace: 'http://studio/project',
						name: 'personType',
						kind: 'complexType',
						dynamic: true,
						readOnly: false
					},
					{
						qname: '{http://studio/project}ConvertigoError',
						namespace: 'http://studio/project',
						name: 'ConvertigoError',
						kind: 'complexType',
						dynamic: false,
						readOnly: false
					}
				]
			};
		case 'studio.flowpicker.Get':
			options.flowPickerRequests?.push(new URLSearchParams(params));
			return flowPickerResponse();
		case 'studio.properties.Set':
			options.propertyUpdates?.push(new URLSearchParams(params));
			return { done: true, id: params.get('id'), state: 'success' };
		default:
			return {};
	}
}

function flowPropertiesResponse() {
	return {
		id: frontendBuilderId,
		properties: {
			Summary: {
				displayName: 'Summary',
				category: 'Information',
				value: 'Svelte builder'
			},
			text: {
				name: 'text',
				displayName: 'Text',
				category: 'Base properties',
				shortDescription: 'Visible text rendered by the component.',
				editorClass: 'flow-binding-editor',
				flowKind: 'binding',
				flowType: 'binding',
				value: JSON.stringify({ mode: 'literal', value: 'Fresh Flow chart benchmark' })
			},
			classes: {
				name: 'classes',
				displayName: 'Classes',
				category: 'Base properties',
				shortDescription: 'Application CSS class names.',
				editorClass: 'flow-binding-editor',
				flowKind: 'binding',
				flowType: 'string',
				value: ''
			}
		}
	};
}

function flowPickerResponse() {
	const initial = { mode: 'literal', value: 'Fresh Flow chart benchmark' };
	return {
		html: `<!doctype html><html><body><div id="app"></div><script>
		window.flowSetTheme = function (theme) {
			document.documentElement.dataset.flowTheme = theme;
			document.documentElement.style.colorScheme = theme;
		};
		customElements.define('flow-binding-editor', class extends HTMLElement {
			connectedCallback() {
				this.attachShadow({ mode: 'open' }).innerHTML = '<button type="button">Literal</button><button type="button">Source</button><label>Source<select aria-label="Source"><option value="local.points">Local points</option></select></label>';
				this.shadowRoot.querySelector('button:nth-child(2)').onclick = () => { this.mode = 'source'; };
			}
			setState(state) { this.state = state; }
			get value() { return JSON.stringify({ mode: 'source', source: { category: 'local', name: 'points' }, path: [{ kind: 'property', name: 'value' }] }); }
		});
		window.receiveFromJava = function (state) {
			window.flowSetTheme(state.theme || 'dark');
			const app = document.getElementById('app');
			app.innerHTML = '<flow-binding-editor></flow-binding-editor><button type="button" id="apply">Apply</button><output></output>';
			const editor = app.querySelector('flow-binding-editor'); editor.setState(state);
			app.querySelector('#apply').onclick = () => { window.flowEditor.receive(JSON.stringify({ type: 'value', value: editor.value, valid: true })); app.querySelector('output').textContent = 'Applied text'; };
		};
		</script></body></html>`,
		state: {
			mode: 'picker',
			property: 'text',
			virtualPath: 'frontends.svelte.routes.home.structure.heading',
			summary: 'Heading',
			definition: { text: initial },
			info: {
				propertyDefinitions: {
					text: {
						label: 'Text',
						kind: 'binding',
						type: 'binding',
						bindingSources: [{ category: 'local', id: 'local.points', label: 'Local points' }]
					}
				}
			},
			applied: { property: 'text', value: JSON.stringify(initial) }
		},
		requests: {}
	};
}

function createStudioState(overrides = {}) {
	return {
		nextStepIndex: 1,
		devRunning: false,
		contextMenuDevRunning: false,
		nextFrontendIndex: 1,
		frontendNodes: [
			{ id: 'firstText', label: 'First text' },
			{ id: 'secondText', label: 'Second text' }
		],
		steps: [
			{ name: 'Init', classname: 'com.twinsoft.convertigo.beans.steps.SimpleStep' },
			{ name: 'return', classname: 'com.twinsoft.convertigo.beans.steps.ReturnStep' }
		],
		files: ['readme.md'],
		/** @type {string[] | undefined} the projects closed in the workspace */
		closedProjects: undefined,
		...overrides
	};
}

/**
 * @param {ReturnType<typeof createStudioState>} state
 */
function testPlatformResponse(state) {
	const executionState = /** @type {{ variables?: any[], testcases?: any[] }} */ (state);
	return {
		admin: {
			project: {
				name: projectName,
				connector: [],
				sequence: [
					{
						name: sequenceName,
						comment: 'Mocked Studio sequence',
						accessibility: 'Public',
						variable: executionState.variables ?? [
							{
								name: 'input',
								value: '',
								comment: 'Execution variable'
							}
						],
						testcase: executionState.testcases ?? [
							{
								name: 'Default',
								variable: []
							}
						]
					}
				]
			}
		}
	};
}

/**
 * @param {URLSearchParams} params
 */
function acceptDboResponse(params) {
	const target = params.get('target') ?? '';
	const position = params.get('position') ?? '';
	if (position === 'inside' && target.startsWith(`${sequenceId}.st:`)) {
		return { accept: false };
	}
	// the frontend texts take no child, as the engine tells from their slots
	if (position === 'inside' && target.startsWith(`${frontendStructureId}.`)) {
		return { accept: false };
	}
	return { accept: true };
}

/**
 * @param {URLSearchParams} params
 * @param {ReturnType<typeof createStudioState>} state
 */
function addDboResponse(params, state) {
	const target = params.get('target') ?? '';
	const position = params.get('position') ?? 'inside';
	const payload = parseServiceJson(params.get('data'));
	if (payload?.data?.type === 'FrontendBlock') {
		const node = { id: `text${state.nextFrontendIndex++}`, label: 'New text' };
		const index = frontendInsertionIndex(state.frontendNodes, target, position);
		state.frontendNodes.splice(index, 0, node);
		return {
			done: true,
			id: `${frontendStructureId}.${node.id}`,
			parentId: frontendStructureId,
			projected: true,
			projectedSourcePath: 'model/StudioProject/src/routes/+page.flow.svelte',
			selectionSourcePath: 'model/StudioProject/src/routes/+page.flow.svelte',
			selectionMutationPath: `frontAst.nodes[${index}]`,
			selectionId: node.id
		};
	}
	const classname = payload?.data?.classname ?? payload?.data?.id ?? '';
	const name = uniqueStepName(state, stepBaseName(classname));
	const index = insertionIndex(state.steps, target, position);
	state.steps.splice(index, 0, { name, classname });
	return {
		done: true,
		id: `${sequenceId}.st:${name}`,
		parentId: sequenceId
	};
}

/**
 * @param {URLSearchParams} params
 * @param {ReturnType<typeof createStudioState>} state
 */
function moveDboResponse(params, state) {
	const target = params.get('target') ?? '';
	const position = params.get('position') ?? 'inside';
	const payload = parseServiceJson(params.get('data'));
	const frontendSourceId = payload?.data?.id ?? '';
	if (frontendSourceId.startsWith(`${frontendStructureId}.`)) {
		const sourceId = frontendSourceId.slice(frontendStructureId.length + 1);
		const sourceIndex = state.frontendNodes.findIndex((node) => node.id === sourceId);
		if (sourceIndex < 0) {
			return { done: false };
		}
		const [source] = state.frontendNodes.splice(sourceIndex, 1);
		const index = frontendInsertionIndex(state.frontendNodes, target, position);
		state.frontendNodes.splice(index, 0, source);
		return {
			done: true,
			id: `${frontendStructureId}.${source.id}`,
			parentId: frontendStructureId,
			previousParentId: frontendStructureId,
			projected: true,
			projectedSourcePath: 'model/StudioProject/src/routes/+page.flow.svelte',
			selectionSourcePath: 'model/StudioProject/src/routes/+page.flow.svelte',
			selectionMutationPath: `frontAst.nodes[${index}]`,
			selectionId: source.id
		};
	}
	const sourceEntry = findStepEntry(state, payload?.data?.id ?? '');
	if (!sourceEntry) {
		return { done: false };
	}
	const [source] = sourceEntry.siblings.splice(sourceEntry.index, 1);
	const targetParentId =
		position === 'inside' ? target : parentObjectId(target) || sourceEntry.parentId;
	const targetEntry = findStepEntry(state, targetParentId);
	const targetSiblings =
		position === 'inside'
			? targetEntry?.step
				? (targetEntry.step.children ??= [])
				: state.steps
			: (targetEntry?.step?.children ?? state.steps);
	const index = insertionIndex(targetSiblings, target, position);
	targetSiblings.splice(index, 0, source);
	return {
		done: true,
		id: stepId(source, targetParentId),
		parentId: targetParentId,
		previousParentId: sourceEntry.parentId
	};
}

/**
 * @param {URLSearchParams} params
 * @param {ReturnType<typeof createStudioState>} state
 */
function treeviewResponse(params, state) {
	const id = params.get('id') ?? projectName;
	const flow = params.get('flow') === 'true';
	const ids = JSON.parse(params.get('ids') ?? '[]');

	if (Array.isArray(ids) && ids.length) {
		return Object.fromEntries(ids.map((item) => [item, treeviewChildren(item, false, state)]));
	}

	return { children: treeviewChildren(id, flow, state) };
}

/**
 * @param {string} id
 * @param {boolean} flow
 * @param {ReturnType<typeof createStudioState>} state
 */
function treeviewChildren(id, flow, state) {
	if (flow && id === sequenceId) {
		return flowSteps(state);
	}
	const step = findStepEntry(state, id)?.step;
	if (step?.children) {
		return flowSteps({ ...state, steps: step.children }, id);
	}

	switch (id) {
		case projectName:
			return [
				folderNode(`${projectName}:cn`, 'Connectors'),
				folderNode(`${projectName}:sq`, 'Sequences'),
				folderNode(`${projectName}:ref`, 'References'),
				treeNode(flowEngineId, 'Engine', 'FlowEngine', { children: true }),
				folderNode(`${projectName}/`, 'Files')
			];
		case `${projectName}/`:
			return [...state.files]
				.sort()
				.map((name) => ({ id: `${projectName}//${name}`, name, label: name, icon: 'file' }));
		case flowEngineId:
			return [
				treeNode(frontendBuilderId, 'Svelte frontend', 'FlowVirtualObject', { children: true })
			];
		case frontendBuilderId:
			return [
				treeNode(frontendStructureId, 'Home structure', 'FlowVirtualObject', { children: true })
			];
		case frontendStructureId:
			return state.frontendNodes.map((node) =>
				treeNode(`${frontendStructureId}.${node.id}`, node.label, 'FlowVirtualObject')
			);
		case `${projectName}:sq`:
			return [
				treeNode(
					sequenceId,
					sequenceName,
					'com.twinsoft.convertigo.beans.sequences.GenericSequence',
					{
						children: true,
						isSourceContainer: true
					}
				)
			];
		case sequenceId:
			return [
				folderNode(`${sequenceId}:st`, 'Steps'),
				folderNode(`${sequenceId}:vr`, 'Variables'),
				folderNode(`${sequenceId}:tc`, 'Test Cases')
			];
		case `${sequenceId}:st`:
			return flowSteps(state);
		default:
			return [];
	}
}

/**
 * @param {URLSearchParams} params
 * @param {ReturnType<typeof createStudioState>} state
 */
function filesResponse(params, state) {
	const name = String(params.get('id')).replace(/^.*\//, '');
	const action = params.get('action');
	if (action === 'newFile') {
		state.files = [...state.files, String(params.get('name'))].sort();
		return { done: true, id: `${projectName}//${params.get('name')}` };
	}
	if (action === 'rename') {
		state.files = state.files.map((file) => (file === name ? String(params.get('name')) : file));
		return { done: true, id: `${projectName}//${params.get('name')}` };
	}
	if (action === 'delete') {
		state.files = state.files.filter((file) => file !== name);
		return { done: true };
	}
	return {};
}

/**
 * @param {URLSearchParams} params
 * @param {ReturnType<typeof createStudioState>} state
 */
function contextMenuResponse(params, state) {
	const id = params.get('id') ?? '';
	const items =
		id === frontendBuilderId
			? [
					contextMenuItem(
						'frontbuilder.svelte.dev.start',
						'Start dev mode',
						'Start Vite behind the Studio gateway.',
						'Svelte dev',
						!state.contextMenuDevRunning
					),
					contextMenuItem(
						'frontbuilder.svelte.dev.stop',
						'Stop dev mode',
						'Stop the Vite dev server.',
						'Svelte dev',
						state.contextMenuDevRunning
					),
					contextMenuItem(
						'frontbuilder.svelte.dev.open',
						'Open dev mode',
						'Open the running Vite dev server.',
						'Svelte dev',
						state.contextMenuDevRunning
					),
					contextMenuItem(
						'frontbuilder.svelte.generate',
						'Update generated source',
						'Regenerate the Svelte sources.',
						'Svelte build'
					),
					contextMenuItem(
						'frontbuilder.svelte.build',
						'Build prod',
						'Build DisplayObjects/mobile.',
						'Svelte build'
					),
					contextMenuItem(
						'frontbuilder.svelte.openBuilt',
						'Open built prod',
						'Open the production frontend.',
						'Svelte build'
					)
				]
			: id.startsWith(`${frontendStructureId}.`)
				? [
						contextMenuItem(
							'flow.node.disable',
							'Disable',
							'Skip this Flow node as if it was absent.',
							'Flow'
						),
						contextMenuItem(
							'frontbuilder.svelte.dev.start',
							'Start dev mode',
							'Start Vite behind the Studio gateway.',
							'Svelte dev'
						)
					]
				: [];
	return {
		id,
		menu: {
			ok: true,
			protocol: 'flow.studio.menu.v1',
			label: 'Flow',
			items
		}
	};
}

/**
 * @param {URLSearchParams} params
 * @param {ReturnType<typeof createStudioState>} state
 * @param {string[]=} contextActions
 */
function contextActionResponse(params, state, contextActions) {
	const id = params.get('id') ?? '';
	const action = parseServiceJson(params.get('action'));
	contextActions?.push(action.id);
	if (action.id === 'frontbuilder.svelte.dev.start') {
		state.devRunning = true;
		state.contextMenuDevRunning = true;
		return {
			id,
			result: {
				ok: true,
				title: 'Svelte dev mode',
				message: 'Svelte dev mode started.',
				openUrl: 'http://localhost:28080/convertigo/gw/studio-test-ticket/'
			}
		};
	}
	if (action.id === 'frontbuilder.svelte.dev.stop') {
		state.devRunning = false;
		return {
			id,
			result: {
				ok: true,
				title: 'Svelte dev mode',
				message: 'Svelte dev mode stopped.'
			}
		};
	}
	return {
		id,
		result: {
			ok: true,
			openUrl:
				action.id === 'frontbuilder.svelte.dev.open'
					? 'http://localhost:28080/convertigo/gw/studio-test-ticket/'
					: ''
		}
	};
}

function contextMenuItem(id, label, description, group, enabled = true) {
	return { id, label, description, group, enabled, payload: {}, confirm: '', icon: '' };
}

/**
 * @param {URLSearchParams} params
 * @param {ReturnType<typeof createStudioState>} state
 */
function renameDboResponse(params, state) {
	const id = params.get('id') ?? '';
	const name = params.get('name') ?? '';
	const previousName = id.split('.st:').at(-1) ?? '';
	const step = state.steps.find((item) => item.name === previousName);
	if (step && name) {
		step.name = name;
	}
	return { done: Boolean(step && name) };
}

/**
 * @param {string | null} value
 */
function parseServiceJson(value) {
	try {
		return JSON.parse(value ?? '{}');
	} catch {
		return {};
	}
}

/**
 * @param {ReturnType<typeof createStudioState>} state
 * @param {string} baseName
 */
function uniqueStepName(state, baseName) {
	let name = `${baseName}${state.nextStepIndex++}`;
	while (state.steps.some((step) => step.name === name)) {
		name = `${baseName}${state.nextStepIndex++}`;
	}
	return name;
}

/**
 * @param {string} classname
 */
function stepBaseName(classname) {
	if (/returnstep$/i.test(classname)) {
		return 'return';
	}
	return 'SimpleStep';
}

/**
 * @param {any[]} siblings
 * @param {string} target
 * @param {string} position
 */
function insertionIndex(siblings, target, position) {
	const targetName = target.split('.st:').at(-1) ?? '';
	const targetIndex = siblings.findIndex((step) => step.name === targetName);
	if (targetIndex < 0) {
		return siblings.length;
	}
	if (position === 'before' || position === 'first') {
		return targetIndex;
	}
	if (position === 'after') {
		return targetIndex + 1;
	}
	return siblings.length;
}

/**
 * @param {URLSearchParams} params
 * @param {ReturnType<typeof createStudioState>} state
 */
function removeDboResponse(params, state) {
	const id = params.get('id') ?? '';
	if (id.startsWith(`${frontendStructureId}.`)) {
		const index = state.frontendNodes.findIndex(
			(node) => node.id === id.slice(frontendStructureId.length + 1)
		);
		if (index >= 0) {
			state.frontendNodes.splice(index, 1);
		}
		return { done: index >= 0 };
	}
	const entry = findStepEntry(state, id);
	if (!entry) {
		return { done: false };
	}
	entry.siblings.splice(entry.index, 1);
	return { done: true };
}

/**
 * @param {ReturnType<typeof createStudioState>} state
 */
function flowSteps(state, parentId = sequenceId) {
	return state.steps.map((step) => stepTreeNode(step, parentId));
}

function paletteResponse(params) {
	if ((params.get('id') ?? '').startsWith(frontendStructureId)) {
		return {
			categories: [
				{
					name: 'Typography',
					items: [
						{
							type: 'FrontendBlock',
							id: 'frontend:Text',
							name: 'Text',
							classname: 'Text',
							block: 'Text',
							insert: { id: 'text', block: 'Text', props: { text: 'New text' } }
						}
					]
				}
			]
		};
	}
	return {
		categories: [
			{
				name: 'Steps',
				items: [
					{
						id: 'com.twinsoft.convertigo.beans.steps.SimpleStep',
						name: 'Simple step',
						classname: 'com.twinsoft.convertigo.beans.steps.SimpleStep'
					},
					{
						id: 'com.twinsoft.convertigo.beans.steps.JsonObjectStep',
						name: 'JSON object',
						classname: 'com.twinsoft.convertigo.beans.steps.JsonObjectStep',
						inputs: 1,
						outputs: 1,
						bottomInputs: 1,
						bottomOutputs: 1
					},
					{
						id: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep',
						name: 'JSON field',
						classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
					},
					{
						id: 'com.twinsoft.convertigo.beans.steps.ReturnStep',
						name: 'Return',
						classname: 'com.twinsoft.convertigo.beans.steps.ReturnStep'
					}
				]
			}
		]
	};
}

/**
 * @param {{ id: string }[]} siblings
 * @param {string} target
 * @param {string} position
 */
function frontendInsertionIndex(siblings, target, position) {
	if (position === 'inside') {
		return siblings.length;
	}
	const targetId = target.startsWith(`${frontendStructureId}.`)
		? target.slice(frontendStructureId.length + 1)
		: '';
	const targetIndex = siblings.findIndex((node) => node.id === targetId);
	if (targetIndex < 0) {
		return siblings.length;
	}
	return position === 'before' || position === 'first' ? targetIndex : targetIndex + 1;
}

/**
 * @param {{ name: string, classname: string, children?: any[], isSourceContainer?: boolean }} step
 * @param {string} parentId
 */
function stepTreeNode(step, parentId) {
	return treeNode(stepId(step, parentId), step.name, step.classname, {
		children: step.children ? true : false,
		isSourceContainer: Boolean(step.isSourceContainer)
	});
}

/**
 * @param {{ name: string }} step
 * @param {string} parentId
 */
function stepId(step, parentId) {
	return `${parentId}.st:${step.name}`;
}

/**
 * @param {string} id
 * @param {string} label
 * @param {string} classname
 * @param {{ children?: boolean | any[], isSourceContainer?: boolean, taggable?: boolean, tagScope?: string }} [options]
 */
function treeNode(id, label, classname, options = {}) {
	return {
		id,
		name: label,
		label,
		classname,
		taggable: options.taggable ?? (id === projectName || id === sequenceId),
		tagScope:
			options.tagScope ??
			(id === projectName ? 'workspaceProjects' : id === sequenceId ? 'projectObjects' : undefined),
		icon: 'file',
		children: options.children ?? false,
		isLoop: false,
		isXml: false,
		isSourceContainer: Boolean(options.isSourceContainer)
	};
}

/**
 * @param {string} id
 * @param {string} label
 */
function folderNode(id, label) {
	return {
		id,
		name: label,
		label,
		icon: 'folder',
		children: true
	};
}

/**
 * @param {ReturnType<typeof createStudioState>} state
 * @param {string} id
 */
function findStepEntry(state, id) {
	return findStepEntryInSiblings(state.steps, sequenceId, id);
}

/**
 * @param {any[]} siblings
 * @param {string} parentId
 * @param {string} id
 */
function findStepEntryInSiblings(siblings, parentId, id) {
	for (let index = 0; index < siblings.length; index += 1) {
		const step = siblings[index];
		const currentId = stepId(step, parentId);
		if (currentId === id) {
			return { step, siblings, index, parentId };
		}
		if (step.children) {
			const childEntry = findStepEntryInSiblings(step.children, currentId, id);
			if (childEntry) {
				return childEntry;
			}
		}
	}
	return null;
}

/**
 * @param {string} id
 */
function parentObjectId(id) {
	const index = id.lastIndexOf('.st:');
	return index > 0 ? id.slice(0, index) : '';
}
