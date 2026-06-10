import { expect, test } from '@playwright/test';

const projectName = 'StudioProject';
const sequenceName = 'TestSequence';
const sequenceId = `${projectName}.sq:${sequenceName}`;
const initStepId = `${sequenceId}.st:Init`;

test('studio opens a selected backend object with tree, execution and flow synchronized', async ({
	page
}) => {
	await mockStudioServices(page);
	await page.goto('/studio/#flow');

	await expect(page.getByText('Convertigo Studio')).toBeVisible();
	await expect(page.getByRole('radio', { name: 'Backend' })).toHaveAttribute(
		'aria-checked',
		'true'
	);
	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);

	await page.getByRole('button', { name: 'Execution', exact: true }).click();
	await expect(page.getByText('Variables (1)')).toBeVisible();
	await page.getByRole('button', { name: 'Execute' }).click();
	await expect(responseEditor(page)).toBeVisible();
	await page.getByRole('button', { name: 'Clear' }).click();
	await expect(responseEditor(page)).toHaveCount(0);

	await page.getByRole('button', { name: 'Flow', exact: true }).click();
	await expect(page.getByRole('button', { name: 'Flow', exact: true })).toHaveAttribute(
		'aria-expanded',
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
		new RegExp(`/studio/${projectName}\\.sq~${sequenceName}\\.st~Init/#flow$`)
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
	await page.getByRole('button', { name: 'Execution', exact: true }).click();

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
	expect(executionRequests[0]).toContain('input=from-testcase');

	await page.locator('.requestable-testcases__grid button').filter({ hasText: 'Execute' }).click();
	await expect.poll(() => executionRequests.length).toBe(2);
	expect(executionRequests[1]).toContain('__sequence=TestSequence');
	expect(executionRequests[1]).toContain('__testcase=PresetInput');
	expect(executionRequests[1]).not.toContain('input=');
});

test('studio redirects authenticated users without the web admin role', async ({ page }) => {
	await mockStudioServices(page, { roles: ['TEST_PLATFORM'] });

	await page.goto('/studio/');

	await expect(page).toHaveURL(/\/dashboard\/$/);
});

test('studio frontend profile exposes a dashboard-like device rail', async ({ page }) => {
	await mockStudioServices(page);
	await page.goto('/studio/');

	await selectTreeNode(page, projectName);
	await page.getByRole('radio', { name: 'Frontend' }).click();

	await expect(page.getByRole('tab', { name: /Devices/ })).toHaveAttribute('aria-selected', 'true');
	await expect(page.getByText('Current device')).toBeVisible();
	await expect(page.getByRole('button', { name: 'Select device Responsive' })).toBeVisible();

	await page.getByRole('button', { name: /Apple iPhone/ }).click();
	await page.getByRole('button', { name: 'Select device iPhone 17 Pro', exact: true }).click();

	await expect(page.getByText('iPhone 17 Pro').first()).toBeVisible();
	await expect(page.getByRole('button', { name: 'Landscape orientation' })).toBeEnabled();

	await page.getByRole('button', { name: 'Landscape orientation' }).click();
	await expect(page.getByText(/iPhone 17 Pro landscape - 874x402/)).toBeVisible();
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
	await page.getByRole('button', { name: 'Flow', exact: true }).click();

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
		new RegExp(`/studio/${projectName}\\.sq~${sequenceName}\\.st~RenamedInit/#flow$`)
	);
});

test('studio keeps flow and url synchronized after a tree rename mutation', async ({ page }) => {
	const state = createStudioState();
	await mockStudioServices(page, { state });
	await page.goto('/studio/#flow');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('button', { name: 'Flow', exact: true }).click();
	await ensureTreeNodeExpanded(page, sequenceId);
	await ensureTreeNodeExpanded(page, `${sequenceId}:st`);
	await selectTreeNode(page, initStepId);

	await page.getByRole('button', { name: 'Rename object' }).click();
	const input = page.getByRole('textbox', { name: 'Rename object' });
	await expect(input).toBeFocused();
	await input.fill('TreeRenamedInit');
	await input.press('Enter');

	await expect(page.locator('[role="treeitem"][aria-selected="true"]')).toContainText(
		'TreeRenamedInit'
	);
	await expect(flowNodeName(page, 'TreeRenamedInit')).toBeVisible();
	await expect(page).toHaveURL(
		new RegExp(`/studio/${projectName}\\.sq~${sequenceName}\\.st~TreeRenamedInit/#flow$`)
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
	await page.getByRole('button', { name: 'Flow', exact: true }).click();
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
	await expect(page).toHaveURL(new RegExp(`/studio/${projectName}\\.sq~${sequenceName}/#flow$`));
});

test('studio adds a palette step from the flow and opens inline rename', async ({ page }) => {
	const state = createStudioState();
	await mockStudioServices(page, { state });
	await page.goto('/studio/#flow');

	await expandTreeNode(page, projectName);
	await expandTreeNode(page, `${projectName}:sq`);
	await selectTreeNode(page, sequenceId);
	await page.getByRole('button', { name: 'Flow', exact: true }).click();
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
		new RegExp(`/studio/${projectName}\\.sq~${sequenceName}\\.st~DroppedFromPalette/#flow$`)
	);
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
	await page.getByRole('button', { name: 'Flow', exact: true }).click();
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
		new RegExp(`/studio/${projectName}\\.sq~${sequenceName}\\.st~Init/#flow$`)
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
	await page.getByRole('button', { name: 'Flow', exact: true }).click();
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
		new RegExp(`/studio/${projectName}\\.sq~${sequenceName}\\.st~object\\.st~field2/#flow$`)
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
	await page.getByRole('button', { name: 'Flow', exact: true }).click();
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
		new RegExp(`/studio/${projectName}\\.sq~${sequenceName}\\.st~object\\.st~field2/#flow$`)
	);
});

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} nodeId
 */
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
			window.__studioFlowDragContext = {
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
		const context = window.__studioFlowDragContext;
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
		delete window.__studioFlowDragContext;
	});
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} sourceNodeId
 * @param {string} targetNodeId
 * @param {{ yRatio?: number, beforeDrop?: () => Promise<void> }} [options]
 */
async function dragTreeNodeToTreeNode(page, sourceNodeId, targetNodeId, options = {}) {
	const sourceBox = await treeRowBox(page, sourceNodeId);
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
 *  executionRequests?: string[]
 * }} [options]
 */
async function mockStudioServices(page, options = {}) {
	await page.route('**/admin/services/**', async (route) => {
		const request = route.request();
		const service = serviceName(request.url());
		const params = new URLSearchParams(request.postData() ?? '');
		const body = responseForService(service, params, options);

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
 *  executionRequests?: string[]
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
						project: [{ name: projectName, comment: '', ref: [] }]
					}
				}
			};
		case 'projects.GetTestPlatform':
			return testPlatformResponse(state);
		case 'studio.treeview.Get':
			return treeviewResponse(params, state);
		case 'studio.dbo.Accept':
			return acceptDboResponse(params);
		case 'studio.dbo.Add':
			return addDboResponse(params, state);
		case 'studio.dbo.Move':
			return moveDboResponse(params, state);
		case 'studio.dbo.Rename':
			return renameDboResponse(params, state);
		case 'studio.dbo.Remove':
			return removeDboResponse(params, state);
		case 'studio.palette.Get':
			return paletteResponse();
		case 'studio.properties.Get':
			return {
				properties: {
					comment: {
						displayName: 'Comment',
						value: '',
						originalValue: '',
						type: 'java.lang.String',
						isMultiline: true
					}
				}
			};
		default:
			return {};
	}
}

function createStudioState(overrides = {}) {
	return {
		nextStepIndex: 1,
		steps: [
			{ name: 'Init', classname: 'com.twinsoft.convertigo.beans.steps.SimpleStep' },
			{ name: 'return', classname: 'com.twinsoft.convertigo.beans.steps.ReturnStep' }
		],
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
				folderNode(`${projectName}:ref`, 'References')
			];
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

function paletteResponse() {
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
 * @param {{ children?: boolean | any[], isSourceContainer?: boolean }} [options]
 */
function treeNode(id, label, classname, options = {}) {
	return {
		id,
		name: label,
		label,
		classname,
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
