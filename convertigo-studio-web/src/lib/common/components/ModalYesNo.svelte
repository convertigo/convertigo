<script>
	import Card from '$lib/admin/components/Card.svelte';
	import ModalDynamic from './ModalDynamic.svelte';

	let { title = '', message = '', class: cls = 'max-w-md' } = $props();

	export async function open(props) {
		props ??= {};
		[title, message, cls] = [props.title ?? title, props.message ?? message, props.class ?? cls];
		return await modal.open(props);
	}

	let modal;
</script>

<ModalDynamic bind:this={modal}>
	<Card {title} class={cls}>
		{#if message}
			<span>{message}</span>
		{/if}
		<div class="layout-x w-full justify-end">
			<button onclick={() => modal.close(true)} class="button-success">Yes</button>
			<button onclick={() => modal.close(false)} class="button-error">No</button>
		</div>
	</Card>
</ModalDynamic>
