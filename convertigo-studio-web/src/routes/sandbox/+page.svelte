<script>
	import { Switch } from '@skeletonlabs/skeleton-svelte';
	import MyComp from './MyComp.svelte';
	import DynamicModal from '../../lib/common/components/ModalDynamic.svelte';
	// import val from './mytest.svelte';
	import Monitor from '$lib/admin/Monitor.svelte';

	// let { name, age, reset, rules } = $derived(val);
	// const n = $derived({
	// 	r: rules.length ? rules[0] : 'ko'
	// });
	let { memoryUsed } = $derived(Monitor);
	const m = $derived({
		truc: memoryUsed.length ? `${memoryUsed[memoryUsed.length - 1]} MB` : null
	});
	let form;
	$effect(() => {
		console.log(JSON.stringify([...new FormData(form).entries()]));
	});
	let comp;
	async function callComp() {
		comp.hello();
		const res = await modal.open();
		console.log('closed', res);
	}
	let modal;
	let modal2;
</script>

<p>{m.truc}</p>
<!-- <p>{n.r}</p>
{#if rules.length}
<p>{JSON.stringify(rules)}</p>
{/if} -->
<div class="min-h-screen flex flex-col">
	<h1 class="text-red-500 dark:text-green-500 dark:hover:text-blue-500 hover:text-green-500">
		sandbox !
	</h1>
	<MyComp bind:this={comp} />
	<MyComp />

	<!-- <button class="basic-button" onclick={() => val.age++}>{name}: {age}</button>
	<button class="basic-button" onclick={() => reset()}>reset</button> -->
	<div class="layout-x-p">
		<div class="w-12 h-6 bg-red-500"></div>

		<div class="w-36 h-4 bg-green-500"></div>
	</div>
	<form bind:this={form}>
		<input type="hidden" name="hidden" value="hidden" />
		<Switch name="switch" value="true" checked>
			<span>Switch</span>
		</Switch>
	</form>
	<button onclick={callComp}>click</button>
	<DynamicModal bind:this={modal}>HELLO</DynamicModal>
	<button onclick={async () => console.log(await modal2.open())}>click</button>
	<DynamicModal bind:this={modal2}>
		{#snippet children({ setResult, close })}
			HELLO 2
			<input
				class="input-text"
				type="text"
				oninput={(/** @type {any} */ e) => setResult(e?.target?.value)}
			/>
			<button onclick={close}>close</button>
		{/snippet}
	</DynamicModal>
</div>
