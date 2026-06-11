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

	let laneY = $derived(typeof data?.laneY === 'number' ? data.laneY : targetY);
	let geometry = $derived(loopBodyGeometry(sourceX, sourceY, targetX, targetY, laneY));

	/**
	 * @param {number} sourceX
	 * @param {number} sourceY
	 * @param {number} targetX
	 * @param {number} targetY
	 * @param {number} laneY
	 * @returns {{ path: string, labelX: number, labelY: number }}
	 */
	function loopBodyGeometry(sourceX, sourceY, targetX, targetY, laneY) {
		const sourceLeadX = sourceX + 36;
		const targetLeadX = targetX - 28;
		const labelX = sourceLeadX + 8;
		const labelY = sourceY + (laneY - sourceY) * 0.48;
		return {
			path: [
				`M ${sourceX} ${sourceY}`,
				`L ${sourceLeadX} ${sourceY}`,
				`L ${sourceLeadX} ${laneY}`,
				`L ${targetLeadX} ${laneY}`,
				`L ${targetLeadX} ${targetY}`,
				`L ${targetX} ${targetY}`
			].join(' '),
			labelX,
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
