<script>
	import { onDestroy, onMount, tick } from 'svelte';
	import Bezels from '$lib/dashboard/Bezels';
	import { assets } from '$app/paths';
	import { getFrontendUrl } from '$lib/utils/service';
	import { page } from '$app/stores';
	import RightPart from '../../../../admin/RightPart.svelte';
	import { fade, fly } from 'svelte/transition';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import Last from '../Last.svelte';
	import { goto } from '$app/navigation';
	import { Spring } from 'svelte/motion';

	let orientation = $derived($page.params.model.split('_')[1] == 'h' ? 'horizontal' : 'vertical');
	let selectedDevice = $derived(Bezels[$page.params.model.split('_')[0]]);
	let selectedIndex = $derived(selectedDevice.index);
	let selectedIndexLast = $state(-1);
	let projectUrl = $derived(getFrontendUrl($page.params.project));
	let scale = 1;
	let angle = Spring.of(() => (orientation == 'horizontal' ? 1 : 0));

	$effect(() => {
		Last.model = selectedDevice.title;
		Last.orientation = orientation;
		angle.target = orientation == 'horizontal' ? 1 : 0;
	});

	$effect(() => {
		selectedIndex;
		tick().then(() => {
			selectedIndexLast = selectedIndex;
		});
	});

	// function calculateScale() {
	// 	if (typeof window !== 'undefined') {
	// 		const viewportWidth = window.innerWidth * 0.9;
	// 		const viewportHeight = window.innerHeight * 0.9;

	// 		const deviceWidth = selectedDevice.screen[orientation].width;
	// 		const deviceHeight = selectedDevice.screen[orientation].height;

	// 		scale = Math.min(viewportWidth / deviceWidth, viewportHeight / deviceHeight, 1);
	// 	}
	// }

	// onMount(() => {
	// 	calculateScale();
	// 	window.addEventListener('resize', calculateScale);
	// 	return () => window.removeEventListener('resize', calculateScale);
	// });

	let iframe;
	let bezel;

	$effect(() => {
		for (const [key, value] of Object.entries(selectedDevice.bezel)) {
			bezel.style[key] = `${value}px`;
		}
		for (const [key, value] of Object.entries(selectedDevice.iframe)) {
			iframe.style[key] = `${value}px`;
		}
		iframe.style.transform = `rotate(${angle.target * 90}deg)`;
		if (angle.target == 1) {
			const w = iframe.style.height;
			iframe.style.height = iframe.style.width;
			iframe.style.width = w;
			iframe.style.transformOrigin = '0 0';
			iframe.style.transform += ' translate(0, -100%)';
		}
	});

	RightPart.snippet = rightPart;
	onDestroy(() => {
		RightPart.snippet = undefined;
	});
</script>

{#snippet rightPart()}
	<nav
		class="bg-surface-200-800 border-r-[0.5px] border-color p-low h-full max-md:layout-grid-[100px]"
	>
		<PropertyType
			type="segment"
			bind:value={() => orientation,
			(v) => {
				goto(`../${selectedDevice.id}_${v.substring(0, 1)}/`);
			}}
			item={['horizontal', 'vertical']}
			orientation="vertical"
		/>
		{#each Object.values(Bezels) as { id, title }, i}
			<a
				href="../{id}_{orientation.substring(0, 1)}/"
				class="relative layout-x-p-low !gap py-2 hover:bg-surface-200-800 rounded min-w-36"
			>
				{#if i == selectedIndex}
					<span
						in:fly={{ y: (selectedIndexLast - selectedIndex) * 50 }}
						out:fade
						class="absolute inset-0 preset-filled-primary-500 opacity-40 rounded"
					></span>
				{/if}
				<span class="text-[13px] z-10 font-{i == selectedIndex ? 'medium' : 'light'}">{title}</span>
			</a>
		{/each}
	</nav>
{/snippet}

<div class="h-full layout-x justify-center">
	<div
		class="relative"
		style="transform: rotate({angle.current * -90}deg); width: {selectedDevice.bezel
			.width}px; height: {selectedDevice.bezel.height}px;"
	>
		<iframe
			bind:this={iframe}
			src={projectUrl}
			title={`${selectedDevice.title} Preview`}
			class="absolute overflow-hidden"
		></iframe>
		<img
			bind:this={bezel}
			src="{assets}/bezels/{selectedDevice.id}.png"
			alt={`${selectedDevice.title} Bezel`}
			class="absolute pointer-events-none"
		/>
	</div>
</div>

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
