<script>
	import ActionBar from '$lib/admin/components/ActionBar.svelte';
	import Button from '$lib/admin/components/Button.svelte';
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
		<ActionBar>
			<Button
				onclick={() => modal.close(true)}
				class="button-success w-fit!"
				icon="mdi:check"
				label="Yes"
			/>
			<Button
				onclick={() => modal.close(false)}
				class="button-error w-fit!"
				icon="mdi:close-circle-outline"
				label="No"
			/>
		</ActionBar>
	</Card>
</ModalDynamic>
