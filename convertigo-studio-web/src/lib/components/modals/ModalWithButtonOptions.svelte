<script>
	import { getModalStore } from '@skeletonlabs/skeleton';

	/** Exposes parent props to this component. */
	export let parent;

	export let buttons = [];

	const modalStore = getModalStore();

	function onButtonClick(e) {
		let value = false;
		try {
			value = e.srcElement.attributes['value'].nodeValue;
			if ($modalStore[0].response) {
				$modalStore[0].response(value);
			}
		} catch (e) {
			console.log('CustomModalWithButtons error', e);
		}
		modalStore.close();
	}
</script>

{#if $modalStore[0]}
	<div class="card p-4 w-modal shadow-xl space-y-2">
		<header>{$modalStore[0].title ?? '(title missing)'}</header>
		<article>{$modalStore[0].body ?? '(body missing)'}</article>
		{#each buttons as button}
			<div class="w-full">
				<button
					class="btn w-full {parent.buttonPositive}"
					value={button.value}
					on:click={onButtonClick}>{button.label}</button
				>
			</div>
		{/each}
		<footer class="modal-footer {parent.regionFooter}">
			<button class="btn {parent.buttonNeutral}" on:click={parent.onClose}
				>{parent.buttonTextCancel}</button
			>
		</footer>
	</div>
{/if}
