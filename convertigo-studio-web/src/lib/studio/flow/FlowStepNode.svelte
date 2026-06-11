<script>
	import { Handle, Position } from '@xyflow/svelte';
	import { objectNameFromId } from '$lib/studio/dnd';
	import AutoSvg from '$lib/utils/AutoSvg.svelte';
	import { draggedData } from '$lib/utils/dndStore';
	import Ico from '$lib/utils/Ico.svelte';
	import { getUrl } from '$lib/utils/service';
	import {
		branchLabel,
		branchLabelForStep,
		branchTone,
		dropLabel,
		dropZoneLabel,
		dropZoneTone,
		isIfStep
	} from './flowStepLabels';
	import { inputHandleId, outputHandleId } from './xyflow';

	/** @type {{ data: import('./types').FlowStepNodeData }} */
	let { data } = $props();

	let sideInputCount = $derived(Math.max(0, data.inputs - data.bottomInputs));
	let sideOutputCount = $derived(Math.max(0, data.outputs - data.bottomOutputs));
	let draggableObjectId = $derived(data.originalId ?? '');
	let renameValue = $state('');
	let renameFocusId = '';

	$effect(() => {
		const objectId = draggableObjectId;
		if (!data.isRenaming) {
			renameFocusId = '';
			return;
		}
		if (!data.isRenaming || !objectId || renameFocusId === objectId) {
			return;
		}
		renameFocusId = objectId;
		renameValue = objectNameFromId(objectId);
	});

	/**
	 * @param {HTMLInputElement} node
	 */
	function focusRenameInput(node) {
		/** @type {number | undefined} */
		let frame;
		/** @type {ReturnType<typeof setTimeout> | undefined} */
		let timeout;
		const focusInput = () => {
			if (!data.isRenaming) {
				return;
			}
			node.focus({ preventScroll: true });
			node.select();
		};
		focusInput();
		if (typeof requestAnimationFrame === 'function') {
			frame = requestAnimationFrame(() => {
				focusInput();
				timeout = setTimeout(focusInput, 0);
			});
		} else {
			timeout = setTimeout(focusInput, 0);
		}
		return () => {
			if (frame) {
				cancelAnimationFrame(frame);
			}
			if (timeout) {
				clearTimeout(timeout);
			}
		};
	}

	/**
	 * @param {number} count
	 * @returns {number[]}
	 */
	function slots(count) {
		return Array.from({ length: Math.max(0, count) }, (_, index) => index);
	}

	/**
	 * @param {number} index
	 * @param {number} count
	 * @returns {string}
	 */
	function sideTop(index, count) {
		if (count <= 1) {
			return '50%';
		}
		return `${((index + 1) / (count + 1)) * 100}%`;
	}

	/**
	 * @param {'out' | 'in'} kind
	 * @param {number} index
	 * @returns {string}
	 */
	function bottomLeft(kind, index) {
		const total = data.bottomOutputs + data.bottomInputs;
		if (total <= 0) {
			return '50%';
		}
		const order = kind === 'in' ? data.bottomOutputs + index : index;
		return `${((order + 1) / (total + 1)) * 100}%`;
	}

	/**
	 * @param {number} index
	 * @returns {string}
	 */
	function outputLabel(index) {
		return branchLabelForStep(data.outputLabels?.[index] ?? '', data);
	}

	/**
	 * @param {number} index
	 * @returns {string}
	 */
	function outputTop(index) {
		const labelTone = branchTone(outputLabel(index));
		if (isIfStep(data) && !data.hasElseBranch && sideOutputCount === 2) {
			if (labelTone === 'then') {
				return sideTop(1, sideOutputCount);
			}
			if (labelTone === 'next') {
				return sideTop(0, sideOutputCount);
			}
		}
		return sideTop(index, sideOutputCount);
	}

	/**
	 * @param {number} index
	 * @returns {string}
	 */
	function bottomOutputLabel(index) {
		return branchLabelForStep(data.outputLabels?.[sideOutputCount + index] ?? '', data);
	}

	/**
	 * @param {number} index
	 * @returns {string}
	 */
	function bottomInputLabel(index) {
		return branchLabel(data.inputLabels?.[sideInputCount + index] ?? '');
	}

	/**
	 * @param {string} label
	 * @returns {string}
	 */
	function portLabelClass(label) {
		const kind = branchTone(label);
		return kind ? `flow-step-node__port-label--${kind}` : '';
	}

	/**
	 * @param {string} label
	 * @returns {string}
	 */
	function portHandleClass(label) {
		const kind = branchTone(label);
		return kind ? `flow-step-node__handle--branch-${kind}` : '';
	}

	/**
	 * @param {...string} values
	 * @returns {string}
	 */
	function classNames(...values) {
		return values.filter(Boolean).join(' ');
	}

	/**
	 * @param {string | undefined} icon
	 * @returns {string}
	 */
	function iconSource(icon) {
		if (!icon) {
			return '';
		}
		if (/^(https?:|data:|\/)/.test(icon)) {
			return icon;
		}
		if (icon.includes('/')) {
			return `${getUrl()}${icon}`;
		}
		return `${getUrl()}studio.dbo.GetIcon?iconPath=${encodeURIComponent(icon)}`;
	}

	/**
	 * @param {MouseEvent} event
	 */
	function toggleSubsteps(event) {
		event.stopPropagation();
		event.preventDefault();
		data.onToggleSubsteps?.(data.id, event.shiftKey);
	}

	/**
	 * @param {DragEvent} event
	 */
	function handleDragStart(event) {
		if (!draggableObjectId) {
			event.preventDefault();
			return;
		}
		event.stopPropagation();
		const treeData = {
			type: 'treeData',
			data: { id: draggableObjectId, classname: data.classname ?? '' },
			options: {}
		};
		event.dataTransfer?.setData('text/plain', JSON.stringify(treeData));
		event.dataTransfer?.setData('treedata', JSON.stringify(treeData));
		if (event.dataTransfer) {
			event.dataTransfer.effectAllowed = 'move';
			event.dataTransfer.dropEffect = 'move';
		}
		$draggedData = treeData;
	}

	/**
	 * @param {DragEvent} event
	 */
	function handleDragEnd(event) {
		event.stopPropagation();
		$draggedData = undefined;
	}

	/**
	 * @param {MouseEvent} event
	 */
	function requestRename(event) {
		event.preventDefault();
		event.stopPropagation();
		if (draggableObjectId) {
			data.onRequestRename?.(draggableObjectId);
		}
	}

	/**
	 * @param {MouseEvent} event
	 */
	function requestDelete(event) {
		event.preventDefault();
		event.stopPropagation();
		if (draggableObjectId) {
			data.onDelete?.(draggableObjectId);
		}
	}

	/**
	 * @param {Event} event
	 */
	function commitRename(event) {
		event.preventDefault();
		event.stopPropagation();
		if (!data.isRenaming || !draggableObjectId) {
			return;
		}
		const nextName = renameValue.trim();
		if (!nextName || nextName === objectNameFromId(draggableObjectId)) {
			data.onRename?.(draggableObjectId, objectNameFromId(draggableObjectId));
			return;
		}
		data.onRename?.(draggableObjectId, nextName);
	}

	/**
	 * @param {KeyboardEvent} event
	 */
	function handleRenameKeydown(event) {
		if (event.key === 'Escape') {
			event.preventDefault();
			event.stopPropagation();
			data.onRename?.(draggableObjectId, objectNameFromId(draggableObjectId));
		}
	}
</script>

<div
	class="flow-step-node"
	class:flow-step-node--terminal={data.isFlowTerminal}
	class:flow-step-node--request={data.terminalKind === 'request'}
	class:flow-step-node--response={data.terminalKind === 'response'}
	class:flow-step-node--selected={data.isSelected}
	class:flow-step-node--drop-target={data.isDropTarget}
	class:flow-step-node--drop-denied={data.isDropTarget && data.isDropDenied}
	class:flow-step-node--drop-before={data.isDropTarget && data.dropPosition === 'before'}
	class:flow-step-node--drop-after={data.isDropTarget && data.dropPosition === 'after'}
	class:flow-step-node--drop-inside={data.isDropTarget && data.dropPosition === 'inside'}
	style={`--step-color: ${data.color ?? '#64748b'}`}
	title={data.classname ?? data.type}
	role="presentation"
>
	{#if data.isSelected && !data.isRenaming && draggableObjectId}
		<div
			class="flow-step-node__actions nopan nowheel"
			role="toolbar"
			aria-label="Selected step actions"
		>
			<button
				type="button"
				class="flow-step-node__action flow-step-node__drag-handle nopan nowheel"
				title="Move step"
				aria-label="Move step"
				draggable={true}
				onpointerdown={(event) => event.stopPropagation()}
				onmousedown={(event) => event.stopPropagation()}
				onclick={(event) => event.stopPropagation()}
				ondragstart={handleDragStart}
				ondragend={handleDragEnd}
			>
				<span aria-hidden="true"></span>
				<span aria-hidden="true"></span>
			</button>
			<button
				type="button"
				class="flow-step-node__action nopan nowheel"
				title="Rename step"
				aria-label="Rename step"
				onpointerdown={(event) => event.stopPropagation()}
				onmousedown={(event) => event.stopPropagation()}
				onclick={requestRename}
			>
				<Ico icon="mdi:pencil-outline" size={3.2} />
			</button>
			<button
				type="button"
				class="flow-step-node__action flow-step-node__action--danger nopan nowheel"
				title="Delete step"
				aria-label="Delete step"
				onpointerdown={(event) => event.stopPropagation()}
				onmousedown={(event) => event.stopPropagation()}
				onclick={requestDelete}
			>
				<Ico icon="mdi:delete-outline" size={3.2} />
			</button>
		</div>
	{/if}
	{#if data.isDropTarget}
		{@const currentDropLabel = dropLabel({
			denied: data.isDropDenied,
			position: data.dropPosition,
			branch: data.dropBranch || data.parentBranch,
			host: data.dropHostLabel,
			isIf: isIfStep(data),
			step: data
		})}
		{@const currentDropBranch = data.dropBranch || data.parentBranch}
		{@const currentDropIsIf = isIfStep(data)}
		{@const beforeZoneTone = dropZoneTone({
			zone: 'before',
			branch: currentDropBranch,
			isIf: currentDropIsIf,
			step: data
		})}
		{@const insideZoneTone = dropZoneTone({
			zone: 'inside',
			branch: currentDropBranch,
			isIf: currentDropIsIf,
			step: data
		})}
		{@const afterZoneTone = dropZoneTone({
			zone: 'after',
			branch: currentDropBranch,
			isIf: currentDropIsIf,
			step: data
		})}
		<span
			class={classNames(
				'flow-step-node__drop-zone',
				'flow-step-node__drop-zone--before',
				beforeZoneTone && `flow-step-node__drop-zone--${beforeZoneTone}`
			)}
			class:flow-step-node__drop-zone--active={data.dropPosition === 'before'}
			aria-hidden="true"
		>
			<span class="flow-step-node__drop-zone-label">
				{dropZoneLabel({
					zone: 'before',
					branch: currentDropBranch,
					isIf: currentDropIsIf,
					step: data
				})}
			</span>
		</span>
		<span
			class={classNames(
				'flow-step-node__drop-zone',
				'flow-step-node__drop-zone--inside',
				insideZoneTone && `flow-step-node__drop-zone--${insideZoneTone}`
			)}
			class:flow-step-node__drop-zone--active={data.dropPosition === 'inside'}
			aria-hidden="true"
		>
			<span class="flow-step-node__drop-zone-label">
				{dropZoneLabel({
					zone: 'inside',
					branch: currentDropBranch,
					isIf: currentDropIsIf,
					step: data
				})}
			</span>
		</span>
		<span
			class={classNames(
				'flow-step-node__drop-zone',
				'flow-step-node__drop-zone--after',
				afterZoneTone && `flow-step-node__drop-zone--${afterZoneTone}`
			)}
			class:flow-step-node__drop-zone--active={data.dropPosition === 'after'}
			aria-hidden="true"
		>
			<span class="flow-step-node__drop-zone-label">
				{dropZoneLabel({
					zone: 'after',
					branch: currentDropBranch,
					isIf: currentDropIsIf,
					step: data
				})}
			</span>
		</span>
		<span class="flow-step-node__drop-marker" aria-hidden="true"></span>
		<span class="flow-step-node__drop-badge" title={currentDropLabel}>
			{currentDropLabel}
		</span>
	{/if}
	{#each slots(sideInputCount) as index (index)}
		<Handle
			id={inputHandleId(index)}
			type="target"
			position={Position.Left}
			style={`top: ${sideTop(index, sideInputCount)};`}
		/>
	{/each}
	{#each slots(data.bottomInputs) as index (index)}
		{@const label = bottomInputLabel(index)}
		<Handle
			id={inputHandleId(sideInputCount + index)}
			type="target"
			position={Position.Bottom}
			class={classNames('flow-step-node__handle--bottom-in', portHandleClass(label))}
			style={`left: ${bottomLeft('in', index)}; bottom: -7px;`}
		/>
		{#if label}
			<span
				class={classNames(
					'flow-step-node__port-label',
					'flow-step-node__port-label--bottom-in',
					portLabelClass(label)
				)}
				style={`left: ${bottomLeft('in', index)};`}
			>
				{label}
			</span>
		{/if}
	{/each}

	<div class="flow-step-node__accent"></div>
	<div class="flow-step-node__icon">
		{#if iconSource(data.icon)}
			<AutoSvg src={iconSource(data.icon)} alt="" class="h-6 w-6 object-contain" />
		{:else if data.terminalKind === 'request'}
			<Ico icon="mdi:import" size={4} />
		{:else if data.terminalKind === 'response'}
			<Ico icon="mdi:export" size={4} />
		{:else}
			<Ico icon="mdi:cube-outline" size={4} />
		{/if}
	</div>
	<div class="flow-step-node__body">
		{#if data.isRenaming}
			<form class="flow-step-node__rename-form" onsubmit={commitRename}>
				<input
					{@attach focusRenameInput}
					bind:value={renameValue}
					class="flow-step-node__rename"
					aria-label="Rename step"
					onblur={commitRename}
					onclick={(event) => event.stopPropagation()}
					onpointerdown={(event) => event.stopPropagation()}
					onkeydown={handleRenameKeydown}
				/>
			</form>
		{:else}
			<div class="flow-step-node__name">{data.name || data.label}</div>
		{/if}
		<div class="flow-step-node__type">
			{#if data.isLoop}
				Loop
			{:else if data.isReturn}
				Return
			{:else if data.isBreak}
				Break
			{:else}
				{data.type.split('.').pop()}
			{/if}
		</div>
	</div>

	{#each slots(sideOutputCount) as index (index)}
		{@const label = outputLabel(index)}
		<Handle
			id={outputHandleId(index)}
			type="source"
			position={Position.Right}
			class={portHandleClass(label)}
			style={`top: ${outputTop(index)};`}
		/>
		{#if label}
			<span
				class={classNames(
					'flow-step-node__port-label',
					'flow-step-node__port-label--out',
					portLabelClass(label)
				)}
				style={`top: ${outputTop(index)};`}
			>
				{label}
			</span>
		{/if}
	{/each}
	{#each slots(data.bottomOutputs) as index (index)}
		{@const label = bottomOutputLabel(index)}
		<Handle
			id={outputHandleId(sideOutputCount + index)}
			type="source"
			position={Position.Bottom}
			class={classNames('flow-step-node__handle--bottom-out', portHandleClass(label))}
			style={`left: ${bottomLeft('out', index)}; bottom: -7px;`}
		/>
		{#if label}
			<span
				class={classNames(
					'flow-step-node__port-label',
					'flow-step-node__port-label--bottom-out',
					portLabelClass(label)
				)}
				style={`left: ${bottomLeft('out', index)};`}
			>
				{label}
			</span>
		{/if}
	{/each}
	{#if data.substepDescendantCount && data.onToggleSubsteps}
		<button
			type="button"
			class="flow-step-node__substep-toggle"
			class:flow-step-node__substep-toggle--collapsed={data.isSubstepCollapsed}
			title={`${data.isSubstepCollapsed ? 'Expand' : 'Collapse'} substeps (${data.substepDescendantCount})`}
			aria-label={`${data.isSubstepCollapsed ? 'Expand' : 'Collapse'} substeps (${data.substepDescendantCount})`}
			onpointerdown={(event) => event.stopPropagation()}
			onclick={toggleSubsteps}
		>
			{data.isSubstepCollapsed ? '+' : '-'}
		</button>
	{/if}
</div>

<style>
	.flow-step-node {
		position: relative;
		display: grid;
		grid-template-columns: auto minmax(0, 1fr);
		align-items: center;
		gap: 0.55rem;
		width: 150px;
		height: 72px;
		border: 1px solid
			color-mix(in oklab, var(--step-color) 35%, var(--flow-node-border-base, #e2e8f0));
		border-radius: 0.45rem;
		background: linear-gradient(
			180deg,
			var(--flow-node-bg-start, #242b3a) 0%,
			var(--flow-node-bg-end, #1b2230) 100%
		);
		box-shadow:
			0 14px 28px -20px var(--flow-node-shadow, #020617),
			0 0 0 1px var(--flow-node-inset, rgb(255 255 255 / 0.05)) inset;
		color: var(--flow-node-text, #f8fafc);
		isolation: isolate;
		padding: 0.45rem 0.55rem 0.45rem 0.7rem;
		overflow: visible;
	}

	.flow-step-node--selected {
		border-color: var(--flow-node-selected-border, #38bdf8);
		box-shadow:
			0 14px 28px -20px var(--flow-node-shadow, #020617),
			0 0 0 2px var(--flow-node-selected-ring, rgb(56 189 248 / 0.72)),
			0 0 30px var(--flow-node-selected-glow, rgb(56 189 248 / 0.48));
	}

	.flow-step-node--terminal {
		border-style: dashed;
		background: linear-gradient(
			180deg,
			color-mix(in oklab, var(--step-color) 18%, var(--flow-node-bg-start, #242b3a)) 0%,
			var(--flow-node-bg-end, #1b2230) 100%
		);
	}

	.flow-step-node--request {
		--step-color: #22c55e;
	}

	.flow-step-node--response {
		--step-color: #f59e0b;
	}

	.flow-step-node--terminal .flow-step-node__name {
		text-transform: lowercase;
	}

	.flow-step-node--selected::before {
		position: absolute;
		z-index: -1;
		inset: -0.62rem;
		border: 1px solid var(--flow-node-selected-halo-border, rgb(125 211 252 / 0.72));
		border-radius: 0.95rem;
		background: radial-gradient(
			circle,
			var(--flow-node-selected-halo-bg, rgb(14 165 233 / 0.24)) 0%,
			transparent 68%
		);
		box-shadow:
			0 0 0 5px color-mix(in oklab, var(--flow-node-selected-halo-shadow, #0ea5e9) 30%, transparent),
			0 0 38px var(--flow-node-selected-halo-shadow, rgb(14 165 233 / 0.48));
		content: '';
		pointer-events: none;
		animation: flow-step-selected-halo 1.8s ease-in-out infinite;
	}

	.flow-step-node--drop-target {
		border-color: var(--flow-drop-allowed, #22c55e);
		box-shadow:
			0 0 0 2px var(--flow-drop-allowed-ring, rgb(34 197 94 / 0.72)),
			0 0 30px var(--flow-drop-allowed-soft, rgb(34 197 94 / 0.36)),
			0 14px 28px -20px var(--flow-node-shadow, #020617);
	}

	.flow-step-node--drop-denied {
		border-color: var(--flow-drop-denied, #f97316);
		box-shadow:
			0 0 0 2px var(--flow-drop-denied-ring, rgb(249 115 22 / 0.72)),
			0 0 30px var(--flow-drop-denied-soft, rgb(249 115 22 / 0.32)),
			0 14px 28px -20px var(--flow-node-shadow, #020617);
	}

	.flow-step-node--drop-target .flow-step-node__drop-marker {
		position: absolute;
		z-index: 2;
		border-radius: 999px;
		background: var(--flow-drop-allowed, #22c55e);
		box-shadow: 0 0 0 5px var(--flow-drop-allowed-soft, rgb(34 197 94 / 0.18));
		content: '';
		pointer-events: none;
	}

	.flow-step-node__drop-zone {
		position: absolute;
		z-index: 0;
		top: 0.22rem;
		bottom: 0.22rem;
		display: grid;
		place-items: center;
		border: 1px solid transparent;
		background: transparent;
		opacity: 0.72;
		pointer-events: none;
		transition:
			background 0.12s ease,
			border-color 0.12s ease,
			opacity 0.12s ease;
	}

	.flow-step-node__drop-zone-label {
		opacity: 0;
		color: var(--flow-drop-zone-label-text, #dcfce7);
		font-size: 0.46rem;
		font-weight: 850;
		line-height: 1;
		text-transform: uppercase;
		transition: opacity 0.12s ease;
	}

	.flow-step-node__drop-zone--before {
		left: 0.22rem;
		width: 26%;
		border-right-color: var(--flow-drop-allowed-soft, rgb(34 197 94 / 0.28));
		border-radius: 0.34rem 0 0 0.34rem;
	}

	.flow-step-node__drop-zone--inside {
		left: 26%;
		right: 26%;
		border-right-color: var(--flow-drop-allowed-soft, rgb(34 197 94 / 0.18));
		border-left-color: var(--flow-drop-allowed-soft, rgb(34 197 94 / 0.18));
	}

	.flow-step-node__drop-zone--after {
		right: 0.22rem;
		width: 26%;
		border-left-color: var(--flow-drop-allowed-soft, rgb(34 197 94 / 0.28));
		border-radius: 0 0.34rem 0.34rem 0;
	}

	.flow-step-node__drop-zone--active {
		--flow-drop-zone-active-bg: var(--flow-drop-allowed-soft, rgb(34 197 94 / 0.24));
		--flow-drop-zone-active-border: var(--flow-drop-allowed-ring, rgb(34 197 94 / 0.58));
		--flow-drop-zone-active-text: var(--flow-drop-zone-label-text, #dcfce7);
		border-color: var(--flow-drop-zone-active-border);
		background: var(--flow-drop-zone-active-bg);
		opacity: 1;
	}

	.flow-step-node__drop-zone--active .flow-step-node__drop-zone-label {
		opacity: 0.94;
		color: var(--flow-drop-zone-active-text);
	}

	.flow-step-node__drop-zone--then.flow-step-node__drop-zone--active {
		--flow-drop-zone-active-bg: rgb(34 197 94 / 0.22);
		--flow-drop-zone-active-border: rgb(34 197 94 / 0.72);
		--flow-drop-zone-active-text: #bbf7d0;
	}

	.flow-step-node__drop-zone--else.flow-step-node__drop-zone--active {
		--flow-drop-zone-active-bg: rgb(245 158 11 / 0.24);
		--flow-drop-zone-active-border: rgb(245 158 11 / 0.72);
		--flow-drop-zone-active-text: #fef3c7;
	}

	.flow-step-node__drop-zone--next.flow-step-node__drop-zone--active {
		--flow-drop-zone-active-bg: rgb(14 165 233 / 0.22);
		--flow-drop-zone-active-border: rgb(14 165 233 / 0.72);
		--flow-drop-zone-active-text: #bae6fd;
	}

	.flow-step-node__drop-zone--loop.flow-step-node__drop-zone--active {
		--flow-drop-zone-active-bg: rgb(6 182 212 / 0.22);
		--flow-drop-zone-active-border: rgb(6 182 212 / 0.72);
		--flow-drop-zone-active-text: #cffafe;
	}

	.flow-step-node__drop-zone--done.flow-step-node__drop-zone--active {
		--flow-drop-zone-active-bg: rgb(100 116 139 / 0.22);
		--flow-drop-zone-active-border: rgb(148 163 184 / 0.72);
		--flow-drop-zone-active-text: #e2e8f0;
	}

	.flow-step-node--drop-denied .flow-step-node__drop-zone--before {
		border-right-color: var(--flow-drop-denied-soft, rgb(249 115 22 / 0.3));
	}

	.flow-step-node--drop-denied .flow-step-node__drop-zone--inside {
		border-right-color: var(--flow-drop-denied-soft, rgb(249 115 22 / 0.2));
		border-left-color: var(--flow-drop-denied-soft, rgb(249 115 22 / 0.2));
	}

	.flow-step-node--drop-denied .flow-step-node__drop-zone--after {
		border-left-color: var(--flow-drop-denied-soft, rgb(249 115 22 / 0.3));
	}

	.flow-step-node--drop-denied .flow-step-node__drop-zone--active {
		border-color: var(--flow-drop-denied-ring, rgb(249 115 22 / 0.6));
		background: var(--flow-drop-denied-soft, rgb(249 115 22 / 0.24));
	}

	.flow-step-node--drop-denied .flow-step-node__drop-zone-label {
		color: var(--flow-drop-denied-zone-label-text, #ffedd5);
	}

	.flow-step-node--drop-denied .flow-step-node__drop-marker {
		background: var(--flow-drop-denied, #f97316);
		box-shadow: 0 0 0 5px var(--flow-drop-denied-soft, rgb(249 115 22 / 0.18));
	}

	.flow-step-node--drop-denied.flow-step-node--drop-inside .flow-step-node__drop-marker {
		border-color: var(--flow-drop-denied-ring, rgb(249 115 22 / 0.82));
		background: var(--flow-drop-denied-soft, rgb(249 115 22 / 0.08));
		box-shadow: 0 0 28px var(--flow-drop-denied-soft, rgb(249 115 22 / 0.3));
	}

	.flow-step-node--drop-after .flow-step-node__drop-marker {
		top: -0.42rem;
		right: -0.62rem;
		bottom: -0.42rem;
		width: 4px;
	}

	.flow-step-node--drop-before .flow-step-node__drop-marker {
		top: -0.42rem;
		bottom: -0.42rem;
		left: -0.62rem;
		width: 4px;
	}

	.flow-step-node--drop-before .flow-step-node__drop-marker::before,
	.flow-step-node--drop-after .flow-step-node__drop-marker::after {
		position: absolute;
		top: 50%;
		width: 0.62rem;
		height: 3px;
		transform: translateY(-50%);
		background: inherit;
		content: '';
	}

	.flow-step-node--drop-before .flow-step-node__drop-marker::before {
		right: -0.62rem;
	}

	.flow-step-node--drop-after .flow-step-node__drop-marker::after {
		left: -0.62rem;
	}

	.flow-step-node--drop-inside .flow-step-node__drop-marker {
		z-index: -1;
		inset: -0.42rem;
		width: auto;
		height: auto;
		border: 2px dashed var(--flow-drop-allowed-ring, rgb(34 197 94 / 0.82));
		border-radius: 0.8rem;
		background: var(--flow-drop-allowed-soft, rgb(34 197 94 / 0.08));
		box-shadow: 0 0 28px var(--flow-drop-allowed-soft, rgb(34 197 94 / 0.32));
	}

	.flow-step-node__drop-badge {
		position: absolute;
		z-index: 4;
		top: -1.65rem;
		left: 50%;
		max-width: 12rem;
		transform: translateX(-50%);
		border: 1px solid var(--flow-drop-allowed-ring, rgb(34 197 94 / 0.72));
		border-radius: 999px;
		background: var(--flow-drop-badge-bg, #052e16);
		color: var(--flow-drop-badge-text, #bbf7d0);
		padding: 0.12rem 0.48rem;
		font-size: 0.64rem;
		font-weight: 800;
		line-height: 1;
		overflow: hidden;
		pointer-events: none;
		text-transform: uppercase;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.flow-step-node--drop-denied .flow-step-node__drop-badge {
		border-color: var(--flow-drop-denied-ring, rgb(249 115 22 / 0.72));
		background: var(--flow-drop-denied-badge-bg, #431407);
		color: var(--flow-drop-denied-badge-text, #fed7aa);
	}

	.flow-step-node--drop-before .flow-step-node__drop-badge {
		left: -0.7rem;
		transform: translateX(-100%);
	}

	.flow-step-node--drop-after .flow-step-node__drop-badge {
		right: -0.7rem;
		left: auto;
		transform: translateX(100%);
	}

	.flow-step-node__accent {
		position: absolute;
		inset: 0 auto 0 0;
		width: 0.22rem;
		border-radius: 0.45rem 0 0 0.45rem;
		background: var(--step-color);
	}

	.flow-step-node__accent,
	.flow-step-node__drag-handle,
	.flow-step-node__actions,
	.flow-step-node__icon,
	.flow-step-node__body,
	.flow-step-node__substep-toggle {
		z-index: 1;
	}

	.flow-step-node__drag-handle span {
		width: 0.24rem;
		height: 0.24rem;
		border-radius: 999px;
		background: currentColor;
		box-shadow:
			0 -0.22rem 0 currentColor,
			0 0.22rem 0 currentColor;
		pointer-events: none;
	}

	.flow-step-node__actions {
		position: absolute;
		top: -1.95rem;
		left: 50%;
		display: flex;
		gap: 0.2rem;
		transform: translateX(-50%);
		border: 1px solid var(--flow-node-action-border, var(--color-surface-300-700));
		border-radius: 999px;
		background: color-mix(
			in oklab,
			var(--flow-node-action-bg, var(--color-surface-50-950)) 92%,
			transparent
		);
		padding: 0.16rem;
		box-shadow: 0 12px 28px -20px var(--flow-node-shadow, #020617);
	}

	.flow-step-node__action {
		display: flex;
		width: 1.28rem;
		height: 1.28rem;
		align-items: center;
		justify-content: center;
		gap: 0.16rem;
		border: 0;
		border-radius: 999px;
		background: transparent;
		color: var(--flow-node-action-text, var(--color-surface-700-300));
		padding: 0;
		transition:
			background 0.14s ease,
			color 0.14s ease;
	}

	.flow-step-node__action:hover {
		background: color-mix(in oklab, var(--color-primary-500) 14%, transparent);
		color: var(--color-primary-600-400);
	}

	.flow-step-node__action--danger:hover {
		background: color-mix(in oklab, var(--color-error-500) 14%, transparent);
		color: var(--color-error-600-400);
	}

	.flow-step-node__drag-handle {
		color: var(--flow-node-handle-text, #cbd5e1);
		cursor: grab;
	}

	.flow-step-node__drag-handle:active {
		cursor: grabbing;
	}

	.flow-step-node__icon {
		display: grid;
		position: relative;
		place-items: center;
		width: 2rem;
		height: 2rem;
		border-radius: 0.4rem;
		background: color-mix(in oklab, var(--step-color) 18%, transparent);
		color: var(--step-color);
		overflow: hidden;
	}

	.flow-step-node__body {
		position: relative;
		min-width: 0;
	}

	@keyframes flow-step-selected-halo {
		0%,
		100% {
			opacity: 0.82;
			transform: scale(0.985);
		}

		50% {
			opacity: 1;
			transform: scale(1.02);
		}
	}

	.flow-step-node__name {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		font-size: 0.78rem;
		line-height: 1.15;
		font-weight: 650;
	}

	.flow-step-node__rename-form {
		min-width: 0;
	}

	.flow-step-node__rename {
		width: 100%;
		min-width: 0;
		border: 1px solid var(--flow-node-selected-ring, rgb(56 189 248 / 0.72));
		border-radius: 0.28rem;
		background: var(--flow-node-input-bg, var(--color-surface-50-950));
		color: var(--flow-node-input-text, var(--color-surface-950-50));
		padding: 0.08rem 0.25rem;
		font-size: 0.72rem;
		font-weight: 750;
		outline: none;
	}

	.flow-step-node__rename:focus {
		box-shadow: 0 0 0 2px var(--flow-node-selected-glow, rgb(56 189 248 / 0.28));
	}

	.flow-step-node__type {
		margin-top: 0.15rem;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		font-size: 0.66rem;
		line-height: 1;
		color: var(--flow-node-muted, #cbd5e1);
	}

	:global(.svelte-flow__handle) {
		width: 0.6rem;
		height: 0.6rem;
		border: 2px solid var(--flow-handle-border, #f8fafc);
		background: color-mix(in oklab, var(--step-color) 88%, white);
	}

	:global(.flow-step-node__handle--bottom-in) {
		background: var(--flow-node-bottom-input, #111827);
	}

	:global(.flow-step-node__handle--bottom-out) {
		background: var(--flow-node-bottom-output, #f8fafc);
	}

	:global(.flow-step-node__handle--branch-then) {
		border-color: color-mix(in oklab, #22c55e 42%, var(--flow-handle-border, #f8fafc));
		background: #22c55e;
	}

	:global(.flow-step-node__handle--branch-else) {
		border-color: color-mix(in oklab, #f59e0b 42%, var(--flow-handle-border, #f8fafc));
		background: #f59e0b;
	}

	:global(.flow-step-node__handle--branch-next) {
		border-color: color-mix(in oklab, #0ea5e9 42%, var(--flow-handle-border, #f8fafc));
		background: #0ea5e9;
	}

	:global(.flow-step-node__handle--branch-loop) {
		border-color: color-mix(in oklab, #06b6d4 42%, var(--flow-handle-border, #f8fafc));
		background: #06b6d4;
	}

	:global(.flow-step-node__handle--branch-done) {
		border-color: color-mix(in oklab, #64748b 42%, var(--flow-handle-border, #f8fafc));
		background: #64748b;
	}

	.flow-step-node__port-label {
		position: absolute;
		z-index: 3;
		border: 1px solid var(--flow-node-port-label-border, rgb(148 163 184 / 0.5));
		border-radius: 0.18rem;
		background: var(--flow-node-port-label-bg, rgb(15 23 42 / 0.92));
		color: var(--flow-node-port-label-text, #e2e8f0);
		padding: 0.04rem 0.24rem;
		font-size: 0.56rem;
		font-weight: 800;
		line-height: 1.1;
		pointer-events: none;
		text-transform: uppercase;
		white-space: nowrap;
	}

	.flow-step-node__port-label--then {
		border-color: rgb(34 197 94 / 0.6);
		background: color-mix(
			in oklab,
			#22c55e 26%,
			var(--flow-node-port-label-bg, rgb(15 23 42 / 0.92))
		);
		color: var(--flow-node-port-label-text, #ecfdf5);
	}

	.flow-step-node__port-label--else {
		border-color: rgb(245 158 11 / 0.62);
		background: color-mix(
			in oklab,
			#f59e0b 28%,
			var(--flow-node-port-label-bg, rgb(15 23 42 / 0.92))
		);
		color: var(--flow-node-port-label-text, #fffbeb);
	}

	.flow-step-node__port-label--next {
		border-color: rgb(14 165 233 / 0.62);
		background: color-mix(
			in oklab,
			#0ea5e9 28%,
			var(--flow-node-port-label-bg, rgb(15 23 42 / 0.92))
		);
		color: var(--flow-node-port-label-text, #f0f9ff);
	}

	.flow-step-node__port-label--loop {
		border-color: rgb(6 182 212 / 0.62);
		background: color-mix(
			in oklab,
			#06b6d4 28%,
			var(--flow-node-port-label-bg, rgb(15 23 42 / 0.92))
		);
		color: var(--flow-node-port-label-text, #ecfeff);
	}

	.flow-step-node__port-label--done {
		border-color: rgb(100 116 139 / 0.68);
		background: color-mix(
			in oklab,
			#64748b 30%,
			var(--flow-node-port-label-bg, rgb(15 23 42 / 0.92))
		);
		color: var(--flow-node-port-label-text, #f8fafc);
	}

	.flow-step-node__port-label--out {
		right: -0.42rem;
		transform: translate(100%, -50%);
	}

	.flow-step-node__port-label--bottom-out {
		bottom: -1.45rem;
		transform: translateX(-50%);
	}

	.flow-step-node__port-label--bottom-in {
		bottom: -1.45rem;
		transform: translateX(-50%);
	}

	.flow-step-node__substep-toggle {
		position: absolute;
		left: 50%;
		bottom: -1.25rem;
		z-index: 2;
		display: grid;
		place-items: center;
		width: 1rem;
		height: 1rem;
		transform: translateX(-50%);
		border: 1px solid var(--flow-node-toggle-border, rgb(203 213 225 / 0.8));
		border-radius: 999px;
		background: var(--flow-node-toggle-bg, #1f2937);
		color: var(--flow-node-toggle-text, #f8fafc);
		font-size: 0.68rem;
		font-weight: 800;
		line-height: 1;
		box-shadow: 0 0 10px var(--flow-node-shadow, rgb(15 23 42 / 0.35));
	}

	.flow-step-node__substep-toggle:hover {
		border-color: var(--flow-node-toggle-hover-border, #f8fafc);
		background: var(--flow-node-toggle-hover-bg, #374151);
	}

	.flow-step-node__substep-toggle--collapsed {
		border-color: var(--flow-node-toggle-collapsed-border, #fecaca);
		background: var(--flow-node-toggle-collapsed-bg, #991b1b);
	}
</style>
