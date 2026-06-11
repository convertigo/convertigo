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
		typeof data?.laneY === 'number' ? data.laneY : Math.max(sourceY, targetY) + 140
	);
	let joinX = $derived(typeof data?.joinX === 'number' ? data.joinX : targetX - 72);
	let busStartX = $derived(typeof data?.busStartX === 'number' ? data.busStartX : joinX);
	let connectToResponse = $derived(data?.connectToResponse !== false);
	let path = $derived(
		returnPath(sourceX, sourceY, targetX, targetY, laneY, joinX, busStartX, connectToResponse)
	);

	/**
	 * @param {number} sourceX
	 * @param {number} sourceY
	 * @param {number} targetX
	 * @param {number} targetY
	 * @param {number} laneY
	 * @param {number} joinX
	 * @param {number} busStartX
	 * @param {boolean} connectToResponse
	 * @returns {string}
	 */
	function returnPath(
		sourceX,
		sourceY,
		targetX,
		targetY,
		laneY,
		joinX,
		busStartX,
		connectToResponse
	) {
		const sourceLeadX = sourceX + 32;
		const targetLeadX = targetX - 32;
		const path = [
			`M ${sourceX} ${sourceY}`,
			`L ${sourceLeadX} ${sourceY}`,
			`L ${sourceLeadX} ${laneY}`
		];
		if (connectToResponse) {
			path.push(
				`M ${Math.min(busStartX, sourceLeadX)} ${laneY}`,
				`L ${joinX} ${laneY}`,
				`L ${targetLeadX} ${laneY}`,
				`L ${targetLeadX} ${targetY}`,
				`L ${targetX} ${targetY}`
			);
		}
		return path.join(' ');
	}
</script>

<BaseEdge {id} {path} {markerStart} {markerEnd} {interactionWidth} {style} />
