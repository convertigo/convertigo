<script>
	import { store } from './store.svelte';

	const { name, inc } = $derived(store);
	export function hello() {
		console.log('hello');
	}
</script>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<div onclick={inc} class="custom-[red]">ok {name}</div>
