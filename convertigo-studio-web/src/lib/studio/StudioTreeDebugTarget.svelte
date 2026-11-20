<script>
	import { debugCommand, debugSession } from './debugSession.svelte.js';

	/** @type {{ target: {url: string, line: number, identity?: object} }} */
	let { target } = $props();
	let enabled = $derived(
		debugSession.state?.breakpoints?.[target.url]?.includes(target.line) ?? false
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
				kind: 'participant',
				identity: target.identity ? JSON.stringify(target.identity) : '',
				line: String(target.line),
				set: String(!enabled)
			});
			if (!result?.accepted) error = 'Breakpoint was refused.';
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
		class={['tree-debug-target', { enabled }]}
	>
		●
	</button>
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
