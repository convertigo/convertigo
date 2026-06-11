<script>
	import { BaseEdge } from '@xyflow/svelte';

	/** @type {import('@xyflow/svelte').EdgeProps} */
	let {
		id,
		sourceX,
		sourceY,
		targetX,
		targetY,
		style,
		label,
		labelStyle,
		markerStart,
		markerEnd,
		interactionWidth,
		data
	} = $props();

	let sourceLeadOffset = $derived(
		typeof data?.sourceLeadOffset === 'number' ? data.sourceLeadOffset : 42
	);
	let geometry = $derived(branchGeometry(sourceX, sourceY, targetX, targetY, sourceLeadOffset));

	/**
	 * @param {number} sourceX
	 * @param {number} sourceY
	 * @param {number} targetX
	 * @param {number} targetY
	 * @param {number} sourceLeadOffset
	 * @returns {{ path: string, labelX: number, labelY: number }}
	 */
	function branchGeometry(sourceX, sourceY, targetX, targetY, sourceLeadOffset) {
		const sourceLeadX = sourceX + sourceLeadOffset;
		const deltaY = targetY - sourceY;
		const labelDirection = Math.abs(deltaY) > 28 ? Math.sign(deltaY) : 0;
		const labelY = sourceY + labelDirection * 22;
		return {
			path: [
				`M ${sourceX} ${sourceY}`,
				`L ${sourceLeadX} ${sourceY}`,
				`L ${sourceLeadX} ${targetY}`,
				`L ${targetX} ${targetY}`
			].join(' '),
			labelX: sourceLeadX + 8,
			labelY
		};
	}
</script>

<BaseEdge
	{id}
	path={geometry.path}
	labelX={geometry.labelX}
	labelY={geometry.labelY}
	{label}
	{labelStyle}
	{markerStart}
	{markerEnd}
	{interactionWidth}
	{style}
/>
