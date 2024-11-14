<script>
	import Card from '$lib/admin/components/Card.svelte';
	import ModalDynamic from './ModalDynamic.svelte';

	let { title = '', message = '', class: cls = 'max-w-md' } = $props();

	export async function open(props) {
		({ title = title, message = message, class: cls = cls } = props ?? {});
		return await modal.open();
	}

	let modal;
</script>

<ModalDynamic bind:this={modal}>
	<Card {title} class={cls}>
		{#if message}
			<span>{message}</span>
		{/if}
		<div class="w-full layout-x justify-end">
			<button onclick={() => modal.close(true)} class="basic-button">Yes</button>
			<button onclick={() => modal.close(false)} class="cancel-button">No</button>
		</div>
	</Card>
</ModalDynamic>
