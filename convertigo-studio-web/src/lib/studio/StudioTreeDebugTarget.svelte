<script>
	import { debugCommand, debugSession } from './debugSession.svelte.js';

	/** @type {{ target: {url: string, line: number} }} */
	let { target } = $props();
	let selection = $state({ sessionId: '', enabled: false });
	let enabled = $derived(
		selection.sessionId === debugSession.state?.sessionId && selection.enabled
	);
	let busy = $state(false);
	let error = $state('');
	async function toggle(event) {
		event.stopPropagation();
		busy = true;
		error = '';
		try {
			const result = await debugCommand('breakpoint', {
				url: target.url,
				line: String(target.line),
				set: String(!enabled)
			});
			if (result?.accepted)
				selection = { sessionId: debugSession.state?.sessionId ?? '', enabled: !enabled };
			else error = 'Breakpoint was refused.';
		} catch {
			error = 'Unable to update this breakpoint.';
		} finally {
			busy = false;
		}
	}
</script>

{#if debugSession.state?.attached}
	<button
		type="button"
		aria-label={enabled ? 'Remove breakpoint on this item' : 'Set breakpoint on this item'}
		aria-pressed={enabled}
		disabled={busy}
		title={error || (enabled ? 'Remove breakpoint' : 'Set breakpoint')}
		onclick={toggle}
		class={['tree-debug-target', { enabled }]}>●</button
	>
{/if}

<style>
	.tree-debug-target {
		width: 1rem;
		color: #999;
		flex-shrink: 0;
	}
	.enabled {
		color: #d32f2f;
	}
</style>
