<script>
	import { goto } from '$app/navigation';
	import { asset } from '$app/paths';
	import { page } from '$app/state';
	import MaxRectangle from '$lib/admin/components/MaxRectangle.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import Bezels from '$lib/dashboard/Bezels';
	import { getFrontendUrl } from '$lib/utils/service';
	import { onDestroy, tick } from 'svelte';
	import { Spring } from 'svelte/motion';
	import { blur, fade, fly, slide } from 'svelte/transition';
	import RightPart from '../../../../admin/RightPart.svelte';
	import Last from '../Last.svelte';

	let orientation = $derived(page.params.model?.split('_')[1] == 'h' ? 'horizontal' : 'vertical');
	let selectedDevice = $derived(
		Bezels[page.params.model?.split('_')[0]] ?? Object.values(Bezels)[0]
	);
	let selectedIndex = $derived(selectedDevice.index);
	let selectedIndexLast = $state(-1);
	let projectUrl = $derived(
		(page.params.project ?? '_') == '_' ? '#' : getFrontendUrl(page.params.project)
	);
	let angle = Spring.of(() => (orientation == 'horizontal' ? 1 : 0));

	$effect(() => {
		Last.model = selectedDevice.id;
		Last.orientation = orientation.substring(0, 1);
		angle.target = orientation == 'horizontal' ? 1 : 0;
	});

	$effect(() => {
		selectedIndex;
		tick().then(() => {
			selectedIndexLast = selectedIndex;
		});
	});

	let iframe = $state();
	let bezel = $state();

	$effect(() => {
		if (!bezel || !iframe) return;
		const scale = bezel.style.scale;
		for (const elt of [iframe, bezel]) {
			elt.attributeStyleMap.clear();
			for (let [key, value] of Object.entries(
				elt == iframe ? selectedDevice.iframe : selectedDevice.bezel
			)) {
				if (Number.isFinite(value)) {
					value = `${value}px`;
				}
				if (elt == bezel && (key == 'height' || key == 'width')) {
					elt.style[`min-${key}`] = value;
				}
				elt.style[key] = value;
			}
		}
		bezel.style.scale = scale;

		if (selectedDevice.type == 'phone') {
			bezel.style.transform = `rotate(${angle.current * -90}deg)`;
			iframe.style.transform = `rotate(${angle.target * 90}deg)`;
			if (angle.target == 1) {
				const w = iframe.style.height;
				iframe.style.height = iframe.style.width;
				iframe.style.width = w;
				iframe.style.transformOrigin = '0 0';
				iframe.style.transform += ' translate(0, -100%)';
			}
		}
	});

	let clientHeight = $state(0);
	let clientWidth = $state(0);

	$effect(() => {
		if (!bezel || !iframe) return;
		if (selectedDevice.id != 'none') {
			const { height, width } = selectedDevice.bezel;
			const scaleHeight = clientHeight / (angle.target == 1 ? width : height);
			const scaleWidth = clientWidth / (angle.target == 1 ? height : width);
			bezel.style.scale = Math.min(scaleHeight, scaleWidth);
		} else {
			bezel.style.scale = 1;
		}
	});

	RightPart.snippet = rightPart;
	onDestroy(() => {
		RightPart.snippet = undefined;
	});
</script>

{#snippet rightPart()}
	<nav
		class="h-full border-r-[0.5px] border-color bg-surface-200-800 p-low max-md:layout-grid-[100px]"
	>
		{#if selectedDevice.type == 'phone'}
			<div transition:slide>
				<PropertyType
					type="segment"
					bind:value={
						() => orientation,
						(v) => {
							goto(`../${selectedDevice.id}_${v.substring(0, 1)}/`);
						}
					}
					item={['horizontal', 'vertical']}
					orientation="vertical"
				/>
			</div>
		{/if}
		{#each Object.values(Bezels) as { id, title, type, iframe: { height, width } }, i}
			{@const href = type == 'phone' ? `../${id}_${orientation.substring(0, 1)}/` : `../${id}/`}
			<a
				{href}
				class="relative layout-x-p-low min-w-36 gap! rounded-sm py-2 hover:bg-surface-200-800"
			>
				{#if i == selectedIndex}
					<span
						in:fly={{ y: (selectedIndexLast - selectedIndex) * 50 }}
						out:fade
						class="absolute inset-0 rounded-sm preset-filled-primary-500 opacity-40"
					></span>
				{/if}
				<span class="z-10 text-[13px] font-{i == selectedIndex ? 'medium' : 'light'}"
					>{title}<br /><small>{width} x {height}</small></span
				>
			</a>
		{/each}
		<a
			href={projectUrl}
			target="_blank"
			class="relative layout-x-p-low min-w-36 gap! rounded-sm py-2 hover:bg-surface-200-800"
		>
			<span class="z-10 text-[13px] font-light">New Tab</span>
		</a>
	</nav>
{/snippet}
<MaxRectangle bind:clientHeight bind:clientWidth>
	<!-- <div>
		<div class="grow">bar<button>clic</button></div>
		<div class="h-full w-full"> -->
	<div
		class="layout-x justify-center"
		style="max-height: {clientHeight}px; max-width: {clientWidth}px; height: {clientHeight}px; width: {clientWidth}px;"
	>
		<div bind:this={bezel} class="relative">
			<iframe
				bind:this={iframe}
				src={projectUrl}
				title={`${selectedDevice.title} Preview`}
				class="absolute overflow-hidden"
				class:hidden={!iframe}
			></iframe>
			{#if selectedDevice.id != 'none'}
				{#key selectedDevice.id}
					<img
						src={asset(`/bezels/${selectedDevice.id}.png`)}
						alt={`${selectedDevice.title} Bezel`}
						class="pointer-events-none absolute min-h-full min-w-full"
						transition:blur
					/>
				{/key}
			{/if}
		</div>
	</div>
	<!-- </div>
	</div> -->
</MaxRectangle>

<style>
	.device-iframe {
		position: relative;
		border: none;
		z-index: 1;
		border-radius: 60px;
		-ms-overflow-style: none; /* For Internet Explorer and Edge */
		scrollbar-width: none; /* For Firefox */
	}

	/* For Webkit browsers like Chrome, Safari, and Opera */
	.device-iframe::-webkit-scrollbar {
		display: none;
	}
</style>
