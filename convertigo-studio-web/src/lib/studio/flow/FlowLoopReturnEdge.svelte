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
		markerStart,
		markerEnd,
		interactionWidth,
		data
	} = $props();

	let laneY = $derived(
		typeof data?.laneY === 'number' ? data.laneY : Math.max(sourceY, targetY) + 96
	);
	let joinX = $derived(typeof data?.joinX === 'number' ? data.joinX : targetX - 54);
	let sourceLeadOffset = $derived(
		typeof data?.sourceLeadOffset === 'number' ? data.sourceLeadOffset : 44
	);
	let connectToLoop = $derived(data?.connectToLoop === true);
	let geometry = $derived(
		loopReturnGeometry(
			sourceX,
			sourceY,
			targetX,
			targetY,
			laneY,
			joinX,
			sourceLeadOffset,
			connectToLoop
		)
	);

	/**
	 * @param {number} sourceX
	 * @param {number} sourceY
	 * @param {number} targetX
	 * @param {number} targetY
	 * @param {number} laneY
	 * @param {number} joinX
	 * @param {number} sourceLeadOffset
	 * @param {boolean} connectToLoop
	 * @returns {{ path: string }}
	 */
	function loopReturnGeometry(
		sourceX,
		sourceY,
		targetX,
		targetY,
		laneY,
		joinX,
		sourceLeadOffset,
		connectToLoop
	) {
		const sourceLeadX = sourceX + sourceLeadOffset;
		const path = [
			`M ${sourceX} ${sourceY}`,
			`L ${sourceLeadX} ${sourceY}`,
			`L ${sourceLeadX} ${laneY}`
		];
		if (connectToLoop) {
			path.push(`L ${joinX} ${laneY}`, `L ${joinX} ${targetY}`, `L ${targetX} ${targetY}`);
		}
		return { path: path.join(' ') };
	}
</script>

<BaseEdge {id} path={geometry.path} {markerStart} {markerEnd} {interactionWidth} {style} />
