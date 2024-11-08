<script>
	import { browser } from '$app/environment';
	import Ico from '$lib/utils/Ico.svelte';
	import { Switch } from '@skeletonlabs/skeleton-svelte';
	import { onMount } from 'svelte';
	import Light from '../Light.svelte';
	// let mode = $state(false);

	// $effect(() => {
	//     document.documentElement.classList.toggle('dark', !mode);
	//     if (browser) {
	//         localStorage.theme = mode ? 'light' : 'dark';
	//     }
	// });

	// onMount(() => {
	//     console.log('localStorage.theme', localStorage.theme);
	//     console.log('mode', mode);
	//     mode = localStorage.theme === 'dark' || (!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches);
	//     console.log('on', (!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches));
	// });
</script>

<Switch
	name="mode"
	controlActive="bg-surface-300"
	controlInactive="bg-surface-700"
	classes="!gap-0"
	bind:checked={Light.light}
>
	{#snippet inactiveChild()}<Ico icon="mdi:moon-and-stars" />{/snippet}
	{#snippet activeChild()}<Ico icon="mdi:weather-sunny" />{/snippet}
</Switch>
