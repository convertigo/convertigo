<script>
	import { tick } from 'svelte';
	import { createEventDispatcher } from 'svelte';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import ModalWithButtonOptions from '$lib/components/modals/ModalWithButtonOptions.svelte';

	const dispatch = createEventDispatcher();
	const modalStore = getModalStore();

	/** @type {{nodeData: any}} */
	let { nodeData } = $props();

	let label = getLabel();
	let name = $state(getName());

	let editing = $state(false);
	let initial = getName();
	let input = $state();

	function getName() {
		let s = '' + nodeData.id;
		s = s.substring(s.lastIndexOf('.') + 1);
		s = s.indexOf(':') != -1 ? s.substring(s.indexOf(':') + 1) : s;
		return s;
	}

	function getLabel() {
		return nodeData.label;
	}

	export function edit() {
		toggle();
	}

	async function toggle() {
		editing = !editing;
		if (editing) {
			await tick();
			input.focus();
		}
	}

	function handleInput(e) {
		name = e.target.value;
	}

	function handleKeyUp(e) {
		if (e.key === 'Enter') {
			input.blur();

			let newName = input.value;
			if (newName !== '' && newName !== initial) {
				const cmwb = {
					ref: ModalWithButtonOptions,
					props: {
						buttons: [
							{ label: 'Replace in all loaded projects', value: 'UPDATE_ALL' },
							{ label: 'Replace in current project', value: 'UPDATE_LOCAL' },
							{ label: 'Do not replace anywhere', value: 'UPDATE_NONE' }
						]
					}
				};

				modalStore.trigger({
					type: 'component',
					component: cmwb,
					title: 'Update object references',
					body: 'Please choose between below options',
					response: (update) => {
						if (update) {
							dispatch('rename', {
								id: nodeData.id,
								name: newName,
								update: update
							});
						} else {
							name = initial;
						}
					}
				});
			} else {
				name = initial;
			}
		} else if (e.key === 'Escape') {
			name = initial;
			input.blur();
		}
	}

	function handleBlur(e) {
		toggle();
	}
</script>

{#if editing}
	<input
		type="text"
		class="input"
		autocomplete="off"
		aria-autocomplete="none"
		bind:this={input}
		value={name}
		oninput={handleInput}
		onkeyup={handleKeyUp}
		onblur={handleBlur}
	/>
{:else}
	<span>{label}</span>
{/if}
