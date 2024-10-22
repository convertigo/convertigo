<script>
	import { store } from './store.svelte';
	const { name, inc } = $derived(store);
</script>

<div onclick={inc}>ok {name}</div>
