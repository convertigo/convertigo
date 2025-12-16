<script>
	import Topbar from '$lib/admin/components/Topbar.svelte';

	/** @type {{children?: import('svelte').Snippet}} */
	let { children } = $props();
</script>

<div class="layout-y-stretch-none min-h-screen">
	<Topbar variant="studio" title="Convertigo Low Code Studio" />

	<main class="relative layout-y-stretch-none min-h-0 w-full min-w-0 grow py px">
		{@render children?.()}
	</main>
</div>
